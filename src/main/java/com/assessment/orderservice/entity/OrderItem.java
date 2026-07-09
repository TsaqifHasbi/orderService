package com.assessment.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents a single line item within an {@link Order}.
 *
 * <p>This entity is always managed through its parent {@code Order} via the
 * bidirectional {@code @OneToMany} relationship. Direct persistence operations
 * should not be performed on {@code OrderItem} — use
 * {@link Order#addOrderItem(OrderItem)} instead.
 *
 * <p><strong>Design decision:</strong> {@code unitPrice} uses {@link BigDecimal}
 * with precision(10,2) to avoid floating-point rounding errors that are
 * unacceptable in financial calculations.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Column(name = "product_name", nullable = false)
    private String productName;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Unit price must be greater than 0")
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Required by JPA. Not intended for application use.
     */
    protected OrderItem() {
        // JPA requires a no-arg constructor
    }

    /**
     * Creates a new line item with the given product details.
     *
     * @param productName the name of the product; must not be blank
     * @param quantity    the number of units; must be ≥ 1
     * @param unitPrice   the price per unit; must be > 0
     */
    public OrderItem(String productName, Integer quantity, BigDecimal unitPrice) {
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // ────────────────────────────────────────────────────────────
    //  Getters — all fields are readable
    // ────────────────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public String getProductName() {
        return productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public Order getOrder() {
        return order;
    }

    // ────────────────────────────────────────────────────────────
    //  Setters — controlled mutation
    // ────────────────────────────────────────────────────────────

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    /**
     * Sets the owning {@link Order}. Package-private to ensure only
     * {@link Order#addOrderItem(OrderItem)} can maintain the bidirectional link.
     *
     * @param order the owning order
     */
    void setOrder(Order order) {
        this.order = order;
    }

    // ────────────────────────────────────────────────────────────
    //  Line-item total (convenience)
    // ────────────────────────────────────────────────────────────

    /**
     * Computes the subtotal for this line item: {@code quantity × unitPrice}.
     *
     * @return the line-item total, never {@code null}
     */
    public BigDecimal getLineTotal() {
        if (quantity == null || unitPrice == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // ────────────────────────────────────────────────────────────
    //  equals / hashCode — based on business key (id when persisted)
    // ────────────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem that = (OrderItem) o;
        // Use id only when both entities are persisted; fall back to identity
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        // Stable hash across transient → managed lifecycle
        return Objects.hash(getClass());
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + id +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                '}';
    }
}
