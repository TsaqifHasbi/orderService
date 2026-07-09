package com.assessment.orderservice.sort;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Sorts orders by creation timestamp, newest first.
 */
@Component
public class NewestFirstStrategy implements OrderSortStrategy {

    @Override
    public String getSortKey() {
        return "newest";
    }

    @Override
    public Sort getSort() {
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
