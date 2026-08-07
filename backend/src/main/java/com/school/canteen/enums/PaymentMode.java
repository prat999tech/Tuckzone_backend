package com.school.canteen.enums;

/**
 * How the customer wants a checkout funded. Wallet recharge has no equivalent — it is
 * always gateway-only by definition (that's what a "recharge" is).
 */
public enum PaymentMode {
    WALLET_ONLY,
    GATEWAY_ONLY,
    WALLET_PLUS_GATEWAY
}
