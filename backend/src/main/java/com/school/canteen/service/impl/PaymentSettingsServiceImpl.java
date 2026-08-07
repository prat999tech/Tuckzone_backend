package com.school.canteen.service.impl;

import com.school.canteen.dto.payment.PlatformFeeSettingsResponse;
import com.school.canteen.dto.payment.PlatformFeeSettingsUpdateRequest;
import com.school.canteen.entity.PlatformFeeSettings;
import com.school.canteen.enums.PaymentUseCase;
import com.school.canteen.exception.ResourceNotFoundException;
import com.school.canteen.repository.PlatformFeeSettingsRepository;
import com.school.canteen.service.PaymentSettingsService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentSettingsServiceImpl implements PaymentSettingsService {

    private final PlatformFeeSettingsRepository settingsRepository;

    public PaymentSettingsServiceImpl(PlatformFeeSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlatformFeeSettingsResponse> listAll() {
        return settingsRepository.findAllByOrderByUseCase().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public PlatformFeeSettingsResponse update(PaymentUseCase useCase, PlatformFeeSettingsUpdateRequest request) {
        PlatformFeeSettings settings = settingsRepository.findByUseCase(useCase)
                .orElseThrow(() -> new ResourceNotFoundException("No platform fee settings for " + useCase));
        settings.setEnabled(request.enabled());
        settings.setFeeType(request.feeType());
        settings.setFeeValue(request.feeValue());
        settings.setMinFee(request.minFee());
        settings.setMaxFee(request.maxFee());
        settingsRepository.save(settings);
        return toResponse(settings);
    }

    private PlatformFeeSettingsResponse toResponse(PlatformFeeSettings settings) {
        return new PlatformFeeSettingsResponse(settings.getUseCase(), settings.isEnabled(),
                settings.getFeeType(), settings.getFeeValue(), settings.getMinFee(), settings.getMaxFee());
    }
}
