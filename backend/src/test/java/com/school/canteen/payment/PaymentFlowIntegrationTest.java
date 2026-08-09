package com.school.canteen.payment;

import com.school.canteen.exception.ResourceNotFoundException;
import com.school.canteen.exception.BadRequestException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import com.school.canteen.IntegrationTestBase;
import com.school.canteen.TestDataFactory;
import com.school.canteen.dto.UserSummary;
import com.school.canteen.dto.menu.MenuItemResponse;
import com.school.canteen.dto.order.OrderLineRequest;
import com.school.canteen.dto.order.OrderResponse;
import com.school.canteen.dto.order.PlaceOrderRequest;
import com.school.canteen.dto.payment.PlatformFeeSettingsUpdateRequest;
import com.school.canteen.dto.wallet.MockTopupCompleteRequest;
import com.school.canteen.dto.wallet.TopupInitResponse;
import com.school.canteen.dto.wallet.TopupRequest;
import com.school.canteen.dto.wallet.WalletResponse;
import com.school.canteen.enums.OrderStatus;
import com.school.canteen.enums.PaymentMode;
import com.school.canteen.enums.PaymentUseCase;
import com.school.canteen.enums.PlatformFeeType;
import com.school.canteen.service.AuthService;
import com.school.canteen.service.DailyMenuService;
import com.school.canteen.service.MenuItemService;
import com.school.canteen.service.OrderService;
import com.school.canteen.service.PaymentService;
import com.school.canteen.service.PaymentSettingsService;
import com.school.canteen.service.WalletService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end coverage of the new payment architecture: platform fee application, the
 * wallet+gateway split checkout path, and proportional refunds. Complements
 * OrderConcurrencyIntegrationTest (which exercises the original wallet-only fast path and
 * proves it is unaffected) rather than duplicating it.
 */
class PaymentFlowIntegrationTest extends IntegrationTestBase {

    @Autowired private AuthService authService;
    @Autowired private WalletService walletService;
    @Autowired private MenuItemService menuItemService;
    @Autowired private DailyMenuService dailyMenuService;
    @Autowired private OrderService orderService;
    @Autowired private PaymentService paymentService;
    @Autowired private PaymentSettingsService paymentSettingsService;

    /** Every test that enables a fee must leave it disabled again — platform_fee_settings
     *  is shared, seeded state, not per-test data. */
    @AfterEach
    void resetPlatformFeeSettings() {
        for (PaymentUseCase useCase : List.of(PaymentUseCase.WALLET_RECHARGE, PaymentUseCase.CHECKOUT)) {
            paymentSettingsService.update(useCase,
                    new PlatformFeeSettingsUpdateRequest(false, PlatformFeeType.PERCENTAGE, BigDecimal.ZERO, null, null));
        }
    }

    private LocalDate menuDate() {
        return LocalDate.now().plusDays(1);
    }

    private UUID registerTeacher() {
        return authService.registerTeacher(TestDataFactory.teacher()).id();
    }

    private UUID publishItem(BigDecimal price, int quantity) {
        MenuItemResponse item = menuItemService.create(TestDataFactory.menuItem(price, BigDecimal.valueOf(10)));
        dailyMenuService.addItem(TestDataFactory.dailyMenu(menuDate(), item.id(), quantity));
        return item.id();
    }

    private void enableFee(PaymentUseCase useCase, String percentage) {
        paymentSettingsService.update(useCase, new PlatformFeeSettingsUpdateRequest(
                true, PlatformFeeType.PERCENTAGE, new BigDecimal(percentage), null, null));
    }

