package com.school.canteen.dto.order;

import java.time.LocalTime;
import java.util.UUID;

public record DeliverySlotResponse(
        UUID id,
        String name,
        LocalTime orderCutoffTime,
        LocalTime deliveryTime) {
}
