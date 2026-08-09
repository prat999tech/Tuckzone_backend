package com.school.canteen.payment;

import com.school.canteen.entity.Payment;
import com.school.canteen.enums.PaymentTxnStatus;
import com.school.canteen.repository.PaymentRepository;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Safety net for checkouts nobody ever finished — the customer closed the app mid-payment,
 * or the gateway sheet was killed by the OS.
 *
 * This is the backstop, not the primary path: a customer who actively dismisses the payment
 * sheet is cancelled immediately by {@code PaymentService.cancelPayment}, so their money
 * comes straight back rather than waiting out the window here.
 */
@Component
public class PaymentExpirySweeper {

    private static final Logger log = LoggerFactory.getLogger(PaymentExpirySweeper.class);
    private static final Duration EXPIRY = Duration.ofMinutes(15);

    private final PaymentRepository paymentRepository;
    private final PaymentCompensation compensation;

    public PaymentExpirySweeper(PaymentRepository paymentRepository, PaymentCompensation compensation) {
        this.paymentRepository = paymentRepository;
        this.compensation = compensation;
    }

    @Scheduled(fixedDelay = 120_000L)
    @Transactional
    public void expireStalePendingPayments() {
        Instant cutoff = Instant.now().minus(EXPIRY);
        // Every use case, not just CHECKOUT. Abandoned wallet recharges hold no customer
        // money, but leaving them PENDING forever meant a webhook arriving days later could
        // still credit a wallet for a payment the customer had long since walked away from.
        var stale = paymentRepository.findByStatusAndCreatedAtBefore(PaymentTxnStatus.PENDING, cutoff);
        for (Payment payment : stale) {
            compensation.reversePending(payment, "EXPIRED");
        }
        if (!stale.isEmpty()) {
            log.info("Expired {} abandoned payment(s)", stale.size());
        }
    }
}
