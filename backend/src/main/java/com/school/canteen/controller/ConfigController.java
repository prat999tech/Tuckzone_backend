package com.school.canteen.controller;

import com.school.canteen.config.AppProperties;
import com.school.canteen.config.OtpProperties;
import com.school.canteen.config.PaymentProperties;
import com.school.canteen.dto.AppConfigResponse;
import com.school.canteen.dto.auth.ValidationRules;
import com.school.canteen.notification.OtpSender;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bootstrap configuration for the mobile client.
 *
 * Public on purpose: the app fetches this before anyone has signed in, on the launch
 * screen, to configure itself. It exposes only settings, never data.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final AppProperties appProperties;
    private final OtpProperties otpProperties;
    private final PaymentProperties paymentProperties;
    private final OtpSender otpSender;

    public ConfigController(AppProperties appProperties,
                            OtpProperties otpProperties,
                            PaymentProperties paymentProperties,
                            OtpSender otpSender) {
        this.appProperties = appProperties;
        this.otpProperties = otpProperties;
        this.paymentProperties = paymentProperties;
        this.otpSender = otpSender;
    }

    @Operation(summary = "Server capabilities for the mobile app; call once at launch")
    @SecurityRequirements // explicitly unauthenticated
    @GetMapping
    public AppConfigResponse config() {
        return new AppConfigResponse(
                paymentProperties.currency(),
                appProperties.timezone(),
                otpProperties.length(),
                otpProperties.ttlMinutes(),
                otpSender.exposesCodeInResponse(),
                paymentProperties.allowMockTopup(),
                paymentProperties.maxTopupAmount(),
                ValidationRules.PASSWORD_MIN,
                List.of(100, 200, 500, 1000),
                appProperties.minimumAppVersion());
    }
}
