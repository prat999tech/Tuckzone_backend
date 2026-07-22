package com.school.canteen.exception;

import org.springframework.http.HttpStatus;

/** A line item ran out of stock during placement. Maps to HTTP 409. */
public class OutOfStockException extends ApiException {

    public OutOfStockException(String itemName) {
        super(HttpStatus.CONFLICT, "Out of stock: " + itemName);
    }
}
