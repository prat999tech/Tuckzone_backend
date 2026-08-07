package com.school.canteen.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.school.canteen.entity.PlatformFeeSettings;
import com.school.canteen.enums.PaymentUseCase;
import com.school.canteen.enums.PlatformFeeType;
import com.school.canteen.pricing.impl.PlatformFeeServiceImpl;
import com.school.canteen.repository.PlatformFeeSettingsRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Pure unit test — no Spring context, no database. This is the one place fee arithmetic
 * happens (see PlatformFeeService's javadoc), so it is worth pinning down exactly.
 */
class PlatformFeeServiceImplTest {

    private final PlatformFeeSettingsRepository repository = mock(PlatformFeeSettingsRepository.class);
    private final PlatformFeeServiceImpl service = new PlatformFeeServiceImpl(repository);

    private PlatformFeeSettings settings(boolean enabled, PlatformFeeType type, String value,
                                         String min, String max) {
        PlatformFeeSettings settings = new PlatformFeeSettings();
        settings.setUseCase(PaymentUseCase.WALLET_RECHARGE);
        settings.setEnabled(enabled);
        settings.setFeeType(type);
        settings.setFeeValue(new BigDecimal(value));
        settings.setMinFee(min == null ? null : new BigDecimal(min));
        settings.setMaxFee(max == null ? null : new BigDecimal(max));
        return settings;
    }

    @Test
    void disabledSettingsMeanZeroFee() {
        when(repository.findByUseCase(PaymentUseCase.WALLET_RECHARGE))
                .thenReturn(Optional.of(settings(false, PlatformFeeType.PERCENTAGE, "5", null, null)));

        BigDecimal fee = service.calculateFee(PaymentUseCase.WALLET_RECHARGE, BigDecimal.valueOf(1000));

        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void missingSettingsMeanZeroFee() {
        when(repository.findByUseCase(PaymentUseCase.WALLET_RECHARGE)).thenReturn(Optional.empty());

        BigDecimal fee = service.calculateFee(PaymentUseCase.WALLET_RECHARGE, BigDecimal.valueOf(1000));

        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void percentageFeeMatchesTheWalletRechargeExample() {
        // Recharge 1000, 2% fee -> platform fee 20 (matches the ₹1000/2%/₹20 example this
        // whole feature was specified against).
        when(repository.findByUseCase(PaymentUseCase.WALLET_RECHARGE))
                .thenReturn(Optional.of(settings(true, PlatformFeeType.PERCENTAGE, "2", null, null)));

        BigDecimal fee = service.calculateFee(PaymentUseCase.WALLET_RECHARGE, BigDecimal.valueOf(1000));

        assertThat(fee).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    void fixedFeeIgnoresTheBaseAmount() {
        when(repository.findByUseCase(PaymentUseCase.WALLET_RECHARGE))
                .thenReturn(Optional.of(settings(true, PlatformFeeType.FIXED, "5", null, null)));

        BigDecimal fee = service.calculateFee(PaymentUseCase.WALLET_RECHARGE, BigDecimal.valueOf(1000));

        assertThat(fee).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void percentageFeeIsClampedToTheMinimum() {
        // 0.5% of 100 = 0.50, but a 5 rupee minimum applies.
        when(repository.findByUseCase(PaymentUseCase.WALLET_RECHARGE))
                .thenReturn(Optional.of(settings(true, PlatformFeeType.PERCENTAGE, "0.5", "5", null)));

        BigDecimal fee = service.calculateFee(PaymentUseCase.WALLET_RECHARGE, BigDecimal.valueOf(100));

        assertThat(fee).isEqualByComparingTo(new BigDecimal("5"));
    }

    @Test
    void percentageFeeIsClampedToTheMaximum() {
        // 2% of 10000 = 200, but a 50 rupee ceiling applies.
        when(repository.findByUseCase(PaymentUseCase.WALLET_RECHARGE))
                .thenReturn(Optional.of(settings(true, PlatformFeeType.PERCENTAGE, "2", null, "50")));

        BigDecimal fee = service.calculateFee(PaymentUseCase.WALLET_RECHARGE, BigDecimal.valueOf(10000));

        assertThat(fee).isEqualByComparingTo(new BigDecimal("50"));
    }
}