    @Test
    @DisplayName("wallet recharge with a platform fee credits the wallet the subtotal, never the fee")
    void walletRechargeAppliesPlatformFeeButNeverCreditsIt() {
        enableFee(PaymentUseCase.WALLET_RECHARGE, "2"); // matches the ₹1000/2%/₹20 spec example
        UUID userId = registerTeacher();

        TopupInitResponse topup = walletService.initiateTopup(userId, new TopupRequest(BigDecimal.valueOf(1000)));
        assertThat(topup.platformFee()).isEqualByComparingTo("20.00");
        assertThat(topup.grandTotal()).isEqualByComparingTo("1020.00"); // what the gateway actually charges

        WalletResponse wallet = walletService.mockCompleteTopup(userId,
                new MockTopupCompleteRequest(topup.gatewayOrderId()));

        // Wallet gets exactly the recharge amount — the fee never touches it.
        assertThat(wallet.balance()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("checkout with insufficient wallet balance splits payment across wallet and gateway")
    void checkoutSplitsAcrossWalletAndGateway() {
        UUID userId = registerTeacher();
        // Fund the wallet with only part of what the order will cost.
        TopupInitResponse topup = walletService.initiateTopup(userId, new TopupRequest(BigDecimal.valueOf(100)));
        walletService.mockCompleteTopup(userId, new MockTopupCompleteRequest(topup.gatewayOrderId()));

        UUID itemId = publishItem(BigDecimal.valueOf(50), 10); // 3 units = 150, more than the 100 in wallet
        PlaceOrderRequest request = new PlaceOrderRequest(null, menuDate(), null, null, "Staff Room",
                List.of(new OrderLineRequest(itemId, 3)), "split-" + UUID.randomUUID(), PaymentMode.WALLET_PLUS_GATEWAY);

        OrderResponse order = orderService.placeOrder(userId, request);

        // Not settled yet — a gateway leg is still outstanding, and the wallet portion was
        // already taken synchronously.
        assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.payment()).isNotNull();
        assertThat(order.payment().providerOrderId()).isNotNull();
        assertThat(order.payment().pricing().walletUsed()).isEqualByComparingTo("100.00");
        assertThat(order.payment().pricing().gatewayAmount()).isEqualByComparingTo("50.00");
        assertThat(walletService.getWallet(userId).balance()).isEqualByComparingTo("0.00");

        // Complete the gateway leg (dev/mock path) — this is what a real checkout widget's
        // callback, verified through /payments/{id}/verify, would also settle.
        var status = paymentService.mockComplete(userId, order.payment().paymentId());
        assertThat(status.status()).isEqualTo("PAID");

        // Wallet balance is unaffected by the gateway leg settling — it was already spent.
        assertThat(walletService.getWallet(userId).balance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("checkout fully covered by wallet under WALLET_PLUS_GATEWAY settles immediately, no gateway leg")
    void checkoutFullyCoveredByWalletSettlesImmediately() {
        UUID userId = registerTeacher();
        TopupInitResponse topup = walletService.initiateTopup(userId, new TopupRequest(BigDecimal.valueOf(500)));
        walletService.mockCompleteTopup(userId, new MockTopupCompleteRequest(topup.gatewayOrderId()));

        UUID itemId = publishItem(BigDecimal.valueOf(40), 10);
        PlaceOrderRequest request = new PlaceOrderRequest(null, menuDate(), null, null, "Staff Room",
                List.of(new OrderLineRequest(itemId, 2)), "full-wallet-" + UUID.randomUUID(),
                PaymentMode.WALLET_PLUS_GATEWAY);

        OrderResponse order = orderService.placeOrder(userId, request);

        // Wallet alone covered the whole 80 — nothing left for a gateway, so it's PAID
        // immediately with no payment object for the client to act on.
        assertThat(order.paymentStatus().name()).isEqualTo("PAID");
        assertThat(order.payment()).isNull();
        assertThat(walletService.getWallet(userId).balance()).isEqualByComparingTo("420.00");
    }

    // A placed order (wallet-funded or otherwise) can no longer be cancelled at all — see
    // OrderConcurrencyIntegrationTest.placedOrderHasNoCancellationOrRejectionPath, which
    // covers that directly. cancelMyOrder no longer exists on OrderService.
    //
    // PaymentService.cancelPayment (voiding a still-pending gateway leg before an order is
    // ever confirmed — explicitly preserved, see PaymentCancellationIntegrationTest) has its
    // own dedicated coverage there too: cancellingPendingCheckoutVoidsPaymentAndOrder,
    // cancellingTwiceIsIdempotent, cannotCancelAPaidPayment, cannotCancelAnotherUsersPayment.
    // Duplicating those scenarios here (as an earlier, independently-evolved copy of this
    // same feature once did) would just be the same coverage twice.
}
