package com.marketplace.repository;

import com.marketplace.entity.ItemStatus;
import com.marketplace.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    Page<OrderItem> findByVendorId(Long vendorId, Pageable pageable);

    Page<OrderItem> findByVendorIdAndStatus(Long vendorId, ItemStatus status, Pageable pageable);

    /** Scoped by vendor: a vendor can only ever touch its own order lines. */
    Optional<OrderItem> findByIdAndVendorId(Long id, Long vendorId);
}
