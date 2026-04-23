package com.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int productId;

    @Column(updatable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
    }

    @NotBlank(message = "Product name is required")
    @Column(unique = true, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @DecimalMin(value = "0.0", message = "Price must be greater than or equal to 0")
    @Column(nullable = false)
    private double price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int stockQuantity;

    public Product() {}

    public Product(int productId, String name, String description, double price, Category category, int stockQuantity) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.stockQuantity = stockQuantity;
    }

    // Getters and Setters
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    // Backward-compatible convenience methods for existing JavaFX code
    @Transient
    public int getCategoryId() {
        return category != null ? category.getCategoryId() : 0;
    }

    public void setCategoryId(int categoryId) {
        if (this.category == null) {
            this.category = new Category();
        }
        this.category.setCategoryId(categoryId);
    }

    @Transient
    public String getCategoryName() {
        return category != null ? category.getName() : "";
    }

    public void setCategoryName(String categoryName) {
        if (this.category == null) {
            this.category = new Category();
        }
        this.category.setName(categoryName);
    }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    @org.hibernate.annotations.Formula("(SELECT COALESCE(AVG(r.rating), 0) FROM reviews r WHERE r.product_id = product_id)")
    private double averageRating;

    public double getAverageRating() {
        return averageRating;
    }

    @Transient
    public String getRatingStars() {
        double avg = getAverageRating();
        int rating = (int) Math.round(avg);
        return "★".repeat(Math.max(0, rating)) + "☆".repeat(Math.max(0, 5 - rating));
    }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "Product{" +
                "productId=" + productId +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", stockQuantity=" + stockQuantity +
                '}';
    }
}
