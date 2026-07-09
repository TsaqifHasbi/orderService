package com.assessment.orderservice.exception;

import java.util.UUID;

/**
 * Thrown when an order lookup by ID yields no result.
 */
public class OrderNotFoundException extends RuntimeException {

    private final UUID orderId;

    public OrderNotFoundException(UUID orderId) {
        super("Order not found with id: " + orderId);
        this.orderId = orderId;
    }

    public UUID getOrderId() {
        return orderId;
    }
}
