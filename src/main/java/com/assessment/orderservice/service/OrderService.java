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
import com.assessment.orderservice.sort.OrderSortStrategy;
import com.assessment.orderservice.sort.OrderSortStrategyRegistry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Core business logic for order management.
 *
 * <p>All domain invariants are enforced here:
 * <ul>
 *   <li>Total amount is always server-computed before persist</li>
 *   <li>Status transitions are validated against the {@link OrderStatus} state machine</li>
 *   <li>Items are immutable once the order is PAID or beyond</li>
 *   <li>Cancellation requires a reason</li>
 * </ul>
 *
 * <p>The service layer is the single authority for mutation rules.
 * The controller layer handles HTTP concerns only.
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderSortStrategyRegistry sortRegistry;

    public OrderService(OrderRepository orderRepository,
                        OrderSortStrategyRegistry sortRegistry) {
        this.orderRepository = orderRepository;
        this.sortRegistry = sortRegistry;
    }

    // ────────────────────────────────────────────────────────────
    //  CREATE
    // ────────────────────────────────────────────────────────────

    /**
     * Creates a new order from the given request.
     *
     * <p>The order is created with status CREATED and the total is computed
     * from the line items. The client cannot influence orderId, status,
     * totalAmount, or timestamps.
     *
     * @param request the creation request
     * @return the persisted order as a response DTO
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order(request.getCustomerName());

        for (OrderItemRequest itemReq : request.getItems()) {
            OrderItem item = new OrderItem(
                    itemReq.getProductName(),
                    itemReq.getQuantity(),
                    itemReq.getUnitPrice()
            );
            order.addOrderItem(item);
        }

        order.recalculateTotalAmount();

        Order saved = orderRepository.save(order);
        return OrderResponse.fromEntity(saved);
    }

    // ────────────────────────────────────────────────────────────
    //  READ (single)
    // ────────────────────────────────────────────────────────────

    /**
     * Retrieves a single order by its ID.
     *
     * @param orderId the order UUID
     * @return the order response DTO
     * @throws OrderNotFoundException if the order does not exist
     */
    public OrderResponse getOrder(UUID orderId) {
        Order order = findOrderOrThrow(orderId);
        return OrderResponse.fromEntity(order);
    }

    // ────────────────────────────────────────────────────────────
    //  READ (list — paginated, sorted)
    // ────────────────────────────────────────────────────────────

    /**
     * Lists orders with pagination and a pluggable sort strategy.
     *
     * @param page    zero-based page index
     * @param size    page size
     * @param sortKey the sort strategy key (e.g., "newest", "highest_total")
     * @return a page of order response DTOs
     * @throws IllegalArgumentException if the sort key is not recognised
     */
    public Page<OrderResponse> listOrders(int page, int size, String sortKey) {
        Sort sort = resolveSort(sortKey);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        return orderRepository.findAll(pageRequest)
                .map(OrderResponse::fromEntity);
    }

    // ────────────────────────────────────────────────────────────
    //  UPDATE
    // ────────────────────────────────────────────────────────────

    /**
     * Updates an existing order's editable fields (customerName, items).
     *
     * <p><strong>Part 2 rule:</strong> If the order's items are locked
     * (status ≥ PAID), only {@code customerName} may be changed.
     * Attempting to modify items will throw {@link ItemsNotModifiableException}.
     *
     * @param orderId the order UUID
     * @param request the update request
     * @return the updated order as a response DTO
     * @throws OrderNotFoundException     if the order does not exist
     * @throws ItemsNotModifiableException if items are locked
     */
    @Transactional
    public OrderResponse updateOrder(UUID orderId, UpdateOrderRequest request) {
        Order order = findOrderOrThrow(orderId);

        // Always allow customerName updates
        order.setCustomerName(request.getCustomerName());

        // Guard: items are immutable after PAID
        if (order.getStatus().areItemsLocked()) {
            // Check if the client is actually trying to change items
            if (hasItemChanges(order, request.getItems())) {
                throw new ItemsNotModifiableException(order.getStatus());
            }
        } else {
            // Replace all items: clear existing, add new ones
            replaceItems(order, request.getItems());
            order.recalculateTotalAmount();
        }

        Order saved = orderRepository.save(order);
        return OrderResponse.fromEntity(saved);
    }

    // ────────────────────────────────────────────────────────────
    //  STATUS TRANSITION
    // ────────────────────────────────────────────────────────────

    /**
     * Transitions an order to a new status.
     *
     * <p>Validates:
     * <ul>
     *   <li>The transition is legal per the {@link OrderStatus} state machine</li>
     *   <li>Cancellation includes a non-blank reason</li>
     * </ul>
     *
     * @param orderId the order UUID
     * @param request the transition request (target status + optional metadata)
     * @return the updated order as a response DTO
     * @throws OrderNotFoundException            if the order does not exist
     * @throws IllegalStatusTransitionException   if the transition is not allowed
     * @throws CancellationReasonRequiredException if cancelling without a reason
     */
    @Transactional
    public OrderResponse transitionStatus(UUID orderId, StatusTransitionRequest request) {
        Order order = findOrderOrThrow(orderId);
        OrderStatus currentStatus = order.getStatus();
        OrderStatus targetStatus = request.getStatus();

        // Validate transition legality
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new IllegalStatusTransitionException(currentStatus, targetStatus);
        }

        // Validate transition-specific data requirements
        if (targetStatus == OrderStatus.CANCELLED) {
            if (request.getReason() == null || request.getReason().isBlank()) {
                throw new CancellationReasonRequiredException();
            }
        }

        order.setStatus(targetStatus);
        Order saved = orderRepository.save(order);
        return OrderResponse.fromEntity(saved);
    }

    // ────────────────────────────────────────────────────────────
    //  DELETE
    // ────────────────────────────────────────────────────────────

    /**
     * Deletes an order by its ID.
     *
     * @param orderId the order UUID
     * @throws OrderNotFoundException if the order does not exist
     */
    @Transactional
    public void deleteOrder(UUID orderId) {
        Order order = findOrderOrThrow(orderId);
        orderRepository.delete(order);
    }

    // ────────────────────────────────────────────────────────────
    //  Private helpers
    // ────────────────────────────────────────────────────────────

    private Order findOrderOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private Sort resolveSort(String sortKey) {
        final String key = (sortKey == null || sortKey.isBlank()) ? "newest" : sortKey;
        return sortRegistry.findByKey(key)
                .map(OrderSortStrategy::getSort)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown sort key: '" + key + "'. Available: " + sortRegistry.availableKeys()));
    }

    /**
     * Replaces all items on the order with new ones from the request.
     * Uses the aggregate root's helper methods to maintain bidirectional sync.
     */
    private void replaceItems(Order order, List<OrderItemRequest> itemRequests) {
        // Clear existing items (orphanRemoval handles DB deletion)
        order.getItems().forEach(item -> {}); // force init of lazy collection
        List<OrderItem> currentItems = new java.util.ArrayList<>(order.getItems());
        currentItems.forEach(order::removeOrderItem);

        // Add new items
        for (OrderItemRequest itemReq : itemRequests) {
            OrderItem item = new OrderItem(
                    itemReq.getProductName(),
                    itemReq.getQuantity(),
                    itemReq.getUnitPrice()
            );
            order.addOrderItem(item);
        }
    }

    /**
     * Detects whether the update request contains item changes compared
     * to the current order. Used to provide a clear error when items are locked.
     */
    private boolean hasItemChanges(Order order, List<OrderItemRequest> requestItems) {
        List<OrderItem> current = order.getItems();
        if (current.size() != requestItems.size()) {
            return true;
        }
        for (int i = 0; i < current.size(); i++) {
            OrderItem existing = current.get(i);
            OrderItemRequest incoming = requestItems.get(i);
            if (!existing.getProductName().equals(incoming.getProductName())
                    || !existing.getQuantity().equals(incoming.getQuantity())
                    || existing.getUnitPrice().compareTo(incoming.getUnitPrice()) != 0) {
                return true;
            }
        }
        return false;
    }
}
