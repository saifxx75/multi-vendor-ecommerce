package com.marketplace.service;

import com.marketplace.dto.CategoryRequest;
import com.marketplace.dto.CategoryResponse;
import com.marketplace.entity.Category;
import com.marketplace.exception.ConflictException;
import com.marketplace.exception.ResourceNotFoundException;
import com.marketplace.repository.CategoryRepository;
import com.marketplace.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findAll(Sort.by("name")).stream().map(Mapper::toCategory).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Category '" + name + "' already exists");
        }
        Category category = new Category();
        category.setName(name);
        category.setDescription(request.description());
        return Mapper.toCategory(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getEntity(id);
        String name = request.name().trim();
        if (!category.getName().equalsIgnoreCase(name) && categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Category '" + name + "' already exists");
        }
        category.setName(name);
        category.setDescription(request.description());
        return Mapper.toCategory(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = getEntity(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException("Category still has products and cannot be deleted");
        }
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public Category getEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }
}
