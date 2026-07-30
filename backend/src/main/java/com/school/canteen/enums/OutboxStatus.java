package com.school.canteen.enums;

/**
 * Delivery state of an outbox row.
 *
 * PROCESSING exists so a row being worked on is not picked up twice — by the immediate
 * after-commit attempt and the sweeper at the same time.
 */
public enum OutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED,
    /** Superseded before it was ever sent; kept for audit rather than deleted. */
    SKIPPED
}
