package com.ecommerce.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

/**
 * SequenceResetService ensures that PostgreSQL IDENTITY/SERIAL sequences 
 * are synchronized with existing data to prevent UniqueConstraintViolations.
 */
@Service
public class SequenceResetService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void resetSequences() {
        try {
            System.out.println("Syncing database sequences...");
            reset("users", "user_id");
            reset("products", "product_id");
            reset("categories", "category_id");
            reset("orders", "order_id");
            reset("orderitems", "order_item_id");
            reset("reviews", "review_id");
            reset("cartitems", "cart_item_id");
            System.out.println("Sequences synchronized successfully.");
        } catch (Exception e) {
            System.err.println("Warning: Could not reset sequences: " + e.getMessage());
        }
    }

    private void reset(String table, String idColumn) {
        try {
            String sql = String.format(
                "SELECT setval(pg_get_serial_sequence('%s', '%s'), coalesce(max(%s), 1), max(%s) IS NOT null) FROM %s",
                table, idColumn, idColumn, idColumn, table
            );
            jdbcTemplate.execute(sql);
            System.out.println("  ✓ Synced sequence for " + table + "." + idColumn);
        } catch (Exception e) {
            System.err.println("  ✗ Could not sync " + table + "." + idColumn + ": " + e.getMessage());
        }
    }
}
