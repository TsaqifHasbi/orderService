package com.assessment.orderservice.sort;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Sorts orders by total amount, highest first.
 */
@Component
public class HighestTotalStrategy implements OrderSortStrategy {

    @Override
    public String getSortKey() {
        return "highest_total";
    }

    @Override
    public Sort getSort() {
        return Sort.by(Sort.Direction.DESC, "totalAmount");
    }
}
