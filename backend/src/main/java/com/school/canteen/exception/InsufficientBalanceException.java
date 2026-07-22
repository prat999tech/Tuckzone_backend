package com.school.canteen.exception;

import org.springframework.http.HttpStatus;

/** Wallet debit requested for more than the available balance. Maps to HTTP 422. */
public class InsufficientBalanceException extends ApiException {

    public InsufficientBalanceException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient wallet balance");
    }
}
