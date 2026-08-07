package com.school.canteen.entity;

import com.school.canteen.enums.OrderStatus;
import com.school.canteen.enums.OrderType;
import com.school.canteen.enums.PaymentMethod;
import com.school.canteen.enums.PaymentStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A placed order. Named "orders" (ORDER is a SQL keyword). Carries who placed/pays for it,
 * who receives it, when/where it's delivered, its lifecycle status, and payment state.
 */
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Column(name = "order_number", nullable = false, unique = true)
    private Long orderNumber;

    /** The user who placed and pays for the order (parent, student, or teacher). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "placed_by_user_id", nullable = false)
    private User placedBy;

    /** Set only when a parent orders for a child; null for self-orders. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_student_profile_id")
    private StudentProfile beneficiaryStudentProfile;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "delivery_location", nullable = false)
    private String deliveryLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private DeliverySlot slot;

    @Column(name = "menu_date", nullable = false)
    private LocalDate menuDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "delivery_person_name")
    private String deliveryPersonName;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType = OrderType.DELIVERY;

    /** Short code a takeaway customer shows at the counter. Null for delivery orders. */
    @Column(name = "pickup_code", length = 12)
    private String pickupCode;

    /**
     * Set only when this order used the gateway/split-payment path (see PaymentService).
     * Null for the existing wallet-only fast path, which never creates a Payment row.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    /** Adds a line and keeps both sides of the relationship consistent. */
    public void addItem(OrderItem item) {
        item.setOrder(this);
        this.items.add(item);
    }

    public Long getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Long orderNumber) {
        this.orderNumber = orderNumber;
    }

    public User getPlacedBy() {
        return placedBy;
    }

    public void setPlacedBy(User placedBy) {
        this.placedBy = placedBy;
    }

    public StudentProfile getBeneficiaryStudentProfile() {
        return beneficiaryStudentProfile;
    }

    public void setBeneficiaryStudentProfile(StudentProfile beneficiaryStudentProfile) {
        this.beneficiaryStudentProfile = beneficiaryStudentProfile;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getDeliveryLocation() {
        return deliveryLocation;
    }

    public void setDeliveryLocation(String deliveryLocation) {
        this.deliveryLocation = deliveryLocation;
    }

    public DeliverySlot getSlot() {
        return slot;
    }

    public void setSlot(DeliverySlot slot) {
        this.slot = slot;
    }

    public LocalDate getMenuDate() {
        return menuDate;
    }

    public void setMenuDate(LocalDate menuDate) {
        this.menuDate = menuDate;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getDeliveryPersonName() {
        return deliveryPersonName;
    }

    public void setDeliveryPersonName(String deliveryPersonName) {
        this.deliveryPersonName = deliveryPersonName;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public String getPickupCode() {
        return pickupCode;
    }

    public void setPickupCode(String pickupCode) {
        this.pickupCode = pickupCode;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
}
