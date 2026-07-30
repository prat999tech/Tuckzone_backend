package com.school.canteen.dto.order;

import com.school.canteen.enums.OrderingStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * @param acceptingOrders the answer customers actually care about: combines the manual
 *                        open/closed switch with the effective cutoff time.
 */
public record OrderingWindowResponse(
        LocalDate menuDate,
        UUID slotId,
        String slotName,
        OrderingStatus status,
        LocalTime effectiveCutoffTime,
        boolean acceptingOrders,
        String reason) {
}
