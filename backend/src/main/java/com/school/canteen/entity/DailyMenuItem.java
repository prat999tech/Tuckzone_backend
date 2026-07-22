package com.school.canteen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * A catalog item offered on a specific date, with stock for that day. Unique per
 * (menu_date, menu_item) — you can't list the same item twice on the same day.
 */
@Entity
@Table(name = "daily_menu_items")
public class DailyMenuItem extends BaseEntity {

    @Column(name = "menu_date", nullable = false)
    private LocalDate menuDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    /** Decremented as orders are placed (Phase 4). Never below 0 or above total. */
    @Column(name = "remaining_quantity", nullable = false)
    private int remainingQuantity;

    /** Canteen admin can pull an item from the day's menu without deleting the row. */
    @Column(name = "available", nullable = false)
    private boolean available = true;

    public LocalDate getMenuDate() {
        return menuDate;
    }

    public void setMenuDate(LocalDate menuDate) {
        this.menuDate = menuDate;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public void setMenuItem(MenuItem menuItem) {
        this.menuItem = menuItem;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(int remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
