package com.school.canteen.repository;

import com.school.canteen.entity.DailyMenuItem;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyMenuItemRepository extends JpaRepository<DailyMenuItem, UUID> {

    /** All items listed for a date (admin view — includes unavailable). */
    List<DailyMenuItem> findByMenuDate(LocalDate menuDate);

    /** Items a customer may order on a date: available, and whose catalog item is active. */
    List<DailyMenuItem> findByMenuDateAndAvailableTrueAndMenuItem_ActiveTrue(LocalDate menuDate);

    boolean existsByMenuDateAndMenuItem_Id(LocalDate menuDate, UUID menuItemId);

    Optional<DailyMenuItem> findByMenuDateAndMenuItem_Id(LocalDate menuDate, UUID menuItemId);

    /** Scheduling entries are disposable (unlike order history), so a permanent catalog
     *  delete clears every date this item was ever scheduled for first. */
    void deleteByMenuItem_Id(UUID menuItemId);

    /**
     * Race-safe stock decrement in a single statement: the row is only updated if it is
     * available and still has enough stock. Returns the number of rows changed (1 = the
     * decrement succeeded, 0 = out of stock / unavailable). Because the check and the write
     * are one atomic UPDATE, two concurrent orders for the last unit can't both succeed.
     */
    @Modifying
    @Query("""
            update DailyMenuItem d
               set d.remainingQuantity = d.remainingQuantity - :qty
             where d.menuDate = :menuDate
               and d.menuItem.id = :menuItemId
               and d.available = true
               and d.remainingQuantity >= :qty
            """)
    int tryDecrement(@Param("menuDate") LocalDate menuDate,
                     @Param("menuItemId") UUID menuItemId,
                     @Param("qty") int qty);

    /**
     * Puts stock back on cancellation/rejection, clamped to the day's total.
     *
     * The clamp matters because an admin can lower total_quantity (or delete and re-add
     * the item) after orders were placed. Adding the full quantity back blindly could push
     * remaining above total and violate chk_remaining_within_total, turning a customer's
     * cancellation into a 500 and leaving them unrefunded.
     */
    // NOTE: deliberately no clearAutomatically — callers keep working with the Order
    // entity after this runs (setting status/payment state), and clearing the persistence
    // context would detach it and silently discard those updates.
    @Modifying
    @Query(value = """
            update daily_menu_items
               set remaining_quantity = least(remaining_quantity + :qty, total_quantity),
                   updated_at = now()
             where menu_date = :menuDate
               and menu_item_id = :menuItemId
            """, nativeQuery = true)
    int restore(@Param("menuDate") LocalDate menuDate,
                @Param("menuItemId") UUID menuItemId,
                @Param("qty") int qty);

    /**
     * Locks the row (SELECT ... FOR UPDATE) before the admin recomputes stock.
     * Without it, an admin editing the day's total while orders are arriving reads a stale
     * remaining quantity and writes back a value that erases those concurrent orders.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DailyMenuItem d where d.id = :id")
    Optional<DailyMenuItem> lockById(@Param("id") UUID id);

    /** True when at least one non-terminal order for this date already contains the item. */
    @Query("""
            select count(oi) > 0
              from OrderItem oi
             where oi.order.menuDate = :menuDate
               and oi.menuItem.id = :menuItemId
               and oi.order.status not in (
                   com.school.canteen.enums.OrderStatus.CANCELLED,
                   com.school.canteen.enums.OrderStatus.REJECTED)
            """)
    boolean hasActiveOrders(@Param("menuDate") LocalDate menuDate,
                            @Param("menuItemId") UUID menuItemId);
}
