package com.school.canteen.repository;

import com.school.canteen.entity.Wallet;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByUser_Id(UUID userId);

    /**
     * Loads the wallet with a PESSIMISTIC_WRITE lock (SELECT ... FOR UPDATE). Any other
     * transaction trying to lock the same wallet row blocks until this one commits, which
     * is what serializes concurrent credits/debits on a single wallet.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.id = :walletId")
    Optional<Wallet> lockById(@Param("walletId") UUID walletId);

    /**
     * Creates the wallet only if the user does not already have one, atomically.
     *
     * Uses ON CONFLICT DO NOTHING rather than catching a constraint violation: in
     * PostgreSQL a failed statement aborts the entire transaction, so the previous
     * "insert, catch duplicate, re-select" recovery could never actually run — every
     * follow-up query failed with "current transaction is aborted". Letting the database
     * absorb the conflict keeps the transaction healthy.
     *
     * @return 1 if a wallet was inserted, 0 if one already existed
     */
    // No clearAutomatically: this runs mid-transaction during order placement, and
    // detaching the in-flight Order entity would drop its payment-status update.
    @Modifying
    @Query(value = """
            insert into wallets (id, user_id, balance, created_at, updated_at)
            values (:id, :userId, 0, now(), now())
            on conflict (user_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("userId") UUID userId);
}
