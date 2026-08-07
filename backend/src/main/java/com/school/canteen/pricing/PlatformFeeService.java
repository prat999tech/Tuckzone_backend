package com.school.canteen.pricing;

import com.school.canteen.enums.PaymentUseCase;
import java.math.BigDecimal;

/**
 * The one place platform-fee arithmetic happens. Nothing else in the codebase computes a
 * fee amount — {@link PaymentUseCase}, percentage-vs-fixed, and the min/max clamps all
 * come from admin-configured {@link com.school.canteen.entity.PlatformFeeSettings}, never
 * from a constant in code.
 */
public interface PlatformFeeService {

    /**
     * @param baseAmount the amount the fee is computed against (recharge amount, or the
     *                   order's post-discount pre-fee total). Never negative fee, never
     *                   more than a configured max, never less than a configured min.
     * @return the fee, or {@link BigDecimal#ZERO} if this use case has no enabled setting.
     */
    BigDecimal calculateFee(PaymentUseCase useCase, BigDecimal baseAmount);
}
