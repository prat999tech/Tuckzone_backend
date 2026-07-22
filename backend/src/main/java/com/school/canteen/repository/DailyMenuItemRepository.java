package com.school.canteen.repository;

import com.school.canteen.entity.DailyMenuItem;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
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

    /** Puts stock back on cancellation/rejection. */
    @Modifying
    @Query("""
            update DailyMenuItem d
               set d.remainingQuantity = d.remainingQuantity + :qty
             where d.menuDate = :menuDate
               and d.menuItem.id = :menuItemId
            """)
    int restore(@Param("menuDate") LocalDate menuDate,
                @Param("menuItemId") UUID menuItemId,
                @Param("qty") int qty);
}
