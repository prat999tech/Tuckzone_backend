package com.school.canteen.controller;

import com.school.canteen.dto.order.DeliverySlotResponse;
import com.school.canteen.dto.order.OrderResponse;
import com.school.canteen.dto.order.OrderStatusUpdateRequest;
import com.school.canteen.dto.order.PlaceOrderRequest;
import com.school.canteen.enums.OrderStatus;
import com.school.canteen.security.AppUserDetails;
import com.school.canteen.service.OrderService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ─── Customer endpoints ─────────────────────────────────────────────

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@AuthenticationPrincipal AppUserDetails principal,
                                    @Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(principal.getUser().getId(), request);
    }

    @GetMapping("/orders")
    public List<OrderResponse> myOrders(@AuthenticationPrincipal AppUserDetails principal,
                                        @RequestParam(required = false) Integer page,
                                        @RequestParam(required = false) Integer size) {
        return orderService.myOrders(principal.getUser().getId(), page, size);
    }

    @GetMapping("/orders/{id}")
    public OrderResponse getMyOrder(@AuthenticationPrincipal AppUserDetails principal,
                                    @PathVariable UUID id) {
        return orderService.getMyOrder(principal.getUser().getId(), id);
    }

    @DeleteMapping("/orders/{id}")
    public OrderResponse cancelOrder(@AuthenticationPrincipal AppUserDetails principal,
                                     @PathVariable UUID id) {
        return orderService.cancelMyOrder(principal.getUser().getId(), id);
    }

    @GetMapping("/delivery-slots")
    public List<DeliverySlotResponse> listSlots() {
        return orderService.listSlots();
    }

    // ─── Canteen admin endpoints ────────────────────────────────────────

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('CANTEEN_ADMIN')")
    public List<OrderResponse> adminListOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return orderService.adminList(date, status, page, size);
    }

    /** Counter hand-over: staff type the code from the teacher's receipt. */
    @PostMapping("/admin/orders/collect")
    @PreAuthorize("hasRole('CANTEEN_ADMIN')")
    public OrderResponse collect(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String pickupCode) {
        return orderService.collectByPickupCode(date, pickupCode);
    }

    @PutMapping("/admin/orders/{id}/status")
    @PreAuthorize("hasRole('CANTEEN_ADMIN')")
    public OrderResponse adminTransition(@PathVariable UUID id,
                                         @Valid @RequestBody OrderStatusUpdateRequest request) {
        return orderService.adminTransition(id, request);
    }
}
