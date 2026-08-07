package com.school.canteen.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/** @param amount null means a full refund of whatever remains unrefunded on the payment. */
public record RefundRequest(@DecimalMin("0.01") BigDecimal amount, String reason) {
}
