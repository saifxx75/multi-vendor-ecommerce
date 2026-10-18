package com.marketplace.controller;

import com.marketplace.dto.CartItemRequest;
import com.marketplace.dto.CartResponse;
import com.marketplace.dto.CartUpdateRequest;
import com.marketplace.security.UserPrincipal;
import com.marketplace.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal UserPrincipal principal) {
        return cartService.getCart(principal.getId());
    }

    @PostMapping("/items")
    public CartResponse addItem(@AuthenticationPrincipal UserPrincipal principal,
                                @Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(principal.getId(), request);
    }

    @PutMapping("/items/{productId}")
    public CartResponse updateItem(@AuthenticationPrincipal UserPrincipal principal,
                                   @PathVariable Long productId,
                                   @Valid @RequestBody CartUpdateRequest request) {
        return cartService.updateQuantity(principal.getId(), productId, request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long productId) {
        return cartService.removeItem(principal.getId(), productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@AuthenticationPrincipal UserPrincipal principal) {
        cartService.clear(principal.getId());
    }
}
