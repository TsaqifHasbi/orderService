package com.assessment.orderservice.repository;

import com.assessment.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link Order} aggregate.
 *
 * <p>Inherits the full set of CRUD and paging/sorting operations from
 * {@link JpaRepository}. Custom query methods will be added as the
 * service layer evolves (e.g., filtering by status, date ranges,
 * or custom sort strategies for Part 2).
 *
 * <p>Note: {@code OrderItem} does not need its own repository because
 * it is always managed through the {@code Order} aggregate root via
 * {@code CascadeType.ALL} and {@code orphanRemoval = true}.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
}
