package com.school.canteen.mapper;

import com.school.canteen.dto.order.DeliverySlotResponse;
import com.school.canteen.dto.order.OrderItemResponse;
import com.school.canteen.dto.order.OrderResponse;
import com.school.canteen.entity.DeliverySlot;
import com.school.canteen.entity.Order;
import com.school.canteen.entity.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                formatOrderNumber(order.getOrderNumber()),
                order.getStatus(),
                order.getMenuDate(),
                order.getSlot().getName(),
                order.getSlot().getDeliveryTime(),
                order.getRecipientName(),
                order.getDeliveryLocation(),
                order.getDeliveryPersonName(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getItems().stream().map(this::toItemResponse).toList(),
                order.getCreatedAt());
    }

    public OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getMenuItem().getId(),
                item.getItemName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal());
    }

    public DeliverySlotResponse toSlotResponse(DeliverySlot slot) {
        return new DeliverySlotResponse(
                slot.getId(),
                slot.getName(),
                slot.getOrderCutoffTime(),
                slot.getDeliveryTime());
    }

    public static String formatOrderNumber(Long orderNumber) {
        return "ORD-" + orderNumber;
    }
}
