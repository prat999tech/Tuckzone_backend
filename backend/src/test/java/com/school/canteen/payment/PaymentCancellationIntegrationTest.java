package com.school.canteen.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.school.canteen.IntegrationTestBase;
import com.school.canteen.TestDataFactory;
import com.school.canteen.dto.UserSummary;
import com.school.canteen.dto.menu.MenuItemResponse;
import com.school.canteen.dto.order.OrderLineRequest;
import com.school.canteen.dto.order.OrderResponse;
import com.school.canteen.dto.order.PlaceOrderRequest;
import com.school.canteen.dto.payment.RefundRequest;
import com.school.canteen.dto.payment.VerifyPaymentRequest;
import com.school.canteen.dto.wallet.MockTopupCompleteRequest;
import com.school.canteen.dto.wallet.TopupInitResponse;
import com.school.canteen.dto.wallet.TopupRequest;
import com.school.canteen.entity.Payment;
import com.school.canteen.entity.Refund;
import com.school.canteen.enums.OrderStatus;
import com.school.canteen.enums.PaymentMode;
import com.school.canteen.enums.PaymentStatus;
import com.school.canteen.enums.PaymentTxnStatus;
import com.school.canteen.exception.BadRequestException;
import com.school.canteen.exception.ResourceNotFoundException;
import com.school.canteen.repository.DailyMenuItemRepository;
import com.school.canteen.repository.PaymentRepository;
import com.school.canteen.repository.RefundRepository;
import com.school.canteen.service.AuthService;
import com.school.canteen.service.DailyMenuService;
import com.school.canteen.service.MenuItemService;
import com.school.canteen.service.OrderService;
import com.school.canteen.service.PaymentService;
import com.school.canteen.service.WalletService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The reported bug, reproduced and pinned down: a customer who cancels payment before it
 * completes must never end up with what looks like a placed order. Covers the fix in two
 * halves — {@code PaymentService.cancelPayment} (the client-triggered path, wired to the
 * gateway widget's dismiss/cancel) and the {@code Order.payment} linkage fix that makes a
 * later refund/rejection of a genuinely split-paid order proportional instead of
 * over-crediting the wallet with money that was never taken from it.
 */
class PaymentCancellationIntegrationTest extends IntegrationTestBase {

    @Autowired private AuthService authService;
    @Autowired private WalletService walletService;
    @Autowired private MenuItemService menuItemService;
    @Autowired private DailyMenuService dailyMenuService;
    @Autowired private OrderService orderService;
    @Autowired private PaymentService paymentService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private RefundRepository refundRepository;
    @Autowired private DailyMenuItemRepository dailyMenuItemRepository;

    private LocalDate menuDate() {
        return LocalDate.now().plusDays(1);
    }

    private UUID teacherWithWallet(BigDecimal balance) {
        UserSummary user = authService.registerTeacher(TestDataFactory.teacher());
        if (balance.signum() > 0) {
            TopupInitResponse topup = walletService.initiateTopup(user.id(), new TopupRequest(balance));
            walletService.mockCompleteTopup(user.id(), new MockTopupCompleteRequest(topup.gatewayOrderId()));
        }
        return user.id();
    }

    private UUID publishItem(BigDecimal price, int quantity) {
        MenuItemResponse item = menuItemService.create(TestDataFactory.menuItem(price, BigDecimal.valueOf(10)));
        dailyMenuService.addItem(TestDataFactory.dailyMenu(menuDate(), item.id(), quantity));
        return item.id();
    }

    private int remainingStock(UUID itemId) {
        return dailyMenuItemRepository.findByMenuDateAndMenuItem_Id(menuDate(), itemId)
                .orElseThrow().getRemainingQuantity();
    }

    @Test
    @DisplayName("cancelling the gateway leg voids the payment and the order immediately, restores wallet and stock")
    void cancellingPendingCheckoutVoidsPaymentAndOrder() {
        UUID userId = teacherWithWallet(BigDecimal.valueOf(20));
        UUID itemId = publishItem(BigDecimal.valueOf(30), 5); // total 90, wallet only covers 20 of it

        OrderResponse order = orderService.placeOrder(userId, new PlaceOrderRequest(null, menuDate(), null,
                "Staff Room", List.of(new OrderLineRequest(itemId, 3)), "cancel-" + UUID.randomUUID(),
                PaymentMode.WALLET_PLUS_GATEWAY));

        // Sanity: this order genuinely has an outstanding gateway leg — the bug this guards
        // against only exists when payment hasn't completed yet.
        assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(order.payment()).isNotNull();
        assertThat(walletService.getWallet(userId).balance()).isEqualByComparingTo("0.00");
        assertThat(remainingStock(itemId)).isEqualTo(2);

        // The moment the customer dismisses the checkout widget, the frontend calls this —
        // not 15 minutes later via PaymentExpirySweeper.
        var status = paymentService.cancelPayment(userId, order.payment().paymentId());
        assertThat(status.status()).isEqualTo("FAILED");

        OrderResponse afterCancel = orderService.getMyOrder(userId, order.id());
        assertThat(afterCancel.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(walletService.getWallet(userId).balance()).isEqualByComparingTo("20.00");
        assertThat(remainingStock(itemId)).isEqualTo(5);

        Payment payment = paymentRepository.findById(order.payment().paymentId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentTxnStatus.FAILED);
    }

    @Test
    @DisplayName("cancelling an already-cancelled payment is a safe no-op, not a double refund")
    void cancellingTwiceIsIdempotent() {
        UUID userId = teacherWithWallet(BigDecimal.valueOf(20));
        UUID itemId = publishItem(BigDecimal.valueOf(30), 5);
        OrderResponse order = orderService.placeOrder(userId, new PlaceOrderRequest(null, menuDate(), null,
                "Staff Room", List.of(new OrderLineRequest(itemId, 3)), "cancel-twice-" + UUID.randomUUID(),
                PaymentMode.WALLET_PLUS_GATEWAY));

        paymentService.cancelPayment(userId, order.payment().paymentId());
        assertThat(walletService.getWallet(userId).balance()).isEqualByComparingTo("20.00");

        // A retried dismiss event, a slow double-tap, whatever — must not credit again.
        var secondCancel = paymentService.cancelPayment(userId, order.payment().paymentId());
        assertThat(secondCancel.status()).isEqualTo("FAILED");
        assertThat(walletService.getWallet(userId).balance()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("a cancelled payment can never later be verified into existence")
    void cancelledPaymentCannotBeVerifiedAfterTheFact() {
        UUID userId = teacherWithWallet(BigDecimal.valueOf(20));
        UUID itemId = publishItem(BigDecimal.valueOf(30), 5);
        OrderResponse order = orderService.placeOrder(userId, new PlaceOrderRequest(null, menuDate(), null,
                "Staff Room", List.of(new OrderLineRequest(itemId, 3)), "cancel-then-verify-" + UUID.randomUUID(),
                PaymentMode.WALLET_PLUS_GATEWAY));
        UUID paymentId = order.payment().paymentId();
        String providerOrderId = order.payment().providerOrderId();

        paymentService.cancelPayment(userId, paymentId);

        assertThatThrownBy(() -> paymentService.verifyPayment(userId, paymentId,
                new VerifyPaymentRequest(providerOrderId, "pay_fake", "sig_fake")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("cancelling someone else's payment is rejected, not just ignored")
    void cannotCancelAnotherUsersPayment() {
        UUID owner = teacherWithWallet(BigDecimal.valueOf(20));
        UUID itemId = publishItem(BigDecimal.valueOf(30), 5);
        OrderResponse order = orderService.placeOrder(owner, new PlaceOrderRequest(null, menuDate(), null,
                "Staff Room", List.of(new OrderLineRequest(itemId, 3)), "not-yours-" + UUID.randomUUID(),
                PaymentMode.WALLET_PLUS_GATEWAY));

        UUID intruder = teacherWithWallet(BigDecimal.ZERO);
        assertThatThrownBy(() -> paymentService.cancelPayment(intruder, order.payment().paymentId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("an already-paid payment cannot be cancelled")
    void cannotCancelAPaidPayment() {
        UUID userId = teacherWithWallet(BigDecimal.valueOf(20));
        UUID itemId = publishItem(BigDecimal.valueOf(30), 5);
        OrderResponse order = orderService.placeOrder(userId, new PlaceOrderRequest(null, menuDate(), null,
                "Staff Room", List.of(new OrderLineRequest(itemId, 3)), "already-paid-" + UUID.randomUUID(),
                PaymentMode.WALLET_PLUS_GATEWAY));
        paymentService.mockComplete(userId, order.payment().paymentId());

        assertThatThrownBy(() -> paymentService.cancelPayment(userId, order.payment().paymentId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("refunding a paid split order (via the standalone admin refund endpoint — order-level "
            + "rejection no longer exists) splits the wallet share and the gateway share separately, "
            + "not the full total to the wallet")
    void refundingSplitPaidOrderRefundsProportionally() {
        UUID userId = teacherWithWallet(BigDecimal.valueOf(100));
        UUID itemId = publishItem(BigDecimal.valueOf(30), 10); // total 150: wallet 100 + gateway 50
        OrderResponse order = orderService.placeOrder(userId, new PlaceOrderRequest(null, menuDate(), null,
                "Staff Room", List.of(new OrderLineRequest(itemId, 5)), "split-refund-" + UUID.randomUUID(),
                PaymentMode.WALLET_PLUS_GATEWAY));
        assertThat(order.payment().pricing().walletUsed()).isEqualByComparingTo("100.00");
        assertThat(order.payment().pricing().gatewayAmount()).isEqualByComparingTo("50.00");

        paymentService.mockComplete(userId, order.payment().paymentId());
        assertThat(walletService.getWallet(userId).balance()).isEqualByComparingTo("0.00");

        // Order-level rejection is gone entirely (see OrderConcurrencyIntegrationTest) — the
        // Order<->Payment link this test protects is still exercised through the standalone
        // admin refund endpoint (PaymentController#refund), which remains for genuine
        // adjustments even though no order-status action can reach it automatically anymore.
        paymentService.refundPayment(order.payment().paymentId(), new RefundRequest(null, "test refund"));

        // Without the Order<->Payment link, this used to credit the wallet the FULL 150
        // (order.totalAmount) while never refunding the 50 actually charged through the
        // gateway — the platform ate that 50, and the customer got 50 of free wallet money
        // on top of what they paid. The correct outcome refunds exactly what each side paid.
        assertThat(walletService.getWallet(userId).balance()).isEqualByComparingTo("100.00");

        Payment payment = paymentRepository.findById(order.payment().paymentId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentTxnStatus.REFUNDED);

        List<Refund> refunds = refundRepository.findByPayment_IdOrderByCreatedAtDesc(payment.getId());
        assertThat(refunds).hasSize(1);
        assertThat(refunds.getFirst().getWalletAmount()).isEqualByComparingTo("100.00");
        assertThat(refunds.getFirst().getGatewayAmount()).isEqualByComparingTo("50.00");
    }
}
