package com.ecommerce.repository;

import com.ecommerce.model.Review;
import com.ecommerce.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    java.util.List<Review> findByProduct(com.ecommerce.model.Product product);

    /**
     * Gets all reviews for a product.
     */
    java.util.List<Review> findByProductProductId(int productId);

    /**
     * Gets all reviews written by a specific user.
     */
    java.util.List<Review> findByUserUserId(int userId);

    /**
     * Counts reviews by rating value.
     */
    long countByRating(int rating);

    /**
     * Calculates the average rating for a given product.
     */
    @org.springframework.data.jpa.repository.Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.productId = :productId")
    java.lang.Double getAverageRatingByProductId(@org.springframework.data.repository.query.Param("productId") int productId);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM Review r LEFT JOIN FETCH r.product LEFT JOIN FETCH r.user")
    java.util.List<Review> findAllWithUserAndProduct();
}
