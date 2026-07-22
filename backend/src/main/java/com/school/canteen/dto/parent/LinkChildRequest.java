package com.school.canteen.dto.parent;

import jakarta.validation.constraints.NotBlank;

/** A parent proves the relationship with the child's admission number and the parent
 *  mobile the student recorded at registration. Both must match. */
public record LinkChildRequest(
        @NotBlank String admissionNumber,
        @NotBlank String parentMobile) {
}
