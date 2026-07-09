package com.assessment.orderservice.entity;

import java.util.Set;

/**
 * Represents the lifecycle status of an {@link Order}.
 *
 * <p>Legal transitions follow a directed acyclic graph:
 * <pre>
 *   CREATED  → PAID → SHIPPED → DELIVERED
 *      ↓        ↓
 *   CANCELLED  CANCELLED
 * </pre>
 *
 * <p>Each enum constant declares its own set of reachable statuses,
 * making illegal transitions impossible at the domain level.
 * Adding a new status only requires adding a constant — existing
 * transition rules remain untouched.
 */
public enum OrderStatus {

    CREATED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(PAID, CANCELLED);
        }
    },
    PAID {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(SHIPPED, CANCELLED);
        }
    },
    SHIPPED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(DELIVERED);
        }
    },
    DELIVERED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of();
        }
    },
    CANCELLED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of();
        }
    };

    /**
     * Returns the set of statuses that this status may legally transition to.
     *
     * @return an unmodifiable set of valid target statuses; empty if terminal
     */
    public abstract Set<OrderStatus> allowedTransitions();

    /**
     * Checks whether transitioning from this status to {@code target} is legal.
     *
     * @param target the desired next status
     * @return {@code true} if the transition is permitted
     */
    public boolean canTransitionTo(OrderStatus target) {
        return allowedTransitions().contains(target);
    }

    /**
     * Returns {@code true} if this status represents a terminal (immutable) state
     * from which no further transitions are possible.
     *
     * @return {@code true} for DELIVERED and CANCELLED
     */
    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }

    /**
     * Returns {@code true} if line items are locked and may no longer be modified.
     * Per business rules, items become immutable once the order is PAID or beyond.
     *
     * @return {@code true} if items are frozen
     */
    public boolean areItemsLocked() {
        return this != CREATED;
    }
}
