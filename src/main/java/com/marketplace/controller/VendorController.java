package com.marketplace.controller;

import com.marketplace.dto.ItemStatusRequest;
import com.marketplace.dto.PageResponse;
import com.marketplace.dto.ProductRequest;
import com.marketplace.dto.ProductResponse;
import com.marketplace.dto.VendorOrderItemResponse;
import com.marketplace.dto.VendorResponse;
import com.marketplace.entity.ItemStatus;
import com.marketplace.security.UserPrincipal;
import com.marketplace.service.OrderService;
import com.marketplace.service.ProductService;
import com.marketplace.service.VendorService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/vendor")
@RequiredArgsConstructor
public class VendorController {

    private static final Set<String> PRODUCT_SORT_FIELDS = Set.of("createdAt", "price", "name", "id");
    private static final Set<String> ORDER_SORT_FIELDS = Set.of("id");

    private final VendorService vendorService;
    private final ProductService productService;
    private final OrderService orderService;

    @GetMapping("/profile")
    public VendorResponse profile(@AuthenticationPrincipal UserPrincipal principal) {
        return vendorService.profile(principal.getId());
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@AuthenticationPrincipal UserPrincipal principal,
                                         @Valid @RequestBody ProductRequest request) {
        return productService.create(principal.getId(), request);
    }

    @PutMapping("/products/{id}")
    public ProductResponse updateProduct(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable Long id,
                                         @Valid @RequestBody ProductRequest request) {
        return productService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        productService.delete(principal.getId(), id);
    }

    @GetMapping("/products")
    public PageResponse<ProductResponse> myProducts(@AuthenticationPrincipal UserPrincipal principal,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    @RequestParam(required = false) String sortBy,
                                                    @RequestParam(defaultValue = "desc") String direction) {
        return productService.listOwn(principal.getId(),
                PageUtil.of(page, size, sortBy, direction, PRODUCT_SORT_FIELDS, "createdAt"));
    }

    @GetMapping("/orders")
    public PageResponse<VendorOrderItemResponse> myOrderItems(@AuthenticationPrincipal UserPrincipal principal,
                                                              @RequestParam(required = false) ItemStatus status,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        return orderService.vendorOrderItems(principal.getId(), status,
                PageUtil.of(page, size, "id", "desc", ORDER_SORT_FIELDS, "id"));
    }

    @PutMapping("/orders/items/{itemId}/status")
    public VendorOrderItemResponse updateItemStatus(@AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable Long itemId,
                                                    @Valid @RequestBody ItemStatusRequest request) {
        return orderService.updateItemStatus(principal.getId(), itemId, request.status());
    }
}
