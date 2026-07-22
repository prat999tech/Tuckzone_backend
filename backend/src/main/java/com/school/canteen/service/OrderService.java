package com.school.canteen.service;

import com.school.canteen.dto.order.DeliverySlotResponse;
import com.school.canteen.dto.order.OrderResponse;
import com.school.canteen.dto.order.OrderStatusUpdateRequest;
import com.school.canteen.dto.order.PlaceOrderRequest;
import com.school.canteen.enums.OrderStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface OrderService {

    // --- customer ---
    OrderResponse placeOrder(UUID userId, PlaceOrderRequest request);

    List<OrderResponse> myOrders(UUID userId);

    OrderResponse getMyOrder(UUID userId, UUID orderId);

    OrderResponse cancelMyOrder(UUID userId, UUID orderId);

    List<DeliverySlotResponse> listSlots();

    // --- canteen admin ---
    List<OrderResponse> adminList(LocalDate date, OrderStatus status);

    OrderResponse adminTransition(UUID orderId, OrderStatusUpdateRequest request);
}
