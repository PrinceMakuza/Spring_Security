package com.ecommerce.service;

import com.ecommerce.dto.ProductDTO;
import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ProductService layer sitting between controller and repository.
 * Integrates Spring Caching and Jpa Specifications for high performance.
 */
@Service("springProductService")
@Transactional
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Retrieve products with filtering, sorting, and pagination.
     * Uses Spring Cache to avoid redundant DB hits for the same search criteria.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "{#page, #size, #sortBy, #sortDir, #name, #categoryId, #minPrice, #maxPrice}")
    public Page<Product> getProducts(int page, int size, String sortBy, String sortDir,
                                      String name, Integer categoryId,
                                      Double minPrice, Double maxPrice) {
        // Map 'date' to 'createdAt' for sorting
        String sortField = sortBy.equalsIgnoreCase("date") ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String nameFilter = (name != null && !name.trim().isEmpty()) ? "%" + name.trim().toLowerCase() + "%" : null;
        return productRepository.searchProducts(nameFilter, categoryId, minPrice, maxPrice, pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "product", key = "#id")
    public Optional<Product> getProductById(int id) {
        return productRepository.findById(id);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "categories", allEntries = true)
    })
    public Product createProduct(ProductDTO dto) {
        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + dto.categoryId()));

        Product product = new Product();
        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setPrice(dto.price());
        product.setCategory(category);
        product.setStockQuantity(dto.stockQuantity() != null ? dto.stockQuantity() : 0);
        return productRepository.save(product);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "product", key = "#id"),
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "categories", allEntries = true)
    })
    public Product updateProduct(int id, ProductDTO dto) {
        return productRepository.findById(id).map(product -> {
            product.setName(dto.name());
            product.setDescription(dto.description());
            product.setPrice(dto.price());
            if (dto.categoryId() != null) {
                Category category = categoryRepository.findById(dto.categoryId())
                        .orElseThrow(() -> new RuntimeException("Category not found with id: " + dto.categoryId()));
                product.setCategory(category);
            }
            if (dto.stockQuantity() != null) {
                product.setStockQuantity(dto.stockQuantity());
            }
            return productRepository.save(product);
        }).orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "product", key = "#id"),
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "categories", allEntries = true)
    })
    public void deleteProduct(int id) {
        productRepository.deleteById(id);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "product", allEntries = true),
        @CacheEvict(value = "categories", allEntries = true)
    })
    public void batchUpdatePrices(List<Integer> ids, double percentage) {
        for (Integer id : ids) {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + id));
            product.setPrice(product.getPrice() * (1 + percentage / 100.0));
            productRepository.save(product);
        }
    }
}
