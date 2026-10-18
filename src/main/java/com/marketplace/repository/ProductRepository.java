package com.marketplace.repository;

import com.marketplace.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByIdAndActiveTrue(Long id);

    Optional<Product> findByIdAndVendorId(Long id, Long vendorId);

    Page<Product> findByVendorId(Long vendorId, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    /**
     * Loads the product row with SELECT ... FOR UPDATE. Used while changing stock (checkout, cancel)
     * so two concurrent buyers can never both take the last unit (no overselling).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
