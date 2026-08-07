package com.school.canteen.pricing.impl;

import com.school.canteen.entity.PlatformFeeSettings;
import com.school.canteen.enums.PaymentUseCase;
import com.school.canteen.enums.PlatformFeeType;
import com.school.canteen.pricing.PlatformFeeService;
import com.school.canteen.repository.PlatformFeeSettingsRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class PlatformFeeServiceImpl implements PlatformFeeService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PlatformFeeSettingsRepository settingsRepository;

    public PlatformFeeServiceImpl(PlatformFeeSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Override
    public BigDecimal calculateFee(PaymentUseCase useCase, BigDecimal baseAmount) {
        return settingsRepository.findByUseCase(useCase)
                .filter(PlatformFeeSettings::isEnabled)
                .map(settings -> apply(settings, baseAmount))
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal apply(PlatformFeeSettings settings, BigDecimal baseAmount) {
        BigDecimal fee = settings.getFeeType() == PlatformFeeType.PERCENTAGE
                ? baseAmount.multiply(settings.getFeeValue())
                        .divide(HUNDRED, 2, RoundingMode.HALF_UP)
                : settings.getFeeValue().setScale(2, RoundingMode.HALF_UP);

        if (settings.getMinFee() != null && fee.compareTo(settings.getMinFee()) < 0) {
            fee = settings.getMinFee();
        }
        if (settings.getMaxFee() != null && fee.compareTo(settings.getMaxFee()) > 0) {
            fee = settings.getMaxFee();
        }
        // A fee can never make the customer pay less than the base amount.
        return fee.max(BigDecimal.ZERO);
    }
}
