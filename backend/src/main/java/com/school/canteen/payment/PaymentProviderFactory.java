package com.school.canteen.payment;

import com.school.canteen.config.PaymentProperties;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Factory pattern: Spring hands this every registered {@link PaymentProvider} bean, and it
 * resolves the one to use by {@link PaymentProviderType}. This is the literal mechanism
 * behind "change {@code app.payment.provider} and nothing else changes" — {@code
 * PaymentService} never instantiates or conditionally-wires a provider itself, it just
 * asks this factory.
 */
@Component
public class PaymentProviderFactory {

    private final Map<PaymentProviderType, PaymentProvider> providersByType;
    private final PaymentProperties paymentProperties;

    public PaymentProviderFactory(List<PaymentProvider> providers, PaymentProperties paymentProperties) {
        this.providersByType = providers.stream()
                .collect(Collectors.toUnmodifiableMap(PaymentProvider::type, Function.identity()));
        this.paymentProperties = paymentProperties;
    }

    public PaymentProvider resolve(PaymentProviderType type) {
        PaymentProvider provider = providersByType.get(type);
        if (provider == null) {
            throw new IllegalStateException("No PaymentProvider registered for " + type
                    + " — is app.payment.provider=" + type.name().toLowerCase(Locale.ROOT)
                    + " but the matching @Component missing/not on the classpath?");
        }
        return provider;
    }

    /** The provider selected by {@code app.payment.provider} — what new payments use. */
    public PaymentProvider active() {
        PaymentProviderType type;
        try {
            type = PaymentProviderType.valueOf(paymentProperties.provider().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Unknown app.payment.provider '" + paymentProperties.provider() + "'. Valid values: "
                            + java.util.Arrays.toString(PaymentProviderType.values()), ex);
        }
        return resolve(type);
    }
}
