package com.assessment.orderservice.sort;

import org.springframework.data.domain.Sort;

/**
 * Strategy interface for defining order sorting rules.
 *
 * <p>Implements the <strong>Strategy Pattern</strong> so that new sorting rules
 * can be added by creating a new implementation and registering it as a Spring bean,
 * without modifying existing strategies or the service layer.
 *
 * <p>Each implementation declares its own key (e.g., "newest", "highest_total")
 * which is matched against the {@code sort} query parameter.
 */
public interface OrderSortStrategy {

    /**
     * Returns the unique key that identifies this sort strategy in API requests.
     * This key is used as the value of the {@code sort} query parameter.
     *
     * @return the sort key (e.g., "newest", "highest_total", "oldest_unpaid")
     */
    String getSortKey();

    /**
     * Returns the Spring Data {@link Sort} specification for this strategy.
     *
     * @return the sort definition
     */
    Sort getSort();
}
