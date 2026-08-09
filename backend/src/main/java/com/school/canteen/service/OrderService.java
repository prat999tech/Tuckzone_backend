package com.school.canteen.service;

import com.school.canteen.dto.order.AdminOrderResponse;
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

    /**
     * The transactional half of {@link #placeOrder}. Exposed on the interface only so it
     * can be called through the Spring proxy; callers should always use placeOrder.
     */
    OrderResponse placeOrderInTransaction(UUID userId, PlaceOrderRequest request);

    /**
     * Returns the order already stored under this idempotency key, or null. Used to
     * resolve a duplicate submission; transactional so lazy associations can be mapped.
     */
    OrderResponse findByIdempotencyKey(UUID userId, String idempotencyKey);

    List<OrderResponse> myOrders(UUID userId, Integer page, Integer size);

    OrderResponse getMyOrder(UUID userId, UUID orderId);

    List<DeliverySlotResponse> listSlots();

    // --- canteen admin / sub admin ---
    List<AdminOrderResponse> adminList(LocalDate date, OrderStatus status, String search, String sort,
                                       Integer page, Integer size);

    /** Unpaged (capped generously) variant of {@link #adminList} for file export, so an
     *  export can never be silently truncated at the normal page-size cap. */
    List<AdminOrderResponse> adminListForExport(LocalDate date, OrderStatus status, String search);

    AdminOrderResponse adminTransition(UUID orderId, OrderStatusUpdateRequest request);
}
