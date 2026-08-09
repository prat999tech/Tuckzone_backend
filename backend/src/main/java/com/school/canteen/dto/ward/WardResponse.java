package com.school.canteen.dto.ward;

import java.util.UUID;

public record WardResponse(
        UUID id,
        String name,
        String studentClass,
        String section) {
}
