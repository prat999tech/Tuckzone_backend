package com.school.canteen.service;

import com.school.canteen.dto.order.DemandRow;
import com.school.canteen.dto.order.OrderingWindowRequest;
import com.school.canteen.dto.order.OrderingWindowResponse;
import com.school.canteen.entity.DeliverySlot;
import java.time.LocalDate;
import java.util.List;

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

    /** What has been ordered so far for a date, against what is stocked. */
    List<DemandRow> demandFor(LocalDate menuDate);

    /**
     * Throws when ordering is not currently permitted for this date + slot. Combines the
     * manual open/closed switch with the effective cutoff time.
     */
    void assertAcceptingOrders(LocalDate menuDate, DeliverySlot slot);
}
