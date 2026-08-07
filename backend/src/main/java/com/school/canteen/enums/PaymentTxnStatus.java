package com.school.canteen.enums;

/**
 * Lifecycle of a {@link com.school.canteen.entity.Payment} row. Named distinctly from
 * {@link PaymentStatus} (which stays on {@code Order} and only ever means PENDING/PAID/
 * REFUNDED for that order) to avoid the two being confused — a Payment can be PARTIALLY_
 * REFUNDED while its order is simply REFUNDED.
 */
public enum PaymentTxnStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED,
    PARTIALLY_REFUNDED
}
