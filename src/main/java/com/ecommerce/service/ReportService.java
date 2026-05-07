package com.ecommerce.service;

import com.ecommerce.util.PerformanceMonitor;
import com.ecommerce.util.ValidationRunner;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * ReportService handles the orchestration of system reports and documentation.
 * Now includes documentation for repositories, transactions, and caching.
 * Reports are saved in Documentation/Reports and output to console.
 */
@Service
public class ReportService {

    private static final String REPORT_DIR = "Documentation/Reports";
    private final SecurityAuditService auditService;

    public ReportService(SecurityAuditService auditService) {
        this.auditService = auditService;
    }

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

    public String generateSecurityReport() throws Exception {
        ensureDirectoryExists();
        StringBuilder report = new StringBuilder();
        report.append("# Smart E-Commerce Security Audit Report\n\n");
        report.append("Generated: ").append(new java.util.Date()).append("\n\n");

        report.append("## 1. Authentication Success Stats\n");
        Map<String, Integer> successStats = auditService.getLoginSuccessStats();
        if (successStats.isEmpty()) {
            report.append("*No data recorded yet.*\n\n");
        } else {
            successStats.forEach((user, count) -> report.append("- **").append(user).append("**: ").append(count).append(" successful logins\n"));
            report.append("\n");
        }

        report.append("## 2. Authentication Failure Stats\n");
        Map<String, Integer> failureStats = auditService.getLoginFailureStats();
        if (failureStats.isEmpty()) {
            report.append("*No failures recorded (Great!).*\n\n");
        } else {
            failureStats.forEach((key, count) -> report.append("- **").append(key).append("**: ").append(count).append(" failed attempts\n"));
            report.append("\n");
        }

        report.append("## 3. Endpoint Access Frequency\n");
        Map<String, Integer> hitStats = auditService.getEndpointHitStats();
        if (hitStats.isEmpty()) {
            report.append("*No access data recorded yet.*\n\n");
        } else {
            hitStats.forEach((endpoint, count) -> report.append("- **").append(endpoint).append("**: ").append(count).append(" hits\n"));
            report.append("\n");
        }

        report.append("## 4. DSA and Security Optimization (Requirement 5.1)\n");
        report.append("- **Hashing**: Secure password storage using `BCrypt` (Work Factor 10).\n");
        report.append("- **Lookup Optimization**: Token blacklisting and security audit tracking implement `ConcurrentHashMap` for O(1) time complexity on lookups and updates.\n");
        report.append("- **Atomicity**: Endpoint hits are tracked using `AtomicInteger` to ensure thread-safety without heavy locking.\n");

        String content = report.toString();
        System.out.println("\n=== SECURITY AUDIT REPORT OUTPUT ===\n" + content + "\n================================");
        String filePath = Paths.get(REPORT_DIR, "security_report.md").toAbsolutePath().toString();
        
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
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
