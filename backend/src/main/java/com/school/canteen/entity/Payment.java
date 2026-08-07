package com.school.canteen.entity;

import com.school.canteen.enums.PaymentTxnStatus;
import com.school.canteen.enums.PaymentUseCase;
import com.school.canteen.payment.PaymentProviderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * The canonical ledger row for one payment attempt — wallet recharge or order checkout
 * alike. Everything money-shaped that this app ever charges lands here: {@link #subtotal}
 * plus {@link #platformFee} (this app's revenue, never wallet's), minus {@link #discount},
 * plus {@link #tax}, minus {@link #walletUsed}, equals {@link #gatewayAmount} — and
 * subtotal + platformFee - discount + tax = {@link #grandTotal}.
 *
 * The unique {@link #providerOrderId} plus the one-way PENDING -> PAID transition (locked
 * via {@code PaymentRepository.lockByProviderOrderId}) is what makes verification
 * idempotent against retried client calls and webhook replays alike — the same mechanism
 * {@code WalletTopup} used, generalised to every use case.
 */
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "use_case", nullable = false)
    private PaymentUseCase useCase;

    /** e.g. "ORDER" when this payment settles an order; null for wallet recharge. */
    @Column(name = "reference_type")
    private String referenceType;

    /** e.g. the order id, as a string — mirrors WalletTransaction's reference columns. */
    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "platform_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal platformFee = BigDecimal.ZERO;

    @Column(name = "discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "tax", nullable = false, precision = 12, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    /** Portion of grandTotal paid from wallet balance. Wallet never receives platformFee. */
    @Column(name = "wallet_used", nullable = false, precision = 12, scale = 2)
    private BigDecimal walletUsed = BigDecimal.ZERO;

    /** Portion of grandTotal charged through the payment provider. */
    @Column(name = "gateway_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal gatewayAmount = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private PaymentProviderType provider;

    @Column(name = "provider_order_id", nullable = false, unique = true)
    private String providerOrderId;

    @Column(name = "provider_payment_id")
    private String providerPaymentId;

    /** Signature supplied at verification, kept for audit — never re-derived from this. */
    @Column(name = "signature")
    private String signature;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentTxnStatus status;

    /** Client-supplied dedupe key; null for provider- or admin-originated payments. */
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    public boolean isSettled(java.time.Instant now) {
        return status == PaymentTxnStatus.PAID || status == PaymentTxnStatus.REFUNDED
                || status == PaymentTxnStatus.PARTIALLY_REFUNDED;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public PaymentUseCase getUseCase() {
        return useCase;
    }

    public void setUseCase(PaymentUseCase useCase) {
        this.useCase = useCase;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getPlatformFee() {
        return platformFee;
    }

    public void setPlatformFee(BigDecimal platformFee) {
        this.platformFee = platformFee;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getWalletUsed() {
        return walletUsed;
    }

    public void setWalletUsed(BigDecimal walletUsed) {
        this.walletUsed = walletUsed;
    }

    public BigDecimal getGatewayAmount() {
        return gatewayAmount;
    }

    public void setGatewayAmount(BigDecimal gatewayAmount) {
        this.gatewayAmount = gatewayAmount;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentProviderType getProvider() {
        return provider;
    }

    public void setProvider(PaymentProviderType provider) {
        this.provider = provider;
    }

    public String getProviderOrderId() {
        return providerOrderId;
    }

    public void setProviderOrderId(String providerOrderId) {
        this.providerOrderId = providerOrderId;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public void setProviderPaymentId(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public PaymentTxnStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentTxnStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
