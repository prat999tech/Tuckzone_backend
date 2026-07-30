package com.school.canteen.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Opens or closes ordering for a date + slot.
 *
 * @param overrideCutoffTime when reopening after the normal deadline, the new time until
 *                           which orders are accepted. Ignored when closing.
 * @param reason             shown to customers in the notification, e.g. "kitchen at
 *                           capacity" or "extra stock added"
 */
public record OrderingWindowRequest(
        @NotNull LocalDate menuDate,
        @NotNull UUID slotId,
        LocalTime overrideCutoffTime,
        @Size(max = 255) String reason) {
}
