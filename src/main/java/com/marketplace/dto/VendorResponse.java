package com.marketplace.dto;

import com.marketplace.entity.VendorStatus;

import java.time.LocalDateTime;

public record VendorResponse(
        Long id,
        Long userId,
        String ownerName,
        String ownerEmail,
        String storeName,
        String description,
        VendorStatus status,
        LocalDateTime createdAt) {
}
