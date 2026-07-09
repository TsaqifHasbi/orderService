package com.assessment.orderservice.exception;

import com.assessment.orderservice.entity.OrderStatus;

/**
 * Thrown when an attempt is made to modify line items on an order
 * whose status no longer permits item changes (i.e., PAID or beyond).
 */
public class ItemsNotModifiableException extends RuntimeException {

    private final OrderStatus currentStatus;

    public ItemsNotModifiableException(OrderStatus currentStatus) {
        super("Order items cannot be modified when order status is " + currentStatus);
        this.currentStatus = currentStatus;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }
}
