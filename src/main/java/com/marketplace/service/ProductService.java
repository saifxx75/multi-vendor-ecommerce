package com.marketplace.service;

import com.marketplace.dto.PageResponse;
import com.marketplace.dto.ProductRequest;
import com.marketplace.dto.ProductResponse;
import com.marketplace.entity.Category;
import com.marketplace.entity.Product;
import com.marketplace.entity.Vendor;
import com.marketplace.exception.ResourceNotFoundException;
import com.marketplace.repository.ProductRepository;
import com.marketplace.repository.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final VendorService vendorService;
    private final CategoryService categoryService;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String q, Long categoryId, BigDecimal minPrice,
                                                BigDecimal maxPrice, Pageable pageable) {
        return PageResponse.from(
                productRepository.findAll(ProductSpecifications.publicSearch(q, categoryId, minPrice, maxPrice), pageable)
                        .map(Mapper::toProduct));
    }

    @Transactional(readOnly = true)
    public ProductResponse getPublic(Long id) {
        return productRepository.findByIdAndActiveTrue(id)
                .map(Mapper::toProduct)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    @Transactional
    public ProductResponse create(Long userId, ProductRequest request) {
        Vendor vendor = vendorService.getApprovedVendorForUser(userId);
        Category category = categoryService.getEntity(request.categoryId());

        Product product = new Product();
        product.setVendor(vendor);
        apply(product, request, category);
        return Mapper.toProduct(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long userId, Long productId, ProductRequest request) {
        Vendor vendor = vendorService.getApprovedVendorForUser(userId);
        // Scoped by vendor: another vendor's product id is reported as "not found", not "forbidden".
        Product product = productRepository.findByIdAndVendorId(productId, vendor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        Category category = categoryService.getEntity(request.categoryId());

        apply(product, request, category);
        return Mapper.toProduct(product);
    }

    /** Soft delete: old orders still reference the product, so the row is kept but hidden. */
    @Transactional
    public void delete(Long userId, Long productId) {
        Vendor vendor = vendorService.getApprovedVendorForUser(userId);
        Product product = productRepository.findByIdAndVendorId(productId, vendor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setActive(false);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listOwn(Long userId, Pageable pageable) {
        Vendor vendor = vendorService.getVendorForUser(userId);
        return PageResponse.from(productRepository.findByVendorId(vendor.getId(), pageable).map(Mapper::toProduct));
    }

    private void apply(Product product, ProductRequest request, Category category) {
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCategory(category);
    }
}
