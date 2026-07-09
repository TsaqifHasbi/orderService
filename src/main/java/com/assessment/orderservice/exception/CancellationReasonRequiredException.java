package com.assessment.orderservice.exception;

/**
 * Thrown when a cancellation request is missing the required reason.
 */
public class CancellationReasonRequiredException extends RuntimeException {

    public CancellationReasonRequiredException() {
        super("A reason is required when cancelling an order");
    }
}
