package com.assessment.orderservice.service;

import com.assessment.orderservice.dto.CreateOrderRequest;
import com.assessment.orderservice.dto.OrderItemRequest;
import com.assessment.orderservice.dto.OrderResponse;
import com.assessment.orderservice.dto.StatusTransitionRequest;
import com.assessment.orderservice.dto.UpdateOrderRequest;
import com.assessment.orderservice.entity.Order;
import com.assessment.orderservice.entity.OrderItem;
import com.assessment.orderservice.entity.OrderStatus;
import com.assessment.orderservice.exception.CancellationReasonRequiredException;
import com.assessment.orderservice.exception.IllegalStatusTransitionException;
import com.assessment.orderservice.exception.ItemsNotModifiableException;
import com.assessment.orderservice.exception.OrderNotFoundException;
import com.assessment.orderservice.repository.OrderRepository;
import com.assessment.orderservice.sort.NewestFirstStrategy;
import com.assessment.orderservice.sort.OrderSortStrategyRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OrderService}.
 *
 * <p>Uses Mockito to isolate service logic from the database.
 * Covers success paths AND failure paths per assessment requirement.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderSortStrategyRegistry sortRegistry;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest validCreateRequest;
    private UUID existingOrderId;

    @BeforeEach
    void setUp() {
        existingOrderId = UUID.randomUUID();

        validCreateRequest = new CreateOrderRequest(
                "Andi Wijaya",
                List.of(
                        new OrderItemRequest("Apple", 3, new BigDecimal("0.50")),
                        new OrderItemRequest("Bread Loaf", 1, new BigDecimal("2.20"))
                )
        );
    }

    // ────────────────────────────────────────────────────────────
    //  CREATE
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("creates order with server-computed total")
        void computesTotalFromItems() {
            when(orderRepository.save(any(Order.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            OrderResponse response = orderService.createOrder(validCreateRequest);

            assertEquals("Andi Wijaya", response.getCustomerName());
            assertEquals(OrderStatus.CREATED, response.getStatus());
            assertEquals(new BigDecimal("3.70"), response.getTotalAmount());
            assertEquals(2, response.getItems().size());
        }

        @Test
        @DisplayName("persists order via repository")
        void savesViaRepository() {
            when(orderRepository.save(any(Order.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            orderService.createOrder(validCreateRequest);

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(captor.capture());

            Order saved = captor.getValue();
            assertEquals("Andi Wijaya", saved.getCustomerName());
            assertEquals(2, saved.getItems().size());
        }

        @Test
        @DisplayName("ignores any client-supplied total — always recomputes")
        void alwaysRecomputesTotal() {
            // Even if someone managed to pass a total, the service recomputes it
            when(orderRepository.save(any(Order.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            OrderResponse response = orderService.createOrder(validCreateRequest);

            // 3 * 0.50 + 1 * 2.20 = 3.70
            assertEquals(new BigDecimal("3.70"), response.getTotalAmount());
        }
    }

    // ────────────────────────────────────────────────────────────
    //  READ (single)
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrder")
    class GetOrder {

        @Test
        @DisplayName("returns order when found")
        void returnsOrderWhenFound() {
            Order order = createPersistedOrder();
            when(orderRepository.findById(existingOrderId)).thenReturn(Optional.of(order));

            OrderResponse response = orderService.getOrder(existingOrderId);

            assertEquals("Andi Wijaya", response.getCustomerName());
        }

        @Test
        @DisplayName("throws OrderNotFoundException for unknown ID")
        void throwsNotFoundForUnknownId() {
            UUID unknownId = UUID.randomUUID();
            when(orderRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThrows(OrderNotFoundException.class, () -> orderService.getOrder(unknownId));
        }
    }

    // ────────────────────────────────────────────────────────────
    //  READ (list)
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listOrders")
    class ListOrders {

        @Test
        @DisplayName("returns paginated results with sort strategy")
        void returnsPaginatedResults() {
            Order order = createPersistedOrder();
            Page<Order> page = new PageImpl<>(List.of(order));

            when(sortRegistry.findByKey("newest"))
                    .thenReturn(Optional.of(new NewestFirstStrategy()));
            when(orderRepository.findAll(any(Pageable.class))).thenReturn(page);

            Page<OrderResponse> result = orderService.listOrders(0, 20, "newest");

            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("throws IllegalArgumentException for unknown sort key")
        void throwsForUnknownSortKey() {
            when(sortRegistry.findByKey("nonexistent")).thenReturn(Optional.empty());
            when(sortRegistry.availableKeys()).thenReturn(java.util.Set.of("newest", "highest_total"));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> orderService.listOrders(0, 20, "nonexistent"));

            assertTrue(ex.getMessage().contains("nonexistent"));
        }

        @Test
        @DisplayName("defaults to 'newest' when sort key is blank")
        void defaultsToNewest() {
            Order order = createPersistedOrder();
            Page<Order> page = new PageImpl<>(List.of(order));

            when(sortRegistry.findByKey("newest"))
                    .thenReturn(Optional.of(new NewestFirstStrategy()));
            when(orderRepository.findAll(any(Pageable.class))).thenReturn(page);

            Page<OrderResponse> result = orderService.listOrders(0, 20, "");

            verify(sortRegistry).findByKey("newest");
        }
    }

    // ────────────────────────────────────────────────────────────
    //  UPDATE
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateOrder")
    class UpdateOrder {

        @Test
        @DisplayName("updates customerName and items when status is CREATED")
        void updatesWhenCreated() {
            Order order = createPersistedOrder();
            when(orderRepository.findById(existingOrderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UpdateOrderRequest updateReq = new UpdateOrderRequest(
                    "Updated Name",
                    List.of(new OrderItemRequest("Milk", 2, new BigDecimal("3.00")))
            );

            OrderResponse response = orderService.updateOrder(existingOrderId, updateReq);

            assertEquals("Updated Name", response.getCustomerName());
            assertEquals(1, response.getItems().size());
            assertEquals(new BigDecimal("6.00"), response.getTotalAmount());
        }

        @Test
        @DisplayName("throws ItemsNotModifiableException when items changed after PAID")
        void rejectsItemChangesWhenPaid() {
            Order order = createPersistedOrder();
            order.setStatus(OrderStatus.PAID);
            when(orderRepository.findById(existingOrderId)).thenReturn(Optional.of(order));

            UpdateOrderRequest updateReq = new UpdateOrderRequest(
                    "Andi Wijaya",
                    List.of(new OrderItemRequest("Different Item", 5, new BigDecimal("1.00")))
            );

            assertThrows(ItemsNotModifiableException.class,
                    () -> orderService.updateOrder(existingOrderId, updateReq));
        }

        @Test
        @DisplayName("allows customerName update when items are locked")
        void allowsNameUpdateWhenPaid() {
            Order order = createPersistedOrder();
            order.setStatus(OrderStatus.PAID);
            when(orderRepository.findById(existingOrderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Same items, different name
            UpdateOrderRequest updateReq = new UpdateOrderRequest(
                    "Updated Name",
                    List.of(
                            new OrderItemRequest("Apple", 3, new BigDecimal("0.50")),
                            new OrderItemRequest("Bread Loaf", 1, new BigDecimal("2.20"))
                    )
            );

            OrderResponse response = orderService.updateOrder(existingOrderId, updateReq);
            assertEquals("Updated Name", response.getCustomerName());
        }

        @Test
        @DisplayName("throws OrderNotFoundException for unknown ID")
        void throwsNotFoundOnUpdate() {
            UUID unknownId = UUID.randomUUID();
            when(orderRepository.findById(unknownId)).thenReturn(Optional.empty());

            UpdateOrderRequest updateReq = new UpdateOrderRequest(
                    "Name", List.of(new OrderItemRequest("A", 1, BigDecimal.ONE))
            );

            assertThrows(OrderNotFoundException.class,
                    () -> orderService.updateOrder(unknownId, updateReq));
        }
    }

    // ────────────────────────────────────────────────────────────
    //  STATUS TRANSITION
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("transitionStatus")
    class TransitionStatus {

        @Test
        @DisplayName("CREATED → PAID succeeds")
        void createdToPaid() {
            Order order = createPersistedOrder();
            when(orderRepository.findById(existingOrderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            StatusTransitionRequest req = new StatusTransitionRequest(OrderStatus.PAID, null);
            OrderResponse response = orderService.transitionStatus(existingOrderId, req);

            assertEquals(OrderStatus.PAID, response.getStatus());
        }

        @Test
        @DisplayName("CREATED → SHIPPED is rejected")
        void createdToShippedRejected() {
            Order order = createPersistedOrder();
            when(orderRepository.findById(existingOrderId)).thenReturn(Optional.of(order));

            StatusTransitionRequest req = new StatusTransitionRequest(OrderStatus.SHIPPED, null);

            assertThrows(IllegalStatusTransitionException.class,
                    () -> orderService.transitionStatus(existingOrderId, req));
        }

        @Test
        @DisplayName("CANCELLED → PAID is rejected (no reactivation)")
        void cancelledToPaidRejected() {
            Order order = createPersistedOrder();
            order.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById(existingOrderId)).thenReturn(Optional.of(order));

            StatusTransitionRequest req = new StatusTransitionRequest(OrderStatus.PAID, null);

            assertThrows(IllegalStatusTransitionException.class,
                    () -> orderService.transitionStatus(existingOrderId, req));
        }

        @Test
        @DisplayName("DELIVERED → SHIPPED is rejected")
        void deliveredToShippedRejected() {
            Order order = createPersistedOrder();
            order.setStatus(OrderStatus.DELIVERED);
            when(orderRepository.findById(existingOrderId)).thenReturn(Optional.of(order));

            StatusTransitionRequest req = new StatusTransitionRequest(OrderStatus.SHIPPED, null);

            assertThrows(IllegalStatusTransitionException.class,
                    () -> orderService.transitionStatus(existingOrderId, req));
        }

        @Test
        @DisplayName("Cancellation without reason is rejected")
        void cancellationWithoutReasonRejected() {
            Order order = createPersistedOrder();
            when(orderRepository.findById(existingOrderId)).thenReturn(Optional.of(order));

            StatusTransitionRequest req = new StatusTransitionRequest(OrderStatus.CANCELLED, null);

            assertThrows(CancellationReasonRequiredException.class,
                    () -> orderService.transitionStatus(existingOrderId, req));
        }

        @Test
        @DisplayName("Cancellation with blank reason is rejected")
        void cancellationWithBlankReasonRejected() {
            Order order = createPersistedOrder();
            when(orderRepository.findById(existingOrderId)).thenReturn(Optional.of(order));

            StatusTransitionRequest req = new StatusTransitionRequest(OrderStatus.CANCELLED, "   ");

            assertThrows(CancellationReasonRequiredException.class,
                    () -> orderService.transitionStatus(existingOrderId, req));
        }

        @Test
        @DisplayName("Cancellation with valid reason succeeds")
        void cancellationWithReasonSucceeds() {
            Order order = createPersistedOrder();
            when(orderRepository.findById(existingOrderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            StatusTransitionRequest req = new StatusTransitionRequest(
                    OrderStatus.CANCELLED, "Customer changed their mind");

            OrderResponse response = orderService.transitionStatus(existingOrderId, req);
            assertEquals(OrderStatus.CANCELLED, response.getStatus());
        }
    }

    // ────────────────────────────────────────────────────────────
    //  DELETE
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteOrder")
    class DeleteOrder {

        @Test
        @DisplayName("deletes existing order")
        void deletesExistingOrder() {
            Order order = createPersistedOrder();
            when(orderRepository.findById(existingOrderId)).thenReturn(Optional.of(order));

            orderService.deleteOrder(existingOrderId);

            verify(orderRepository).delete(order);
        }

        @Test
        @DisplayName("throws OrderNotFoundException for unknown ID")
        void throwsNotFoundOnDelete() {
            UUID unknownId = UUID.randomUUID();
            when(orderRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThrows(OrderNotFoundException.class, () -> orderService.deleteOrder(unknownId));
        }
    }

    // ────────────────────────────────────────────────────────────
    //  Helper
    // ────────────────────────────────────────────────────────────

    private Order createPersistedOrder() {
        Order order = new Order("Andi Wijaya");
        order.addOrderItem(new OrderItem("Apple", 3, new BigDecimal("0.50")));
        order.addOrderItem(new OrderItem("Bread Loaf", 1, new BigDecimal("2.20")));
        order.recalculateTotalAmount();

        // Simulate JPA assigning an ID via reflection
        try {
            var field = Order.class.getDeclaredField("orderId");
            field.setAccessible(true);
            field.set(order, existingOrderId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set orderId via reflection", e);
        }

        return order;
    }
}
