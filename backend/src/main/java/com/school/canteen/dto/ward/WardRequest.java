package com.school.canteen.dto.ward;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A parent enters their ward directly — no admission number, no existing account. */
public record WardRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 50) String studentClass,
        @NotBlank @Size(max = 50) String section) {
}
