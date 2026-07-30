package com.school.canteen.service.impl;

import com.school.canteen.dto.notification.DeviceTokenRequest;
import com.school.canteen.dto.notification.NotificationResponse;
import com.school.canteen.entity.DeviceToken;
import com.school.canteen.entity.User;
import com.school.canteen.enums.NotificationChannel;
import com.school.canteen.exception.ResourceNotFoundException;
import com.school.canteen.repository.DeviceTokenRepository;
import com.school.canteen.repository.NotificationOutboxRepository;
import com.school.canteen.repository.UserRepository;
import com.school.canteen.service.DeviceService;
import com.school.canteen.util.PageRequests;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceServiceImpl implements DeviceService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationOutboxRepository outboxRepository;
    private final UserRepository userRepository;

    public DeviceServiceImpl(DeviceTokenRepository deviceTokenRepository,
                             NotificationOutboxRepository outboxRepository,
                             UserRepository userRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.outboxRepository = outboxRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void registerDevice(UUID userId, DeviceTokenRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // A push token belongs to an app install, not to a person. When someone signs out
        // and a colleague signs in on the same phone, FCM hands over the same token — so
        // re-point the existing row instead of creating a duplicate, otherwise the previous
        // user would keep receiving this device's notifications.
        DeviceToken device = deviceTokenRepository.findByToken(request.token())
                .orElseGet(DeviceToken::new);
        device.setUser(user);
        device.setToken(request.token());
        device.setPlatform(request.platform());
        device.setLastSeenAt(Instant.now());
        deviceTokenRepository.save(device);
    }

    @Override
    @Transactional
    public void unregisterDevice(UUID userId, String token) {
        // Scoped to the caller so one user cannot silence another user's device.
        deviceTokenRepository.findByToken(token)
                .filter(device -> device.getUser().getId().equals(userId))
                .ifPresent(deviceTokenRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> feed(UUID userId, Integer page, Integer size) {
        return outboxRepository
                .findByUser_IdOrderByCreatedAtDesc(userId, PageRequests.of(page, size)).stream()
                // The push copy is a delivery mechanism, not a feed entry; showing both
                // would duplicate every notification in the list.
                .filter(row -> row.getChannel() == NotificationChannel.IN_APP)
                .map(row -> new NotificationResponse(
                        row.getId(),
                        row.getEventType(),
                        row.getTitle(),
                        row.getBody(),
                        row.getPayload(),
                        row.getCreatedAt()))
                .toList();
    }
}
