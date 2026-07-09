package com.assessment.orderservice.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link OrderStatus} state machine.
 */
class OrderStatusTest {

    @Nested
    @DisplayName("Transition rules")
    class TransitionRules {

        @Test
        @DisplayName("CREATED can transition to PAID and CANCELLED only")
        void createdTransitions() {
            assertEquals(Set.of(OrderStatus.PAID, OrderStatus.CANCELLED),
                    OrderStatus.CREATED.allowedTransitions());
        }

        @Test
        @DisplayName("PAID can transition to SHIPPED and CANCELLED only")
        void paidTransitions() {
            assertEquals(Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
                    OrderStatus.PAID.allowedTransitions());
        }

        @Test
        @DisplayName("SHIPPED can transition to DELIVERED only")
        void shippedTransitions() {
            assertEquals(Set.of(OrderStatus.DELIVERED),
                    OrderStatus.SHIPPED.allowedTransitions());
        }

        @Test
        @DisplayName("DELIVERED is terminal — no transitions allowed")
        void deliveredIsTerminal() {
            assertTrue(OrderStatus.DELIVERED.allowedTransitions().isEmpty());
            assertTrue(OrderStatus.DELIVERED.isTerminal());
        }

        @Test
        @DisplayName("CANCELLED is terminal — no transitions allowed")
        void cancelledIsTerminal() {
            assertTrue(OrderStatus.CANCELLED.allowedTransitions().isEmpty());
            assertTrue(OrderStatus.CANCELLED.isTerminal());
        }

        @Test
        @DisplayName("CREATED → SHIPPED is illegal")
        void createdToShippedIllegal() {
            assertFalse(OrderStatus.CREATED.canTransitionTo(OrderStatus.SHIPPED));
        }

        @Test
        @DisplayName("CREATED → DELIVERED is illegal")
        void createdToDeliveredIllegal() {
            assertFalse(OrderStatus.CREATED.canTransitionTo(OrderStatus.DELIVERED));
        }

        @Test
        @DisplayName("DELIVERED → CREATED is illegal (no reactivation)")
        void deliveredToCreatedIllegal() {
            assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CREATED));
        }

        @Test
        @DisplayName("CANCELLED → PAID is illegal (no reactivation)")
        void cancelledToPaidIllegal() {
            assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PAID));
        }
    }

    @Nested
    @DisplayName("Item locking rules")
    class ItemLocking {

        @Test
        @DisplayName("Items are editable when CREATED")
        void createdItemsNotLocked() {
            assertFalse(OrderStatus.CREATED.areItemsLocked());
        }

        @Test
        @DisplayName("Items are locked when PAID")
        void paidItemsLocked() {
            assertTrue(OrderStatus.PAID.areItemsLocked());
        }

        @Test
        @DisplayName("Items are locked when SHIPPED")
        void shippedItemsLocked() {
            assertTrue(OrderStatus.SHIPPED.areItemsLocked());
        }

        @Test
        @DisplayName("Items are locked when DELIVERED")
        void deliveredItemsLocked() {
            assertTrue(OrderStatus.DELIVERED.areItemsLocked());
        }

        @Test
        @DisplayName("Items are locked when CANCELLED")
        void cancelledItemsLocked() {
            assertTrue(OrderStatus.CANCELLED.areItemsLocked());
        }
    }
}
