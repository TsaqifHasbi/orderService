package com.assessment.orderservice.sort;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry that collects all {@link OrderSortStrategy} beans and provides
 * lookup by sort key.
 *
 * <p>This implements the <strong>Registry Pattern</strong> backed by Spring's
 * dependency injection. Adding a new sort strategy requires only:
 * <ol>
 *   <li>Creating a new {@code @Component} class implementing {@link OrderSortStrategy}</li>
 *   <li>That's it — Spring auto-discovers it, and this registry indexes it</li>
 * </ol>
 *
 * <p>No existing code needs to be modified.
 */
@Component
public class OrderSortStrategyRegistry {

    private final Map<String, OrderSortStrategy> strategies;

    /**
     * Spring injects all {@link OrderSortStrategy} beans via constructor injection.
     *
     * @param strategyList all registered sort strategies
     */
    public OrderSortStrategyRegistry(List<OrderSortStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        OrderSortStrategy::getSortKey,
                        Function.identity()
                ));
    }

    /**
     * Looks up a sort strategy by its key.
     *
     * @param sortKey the sort key from the query parameter
     * @return the strategy, or empty if the key is not recognised
     */
    public Optional<OrderSortStrategy> findByKey(String sortKey) {
        return Optional.ofNullable(strategies.get(sortKey));
    }

    /**
     * Returns all registered sort keys, useful for error messages.
     *
     * @return an unmodifiable set of available sort keys
     */
    public java.util.Set<String> availableKeys() {
        return java.util.Collections.unmodifiableSet(strategies.keySet());
    }
}
