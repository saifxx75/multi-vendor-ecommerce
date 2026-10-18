package com.marketplace.service;

import com.marketplace.dto.CartItemResponse;
import com.marketplace.dto.CategoryResponse;
import com.marketplace.dto.OrderItemResponse;
import com.marketplace.dto.OrderResponse;
import com.marketplace.dto.ProductResponse;
import com.marketplace.dto.VendorOrderItemResponse;
import com.marketplace.dto.VendorResponse;
import com.marketplace.entity.CartItem;
import com.marketplace.entity.Category;
import com.marketplace.entity.Order;
import com.marketplace.entity.OrderItem;
import com.marketplace.entity.Product;
import com.marketplace.entity.Vendor;

import java.util.List;

/**
 * Entity -> DTO conversion. Entities are never returned from controllers (no lazy-loading surprises,
 * no password hashes leaking). These methods must be called inside a transaction because they touch
 * lazy associations.
 */
public final class Mapper {

    private Mapper() {
    }

    public static CategoryResponse toCategory(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription());
    }

    public static VendorResponse toVendor(Vendor v) {
        return new VendorResponse(v.getId(), v.getUser().getId(), v.getUser().getName(), v.getUser().getEmail(),
                v.getStoreName(), v.getDescription(), v.getStatus(), v.getCreatedAt());
    }

    public static ProductResponse toProduct(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getStock(),
                p.getCategory().getId(), p.getCategory().getName(),
                p.getVendor().getId(), p.getVendor().getStoreName(),
                p.isActive(), p.getCreatedAt());
    }

    public static CartItemResponse toCartItem(CartItem ci) {
        Product p = ci.getProduct();
        return new CartItemResponse(p.getId(), p.getName(), p.getVendor().getStoreName(), p.getPrice(),
                ci.getQuantity(), p.getPrice().multiply(java.math.BigDecimal.valueOf(ci.getQuantity())), p.getStock());
    }

    public static OrderItemResponse toOrderItem(OrderItem i) {
        return new OrderItemResponse(i.getId(), i.getProduct().getId(), i.getProductName(),
                i.getVendor().getId(), i.getVendor().getStoreName(),
                i.getUnitPrice(), i.getQuantity(), i.getLineTotal(), i.getStatus());
    }

    public static OrderResponse toOrder(Order o) {
        List<OrderItemResponse> items = o.getItems().stream().map(Mapper::toOrderItem).toList();
        return new OrderResponse(o.getId(), o.getStatus(), o.getTotalAmount(), o.getShippingAddress(),
                o.getCreatedAt(), items);
    }

    public static VendorOrderItemResponse toVendorOrderItem(OrderItem i) {
        Order o = i.getOrder();
        return new VendorOrderItemResponse(i.getId(), o.getId(), o.getCustomer().getName(), o.getShippingAddress(),
                i.getProduct().getId(), i.getProductName(), i.getUnitPrice(), i.getQuantity(), i.getLineTotal(),
                i.getStatus(), o.getCreatedAt());
    }
}
