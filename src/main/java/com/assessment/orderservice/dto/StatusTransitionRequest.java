package com.assessment.orderservice.dto;

import com.assessment.orderservice.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for transitioning an order's status.
 *
 * <p>Separated from the general update endpoint because status transitions
 * have distinct validation rules:
 * <ul>
 *   <li>Not all transitions are legal (enforced by {@link OrderStatus#canTransitionTo})</li>
 *   <li>Some transitions require additional data (e.g., cancellation requires a {@code reason})</li>
 *   <li>New transition-specific data requirements will be added over time</li>
 * </ul>
 *
 * <p><strong>Extensibility:</strong> When a new transition rule is added (e.g., "shipping
 * requires a tracking number"), add the field here and validate it in the service layer.
 */
public class StatusTransitionRequest {

    @NotNull(message = "Target status is required")
    private OrderStatus status;

    /**
     * Required when transitioning to CANCELLED; ignored for other transitions.
     */
    private String reason;

    public StatusTransitionRequest() {
    }

    public StatusTransitionRequest(OrderStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
