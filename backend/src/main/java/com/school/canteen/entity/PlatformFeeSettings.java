package com.school.canteen.entity;

import com.school.canteen.enums.PaymentUseCase;
import com.school.canteen.enums.PlatformFeeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Admin-configurable platform fee for one {@link PaymentUseCase}. One row per use case
 * (seeded by V15 for WALLET_RECHARGE and CHECKOUT, both disabled) — this is the only place
 * fee numbers live; nothing about the fee is ever hardcoded in application code.
 */
@Entity
@Table(name = "platform_fee_settings")
public class PlatformFeeSettings extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "use_case", nullable = false, unique = true)
    private PaymentUseCase useCase;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false)
    private PlatformFeeType feeType;

    /** Percent (e.g. 2.00 = 2%) when feeType=PERCENTAGE, or a currency amount when FIXED. */
    @Column(name = "fee_value", nullable = false, precision = 8, scale = 4)
    private BigDecimal feeValue;

    @Column(name = "min_fee", precision = 12, scale = 2)
    private BigDecimal minFee;

    @Column(name = "max_fee", precision = 12, scale = 2)
    private BigDecimal maxFee;

    public PaymentUseCase getUseCase() {
        return useCase;
    }

    public void setUseCase(PaymentUseCase useCase) {
        this.useCase = useCase;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public PlatformFeeType getFeeType() {
        return feeType;
    }

    public void setFeeType(PlatformFeeType feeType) {
        this.feeType = feeType;
    }

    public BigDecimal getFeeValue() {
        return feeValue;
    }

    public void setFeeValue(BigDecimal feeValue) {
        this.feeValue = feeValue;
    }

    public BigDecimal getMinFee() {
        return minFee;
    }

    public void setMinFee(BigDecimal minFee) {
        this.minFee = minFee;
    }

    public BigDecimal getMaxFee() {
        return maxFee;
    }

    public void setMaxFee(BigDecimal maxFee) {
        this.maxFee = maxFee;
    }
}
