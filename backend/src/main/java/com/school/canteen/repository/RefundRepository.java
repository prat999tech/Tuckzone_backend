package com.school.canteen.repository;

import com.school.canteen.entity.Refund;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

    List<Refund> findByPayment_IdOrderByCreatedAtDesc(UUID paymentId);
}
