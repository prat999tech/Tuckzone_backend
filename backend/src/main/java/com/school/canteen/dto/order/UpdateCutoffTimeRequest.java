package com.school.canteen.dto.order;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record UpdateCutoffTimeRequest(@NotNull LocalTime orderCutoffTime) {
}
