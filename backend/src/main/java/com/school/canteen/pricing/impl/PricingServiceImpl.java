package com.school.canteen.pricing.impl;

import com.school.canteen.config.PaymentProperties;
import com.school.canteen.enums.PaymentMode;
import com.school.canteen.enums.PaymentUseCase;
import com.school.canteen.pricing.PlatformFeeService;
import com.school.canteen.pricing.PricingBreakdown;
import com.school.canteen.pricing.PricingService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class PricingServiceImpl implements PricingService {

    private final PlatformFeeService platformFeeService;
    private final PaymentProperties paymentProperties;

    public PricingServiceImpl(PlatformFeeService platformFeeService, PaymentProperties paymentProperties) {
        this.platformFeeService = platformFeeService;
        this.paymentProperties = paymentProperties;
    }

    @Override
    public PricingBreakdown calculateWalletRechargePricing(BigDecimal rechargeAmount) {
        BigDecimal fee = platformFeeService.calculateFee(PaymentUseCase.WALLET_RECHARGE, rechargeAmount);
        BigDecimal grandTotal = rechargeAmount.add(fee);
        return new PricingBreakdown(
                rechargeAmount,
                fee,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,      // walletUsed — recharge funds the wallet, it doesn't spend from it
                grandTotal,           // gatewayAmount — recharge is always 100% gateway-funded
                grandTotal,
                paymentProperties.currency());
    }

    @Override
    public PricingBreakdown calculateCheckoutPricing(BigDecimal subtotal, BigDecimal discount, BigDecimal tax,
                                                      PaymentMode mode, BigDecimal walletAmountAvailable) {
        BigDecimal netBeforeFee = subtotal.subtract(discount).add(tax);
        BigDecimal fee = platformFeeService.calculateFee(PaymentUseCase.CHECKOUT, netBeforeFee);
        BigDecimal grandTotal = netBeforeFee.add(fee);

        BigDecimal walletUsed = switch (mode) {
            case GATEWAY_ONLY -> BigDecimal.ZERO;
            // No gateway leg exists in this mode, so wallet is the only funding source —
            // it necessarily covers the fee too here, unlike every other mode.
            case WALLET_ONLY -> grandTotal;
            // Wallet may cover up to the non-fee portion; the fee always rides on the
            // gateway amount so it is genuinely collected by the provider, not the wallet.
            case WALLET_PLUS_GATEWAY -> walletAmountAvailable.min(netBeforeFee).max(BigDecimal.ZERO);
        };
        BigDecimal gatewayAmount = grandTotal.subtract(walletUsed);

        return new PricingBreakdown(subtotal, fee, discount, tax, walletUsed, gatewayAmount, grandTotal,
                paymentProperties.currency());
    }
}
