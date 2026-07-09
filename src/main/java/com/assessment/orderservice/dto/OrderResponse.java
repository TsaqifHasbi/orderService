package com.assessment.orderservice.dto;

import com.assessment.orderservice.entity.Order;
import com.assessment.orderservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for an order. Maps from the {@link Order} entity.
 *
 * <p>This DTO controls exactly which fields are exposed to the client,
 * preventing accidental information leakage (e.g., internal JPA state,
 * lazy-loading proxies).
 */
public class OrderResponse {

    private UUID orderId;
    private String customerName;
    private List<OrderItemResponse> items;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private Instant createdAt;
    private Instant updatedAt;

    public OrderResponse() {
    }

    /**
     * Factory method to convert an {@link Order} entity to its response DTO.
     *
     * @param order the entity to convert
     * @return a fully populated response DTO
     */
    public static OrderResponse fromEntity(Order order) {
        OrderResponse response = new OrderResponse();
        response.orderId = order.getOrderId();
        response.customerName = order.getCustomerName();
        response.status = order.getStatus();
        response.totalAmount = order.getTotalAmount();
        response.createdAt = order.getCreatedAt();
        response.updatedAt = order.getUpdatedAt();

        response.items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()
                ))
                .toList();

        return response;
    }

    // ── Getters ──────────────────────────────────────────────

    public UUID getOrderId() {
        return orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
