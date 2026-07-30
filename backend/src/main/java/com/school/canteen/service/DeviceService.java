package com.school.canteen.service;

import com.school.canteen.dto.notification.DeviceTokenRequest;
import com.school.canteen.dto.notification.NotificationResponse;
import java.util.List;
import java.util.UUID;

/** Device registration and the in-app notification feed. */
public interface DeviceService {

    void registerDevice(UUID userId, DeviceTokenRequest request);

    void unregisterDevice(UUID userId, String token);

    List<NotificationResponse> feed(UUID userId, Integer page, Integer size);
}
