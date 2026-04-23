package com.ecommerce.repository;

import com.ecommerce.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {
    Optional<Product> findByName(String name);
    boolean existsByName(String name);

    /**
     * Finds products by category name with pagination.
     */
    org.springframework.data.domain.Page<Product> findByCategoryName(String name, org.springframework.data.domain.Pageable pageable);

    /**
     * Finds products within a price range.
     */
    java.util.List<Product> findByPriceBetween(double min, double max);

    /**
     * Case-insensitive product name search.
     */
    java.util.List<Product> findByNameContainingIgnoreCase(String name);

    /**
     * Finds low stock products.
     */
    java.util.List<Product> findByStockQuantityLessThan(int quantity);

    /**
     * Combined category and price filtering.
     */
    java.util.List<Product> findByCategoryCategoryIdAndPriceBetween(int categoryId, double min, double max);

    /**
     * Custom JPQL search with optional parameters.
     */
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE " +
            "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:categoryId IS NULL OR p.category.categoryId = :categoryId) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    org.springframework.data.domain.Page<Product> searchProducts(
            @org.springframework.data.repository.query.Param("name") String name,
            @org.springframework.data.repository.query.Param("categoryId") Integer categoryId,
            @org.springframework.data.repository.query.Param("minPrice") Double minPrice,
            @org.springframework.data.repository.query.Param("maxPrice") Double maxPrice,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Load products with category info in a single query (JOIN FETCH).
     */
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p JOIN FETCH p.category")
    java.util.List<Product> findAllWithCategory();

    /**
     * Native query to count products per category.
     */
    @org.springframework.data.jpa.repository.Query(value = "SELECT c.name, COUNT(p.product_id) FROM categories c " +
            "LEFT JOIN products p ON c.category_id = p.category_id " +
            "GROUP BY c.name", nativeQuery = true)
    java.util.List<Object[]> countProductsPerCategory();

    /**
     * Native query to find the top 5 most expensive products.
     */
    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM products ORDER BY price DESC LIMIT 5", nativeQuery = true)
    java.util.List<Product> findTop5MostExpensiveProducts();
}
