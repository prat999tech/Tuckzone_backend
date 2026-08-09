package com.school.canteen.payment;

import com.school.canteen.entity.Order;
import com.school.canteen.entity.OrderItem;
import com.school.canteen.entity.Payment;
import com.school.canteen.enums.NotificationEvent;
import com.school.canteen.enums.OrderStatus;
import com.school.canteen.enums.PaymentStatus;
import com.school.canteen.enums.PaymentTxnStatus;
import com.school.canteen.enums.PaymentUseCase;
import com.school.canteen.mapper.OrderMapper;
import com.school.canteen.repository.DailyMenuItemRepository;
import com.school.canteen.repository.OrderRepository;
import com.school.canteen.repository.PaymentRepository;
import com.school.canteen.service.NotificationService;
import com.school.canteen.service.WalletService;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cleans up CHECKOUT payments a customer started (opening the gateway widget) but never
 * finished. Without this, two things would leak forever: the wallet portion that
 * {@code PaymentServiceImpl.createPayment} eagerly debits before the gateway leg is even
 * attempted (see its javadoc), and the stock reserved for the order.
 *
 * Mirrors the house style for background jobs — see OtpServiceImpl.purgeExpiredCodes and
 * notification/OutboxSweeper: {@code @Scheduled} + {@code @Transactional} on a plain bean,
 * enabled by CanteenApplication's {@code @EnableScheduling}.
 */
@Component
public class PaymentExpirySweeper {

    private static final Logger log = LoggerFactory.getLogger(PaymentExpirySweeper.class);
    private static final Duration EXPIRY = Duration.ofMinutes(15);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final DailyMenuItemRepository dailyMenuItemRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;

    public PaymentExpirySweeper(PaymentRepository paymentRepository, OrderRepository orderRepository,
                                DailyMenuItemRepository dailyMenuItemRepository, WalletService walletService,
                                NotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.dailyMenuItemRepository = dailyMenuItemRepository;
        this.walletService = walletService;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 120_000L)
    @Transactional
    public void expireStalePendingCheckouts() {
        Instant cutoff = Instant.now().minus(EXPIRY);
        var stale = paymentRepository.findByUseCaseAndStatusAndCreatedAtBefore(
                PaymentUseCase.CHECKOUT, PaymentTxnStatus.PENDING, cutoff);
        for (Payment payment : stale) {
            expireOne(payment);
        }
        if (!stale.isEmpty()) {
            log.info("Expired {} abandoned checkout payment(s)", stale.size());
        }
    }

    private void expireOne(Payment payment) {
        payment.setStatus(PaymentTxnStatus.FAILED);
        paymentRepository.save(payment);

        // Reverse the eager wallet debit — the customer never actually paid, so they never
        // lose that money to an abandoned checkout.
        if (payment.getWalletUsed().signum() > 0) {
            walletService.credit(payment.getUser().getId(), payment.getWalletUsed(), "PAYMENT_EXPIRED",
                    payment.getId().toString(), "Reversing an abandoned checkout's wallet portion");
        }

        if (!"ORDER".equals(payment.getReferenceType()) || payment.getReferenceId() == null) {
            return;
        }
        UUID orderId;
        try {
            orderId = UUID.fromString(payment.getReferenceId());
        } catch (IllegalArgumentException ex) {
            log.warn("Expired payment {} has a malformed order reference '{}'",
                    payment.getId(), payment.getReferenceId());
            return;
        }
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getPaymentStatus() != PaymentStatus.PENDING) {
            return; // already settled by a late verify/webhook that raced this sweep, or gone
        }
        for (OrderItem item : order.getItems()) {
            if (item.getMenuItem() != null) {
                dailyMenuItemRepository.restore(order.getMenuDate(), item.getMenuItem().getId(), item.getQuantity());
            }
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // No "order placed" notification was ever sent for this order (see
        // OrderServiceImpl.placeWithGatewayOrSplit — it's deliberately withheld until
        // payment settles), so silently cancelling here would leave the customer with no
        // signal at all about what happened to their checkout. Tell them explicitly.
        String orderNumber = OrderMapper.formatOrderNumber(order.getOrderNumber());
        notificationService.notifyUser(order.getPlacedBy(), NotificationEvent.ORDER_CANCELLED,
                "Order cancelled",
                "Order " + orderNumber + " was cancelled — payment was not received in time.",
                Map.of("orderId", order.getId().toString(), "orderNumber", orderNumber));
    }
}
