package com.school.canteen.controller;

import com.school.canteen.dto.notification.DeviceTokenRequest;
import com.school.canteen.dto.notification.NotificationResponse;
import com.school.canteen.security.AppUserDetails;
import com.school.canteen.service.DeviceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Push-device registration and the in-app notification feed. Everything is scoped to the
 * caller via the JWT principal.
 */
@RestController
@RequestMapping("/api")
public class NotificationController {

    private final DeviceService deviceService;

    public NotificationController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /** Called by the mobile app on sign-in and whenever FCM rotates its token. */
    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerDevice(@AuthenticationPrincipal AppUserDetails principal,
                               @Valid @RequestBody DeviceTokenRequest request) {
        deviceService.registerDevice(principal.getUser().getId(), request);
    }

    /** Called on sign-out so the device stops receiving that account's notifications. */
    @DeleteMapping("/devices")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregisterDevice(@AuthenticationPrincipal AppUserDetails principal,
                                 @RequestParam String token) {
        deviceService.unregisterDevice(principal.getUser().getId(), token);
    }

    @GetMapping("/notifications")
    public List<NotificationResponse> feed(@AuthenticationPrincipal AppUserDetails principal,
                                           @RequestParam(required = false) Integer page,
                                           @RequestParam(required = false) Integer size) {
        return deviceService.feed(principal.getUser().getId(), page, size);
    }
}
