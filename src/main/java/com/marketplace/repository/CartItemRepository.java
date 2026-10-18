package com.marketplace.repository;

import com.marketplace.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /** For displaying the cart: product and vendor are fetched in the same query. */
    @EntityGraph(attributePaths = {"product", "product.vendor"})
    List<CartItem> findByUserIdOrderByIdAsc(Long userId);

    /**
     * For checkout: deliberately does NOT load products. They are re-read with a row lock
     * (ProductRepository.findByIdForUpdate) so the stock value used is always the committed, current one.
     */
    List<CartItem> findByUserId(Long userId);

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);
}
