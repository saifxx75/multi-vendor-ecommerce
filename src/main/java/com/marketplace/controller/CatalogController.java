package com.marketplace.controller;

import com.marketplace.dto.CategoryResponse;
import com.marketplace.dto.PageResponse;
import com.marketplace.dto.ProductResponse;
import com.marketplace.service.CategoryService;
import com.marketplace.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {

    private static final Set<String> PRODUCT_SORT_FIELDS = Set.of("createdAt", "price", "name", "id");

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return categoryService.list();
    }

    @GetMapping("/products")
    public PageResponse<ProductResponse> products(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return productService.search(q, categoryId, minPrice, maxPrice,
                PageUtil.of(page, size, sortBy, direction, PRODUCT_SORT_FIELDS, "createdAt"));
    }

    @GetMapping("/products/{id}")
    public ProductResponse product(@PathVariable Long id) {
        return productService.getPublic(id);
    }
}
