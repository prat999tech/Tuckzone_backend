package com.school.canteen.dto.parent;

import java.util.UUID;

public record ChildResponse(
        UUID linkId,
        UUID studentProfileId,
        UUID studentUserId,
        String fullName,
        String admissionNumber,
        String studentClass,
        String section,
        String rollNumber) {
}
