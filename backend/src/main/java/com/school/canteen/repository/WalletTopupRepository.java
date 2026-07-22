package com.school.canteen.repository;

import com.school.canteen.entity.WalletTopup;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletTopupRepository extends JpaRepository<WalletTopup, UUID> {

    Optional<WalletTopup> findByGatewayOrderId(String gatewayOrderId);

    /** Locked read for the verify path, so two concurrent verifications of the same top-up
     *  can't both credit the wallet. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from WalletTopup t where t.gatewayOrderId = :orderId")
    Optional<WalletTopup> lockByGatewayOrderId(@Param("orderId") String orderId);
}
