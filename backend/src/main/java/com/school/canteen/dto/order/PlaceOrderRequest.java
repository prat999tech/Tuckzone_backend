package com.school.canteen.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.school.canteen.enums.OrderType;
import com.school.canteen.enums.PaymentMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A checkout. beneficiaryStudentProfileId is set only when a parent orders for a child.
 * idempotencyKey is a client-generated id for this checkout — resending it returns the
 * same order instead of placing another. There is a single ordering slot (Recess), so
 * the caller no longer chooses one — the server resolves it automatically.
 */
public record PlaceOrderRequest(
        /** Ignored — every order is DELIVERY now. Kept on the DTO only so an older client
         *  sending a value does not fail request binding. */
        OrderType orderType,
        @NotNull LocalDate menuDate,
        UUID beneficiaryStudentProfileId,
        // Optional at the DTO level because the rule depends on the caller's role, which
        // bean validation cannot express: only a teacher must supply it. A parent's and a
        // student's own location are both derived from a classroom, never from this field.
        @Size(max = 200) String deliveryLocation,
        @NotEmpty @Valid List<OrderLineRequest> items,
        @NotBlank @Size(max = 80) String idempotencyKey,
        /**
         * Null (or WALLET_ONLY) = the original, unchanged behaviour: pay the whole total
         * from wallet balance immediately, synchronously, in this same call — exactly as
         * every order before this field existed. GATEWAY_ONLY / WALLET_PLUS_GATEWAY route
         * through PaymentService instead, creating a Payment the client must complete
         * checkout against; the order is PENDING until that settles.
         */
        PaymentMode paymentMode) {
}
