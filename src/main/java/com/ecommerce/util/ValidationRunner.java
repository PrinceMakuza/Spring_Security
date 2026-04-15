package com.ecommerce.util;

import com.ecommerce.dao.ProductDAO;
import com.ecommerce.model.Product;
import com.ecommerce.service.CartService;
import com.ecommerce.service.ProductService;
import java.sql.SQLException;
import java.util.List;

/**
 * ValidationRunner executes a battery of tests to ensure system integrity.
 * Returns a PASS/FAIL report for core features.
 */
public class ValidationRunner {
    private final ProductService productService = new ProductService();
    private final CartService cartService = new CartService();
    private final ProductDAO productDAO = new ProductDAO();

    public String runTestsAndGenerateReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== SMART E-COMMERCE VALIDATION REPORT ===\n");
        report.append("Generated: ").append(new java.util.Date()).append("\n\n");

        runTest(report, "CRUD: Add Product", this::testAddProduct);
        runTest(report, "CRUD: Update Product", this::testUpdateProduct);
        runTest(report, "CRUD: Delete Product", this::testDeleteProduct);
        runTest(report, "Validation: Rejection of Negative Price", this::testNegativePrice);
        runTest(report, "Search: Case-insensitive 'laPTop'", this::testSearchCaseInsensitive);
        runTest(report, "Search: Empty Results Handler", this::testSearchEmpty);
        runTest(report, "Pagination: Page size enforcement (10)", this::testPagination);
        runTest(report, "Cache: Hit after first retrieval", this::testCacheBehavior);
        runTest(report, "Constraints: NOT NULL Enforcement", this::testNotNullConstraint);
        runTest(report, "Checkout Flow: End-to-End", this::testCheckoutFlow);

        return report.toString();
    }

    private void runTest(StringBuilder report, String name, TestAction action) {
        try {
            boolean pass = action.execute();
            report.append(String.format("[ %-4s ] %s\n", pass ? "PASS" : "FAIL", name));
        } catch (Exception e) {
            report.append(String.format("[ ERROR ] %s (%s)\n", name, e.getMessage()));
        }
    }

    @FunctionalInterface
    interface TestAction {
        boolean execute() throws Exception;
    }

    private boolean testAddProduct() throws SQLException {
        Product p = new Product();
        p.setName("Validation Test Laptop");
        p.setDescription("Test Desc");
        p.setPrice(999.99);
        p.setCategoryId(1);
        p.setStockQuantity(10);
        int id = productDAO.addProduct(p);
        return id > 0;
    }

    private boolean testUpdateProduct() throws SQLException {
        Product p = productDAO.getProductsBySearch("Validation Test Laptop", null, 0, 1).get(0);
        p.setPrice(888.88);
        productService.updateProduct(p);
        Product updated = productDAO.getProductById(p.getProductId());
        return updated.getPrice() == 888.88;
    }

    private boolean testDeleteProduct() throws SQLException {
        Product p = productDAO.getProductsBySearch("Validation Test Laptop", null, 0, 1).get(0);
        productService.deleteProduct(p.getProductId());
        return productDAO.getProductById(p.getProductId()) == null;
    }

    private boolean testNegativePrice() {
        try {
            Product p = new Product();
            p.setPrice(-100);
            productService.addProduct(p);
            return false; // Should not reach here if DB check constraints exist
        } catch (SQLException e) {
            return true; // Caught expected database rejection
        }
    }

    private boolean testSearchCaseInsensitive() throws SQLException {
        List<Product> results = productDAO.getProductsBySearch("laPTop", null, 0, 10);
        return !results.isEmpty();
    }

    private boolean testSearchEmpty() throws SQLException {
        List<Product> results = productDAO.getProductsBySearch("NonExistentXYZ123", null, 0, 10);
        return results.isEmpty();
    }

    private boolean testPagination() throws SQLException {
        List<Product> page1 = productDAO.getProducts(0, 10);
        return page1.size() <= 10;
    }

    private boolean testCacheBehavior() throws SQLException {
        productService.getCacheService().invalidate();
        productService.searchProducts("laptop", null, 1, 10, "Name (A-Z)");
        int misses = productService.getCacheService().getMisses();
        productService.searchProducts("laptop", null, 1, 10, "Name (A-Z)");
        return productService.getCacheService().getHits() > 0;
    }

    private boolean testNotNullConstraint() {
        try {
            Product p = new Product();
            p.setName(null); // Name is NOT NULL
            productDAO.addProduct(p);
            return false;
        } catch (SQLException e) {
            return true;
        }
    }

    private boolean testCheckoutFlow() throws SQLException {
        int userId = 1; // Assuming demo user
        Product p = productDAO.getProducts(0, 1).get(0);
        int initialStock = p.getStockQuantity();
        
        cartService.addToCart(userId, p.getProductId(), 1);
        cartService.checkout(userId);
        
        Product after = productDAO.getProductById(p.getProductId());
        return after.getStockQuantity() == initialStock - 1;
    }
}
