package com.marketplace.repository;

import com.marketplace.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    /** Scoped by customer: asking for someone else's order id looks exactly like "not found". */
    Optional<Order> findByIdAndCustomerId(Long id, Long customerId);
}
