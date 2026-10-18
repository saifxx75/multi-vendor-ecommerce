package com.marketplace.dto;

import com.marketplace.entity.ItemStatus;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        Long vendorId,
        String storeName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        ItemStatus status) {
}
