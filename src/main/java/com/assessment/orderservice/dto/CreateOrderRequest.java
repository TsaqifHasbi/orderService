package com.assessment.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for creating a new order.
 *
 * <p>Deliberately excludes {@code orderId}, {@code status}, {@code totalAmount},
 * {@code createdAt}, and {@code updatedAt} — all of which are server-managed.
 * Even if a malicious client includes these fields in the JSON payload, Jackson
 * will simply ignore them because there are no matching setters or fields here.
 */
public class CreateOrderRequest {

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotEmpty(message = "An order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;

    public CreateOrderRequest() {
    }

    public CreateOrderRequest(String customerName, List<OrderItemRequest> items) {
        this.customerName = customerName;
        this.items = items;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }
}
