package com.marketplace.controller;

import com.marketplace.dto.CheckoutRequest;
import com.marketplace.dto.OrderResponse;
import com.marketplace.dto.PageResponse;
import com.marketplace.security.UserPrincipal;
import com.marketplace.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final Set<String> ORDER_SORT_FIELDS = Set.of("id", "createdAt", "totalAmount");

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@AuthenticationPrincipal UserPrincipal principal,
                                  @Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(principal.getId(), request);
    }

    @GetMapping
    public PageResponse<OrderResponse> myOrders(@AuthenticationPrincipal UserPrincipal principal,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) String sortBy,
                                                @RequestParam(defaultValue = "desc") String direction) {
        return orderService.myOrders(principal.getId(),
                PageUtil.of(page, size, sortBy, direction, ORDER_SORT_FIELDS, "createdAt"));
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return orderService.getMyOrder(principal.getId(), id);
    }

    @PutMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return orderService.cancelOrder(principal.getId(), id);
    }
}
