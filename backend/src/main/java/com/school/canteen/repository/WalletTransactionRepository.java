package com.school.canteen.repository;

import com.school.canteen.entity.WalletTransaction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    List<WalletTransaction> findByWallet_IdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);
}
