package com.school.canteen.service;

import com.school.canteen.dto.order.DefaultOrderingDateResponse;
import com.school.canteen.dto.order.DemandRow;
import com.school.canteen.dto.order.DeliverySlotResponse;
import com.school.canteen.dto.order.OrderingWindowRequest;
import com.school.canteen.dto.order.OrderingWindowResponse;
import com.school.canteen.entity.DeliverySlot;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * The canteen's control over advance ordering: when orders are accepted for a future date
 * and how much demand has already accumulated for it.
 */
public interface OrderingWindowService {

    /** Stops accepting new orders for a date + slot and tells customers why. */
    OrderingWindowResponse close(OrderingWindowRequest request);

    /** Resumes ordering, optionally past the slot's normal cutoff, and announces it. */
    OrderingWindowResponse open(OrderingWindowRequest request);

    List<OrderingWindowResponse> statusFor(LocalDate menuDate);

    /**
     * The date the ordering UI should default to right now: the earliest date, starting
     * tomorrow, that's still accepting orders for the active slot — skipping past a date
     * whose cutoff already elapsed or that the canteen closed manually (e.g. a holiday).
     * The single source of truth for "what date is a customer ordering for" on login, on
     * opening the ordering screen, and after a stale order is rejected past its cutoff.
     */
    DefaultOrderingDateResponse resolveDefaultOrderingDate();

    /** What has been ordered so far for a date, against what is stocked. */
    List<DemandRow> demandFor(LocalDate menuDate);

    /**
     * Throws when ordering is not currently permitted for this date + slot. Combines the
     * manual open/closed switch with the effective cutoff time.
     */
    void assertAcceptingOrders(LocalDate menuDate, DeliverySlot slot);

    /**
     * Changes the slot's standing (every-day) order cutoff time. This is the default the
     * whole app falls back to; a per-date {@link #open} override still takes precedence for
     * the day it targets. Takes effect immediately — the next order placed for any date
     * without its own override is checked against the new time.
     */
    DeliverySlotResponse updateCutoffTime(UUID slotId, LocalTime cutoffTime);
}
