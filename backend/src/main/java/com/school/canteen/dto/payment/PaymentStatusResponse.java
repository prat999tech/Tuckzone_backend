package com.school.canteen.dto.payment;

import com.school.canteen.enums.PaymentUseCase;
import com.school.canteen.pricing.PricingBreakdown;
import java.time.Instant;
import java.util.UUID;

public record PaymentStatusResponse(
        UUID paymentId,
        PaymentUseCase useCase,
        String status,
        String provider,
        String providerPaymentId,
        PricingBreakdown pricing,
        Instant createdAt) {
}
