package com.marketplace.dto;

import com.marketplace.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,
        BigDecimal totalAmount,
        String shippingAddress,
        LocalDateTime createdAt,
        List<OrderItemResponse> items) {
}
