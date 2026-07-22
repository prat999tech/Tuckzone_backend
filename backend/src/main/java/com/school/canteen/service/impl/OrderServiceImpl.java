package com.school.canteen.service.impl;

import com.school.canteen.dto.order.DeliverySlotResponse;
import com.school.canteen.dto.order.OrderLineRequest;
import com.school.canteen.dto.order.OrderResponse;
import com.school.canteen.dto.order.OrderStatusUpdateRequest;
import com.school.canteen.dto.order.PlaceOrderRequest;
import com.school.canteen.entity.DailyMenuItem;
import com.school.canteen.entity.DeliverySlot;
import com.school.canteen.entity.MenuItem;
import com.school.canteen.entity.Order;
import com.school.canteen.entity.OrderItem;
import com.school.canteen.entity.StudentProfile;
import com.school.canteen.entity.User;
import com.school.canteen.enums.OrderStatus;
import com.school.canteen.enums.PaymentMethod;
import com.school.canteen.enums.PaymentStatus;
import com.school.canteen.enums.Role;
import com.school.canteen.exception.ApiException;
import com.school.canteen.exception.BadRequestException;
import com.school.canteen.exception.InvalidOrderStateException;
import com.school.canteen.exception.OutOfStockException;
import com.school.canteen.exception.ResourceNotFoundException;
import com.school.canteen.mapper.OrderMapper;
import com.school.canteen.repository.DailyMenuItemRepository;
import com.school.canteen.repository.DeliverySlotRepository;
import com.school.canteen.repository.OrderRepository;
import com.school.canteen.repository.ParentChildLinkRepository;
import com.school.canteen.repository.StudentProfileRepository;
import com.school.canteen.repository.UserRepository;
import com.school.canteen.service.OrderService;
import com.school.canteen.service.WalletService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

    /** Allowed forward moves the canteen admin can make. REJECTED is handled separately
     *  (it refunds), and CANCELLED is a customer action. */
    private static final Map<OrderStatus, Set<OrderStatus>> ADMIN_FORWARD = new EnumMap<>(OrderStatus.class);

    static {
        ADMIN_FORWARD.put(OrderStatus.PLACED, Set.of(OrderStatus.ACCEPTED));
        ADMIN_FORWARD.put(OrderStatus.ACCEPTED, Set.of(OrderStatus.PREPARING));
        ADMIN_FORWARD.put(OrderStatus.PREPARING, Set.of(OrderStatus.PACKED));
        ADMIN_FORWARD.put(OrderStatus.PACKED, Set.of(OrderStatus.OUT_FOR_DELIVERY));
        ADMIN_FORWARD.put(OrderStatus.OUT_FOR_DELIVERY, Set.of(OrderStatus.DELIVERED));
    }

    private static final Set<Role> ROLES_THAT_CAN_ORDER =
            Set.of(Role.STUDENT, Role.TEACHER, Role.PARENT);

    private final OrderRepository orderRepository;
    private final DailyMenuItemRepository dailyMenuItemRepository;
    private final DeliverySlotRepository deliverySlotRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ParentChildLinkRepository parentChildLinkRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final OrderMapper orderMapper;

    public OrderServiceImpl(OrderRepository orderRepository,
                            DailyMenuItemRepository dailyMenuItemRepository,
                            DeliverySlotRepository deliverySlotRepository,
                            StudentProfileRepository studentProfileRepository,
                            ParentChildLinkRepository parentChildLinkRepository,
                            UserRepository userRepository,
                            WalletService walletService,
                            OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.dailyMenuItemRepository = dailyMenuItemRepository;
        this.deliverySlotRepository = deliverySlotRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.parentChildLinkRepository = parentChildLinkRepository;
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.orderMapper = orderMapper;
    }

    @Override
    @Transactional
    public OrderResponse placeOrder(UUID userId, PlaceOrderRequest request) {
        // 1) Idempotency: a resent checkout returns the same order, never a second one.
        var existing = orderRepository.findByPlacedBy_IdAndIdempotencyKey(userId, request.idempotencyKey());
        if (existing.isPresent()) {
            return orderMapper.toResponse(existing.get());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!ROLES_THAT_CAN_ORDER.contains(user.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Only students, teachers and parents can place orders");
        }

        DeliverySlot slot = deliverySlotRepository.findById(request.slotId())
                .filter(DeliverySlot::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery slot not found"));

        LocalDate date = request.menuDate();
        validateOrderingWindow(date, slot);

        Order order = new Order();
        order.setOrderNumber(orderRepository.nextOrderNumber());
        order.setPlacedBy(user);
        order.setRecipientName(resolveRecipient(user, request, order));
        order.setDeliveryLocation(request.deliveryLocation());
        order.setSlot(slot);
        order.setMenuDate(date);
        order.setStatus(OrderStatus.PLACED);
        order.setPaymentMethod(PaymentMethod.WALLET);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setIdempotencyKey(request.idempotencyKey());

        BigDecimal total = addLinesAndReserveStock(order, date, request.items());
        order.setTotalAmount(total);
        orderRepository.save(order);

        // Pay from the placing user's wallet. If the balance is short, this throws and the
        // whole transaction rolls back — including every stock decrement above.
        walletService.debit(userId, total, "ORDER", order.getId().toString(),
                "Order " + OrderMapper.formatOrderNumber(order.getOrderNumber()));
        order.setPaymentStatus(PaymentStatus.PAID);

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> myOrders(UUID userId) {
        return orderRepository.findByPlacedBy_IdOrderByCreatedAtDesc(userId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndPlacedBy_Id(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelMyOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndPlacedBy_Id(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new InvalidOrderStateException(
                    "Only an order that hasn't been accepted yet can be cancelled");
        }
        refundAndRestore(order, OrderStatus.CANCELLED);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliverySlotResponse> listSlots() {
        return deliverySlotRepository.findByActiveTrueOrderByOrderCutoffTimeAsc().stream()
                .map(orderMapper::toSlotResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> adminList(LocalDate date, OrderStatus status) {
        List<Order> orders = (status == null)
                ? orderRepository.findByMenuDateOrderByCreatedAtAsc(date)
                : orderRepository.findByMenuDateAndStatusOrderByCreatedAtAsc(date, status);
        return orders.stream().map(orderMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public OrderResponse adminTransition(UUID orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        OrderStatus target = request.status();

        if (target == OrderStatus.REJECTED) {
            if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.ACCEPTED) {
                throw new InvalidOrderStateException("Only a new or accepted order can be rejected");
            }
            refundAndRestore(order, OrderStatus.REJECTED);
            return orderMapper.toResponse(order);
        }

        Set<OrderStatus> allowed = ADMIN_FORWARD.getOrDefault(order.getStatus(), Set.of());
        if (!allowed.contains(target)) {
            throw new InvalidOrderStateException(
                    "Cannot move an order from " + order.getStatus() + " to " + target);
        }
        if (target == OrderStatus.OUT_FOR_DELIVERY) {
            if (request.deliveryPersonName() == null || request.deliveryPersonName().isBlank()) {
                throw new BadRequestException("Assign a delivery person before dispatching");
            }
            order.setDeliveryPersonName(request.deliveryPersonName().trim());
        }
        order.setStatus(target);
        return orderMapper.toResponse(order);
    }

    // --- helpers ---------------------------------------------------------------

    private void validateOrderingWindow(LocalDate date, DeliverySlot slot) {
        if (date.isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot order for a past date");
        }
        LocalDateTime cutoff = LocalDateTime.of(date, slot.getOrderCutoffTime());
        if (LocalDateTime.now().isAfter(cutoff)) {
            throw new BadRequestException("Ordering for the " + slot.getName() + " slot has closed");
        }
    }

    private String resolveRecipient(User user, PlaceOrderRequest request, Order order) {
        if (user.getRole() == Role.PARENT) {
            if (request.beneficiaryStudentProfileId() == null) {
                throw new BadRequestException("Select which child this order is for");
            }
            StudentProfile child = studentProfileRepository.findById(request.beneficiaryStudentProfileId())
                    .orElseThrow(() -> new ResourceNotFoundException("Child not found"));
            if (!parentChildLinkRepository.existsByParent_IdAndStudentProfile_Id(
                    user.getId(), child.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "This child is not linked to your account");
            }
            order.setBeneficiaryStudentProfile(child);
            return child.getUser().getFullName();
        }
        // Student/teacher self-order.
        if (request.beneficiaryStudentProfileId() != null) {
            throw new BadRequestException("Only a parent can order on behalf of a child");
        }
        return user.getFullName();
    }

    private BigDecimal addLinesAndReserveStock(Order order, LocalDate date,
                                               List<OrderLineRequest> lines) {
        Set<UUID> seen = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderLineRequest line : lines) {
            if (!seen.add(line.menuItemId())) {
                throw new BadRequestException("Duplicate item in order; combine into one line");
            }
            DailyMenuItem entry = dailyMenuItemRepository
                    .findByMenuDateAndMenuItem_Id(date, line.menuItemId())
                    .filter(e -> e.isAvailable() && e.getMenuItem().isActive())
                    .orElseThrow(() -> new BadRequestException("Item is not on the menu for " + date));

            // Atomic reservation: only succeeds if enough stock is still available.
            int updated = dailyMenuItemRepository.tryDecrement(date, line.menuItemId(), line.quantity());
            if (updated == 0) {
                throw new OutOfStockException(entry.getMenuItem().getName());
            }

            MenuItem menuItem = entry.getMenuItem();
            BigDecimal unitPrice = menuItem.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(line.quantity()));

            OrderItem item = new OrderItem();
            item.setMenuItem(menuItem);
            item.setItemName(menuItem.getName());
            item.setUnitPrice(unitPrice);
            item.setQuantity(line.quantity());
            item.setLineTotal(lineTotal);
            order.addItem(item);

            total = total.add(lineTotal);
        }
        return total;
    }

    /** Puts stock back and refunds the wallet (if paid), then moves to a terminal status. */
    private void refundAndRestore(Order order, OrderStatus terminalStatus) {
        for (OrderItem item : order.getItems()) {
            dailyMenuItemRepository.restore(order.getMenuDate(),
                    item.getMenuItem().getId(), item.getQuantity());
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            walletService.credit(order.getPlacedBy().getId(), order.getTotalAmount(),
                    "REFUND", order.getId().toString(),
                    "Refund for " + OrderMapper.formatOrderNumber(order.getOrderNumber()));
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        order.setStatus(terminalStatus);
    }
}
