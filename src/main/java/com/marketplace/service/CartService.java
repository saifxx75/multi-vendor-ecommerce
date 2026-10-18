package com.marketplace.service;

import com.marketplace.dto.CartItemRequest;
import com.marketplace.dto.CartItemResponse;
import com.marketplace.dto.CartResponse;
import com.marketplace.entity.CartItem;
import com.marketplace.entity.Product;
import com.marketplace.exception.BadRequestException;
import com.marketplace.exception.ResourceNotFoundException;
import com.marketplace.repository.CartItemRepository;
import com.marketplace.repository.ProductRepository;
import com.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * The cart is persisted per customer. Stock is checked when adding (fast feedback) and checked
 * again, under a row lock, at checkout (the authoritative check).
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return buildCart(userId);
    }

    @Transactional
    public CartResponse addItem(Long userId, CartItemRequest request) {
        Product product = findAvailableProduct(request.productId());
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, product.getId()).orElse(null);

        int newQuantity = (item == null ? 0 : item.getQuantity()) + request.quantity();
        ensureStock(product, newQuantity);

        if (item == null) {
            item = new CartItem();
            item.setUser(userRepository.getReferenceById(userId));
            item.setProduct(product);
        }
        item.setQuantity(newQuantity);
        cartItemRepository.save(item);
        return buildCart(userId);
    }

    @Transactional
    public CartResponse updateQuantity(Long userId, Long productId, int quantity) {
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product is not in your cart"));
        ensureStock(findAvailableProduct(productId), quantity);
        item.setQuantity(quantity);
        return buildCart(userId);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product is not in your cart"));
        cartItemRepository.delete(item);
        return buildCart(userId);
    }

    @Transactional
    public void clear(Long userId) {
        cartItemRepository.deleteAll(cartItemRepository.findByUserId(userId));
    }

    private Product findAvailableProduct(Long productId) {
        return productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private void ensureStock(Product product, int requestedQuantity) {
        if (requestedQuantity > product.getStock()) {
            throw new BadRequestException("Only " + product.getStock() + " unit(s) of '"
                    + product.getName() + "' available");
        }
    }

    private CartResponse buildCart(Long userId) {
        List<CartItemResponse> items = cartItemRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(Mapper::toCartItem)
                .toList();
        int totalItems = items.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal total = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(items, totalItems, total);
    }
}
