package com.ecommerce.service;

import com.ecommerce.util.PerformanceMonitor;
import com.ecommerce.util.ValidationRunner;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ReportService handles the orchestration of system reports and documentation.
 * Now includes documentation for repositories, transactions, and caching.
 * Reports are saved in Documentation/Reports and output to console.
 */
@Service
public class ReportService {

    private static final String REPORT_DIR = "Documentation/Reports";

    public String generatePerformanceReport() throws Exception {
        ensureDirectoryExists();
        PerformanceMonitor monitor = new PerformanceMonitor();
        String content = monitor.runTestsAndGenerateReport();
        System.out.println("\n=== PERFORMANCE REPORT OUTPUT ===\n" + content + "\n==================================");
        String filePath = Paths.get(REPORT_DIR, "performance_report.md").toAbsolutePath().toString();
        
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
        }
        return filePath;
    }

    public String generateValidationReport() throws Exception {
        ensureDirectoryExists();
        ValidationRunner runner = new ValidationRunner();
        String content = runner.runTestsAndGenerateReport();
        System.out.println("\n=== VALIDATION REPORT OUTPUT ===\n" + content + "\n================================");
        String filePath = Paths.get(REPORT_DIR, "validation_report.md").toAbsolutePath().toString();
        
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
        }
        return filePath;
    }

    public String generateSystemDocumentation() throws Exception {
        ensureDirectoryExists();
        StringBuilder doc = new StringBuilder();
        doc.append("# Smart E-Commerce System Documentation\n\n");
        
        doc.append("## 1. Repository Usage and Query Logic\n");
        doc.append("- **Spring Data JPA**: All repositories extend `JpaRepository` for standard CRUD.\n");
        doc.append("- **Derived Queries**: Used for simple filters like `findByName`, `findByPriceBetween`.\n");
        doc.append("- **JPQL with JOIN FETCH**: Used in `ProductRepository` and `OrderRepository` to solve the N+1 problem by loading relationships in a single query.\n");
        doc.append("- **Native Queries**: Used in `ProductRepository` for complex reports like `countProductsPerCategory` and `findTop5MostExpensiveProducts`.\n");
        doc.append("- **Specifications**: Used for dynamic filtering in `ProductServiceImpl` (now moved to `searchProducts` for optimization).\n\n");
        
        doc.append("## 2. Transaction Handling and Rollback Strategies\n");
        doc.append("- **Declarative Transactions**: Managed via `@Transactional` in the Service layer.\n");
        doc.append("- **Create Order Transaction**: Uses `propagation = REQUIRED` and `isolation = READ_COMMITTED`. It validates stock first and then saves the order items. If stock is insufficient, it throws a `RuntimeException`, triggering a full rollback of the transaction.\n");
        doc.append("- **Update Status Transaction**: Uses `propagation = REQUIRES_NEW` to ensure status changes are committed even if the parent transaction fails (used for logging or status audit).\n");
        doc.append("- **Read-Only Transactions**: All query-only methods use `readOnly = true` to optimize performance and prevent unnecessary dirty checking.\n\n");
        
        doc.append("## 3. Caching Strategies\n");
        doc.append("- **Spring Caching**: Integrated using `@Cacheable` and `@CacheEvict`.\n");
        doc.append("- **Cache Names**: `products`, `product`, `categories`, `users`.\n");
        doc.append("- **Eviction Policy**: \n");
        doc.append("  - Creating/Updating/Deleting a product evicts the `products` list cache.\n");
        doc.append("  - Updating/Deleting a specific product evicts its specific cache entry.\n");
        doc.append("  - Creating an order evicts the `users` cache to refresh user history.\n\n");
        
        System.out.println("\n=== SYSTEM DOCUMENTATION OUTPUT ===\n" + doc.toString() + "\n===================================");
        String filePath = Paths.get(REPORT_DIR, "system_documentation.md").toAbsolutePath().toString();
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(doc.toString());
        }
        return filePath;
    }

    private void ensureDirectoryExists() throws IOException {
        Path path = Paths.get(REPORT_DIR);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }
}
