package com.marketplace.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String productName,
        String storeName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        int availableStock) {
}
