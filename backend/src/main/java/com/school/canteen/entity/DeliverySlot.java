package com.school.canteen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalTime;

/**
 * A recurring daily delivery window. Orders for a date+slot must be placed before that
 * date's orderCutoffTime; food is delivered around deliveryTime.
 */
@Entity
@Table(name = "delivery_slots")
public class DeliverySlot extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "order_cutoff_time", nullable = false)
    private LocalTime orderCutoffTime;

    @Column(name = "delivery_time", nullable = false)
    private LocalTime deliveryTime;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalTime getOrderCutoffTime() {
        return orderCutoffTime;
    }

    public void setOrderCutoffTime(LocalTime orderCutoffTime) {
        this.orderCutoffTime = orderCutoffTime;
    }

    public LocalTime getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(LocalTime deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
