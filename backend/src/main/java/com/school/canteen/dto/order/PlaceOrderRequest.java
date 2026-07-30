package com.school.canteen.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.school.canteen.enums.OrderType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A checkout. beneficiaryStudentProfileId is set only when a parent orders for a child.
 * idempotencyKey is a client-generated id for this checkout — resending it returns the
 * same order instead of placing another.
 */
public record PlaceOrderRequest(
        @NotNull UUID slotId,
        /** DELIVERY (default) or TAKEAWAY. Takeaway is restricted to teachers. */
        OrderType orderType,
        @NotNull LocalDate menuDate,
        UUID beneficiaryStudentProfileId,
        // Optional at the DTO level because the rule depends on the caller's role, which
        // bean validation cannot express: a student/teacher must supply it, while a
        // parent's location is derived from the linked child's classroom.
        @Size(max = 200) String deliveryLocation,
        @NotEmpty @Valid List<OrderLineRequest> items,
        @NotBlank @Size(max = 80) String idempotencyKey) {
}
