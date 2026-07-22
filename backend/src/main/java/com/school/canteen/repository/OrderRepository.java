package com.school.canteen.repository;

import com.school.canteen.entity.Order;
import com.school.canteen.enums.OrderStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    /** Next human-friendly order number from the DB sequence. */
    @Query(value = "select nextval('order_number_seq')", nativeQuery = true)
    long nextOrderNumber();

    Optional<Order> findByPlacedBy_IdAndIdempotencyKey(UUID userId, String idempotencyKey);

    Optional<Order> findByIdAndPlacedBy_Id(UUID id, UUID userId);

    List<Order> findByPlacedBy_IdOrderByCreatedAtDesc(UUID userId);

    List<Order> findByMenuDateOrderByCreatedAtAsc(LocalDate menuDate);

    List<Order> findByMenuDateAndStatusOrderByCreatedAtAsc(LocalDate menuDate, OrderStatus status);
}
