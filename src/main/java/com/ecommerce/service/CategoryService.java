package com.ecommerce.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import com.ecommerce.dto.CategoryDTO;
import com.ecommerce.model.Category;
import com.ecommerce.repository.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Cacheable(value = "categories")
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Cacheable(value = "categories", key = "{#page, #size, #sortBy, #sortDir, #name}")
    public Page<Category> getCategories(int page, int size, String sortBy, String sortDir, String name) {
        // Map 'date' to 'createdAt' for sorting
        String sortField = sortBy.equalsIgnoreCase("date") ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        if (name != null && !name.isEmpty()) {
            return categoryRepository.findAll((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"), pageable);
        }
        return categoryRepository.findAll(pageable);
    }

    @Cacheable(value = "categories", key = "#id")
    public Optional<Category> getCategoryById(int id) {
        return categoryRepository.findById(id);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "categories", allEntries = true),
        @CacheEvict(value = "products", allEntries = true)
    })
    public Category createCategory(CategoryDTO dto) {
        Category category = new Category();
        category.setName(dto.name());
        category.setDescription(dto.description());
        return categoryRepository.save(category);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "categories", allEntries = true),
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "product", allEntries = true)
    })
    public Category updateCategory(int id, CategoryDTO dto) {
        return categoryRepository.findById(id).map(cat -> {
            cat.setName(dto.name());
            cat.setDescription(dto.description());
            return categoryRepository.save(cat);
        }).orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "categories", allEntries = true),
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "product", allEntries = true)
    })
    public void deleteCategory(int id) {
        categoryRepository.deleteById(id);
    }
}
