package com.school.canteen.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Registers the calling device so it can receive pushes. */
public record DeviceTokenRequest(
        @NotBlank @Size(max = 512) String token,
        @NotBlank @Size(max = 20) String platform) {
}
