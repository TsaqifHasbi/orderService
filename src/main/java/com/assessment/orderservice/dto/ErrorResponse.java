package com.assessment.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Standardised error response returned for all API errors.
 *
 * <p>Provides a consistent structure that clients can parse predictably:
 * <pre>
 * {
 *   "timestamp": "2026-07-08T11:45:00Z",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed",
 *   "details": ["customerName: Customer name is required"]
 * }
 * </pre>
 *
 * <p>The {@code details} field is omitted from the JSON when {@code null}
 * (single-error responses) via {@link JsonInclude}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private List<String> details;

    public ErrorResponse() {
        this.timestamp = Instant.now();
    }

    public ErrorResponse(int status, String error, String message) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.message = message;
    }

    public ErrorResponse(int status, String error, String message, List<String> details) {
        this(status, error, message);
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getDetails() {
        return details;
    }
}
