package com.marketplace.dto;

import com.marketplace.entity.ItemStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** What a vendor sees of an order: only its own lines, plus where to ship them. */
public record VendorOrderItemResponse(
        Long itemId,
        Long orderId,
        String customerName,
        String shippingAddress,
        Long productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        ItemStatus status,
        LocalDateTime orderedAt) {
}
