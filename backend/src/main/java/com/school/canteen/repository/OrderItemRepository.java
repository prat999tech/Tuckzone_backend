package com.school.canteen.repository;

import com.school.canteen.entity.OrderItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    /** True if this catalog item has ever appeared in an order — the line the permanent
     *  delete guard checks before touching a menu_items row. */
    boolean existsByMenuItem_Id(UUID menuItemId);
}
