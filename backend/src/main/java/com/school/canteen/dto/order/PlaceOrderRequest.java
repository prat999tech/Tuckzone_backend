package com.school.canteen.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
        @NotNull LocalDate menuDate,
        UUID beneficiaryStudentProfileId,
        @NotBlank @Size(max = 200) String deliveryLocation,
        @NotEmpty @Valid List<OrderLineRequest> items,
        @NotBlank @Size(max = 80) String idempotencyKey) {
}
