package com.marketplace.service;

import com.marketplace.dto.ProductRequest;
import com.marketplace.entity.Category;
import com.marketplace.entity.Product;
import com.marketplace.entity.Vendor;
import com.marketplace.exception.ResourceNotFoundException;
import com.marketplace.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private VendorService vendorService;
    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private ProductService productService;

    private Vendor vendor(long id) {
        Vendor vendor = new Vendor();
        vendor.setId(id);
        vendor.setStoreName("Store " + id);
        return vendor;
    }

    private ProductRequest request(long categoryId) {
        return new ProductRequest("Phone", "A phone", new BigDecimal("199.99"), 10, categoryId);
    }

    @Test
    @DisplayName("creating a product with an unknown category fails and saves nothing")
    void createWithUnknownCategory() {
        when(vendorService.getApprovedVendorForUser(2L)).thenReturn(vendor(3L));
        when(categoryService.getEntity(99L)).thenThrow(new ResourceNotFoundException("Category not found"));

        assertThatThrownBy(() -> productService.create(2L, request(99L)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("a vendor cannot edit another vendor's product (reported as not found)")
    void cannotUpdateForeignProduct() {
        when(vendorService.getApprovedVendorForUser(2L)).thenReturn(vendor(3L));
        when(productRepository.findByIdAndVendorId(50L, 3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(2L, 50L, request(1L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("an owner can update product fields")
    void ownerCanUpdate() {
        Vendor vendor = vendor(3L);
        Product product = new Product();
        product.setId(50L);
        product.setVendor(vendor);
        product.setName("Old");
        product.setPrice(new BigDecimal("1.00"));
        product.setStock(1);

        Category category = new Category();
        category.setId(1L);
        category.setName("Electronics");

        when(vendorService.getApprovedVendorForUser(2L)).thenReturn(vendor);
        when(productRepository.findByIdAndVendorId(50L, 3L)).thenReturn(Optional.of(product));
        when(categoryService.getEntity(1L)).thenReturn(category);

        productService.update(2L, 50L, request(1L));

        assertThat(product.getName()).isEqualTo("Phone");
        assertThat(product.getPrice()).isEqualByComparingTo("199.99");
        assertThat(product.getStock()).isEqualTo(10);
        assertThat(product.getCategory()).isSameAs(category);
    }

    @Test
    @DisplayName("deleting a product is a soft delete (kept for order history)")
    void deleteIsSoft() {
        Vendor vendor = vendor(3L);
        Product product = new Product();
        product.setId(50L);
        product.setVendor(vendor);
        product.setActive(true);

        when(vendorService.getApprovedVendorForUser(2L)).thenReturn(vendor);
        when(productRepository.findByIdAndVendorId(50L, 3L)).thenReturn(Optional.of(product));

        productService.delete(2L, 50L);

        assertThat(product.isActive()).isFalse();
        verify(productRepository, never()).delete(any(Product.class));
    }
}
