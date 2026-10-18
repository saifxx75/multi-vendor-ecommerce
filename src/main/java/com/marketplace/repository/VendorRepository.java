package com.marketplace.repository;

import com.marketplace.entity.Vendor;
import com.marketplace.entity.VendorStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    Optional<Vendor> findByUserId(Long userId);

    boolean existsByStoreNameIgnoreCase(String storeName);

    // Fetch the owning user in the same query: the admin list shows owner name/email.
    @EntityGraph(attributePaths = "user")
    List<Vendor> findByStatus(VendorStatus status);

    @Override
    @EntityGraph(attributePaths = "user")
    List<Vendor> findAll();
}
