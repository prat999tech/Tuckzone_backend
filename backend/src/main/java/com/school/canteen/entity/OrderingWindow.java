package com.school.canteen.entity;

import com.school.canteen.enums.OrderingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * The canteen's manual control over ordering for one date + slot.
 *
 * Absence of a row is the normal case: ordering follows the slot's cutoff time. A row only
 * exists once the admin has intervened — closing early, or reopening with a later cutoff
 * after adding stock.
 */
@Entity
@Table(name = "ordering_windows")
public class OrderingWindow extends BaseEntity {

    @Column(name = "menu_date", nullable = false)
    private LocalDate menuDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private DeliverySlot slot;

    /** Defaults to OPEN so a freshly created row is never persisted without a status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderingStatus status = OrderingStatus.OPEN;

    /** When set, replaces the slot's cutoff — the mechanism for reopening late. */
    @Column(name = "override_cutoff_time")
    private LocalTime overrideCutoffTime;

    @Column(name = "reason")
    private String reason;

    public LocalDate getMenuDate() {
        return menuDate;
    }

    public void setMenuDate(LocalDate menuDate) {
        this.menuDate = menuDate;
    }

    public DeliverySlot getSlot() {
        return slot;
    }

    public void setSlot(DeliverySlot slot) {
        this.slot = slot;
    }

    public OrderingStatus getStatus() {
        return status;
    }

    public void setStatus(OrderingStatus status) {
        this.status = status;
    }

    public LocalTime getOverrideCutoffTime() {
        return overrideCutoffTime;
    }

    public void setOverrideCutoffTime(LocalTime overrideCutoffTime) {
        this.overrideCutoffTime = overrideCutoffTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
