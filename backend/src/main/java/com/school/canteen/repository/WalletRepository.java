package com.school.canteen.repository;

import com.school.canteen.entity.Wallet;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
}
