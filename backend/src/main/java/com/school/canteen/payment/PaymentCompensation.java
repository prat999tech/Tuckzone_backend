package com.school.canteen.payment;

import com.school.canteen.entity.Order;
import com.school.canteen.entity.OrderItem;
import com.school.canteen.entity.Payment;
import com.school.canteen.enums.NotificationEvent;
import com.school.canteen.enums.OrderStatus;
import com.school.canteen.enums.PaymentStatus;
import com.school.canteen.enums.PaymentTxnStatus;
import com.school.canteen.repository.DailyMenuItemRepository;
import com.school.canteen.repository.OrderRepository;
import com.school.canteen.repository.PaymentRepository;
import com.school.canteen.mapper.OrderMapper;
import com.school.canteen.service.NotificationService;
import com.school.canteen.service.WalletService;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Undoes a checkout that was started but never paid for.
 *
 * The wallet portion of a payment is charged up front, at create time, so anything that
 * ends a PENDING payment without settling it has to hand that money back, release the
 * stock it reserved, and cancel the order it created. Getting one of those three steps
 * wrong is what produces the classic "money gone, no order" complaint.
 *
 * This lives in one place because there are two callers that must behave identically:
 * the customer explicitly abandoning the gateway sheet, and {@link PaymentExpirySweeper}
 * cleaning up someone who simply closed the app. When that logic was duplicated, only the
 * sweeper path existed at all — so a cancelled payment held the customer's money for the
 * full 15-minute expiry window before anything gave it back.
 */
@Component
public class PaymentCompensation {

    private static final Logger log = LoggerFactory.getLogger(PaymentCompensation.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final DailyMenuItemRepository dailyMenuItemRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;

    public PaymentCompensation(PaymentRepository paymentRepository,
                               OrderRepository orderRepository,
                               DailyMenuItemRepository dailyMenuItemRepository,
                               WalletService walletService,
                               NotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.dailyMenuItemRepository = dailyMenuItemRepository;
        this.walletService = walletService;
        this.notificationService = notificationService;
    }

    /**
     * Marks a PENDING payment FAILED and reverses everything it provisionally took.
     *
     * <p>Caller must already hold a row lock on the payment (see
     * {@code PaymentRepository.lockById}) and must have checked it is still PENDING —
     * this method assumes that and does not re-check, so that a caller which needs to
     * report "already settled" to a user can do so before any state changes.
     *
     * @param reason short marker recorded on the wallet credit, e.g. CANCELLED or EXPIRED
     */
    public void reversePending(Payment payment, String reason) {
        payment.setStatus(PaymentTxnStatus.FAILED);
        paymentRepository.save(payment);

        // Give back the wallet money first. If anything below fails, the customer is out of
        // pocket for nothing, which is the worst of the possible partial outcomes.
        if (payment.getWalletUsed().signum() > 0) {
            walletService.credit(payment.getUser().getId(), payment.getWalletUsed(),
                    "PAYMENT_" + reason, payment.getId().toString(),
                    "Reversing the wallet portion of an unpaid checkout");
        }

        releaseOrder(payment, reason);
    }

    /** Cancels the reserved order and puts its stock back, if this payment made one. */
    private void releaseOrder(Payment payment, String reason) {
        if (!"ORDER".equals(payment.getReferenceType()) || payment.getReferenceId() == null) {
            return; // wallet recharge, or a payment that never got as far as an order
        }
        UUID orderId;
        try {
            orderId = UUID.fromString(payment.getReferenceId());
        } catch (IllegalArgumentException ex) {
            log.warn("Payment {} has a malformed order reference '{}'",
                    payment.getId(), payment.getReferenceId());
            return;
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        // A late verify or webhook may have settled this order in the moment before we got
        // here. Only an order still awaiting payment is ours to cancel.
        if (order == null || order.getPaymentStatus() != PaymentStatus.PENDING) {
            return;
        }

        for (OrderItem item : order.getItems()) {
            if (item.getMenuItem() != null) {
                dailyMenuItemRepository.restore(order.getMenuDate(), item.getMenuItem().getId(), item.getQuantity());
            }
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // A gateway checkout deliberately withholds the "order placed" notification until
        // payment settles, so cancelling silently would leave the customer with no signal
        // at all about where their order went. Skipped when they cancelled it themselves —
        // they already know, and "payment was not received in time" would be wrong.
        if (!"CANCELLED".equals(reason)) {
            String orderNumber = OrderMapper.formatOrderNumber(order.getOrderNumber());
            notificationService.notifyUser(order.getPlacedBy(), NotificationEvent.ORDER_CANCELLED,
                    "Order cancelled",
                    "Order " + orderNumber + " was cancelled — payment was not received in time.",
                    Map.of("orderId", order.getId().toString(), "orderNumber", orderNumber));
        }
    }
}
