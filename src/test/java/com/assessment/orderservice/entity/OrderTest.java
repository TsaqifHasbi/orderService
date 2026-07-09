package com.assessment.orderservice.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Order} aggregate root.
 * Tests domain logic without Spring context (pure unit tests).
 */
class OrderTest {

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order("Test Customer");
    }

    @Nested
    @DisplayName("Construction and defaults")
    class Construction {

        @Test
        @DisplayName("New order has CREATED status by default")
        void defaultStatus() {
            assertEquals(OrderStatus.CREATED, order.getStatus());
        }

        @Test
        @DisplayName("New order has zero total by default")
        void defaultTotal() {
            assertEquals(BigDecimal.ZERO, order.getTotalAmount());
        }

        @Test
        @DisplayName("New order has empty items list")
        void defaultEmptyItems() {
            assertTrue(order.getItems().isEmpty());
        }

        @Test
        @DisplayName("Customer name is set from constructor")
        void customerNameSet() {
            assertEquals("Test Customer", order.getCustomerName());
        }
    }

    @Nested
    @DisplayName("Item management")
    class ItemManagement {

        @Test
        @DisplayName("addOrderItem adds item and sets bidirectional link")
        void addItemSetsBidirectionalLink() {
            OrderItem item = new OrderItem("Apple", 3, new BigDecimal("0.50"));
            order.addOrderItem(item);

            assertEquals(1, order.getItems().size());
            assertSame(order, item.getOrder());
        }

        @Test
        @DisplayName("addOrderItem rejects null")
        void addNullItemThrows() {
            assertThrows(NullPointerException.class, () -> order.addOrderItem(null));
        }

        @Test
        @DisplayName("removeOrderItem removes item and clears back-reference")
        void removeItemClearsLink() {
            OrderItem item = new OrderItem("Apple", 3, new BigDecimal("0.50"));
            order.addOrderItem(item);

            boolean removed = order.removeOrderItem(item);

            assertTrue(removed);
            assertTrue(order.getItems().isEmpty());
            assertNull(item.getOrder());
        }

        @Test
        @DisplayName("removeOrderItem returns false for unknown item")
        void removeUnknownItemReturnsFalse() {
            OrderItem item = new OrderItem("Apple", 3, new BigDecimal("0.50"));
            assertFalse(order.removeOrderItem(item));
        }

        @Test
        @DisplayName("getItems returns unmodifiable list")
        void itemsListIsUnmodifiable() {
            OrderItem item = new OrderItem("Apple", 3, new BigDecimal("0.50"));
            order.addOrderItem(item);

            assertThrows(UnsupportedOperationException.class,
                    () -> order.getItems().add(new OrderItem("Hack", 1, BigDecimal.ONE)));
        }
    }

    @Nested
    @DisplayName("Total amount calculation")
    class TotalCalculation {

        @Test
        @DisplayName("recalculateTotalAmount computes sum of line totals")
        void computesCorrectTotal() {
            order.addOrderItem(new OrderItem("Apple", 3, new BigDecimal("0.50")));    // 1.50
            order.addOrderItem(new OrderItem("Bread", 1, new BigDecimal("2.20")));    // 2.20

            order.recalculateTotalAmount();

            assertEquals(new BigDecimal("3.70"), order.getTotalAmount());
        }

        @Test
        @DisplayName("recalculateTotalAmount handles single item")
        void singleItem() {
            order.addOrderItem(new OrderItem("Milk", 2, new BigDecimal("3.00")));

            order.recalculateTotalAmount();

            assertEquals(new BigDecimal("6.00"), order.getTotalAmount());
        }

        @Test
        @DisplayName("recalculateTotalAmount returns zero for empty items")
        void emptyItemsZeroTotal() {
            order.recalculateTotalAmount();
            assertEquals(BigDecimal.ZERO, order.getTotalAmount());
        }

        @Test
        @DisplayName("recalculateTotalAmount updates after item removal")
        void updatesAfterRemoval() {
            OrderItem apple = new OrderItem("Apple", 3, new BigDecimal("0.50"));
            OrderItem bread = new OrderItem("Bread", 1, new BigDecimal("2.20"));
            order.addOrderItem(apple);
            order.addOrderItem(bread);

            order.recalculateTotalAmount();
            assertEquals(new BigDecimal("3.70"), order.getTotalAmount());

            order.removeOrderItem(bread);
            order.recalculateTotalAmount();
            assertEquals(new BigDecimal("1.50"), order.getTotalAmount());
        }
    }
}
