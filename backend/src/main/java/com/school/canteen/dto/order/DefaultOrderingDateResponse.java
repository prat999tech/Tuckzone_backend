package com.school.canteen.dto.order;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * The date the ordering UI should default to right now — see
 * {@code OrderingWindowService#resolveDefaultOrderingDate}. Always the earliest date,
 * starting tomorrow, that is still accepting orders for the active slot; skips past any
 * date whose cutoff has already elapsed or that the canteen closed manually.
 */
public record DefaultOrderingDateResponse(
        LocalDate menuDate,
        UUID slotId,
        String slotName,
        LocalTime cutoffTime) {
}
