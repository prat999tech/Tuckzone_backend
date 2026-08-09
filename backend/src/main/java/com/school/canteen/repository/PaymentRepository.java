package com.school.canteen.repository;

import com.school.canteen.entity.Payment;
import com.school.canteen.enums.PaymentTxnStatus;
import com.school.canteen.enums.PaymentUseCase;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByProviderOrderId(String providerOrderId);

    /**
     * A scalar projection, deliberately NOT {@code findByProviderOrderId(...).map(Payment::getId)}:
     * loading the full entity here would put it in the caller's persistence context, and a
     * later {@link #lockByProviderOrderId} call in the same transaction would then hand back
     * that same pre-lock managed instance instead of genuinely fresh post-lock state —
     * silently defeating the lock. Returning a bare UUID never touches the identity map.
     */
    @Query("select p.id from Payment p where p.providerOrderId = :providerOrderId")
    Optional<UUID> findIdByProviderOrderId(@Param("providerOrderId") String providerOrderId);

    /** Same scalar-projection reasoning as {@link #findIdByProviderOrderId} — backs
     *  PaymentServiceImpl.mockComplete, which must not pre-load the entity before the
     *  locked verify path it delegates to. */
    @Query("select p.providerOrderId from Payment p where p.id = :id and p.user.id = :userId")
    Optional<String> findProviderOrderIdByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    /** Locked read for the verify/webhook path, so two concurrent confirmations of the
     *  same payment can't both settle it — the same guarantee WalletTopup used to give
     *  top-ups, generalised to every use case. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.providerOrderId = :providerOrderId")
    Optional<Payment> lockByProviderOrderId(@Param("providerOrderId") String providerOrderId);

    Optional<Payment> findByUser_IdAndIdempotencyKey(UUID userId, String idempotencyKey);

    /** Locked read for the user-initiated cancel path (PaymentServiceImpl.cancelPayment) —
     *  same PESSIMISTIC_WRITE guarantee as {@link #lockByProviderOrderId}, keyed by our own
     *  id since that is all the client knows when dismissing the checkout widget. Locking
     *  the same row this way still mutually excludes a concurrent verify/webhook, which
     *  lock it via providerOrderId — Postgres locks the row, not the predicate used to find it. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> lockById(@Param("id") UUID id);

    /** Backs PaymentExpirySweeper: gateway legs a customer never came back to finish. */
    List<Payment> findByUseCaseAndStatusAndCreatedAtBefore(
            PaymentUseCase useCase, PaymentTxnStatus status, Instant cutoff);

    // ---- reporting ----
    // A payment counts as revenue once it was ever genuinely PAID, refunded or not — the
    // platform fee itself is never refunded (see Payment#getPlatformFee), so a later
    // refund must not remove it from these totals.

    @Query("""
            select coalesce(sum(p.platformFee), 0) from Payment p
             where p.status in (
                 com.school.canteen.enums.PaymentTxnStatus.PAID,
                 com.school.canteen.enums.PaymentTxnStatus.PARTIALLY_REFUNDED,
                 com.school.canteen.enums.PaymentTxnStatus.REFUNDED)
               and p.createdAt between :from and :to
            """)
    java.math.BigDecimal sumPlatformFeeBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select p.provider, coalesce(sum(p.platformFee), 0), coalesce(sum(p.grandTotal), 0)
              from Payment p
             where p.status in (
                 com.school.canteen.enums.PaymentTxnStatus.PAID,
                 com.school.canteen.enums.PaymentTxnStatus.PARTIALLY_REFUNDED,
                 com.school.canteen.enums.PaymentTxnStatus.REFUNDED)
               and p.createdAt between :from and :to
             group by p.provider
            """)
    List<Object[]> revenueByProviderBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select p.useCase, coalesce(sum(p.platformFee), 0), coalesce(sum(p.grandTotal), 0)
              from Payment p
             where p.status in (
                 com.school.canteen.enums.PaymentTxnStatus.PAID,
                 com.school.canteen.enums.PaymentTxnStatus.PARTIALLY_REFUNDED,
                 com.school.canteen.enums.PaymentTxnStatus.REFUNDED)
               and p.createdAt between :from and :to
             group by p.useCase
            """)
    List<Object[]> revenueByUseCaseBetween(@Param("from") Instant from, @Param("to") Instant to);

    /** Raw ledger for CSV export — every attempt in range, not just settled ones. */
    List<Payment> findByCreatedAtBetweenOrderByCreatedAtAsc(Instant from, Instant to);
}
