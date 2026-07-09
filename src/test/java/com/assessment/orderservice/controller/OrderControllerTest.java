package com.assessment.orderservice.controller;

import com.assessment.orderservice.dto.CreateOrderRequest;
import com.assessment.orderservice.dto.OrderItemRequest;
import com.assessment.orderservice.dto.OrderResponse;
import com.assessment.orderservice.dto.StatusTransitionRequest;
import com.assessment.orderservice.dto.UpdateOrderRequest;
import com.assessment.orderservice.entity.OrderStatus;
import com.assessment.orderservice.exception.CancellationReasonRequiredException;
import com.assessment.orderservice.exception.IllegalStatusTransitionException;
import com.assessment.orderservice.exception.ItemsNotModifiableException;
import com.assessment.orderservice.exception.OrderNotFoundException;
import com.assessment.orderservice.service.OrderService;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer tests using MockMvc.
 *
 * <p>Tests HTTP status codes, request validation, and error response format.
 * The service layer is mocked to isolate controller concerns.
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    // ────────────────────────────────────────────────────────────
    //  POST /api/orders
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/orders")
    class CreateOrderEndpoint {

        @Test
        @DisplayName("returns 201 Created for valid request")
        void validRequestReturns201() throws Exception {
            CreateOrderRequest request = new CreateOrderRequest(
                    "Andi Wijaya",
                    List.of(new OrderItemRequest("Apple", 3, new BigDecimal("0.50")))
            );

            OrderResponse mockResponse = new OrderResponse();
            when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(mockResponse);

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("returns 400 when customerName is blank")
        void blankCustomerNameReturns400() throws Exception {
            CreateOrderRequest request = new CreateOrderRequest(
                    "",
                    List.of(new OrderItemRequest("Apple", 3, new BigDecimal("0.50")))
            );

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Validation failed"));
        }

        @Test
        @DisplayName("returns 400 when items list is empty")
        void emptyItemsReturns400() throws Exception {
            CreateOrderRequest request = new CreateOrderRequest(
                    "Andi Wijaya",
                    Collections.emptyList()
            );

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when items is null")
        void nullItemsReturns400() throws Exception {
            CreateOrderRequest request = new CreateOrderRequest("Andi Wijaya", null);

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when quantity is zero")
        void zeroQuantityReturns400() throws Exception {
            CreateOrderRequest request = new CreateOrderRequest(
                    "Andi Wijaya",
                    List.of(new OrderItemRequest("Apple", 0, new BigDecimal("0.50")))
            );

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when quantity is negative")
        void negativeQuantityReturns400() throws Exception {
            CreateOrderRequest request = new CreateOrderRequest(
                    "Andi Wijaya",
                    List.of(new OrderItemRequest("Apple", -1, new BigDecimal("0.50")))
            );

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when unitPrice is zero")
        void zeroUnitPriceReturns400() throws Exception {
            CreateOrderRequest request = new CreateOrderRequest(
                    "Andi Wijaya",
                    List.of(new OrderItemRequest("Apple", 3, BigDecimal.ZERO))
            );

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 for malformed JSON body")
        void malformedJsonReturns400() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bad Request"));
        }
    }

    // ────────────────────────────────────────────────────────────
    //  GET /api/orders/{id}
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/orders/{id}")
    class GetOrderEndpoint {

        @Test
        @DisplayName("returns 200 for existing order")
        void existingOrderReturns200() throws Exception {
            UUID orderId = UUID.randomUUID();
            when(orderService.getOrder(orderId)).thenReturn(new OrderResponse());

            mockMvc.perform(get("/api/orders/{id}", orderId))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 404 for unknown order ID")
        void unknownIdReturns404() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(orderService.getOrder(unknownId)).thenThrow(new OrderNotFoundException(unknownId));

            mockMvc.perform(get("/api/orders/{id}", unknownId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"));
        }

        @Test
        @DisplayName("returns 400 for malformed UUID")
        void malformedUuidReturns400() throws Exception {
            mockMvc.perform(get("/api/orders/{id}", "not-a-uuid"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ────────────────────────────────────────────────────────────
    //  PATCH /api/orders/{id}/status
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/orders/{id}/status")
    class TransitionStatusEndpoint {

        @Test
        @DisplayName("returns 200 for valid transition")
        void validTransitionReturns200() throws Exception {
            UUID orderId = UUID.randomUUID();
            StatusTransitionRequest req = new StatusTransitionRequest(OrderStatus.PAID, null);
            when(orderService.transitionStatus(eq(orderId), any())).thenReturn(new OrderResponse());

            mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 409 for illegal transition")
        void illegalTransitionReturns409() throws Exception {
            UUID orderId = UUID.randomUUID();
            StatusTransitionRequest req = new StatusTransitionRequest(OrderStatus.SHIPPED, null);

            when(orderService.transitionStatus(eq(orderId), any()))
                    .thenThrow(new IllegalStatusTransitionException(OrderStatus.CREATED, OrderStatus.SHIPPED));

            mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("returns 422 when cancellation lacks reason")
        void missingCancelReasonReturns422() throws Exception {
            UUID orderId = UUID.randomUUID();
            StatusTransitionRequest req = new StatusTransitionRequest(OrderStatus.CANCELLED, null);

            when(orderService.transitionStatus(eq(orderId), any()))
                    .thenThrow(new CancellationReasonRequiredException());

            mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422));
        }
    }

    // ────────────────────────────────────────────────────────────
    //  PUT /api/orders/{id}
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/orders/{id}")
    class UpdateOrderEndpoint {

        @Test
        @DisplayName("returns 409 when items are locked")
        void itemsLockedReturns409() throws Exception {
            UUID orderId = UUID.randomUUID();
            UpdateOrderRequest req = new UpdateOrderRequest(
                    "Name", List.of(new OrderItemRequest("X", 1, BigDecimal.ONE))
            );

            when(orderService.updateOrder(eq(orderId), any()))
                    .thenThrow(new ItemsNotModifiableException(OrderStatus.PAID));

            mockMvc.perform(put("/api/orders/{id}", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }

    // ────────────────────────────────────────────────────────────
    //  DELETE /api/orders/{id}
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/orders/{id}")
    class DeleteOrderEndpoint {

        @Test
        @DisplayName("returns 204 for existing order")
        void existingOrderReturns204() throws Exception {
            UUID orderId = UUID.randomUUID();

            mockMvc.perform(delete("/api/orders/{id}", orderId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("returns 404 for unknown order")
        void unknownOrderReturns404() throws Exception {
            UUID unknownId = UUID.randomUUID();
            doThrow(new OrderNotFoundException(unknownId))
                    .when(orderService).deleteOrder(unknownId);

            mockMvc.perform(delete("/api/orders/{id}", unknownId))
                    .andExpect(status().isNotFound());
        }
    }
}
