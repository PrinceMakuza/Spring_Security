package com.ecommerce.service;

import com.ecommerce.dto.ProductDTO;
import com.ecommerce.model.Product;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface ProductService {
    Page<Product> getProducts(int page, int size, String sortBy, String sortDir,
                              String name, Integer categoryId, Double minPrice, Double maxPrice);

    Optional<Product> getProductById(int id);

    Product createProduct(ProductDTO dto);

    Product updateProduct(int id, ProductDTO dto);

    void deleteProduct(int id);

    void batchUpdatePrices(java.util.List<Integer> ids, double percentage);
}
