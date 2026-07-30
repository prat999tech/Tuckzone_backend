package com.school.canteen.repository;

import com.school.canteen.entity.OrderingWindow;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderingWindowRepository extends JpaRepository<OrderingWindow, UUID> {

    Optional<OrderingWindow> findByMenuDateAndSlot_Id(LocalDate menuDate, UUID slotId);

    List<OrderingWindow> findByMenuDate(LocalDate menuDate);
}
