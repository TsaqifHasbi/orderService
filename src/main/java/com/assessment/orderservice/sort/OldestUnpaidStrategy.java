package com.assessment.orderservice.sort;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Sorts orders to surface the oldest unpaid orders first.
 *
 * <p>Orders are sorted by status (so CREATED comes before PAID, etc.)
 * and then by creation time ascending (oldest first). This is useful for
 * operations teams who need to follow up on orders awaiting payment.
 */
@Component
public class OldestUnpaidStrategy implements OrderSortStrategy {

    @Override
    public String getSortKey() {
        return "oldest_unpaid";
    }

    @Override
    public Sort getSort() {
        return Sort.by(
                Sort.Order.asc("status"),
                Sort.Order.asc("createdAt")
        );
    }
}
