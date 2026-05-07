package com.ecommerce.util;

import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repository.*;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.core.env.Environment;
import java.util.Arrays;

import java.util.List;

/**
 * Seeds the database with default credentials and robust sample data.
 * Implements a "Clean Start" by clearing tables before seeding.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final CartItemRepository cartItemRepository;
    private final Environment environment;

    @PersistenceContext
    private EntityManager entityManager;

    public DataInitializer(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           OrderRepository orderRepository,
                           ReviewRepository reviewRepository,
                           CartItemRepository cartItemRepository,
                           Environment environment) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.reviewRepository = reviewRepository;
        this.cartItemRepository = cartItemRepository;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(String... args) {
        System.out.println("[DataInitializer] Starting database re-initialization...");
        
        try {
            // Perform a full reset as requested by the user
            cleanDatabase();
            
            System.out.println("[DataInitializer] Seeding categories...");
            seedCategories();

            System.out.println("[DataInitializer] Seeding admin user...");
            seedAdmin();

            System.out.println("[DataInitializer] Seeding sample customer...");
            seedCustomer();

            System.out.println("[DataInitializer] Seeding products...");
            seedProducts();

            System.out.println("[DataInitializer] Initialization complete. All tables reset and seeded.");
        } catch (Exception e) {
            System.err.println("[DataInitializer] Error during initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cleanDatabase() {
        boolean isTest = Arrays.asList(environment.getActiveProfiles()).contains("test");
        if (isTest) {
            System.out.println("[DataInitializer] Skipping truncation for H2 test profile (tables are already fresh).");
            return;
        }
        
        System.out.println("[DataInitializer] Truncating tables and resetting identities...");
        // Use TRUNCATE with RESTART IDENTITY CASCADE to clear all data and reset sequences to 1
        entityManager.createNativeQuery("TRUNCATE TABLE cartitems, reviews, orderitems, orders, products, categories, users RESTART IDENTITY CASCADE").executeUpdate();
        System.out.println("[DataInitializer] All tables truncated and sequences reset to 1.");
    }

    private void seedCategories() {
        categoryRepository.saveAll(List.of(
            new Category(0, "Electronics", "Gadgets and electronic devices"),
            new Category(0, "Home & Kitchen", "Appliances and home decor"),
            new Category(0, "Books", "Educational and fictional books"),
            new Category(0, "Fashion", "Clothing and accessories")
        ));
    }

    private void seedAdmin() {
        User admin = new User();
        admin.setName("System Admin");
        admin.setEmail("admin@ecommerce.com");
        admin.setPassword(BCrypt.hashpw("admin123", BCrypt.gensalt()));
        admin.setRole("ADMIN");
        admin.setLocation("Headquarters");
        userRepository.save(admin);
        System.out.println("[DataInitializer] Default admin created: admin@ecommerce.com / admin123");
    }

    private void seedCustomer() {
        User customer = new User();
        customer.setName("John Doe");
        customer.setEmail("john@example.com");
        customer.setPassword(BCrypt.hashpw("password123", BCrypt.gensalt()));
        customer.setRole("CUSTOMER");
        customer.setLocation("Accra, Ghana");
        userRepository.save(customer);
        System.out.println("[DataInitializer] Sample customer created: john@example.com / password123");
    }

    private void seedProducts() {
        List<Category> cats = categoryRepository.findAll();
        Category electronics = cats.stream().filter(c -> c.getName().equalsIgnoreCase("Electronics")).findFirst().orElse(null);
        Category home = cats.stream().filter(c -> c.getName().equalsIgnoreCase("Home & Kitchen")).findFirst().orElse(null);

        if (electronics != null) {
            productRepository.saveAll(List.of(
                new Product(0, "Laptop Pro", "High-performance laptop", 1299.99, electronics, 50),
                new Product(0, "Wireless Mouse", "Ergonomic mouse", 25.50, electronics, 200),
                new Product(0, "4K Monitor", "27-inch display", 349.99, electronics, 30)
            ));
        }

        if (home != null) {
            productRepository.saveAll(List.of(
                new Product(0, "Coffee Maker", "Drip coffee maker", 49.99, home, 40),
                new Product(0, "Air Fryer", "Oil-free cooker", 119.50, home, 25)
            ));
        }
    }
}
