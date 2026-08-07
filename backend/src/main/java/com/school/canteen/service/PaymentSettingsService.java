package com.school.canteen.service;

import com.school.canteen.dto.payment.PlatformFeeSettingsResponse;
import com.school.canteen.dto.payment.PlatformFeeSettingsUpdateRequest;
import com.school.canteen.enums.PaymentUseCase;
import java.util.List;

/**
 * Admin CRUD over {@code PlatformFeeSettings} — the only place platform fee numbers can be
 * changed. No code change is ever needed to adjust or disable a fee, per use case.
 */
public interface PaymentSettingsService {

    List<PlatformFeeSettingsResponse> listAll();

    PlatformFeeSettingsResponse update(PaymentUseCase useCase, PlatformFeeSettingsUpdateRequest request);
}
