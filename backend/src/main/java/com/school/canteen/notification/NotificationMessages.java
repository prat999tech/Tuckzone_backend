package com.school.canteen.notification;

import com.school.canteen.enums.NotificationEvent;
import com.school.canteen.enums.OrderStatus;

/**
 * Wording for every notification, in one place.
 *
 * Keeping copy out of the services means changing "Out for delivery" to something friendlier
 * does not involve touching order logic, and the mobile app can still branch on the
 * {@link NotificationEvent} rather than on text.
 */
public final class NotificationMessages {

    private NotificationMessages() {
    }

    public static NotificationEvent eventFor(OrderStatus status) {
        return switch (status) {
            case PLACED -> NotificationEvent.ORDER_PLACED;
            case ACCEPTED -> NotificationEvent.ORDER_ACCEPTED;
            case PREPARING -> NotificationEvent.ORDER_PREPARING;
            case PACKED -> NotificationEvent.ORDER_PACKED;
            case OUT_FOR_DELIVERY -> NotificationEvent.ORDER_OUT_FOR_DELIVERY;
            case DELIVERED -> NotificationEvent.ORDER_DELIVERED;
            case CANCELLED -> NotificationEvent.ORDER_CANCELLED;
            case REJECTED -> NotificationEvent.ORDER_REJECTED;
        };
    }

    public static String titleFor(OrderStatus status) {
        return switch (status) {
            case PLACED -> "Order placed";
            case ACCEPTED -> "Order accepted";
            case PREPARING -> "Your food is being prepared";
            case PACKED -> "Order packed";
            case OUT_FOR_DELIVERY -> "Out for delivery";
            case DELIVERED -> "Delivered";
            case CANCELLED -> "Order cancelled";
            case REJECTED -> "Order rejected";
        };
    }

    public static String bodyFor(OrderStatus status, String orderNumber, String recipient,
                                 String deliveryPerson) {
        return switch (status) {
            case PLACED -> "Order " + orderNumber + " for " + recipient + " has been placed.";
            case ACCEPTED -> "The canteen accepted order " + orderNumber + ".";
            case PREPARING -> "The kitchen has started preparing order " + orderNumber + ".";
            case PACKED -> "Order " + orderNumber + " is packed and waiting for pickup.";
            case OUT_FOR_DELIVERY -> (deliveryPerson == null || deliveryPerson.isBlank())
                    ? "Order " + orderNumber + " is on its way."
                    : "Order " + orderNumber + " is on its way with " + deliveryPerson + ".";
            case DELIVERED -> "Order " + orderNumber + " was delivered to " + recipient + ".";
            case CANCELLED -> "Order " + orderNumber + " was cancelled and refunded to your wallet.";
            case REJECTED -> "The canteen could not accept order " + orderNumber
                    + ". The amount has been refunded to your wallet.";
        };
    }
}
