package com.school.canteen.service.impl;

import com.school.canteen.dto.order.DemandRow;
import com.school.canteen.dto.order.DeliverySlotResponse;
import com.school.canteen.dto.order.OrderingWindowRequest;
import com.school.canteen.dto.order.OrderingWindowResponse;
import com.school.canteen.entity.DailyMenuItem;
import com.school.canteen.entity.DeliverySlot;
import com.school.canteen.entity.OrderingWindow;
import com.school.canteen.enums.NotificationEvent;
import com.school.canteen.enums.OrderingStatus;
import com.school.canteen.exception.BadRequestException;
import com.school.canteen.exception.ResourceNotFoundException;
import com.school.canteen.repository.DailyMenuItemRepository;
import com.school.canteen.repository.DeliverySlotRepository;
import com.school.canteen.repository.OrderRepository;
import com.school.canteen.repository.OrderingWindowRepository;
import com.school.canteen.service.NotificationService;
import com.school.canteen.service.OrderingWindowService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderingWindowServiceImpl implements OrderingWindowService {

    private final OrderingWindowRepository windowRepository;
    private final DeliverySlotRepository slotRepository;
    private final DailyMenuItemRepository dailyMenuItemRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    public OrderingWindowServiceImpl(OrderingWindowRepository windowRepository,
                                     DeliverySlotRepository slotRepository,
                                     DailyMenuItemRepository dailyMenuItemRepository,
                                     OrderRepository orderRepository,
                                     NotificationService notificationService,
                                     Clock clock) {
        this.windowRepository = windowRepository;
        this.slotRepository = slotRepository;
        this.dailyMenuItemRepository = dailyMenuItemRepository;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public OrderingWindowResponse close(OrderingWindowRequest request) {
        DeliverySlot slot = findSlot(request.slotId());
        OrderingWindow window = findOrCreate(request.menuDate(), slot);
        window.setStatus(OrderingStatus.CLOSED);
        window.setOverrideCutoffTime(null);
        window.setReason(request.reason());

        notificationService.notifyAllCustomers(NotificationEvent.ORDERING_CLOSED,
                "Ordering closed",
                "Orders for " + slot.getName() + " on " + request.menuDate() + " are now closed."
                        + suffix(request.reason()),
                Map.of("menuDate", request.menuDate().toString(),
                        "slotId", slot.getId().toString(),
                        "status", OrderingStatus.CLOSED.name()));

        return toResponse(window, slot);
    }

    @Override
    @Transactional
    public OrderingWindowResponse open(OrderingWindowRequest request) {
        DeliverySlot slot = findSlot(request.slotId());
        if (request.menuDate().isBefore(LocalDate.now(clock))) {
            throw new BadRequestException("Cannot reopen ordering for a past date");
        }
        OrderingWindow window = findOrCreate(request.menuDate(), slot);
        window.setStatus(OrderingStatus.OPEN);
        // An override is how ordering reopens after the normal deadline has passed —
        // typically because the admin has just added more stock.
        window.setOverrideCutoffTime(request.overrideCutoffTime());
        window.setReason(request.reason());

        LocalTime cutoff = effectiveCutoff(window, slot);
        notificationService.notifyAllCustomers(NotificationEvent.ORDERING_OPENED,
                "Ordering open",
                "You can now order for " + slot.getName() + " on " + request.menuDate()
                        + ". Order before " + cutoff + "." + suffix(request.reason()),
                Map.of("menuDate", request.menuDate().toString(),
                        "slotId", slot.getId().toString(),
                        "cutoff", cutoff.toString(),
                        "status", OrderingStatus.OPEN.name()));

        return toResponse(window, slot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderingWindowResponse> statusFor(LocalDate menuDate) {
        Map<UUID, OrderingWindow> overrides = new HashMap<>();
        windowRepository.findByMenuDate(menuDate)
                .forEach(window -> overrides.put(window.getSlot().getId(), window));

        // Every active slot is reported, whether or not the admin has overridden it, so
        // the console shows the full picture rather than only the edited rows.
        return slotRepository.findByActiveTrueOrderByOrderCutoffTimeAsc().stream()
                .map(slot -> toResponse(overrides.get(slot.getId()), slot, menuDate))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandRow> demandFor(LocalDate menuDate) {
        Map<UUID, DailyMenuItem> stockByItem = new HashMap<>();
        dailyMenuItemRepository.findByMenuDate(menuDate)
                .forEach(entry -> stockByItem.put(entry.getMenuItem().getId(), entry));

        return orderRepository.aggregateDemand(menuDate).stream()
                .map(row -> {
                    UUID menuItemId = (UUID) row[0];
                    String itemName = (String) row[1];
                    long ordered = ((Number) row[2]).longValue();
                    DailyMenuItem stock = stockByItem.get(menuItemId);
                    int total = (stock == null) ? 0 : stock.getTotalQuantity();
                    int remaining = (stock == null) ? 0 : stock.getRemainingQuantity();
                    // Positive shortfall means more has been promised than stocked, which
                    // is exactly what the admin needs to see before cooking.
                    return new DemandRow(menuItemId, itemName, ordered, total, remaining,
                            Math.max(0, ordered - total));
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void assertAcceptingOrders(LocalDate menuDate, DeliverySlot slot) {
        OrderingWindow window = windowRepository
                .findByMenuDateAndSlot_Id(menuDate, slot.getId()).orElse(null);

        if (window != null && window.getStatus() == OrderingStatus.CLOSED) {
            throw new BadRequestException("The canteen has stopped taking orders for "
                    + slot.getName() + " on " + menuDate + "." + suffix(window.getReason()));
        }

        LocalTime cutoff = effectiveCutoff(window, slot);
        if (LocalDateTime.now(clock).isAfter(LocalDateTime.of(menuDate, cutoff))) {
            throw new BadRequestException(
                    "Ordering has closed for today. Please place your order before the "
                            + "cut-off time.");
        }
    }

    @Override
    @Transactional
    public DeliverySlotResponse updateCutoffTime(UUID slotId, LocalTime cutoffTime) {
        DeliverySlot slot = findSlot(slotId);
        slot.setOrderCutoffTime(cutoffTime);
        return new DeliverySlotResponse(slot.getId(), slot.getName(), slot.getOrderCutoffTime(),
                slot.getDeliveryTime());
    }

    // --- helpers ---------------------------------------------------------------

    private DeliverySlot findSlot(UUID slotId) {
        return slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery slot not found"));
    }

    private OrderingWindow findOrCreate(LocalDate menuDate, DeliverySlot slot) {
        return windowRepository.findByMenuDateAndSlot_Id(menuDate, slot.getId())
                .orElseGet(() -> {
                    OrderingWindow created = new OrderingWindow();
                    created.setMenuDate(menuDate);
                    created.setSlot(slot);
                    // Must be set before saving: the row is persisted here and the caller
                    // only overwrites the status afterwards, so leaving it null would fail
                    // the NOT NULL constraint at flush time.
                    created.setStatus(OrderingStatus.OPEN);
                    return windowRepository.save(created);
                });
    }

    private LocalTime effectiveCutoff(OrderingWindow window, DeliverySlot slot) {
        return (window != null && window.getOverrideCutoffTime() != null)
                ? window.getOverrideCutoffTime()
                : slot.getOrderCutoffTime();
    }

    private OrderingWindowResponse toResponse(OrderingWindow window, DeliverySlot slot) {
        return toResponse(window, slot, window.getMenuDate());
    }

    private OrderingWindowResponse toResponse(OrderingWindow window, DeliverySlot slot,
                                              LocalDate menuDate) {
        OrderingStatus status = (window == null) ? OrderingStatus.OPEN : window.getStatus();
        LocalTime cutoff = effectiveCutoff(window, slot);
        boolean accepting = status == OrderingStatus.OPEN
                && !LocalDateTime.now(clock).isAfter(LocalDateTime.of(menuDate, cutoff));
        return new OrderingWindowResponse(menuDate, slot.getId(), slot.getName(), status,
                cutoff, accepting, window == null ? null : window.getReason());
    }

    private static String suffix(String reason) {
        return (reason == null || reason.isBlank()) ? "" : " (" + reason + ")";
    }
}
