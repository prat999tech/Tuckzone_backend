package com.school.canteen.enums;

/**
 * Lifecycle of an order. Forward flow is PLACED → ACCEPTED → PREPARING → PACKED →
 * OUT_FOR_DELIVERY → DELIVERED. REJECTED (by canteen) and CANCELLED (by customer) are
 * terminal exits that trigger a refund. Allowed transitions are enforced in the service.
 */
public enum OrderStatus {
    PLACED,
    ACCEPTED,
    PREPARING,
    PACKED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    REJECTED,
    CANCELLED
}
