package com.school.canteen.pricing;

import com.school.canteen.enums.PaymentMode;
import java.math.BigDecimal;

/**
 * Computes what a payment costs. Deliberately separate from {@code PaymentService}: this
 * interface never talks to a payment provider, and {@code PaymentService} never does this
 * arithmetic itself — it always calls here. The backend is the sole source of truth for
 * every number in a {@link PricingBreakdown}; the frontend only ever displays one.
 */
public interface PricingService {

    /**
     * Recharge is always gateway-funded by definition: the customer pays
     * {@code rechargeAmount + platformFee} through the provider, and the wallet is
     * credited {@code rechargeAmount} — never the fee. See the class-level note on
     * {@link com.school.canteen.entity.Payment#getPlatformFee()}.
     */
    PricingBreakdown calculateWalletRechargePricing(BigDecimal rechargeAmount);

    /**
     * @param subtotal              sum of order line totals, before discount/tax/fee
     * @param discount              future coupon/promo hook — always {@link BigDecimal#ZERO} today
     * @param tax                   future GST hook — always {@link BigDecimal#ZERO} today
     * @param mode                  which funding split the customer chose
     * @param walletAmountAvailable how much wallet balance the caller is willing to apply
     *                              (already capped by actual balance) — ignored for
     *                              {@link PaymentMode#GATEWAY_ONLY}. The platform fee is
     *                              always funded by the gateway leg when one exists: in
     *                              {@link PaymentMode#WALLET_PLUS_GATEWAY}, wallet covers
     *                              up to (subtotal - discount + tax) and the fee always
     *                              rides on the gateway amount. {@link PaymentMode#WALLET_ONLY}
     *                              is the one case where wallet does fund the fee, because
     *                              there is no gateway leg at all in that mode.
     */
    PricingBreakdown calculateCheckoutPricing(BigDecimal subtotal, BigDecimal discount, BigDecimal tax,
                                              PaymentMode mode, BigDecimal walletAmountAvailable);
}
