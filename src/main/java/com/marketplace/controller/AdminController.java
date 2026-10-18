package com.marketplace.controller;

import com.marketplace.dto.CategoryRequest;
import com.marketplace.dto.CategoryResponse;
import com.marketplace.dto.VendorResponse;
import com.marketplace.entity.VendorStatus;
import com.marketplace.service.CategoryService;
import com.marketplace.service.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final VendorService vendorService;
    private final CategoryService categoryService;

    @GetMapping("/vendors")
    public List<VendorResponse> vendors(@RequestParam(required = false) VendorStatus status) {
        return vendorService.list(status);
    }

    @PutMapping("/vendors/{id}/approve")
    public VendorResponse approve(@PathVariable Long id) {
        return vendorService.updateStatus(id, VendorStatus.APPROVED);
    }

    @PutMapping("/vendors/{id}/reject")
    public VendorResponse reject(@PathVariable Long id) {
        return vendorService.updateStatus(id, VendorStatus.REJECTED);
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
