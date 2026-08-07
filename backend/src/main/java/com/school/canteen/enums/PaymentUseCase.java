package com.school.canteen.enums;

/**
 * What a {@link com.school.canteen.entity.Payment} is for. Platform fee settings and
 * reporting are both sliced by this, independently of which provider handled the money.
 */
public enum PaymentUseCase {
    WALLET_RECHARGE,
    CHECKOUT,
    /** Reserved for future subscription billing — no code path produces this yet. */
    SUBSCRIPTION
}
