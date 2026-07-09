package com.assessment.orderservice.controller;

import com.assessment.orderservice.dto.CreateOrderRequest;
import com.assessment.orderservice.dto.OrderResponse;
import com.assessment.orderservice.dto.StatusTransitionRequest;
import com.assessment.orderservice.dto.UpdateOrderRequest;
import com.assessment.orderservice.service.OrderService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for order management.
 *
 * <p>Follows RESTful conventions:
 * <ul>
 *   <li>{@code POST   /api/orders}            — create</li>
 *   <li>{@code GET    /api/orders/{id}}        — read single</li>
 *   <li>{@code GET    /api/orders}             — read list (paginated, sortable)</li>
 *   <li>{@code PUT    /api/orders/{id}}        — update</li>
 *   <li>{@code PATCH  /api/orders/{id}/status} — transition status</li>
 *   <li>{@code DELETE /api/orders/{id}}        — delete</li>
 * </ul>
 *
 * <p>This controller is intentionally thin — all business logic lives
 * in {@link OrderService}. The controller's only responsibilities are:
 * HTTP method mapping, request deserialization/validation, and response
 * status code selection.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ────────────────────────────────────────────────────────────
    //  CREATE
    // ────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ────────────────────────────────────────────────────────────
    //  READ (single)
    // ────────────────────────────────────────────────────────────

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
        OrderResponse response = orderService.getOrder(orderId);
        return ResponseEntity.ok(response);
    }

    // ────────────────────────────────────────────────────────────
    //  READ (list — paginated, sorted)
    // ────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "newest") String sort) {
        Page<OrderResponse> response = orderService.listOrders(page, size, sort);
        return ResponseEntity.ok(response);
    }

    // ────────────────────────────────────────────────────────────
    //  UPDATE
    // ────────────────────────────────────────────────────────────

    @PutMapping("/{orderId}")
    public ResponseEntity<OrderResponse> updateOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderRequest request) {
        OrderResponse response = orderService.updateOrder(orderId, request);
        return ResponseEntity.ok(response);
    }

    // ────────────────────────────────────────────────────────────
    //  STATUS TRANSITION
    // ────────────────────────────────────────────────────────────

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> transitionStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody StatusTransitionRequest request) {
        OrderResponse response = orderService.transitionStatus(orderId, request);
        return ResponseEntity.ok(response);
    }

    // ────────────────────────────────────────────────────────────
    //  DELETE
    // ────────────────────────────────────────────────────────────

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();
    }
}
