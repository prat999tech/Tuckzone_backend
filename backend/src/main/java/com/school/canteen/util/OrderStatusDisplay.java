package com.school.canteen.util;

import com.school.canteen.enums.OrderStatus;

/**
 * The admin/customer-facing status label everywhere an order's status is *displayed*
 * (order boards, exports) rather than reasoned about — the underlying {@link OrderStatus}
 * enum and its transition rules are unchanged, only the word shown to a human is
 * simplified to just "Placed" or "Delivered".
 */
public final class OrderStatusDisplay {

    private OrderStatusDisplay() {
    }

    public static String label(OrderStatus status) {
        return status == OrderStatus.DELIVERED ? "Delivered" : "Placed";
    }
}
