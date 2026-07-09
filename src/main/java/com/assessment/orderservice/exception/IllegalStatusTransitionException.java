package com.assessment.orderservice.exception;

import com.assessment.orderservice.entity.OrderStatus;

/**
 * Thrown when a status transition violates the order lifecycle rules.
 *
 * <p>For example, attempting to ship an order that is still in CREATED status.
 */
public class IllegalStatusTransitionException extends RuntimeException {

    private final OrderStatus currentStatus;
    private final OrderStatus targetStatus;

    public IllegalStatusTransitionException(OrderStatus currentStatus, OrderStatus targetStatus) {
        super("Cannot transition from " + currentStatus + " to " + targetStatus);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }

    public OrderStatus getTargetStatus() {
        return targetStatus;
    }
}
