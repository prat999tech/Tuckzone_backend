package com.school.canteen.controller;

import com.school.canteen.dto.payment.PlatformFeeSettingsResponse;
import com.school.canteen.dto.payment.PlatformFeeSettingsUpdateRequest;
import com.school.canteen.enums.PaymentUseCase;
import com.school.canteen.service.PaymentSettingsService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Platform fee configuration. Canteen-admin only — this is pricing policy, not operations. */
@RestController
@RequestMapping("/api/admin/payment-settings")
@PreAuthorize("hasRole('CANTEEN_ADMIN')")
public class PaymentSettingsController {

    private final PaymentSettingsService paymentSettingsService;

    public PaymentSettingsController(PaymentSettingsService paymentSettingsService) {
        this.paymentSettingsService = paymentSettingsService;
    }

    @GetMapping
    public List<PlatformFeeSettingsResponse> list() {
        return paymentSettingsService.listAll();
    }

    @PutMapping("/{useCase}")
    public PlatformFeeSettingsResponse update(@PathVariable PaymentUseCase useCase,
                                              @Valid @RequestBody PlatformFeeSettingsUpdateRequest request) {
        return paymentSettingsService.update(useCase, request);
    }
}
