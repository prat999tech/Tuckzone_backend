package com.school.canteen.repository;

import com.school.canteen.entity.DeliverySlot;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliverySlotRepository extends JpaRepository<DeliverySlot, UUID> {

    List<DeliverySlot> findByActiveTrueOrderByOrderCutoffTimeAsc();
}
