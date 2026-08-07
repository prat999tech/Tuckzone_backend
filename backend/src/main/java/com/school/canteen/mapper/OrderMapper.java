package com.school.canteen.mapper;

import com.school.canteen.dto.order.AdminOrderResponse;
import com.school.canteen.dto.order.DeliverySlotResponse;
import com.school.canteen.dto.order.OrderItemResponse;
import com.school.canteen.dto.order.OrderResponse;
import com.school.canteen.dto.payment.PaymentInitiationResponse;
import com.school.canteen.entity.DeliverySlot;
import com.school.canteen.entity.Order;
import com.school.canteen.entity.OrderItem;
import com.school.canteen.entity.StudentProfile;
import com.school.canteen.enums.Role;
import com.school.canteen.repository.StudentProfileRepository;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    private final StudentProfileRepository studentProfileRepository;

    public OrderMapper(StudentProfileRepository studentProfileRepository) {
        this.studentProfileRepository = studentProfileRepository;
    }

    public OrderResponse toResponse(Order order) {
        return toResponse(order, null);
    }

    /** @param payment attached only right after placing an order that needs the client to
     *                 complete a gateway checkout — see OrderResponse#payment javadoc. */
    public OrderResponse toResponse(Order order, PaymentInitiationResponse payment) {
        StudentInfo info = resolveStudentInfo(order);
        return new OrderResponse(
                order.getId(),
                formatOrderNumber(order.getOrderNumber()),
                order.getStatus(),
                order.getOrderType(),
                order.getPickupCode(),
                order.getMenuDate(),
                order.getSlot().getName(),
                order.getSlot().getDeliveryTime(),
                order.getRecipientName(),
                info.studentClass(),
                info.section(),
                order.getDeliveryLocation(),
                order.getDeliveryPersonName(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getItems().stream().map(this::toItemResponse).toList(),
                order.getCreatedAt(),
                payment);
    }

    /**
     * Admin-facing view: same fields as {@link #toResponse}, plus the ordering student's
     * name/class/section/roll number resolved via {@link #resolveStudentInfo}. Never adds
     * phone, email, wallet or address — those fields do not exist on {@link AdminOrderResponse}.
     */
    public AdminOrderResponse toAdminResponse(Order order) {
        StudentInfo info = resolveStudentInfo(order);
        return new AdminOrderResponse(
                order.getId(),
                formatOrderNumber(order.getOrderNumber()),
                order.getStatus(),
                order.getOrderType(),
                order.getPickupCode(),
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
                order.getCreatedAt(),
                info.name(),
                info.studentClass(),
                info.section(),
                info.rollNumber());
    }

    private record StudentInfo(String name, String studentClass, String section, String rollNumber) {
    }

    /**
     * Resolution order: the linked beneficiary profile (parent-for-child order) first, then
     * the placing user's own profile if they are a student ordering for themselves, and
     * finally a bare name with no class/section/roll for a teacher's own order.
     *
     * Shared by {@link #toResponse} and {@link #toAdminResponse} so the customer's own
     * order view and the admin's view of that same order always agree on the student's
     * class — neither trusts anything the client typed at checkout for it.
     */
    private StudentInfo resolveStudentInfo(Order order) {
        StudentProfile profile = order.getBeneficiaryStudentProfile();
        if (profile == null && order.getPlacedBy().getRole() == Role.STUDENT) {
            profile = studentProfileRepository.findByUser_Id(order.getPlacedBy().getId()).orElse(null);
        }
        if (profile != null) {
            return new StudentInfo(profile.getUser().getFullName(), profile.getStudentClass(),
                    profile.getSection(), profile.getRollNumber());
        }
        return new StudentInfo(order.getRecipientName(), null, null, null);
    }

    public OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                // Null once the catalog row has been permanently deleted — every other
                // field here is a snapshot taken at order time, so the response stays
                // complete and accurate regardless.
                item.getMenuItem() == null ? null : item.getMenuItem().getId(),
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
