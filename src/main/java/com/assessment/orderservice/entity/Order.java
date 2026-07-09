package com.assessment.orderservice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root representing a customer order in the system.
 *
 * <h3>Encapsulation guarantees</h3>
 * <ul>
 *   <li>{@code orderId} — server-generated UUID; no public setter.</li>
 *   <li>{@code totalAmount} — server-computed from line items; no public setter.
 *       Always call {@link #recalculateTotalAmount()} before persisting.</li>
 *   <li>{@code createdAt / updatedAt} — managed by Hibernate timestamps; no public setter.</li>
 *   <li>{@code status} — defaults to {@link OrderStatus#CREATED}; mutation controlled
 *       through a dedicated setter that can later enforce transition rules.</li>
 *   <li>{@code items} — exposed as an unmodifiable view; mutations only through
 *       {@link #addOrderItem(OrderItem)} and {@link #removeOrderItem(OrderItem)}
 *       to keep the bidirectional link consistent.</li>
 * </ul>
 *
 * <h3>Design decisions for Part 2 readiness</h3>
 * <ul>
 *   <li>The {@code OrderStatus} enum encodes its own transition matrix, so adding
 *       transition constraints is a one-line change in the service layer.</li>
 *   <li>Item mutation helpers can check {@code status.areItemsLocked()} to refuse
 *       changes once the order is PAID.</li>
 * </ul>
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", updatable = false, nullable = false)
    private UUID orderId;

    @NotBlank(message = "Customer name is required")
    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @NotEmpty(message = "An order must contain at least one item")
    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = jakarta.persistence.FetchType.LAZY
    )
    private List<@Valid OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.CREATED;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Required by JPA. Not intended for application use.
     */
    protected Order() {
        // JPA requires a no-arg constructor
    }

    /**
     * Constructs a new Order with the given customer name.
     * The order starts in {@link OrderStatus#CREATED} with a zero total.
     *
     * @param customerName the name of the customer placing the order
     */
    public Order(String customerName) {
        this.customerName = customerName;
    }

    // ────────────────────────────────────────────────────────────
    //  Getters — public, read-only access to all fields
    // ────────────────────────────────────────────────────────────

    /**
     * @return the server-generated order identifier; never {@code null} after persistence
     */
    public UUID getOrderId() {
        return orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    /**
     * Returns an <strong>unmodifiable</strong> view of the order's line items.
     * Use {@link #addOrderItem(OrderItem)} and {@link #removeOrderItem(OrderItem)}
     * to mutate the collection.
     *
     * @return an unmodifiable list of items
     */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public OrderStatus getStatus() {
        return status;
    }

    /**
     * @return the server-computed total; never {@code null}
     */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // ────────────────────────────────────────────────────────────
    //  Setters — only for client-editable fields
    // ────────────────────────────────────────────────────────────
    //  orderId      → NO setter (server-generated, immutable)
    //  totalAmount  → NO setter (server-computed via recalculateTotalAmount)
    //  createdAt    → NO setter (Hibernate @CreationTimestamp)
    //  updatedAt    → NO setter (Hibernate @UpdateTimestamp)

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    /**
     * Sets the order status. In Part 1 this is a simple setter; the service
     * layer (Part 2) will invoke {@link OrderStatus#canTransitionTo(OrderStatus)}
     * before calling this method.
     *
     * @param status the new status
     */
    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    // ────────────────────────────────────────────────────────────
    //  Bidirectional relationship helpers
    // ────────────────────────────────────────────────────────────

    /**
     * Adds a line item to this order and maintains the bidirectional link.
     *
     * <p>This is the <strong>only</strong> correct way to add items.
     * Directly modifying the {@code items} list is prevented by returning
     * an unmodifiable view from {@link #getItems()}.
     *
     * @param item the item to add; must not be {@code null}
     * @throws NullPointerException if {@code item} is null
     */
    public void addOrderItem(OrderItem item) {
        Objects.requireNonNull(item, "OrderItem must not be null");
        items.add(item);
        item.setOrder(this);
    }

    /**
     * Removes a line item from this order and clears the bidirectional link.
     *
     * @param item the item to remove
     * @return {@code true} if the item was found and removed
     */
    public boolean removeOrderItem(OrderItem item) {
        boolean removed = items.remove(item);
        if (removed) {
            item.setOrder(null);
        }
        return removed;
    }

    // ────────────────────────────────────────────────────────────
    //  Server-side total calculation
    // ────────────────────────────────────────────────────────────

    /**
     * Recalculates {@code totalAmount} from the current line items.
     *
     * <p>Uses {@link BigDecimal} arithmetic throughout to guarantee
     * precision for financial calculations. This method <strong>must</strong>
     * be called by the service layer before every persist/merge operation
     * to ensure the stored total is always consistent with the items.
     *
     * <p>This deliberate design — computing the total in the entity rather than
     * accepting it from the client — protects against price-manipulation attacks
     * where a malicious client submits a lower {@code totalAmount}.
     */
    public void recalculateTotalAmount() {
        this.totalAmount = items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ────────────────────────────────────────────────────────────
    //  equals / hashCode
    // ────────────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        // Use the natural key (orderId) only when both are persisted
        return orderId != null && orderId.equals(order.orderId);
    }

    @Override
    public int hashCode() {
        // Constant hash guarantees stability across the transient → managed lifecycle.
        // For aggregate roots with a surrogate key, this is the Vlad Mihalcea pattern.
        return Objects.hash(getClass());
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", customerName='" + customerName + '\'' +
                ", status=" + status +
                ", totalAmount=" + totalAmount +
                ", itemCount=" + items.size() +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
