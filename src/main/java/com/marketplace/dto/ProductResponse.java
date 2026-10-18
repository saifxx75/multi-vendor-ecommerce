package com.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int stock,
        Long categoryId,
        String categoryName,
        Long vendorId,
        String storeName,
        boolean active,
        LocalDateTime createdAt) {
}
