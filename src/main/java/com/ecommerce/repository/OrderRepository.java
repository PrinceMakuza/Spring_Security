package com.ecommerce.repository;

import com.ecommerce.model.Order;
import com.ecommerce.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    java.util.List<Order> findByUser(com.ecommerce.model.User user);

    /**
     * Loads all orders with their user details eagerly (prevents LazyInitializationException).
     */
    @org.springframework.data.jpa.repository.Query(value = "SELECT o FROM Order o LEFT JOIN FETCH o.user",
            countQuery = "SELECT count(o) FROM Order o")
    org.springframework.data.domain.Page<Order> findAllWithUser(org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o LEFT JOIN FETCH o.user ORDER BY o.orderDate DESC")
    java.util.List<Order> findAllWithUser();

    /**
     * Loads all orders for a specific user with items eagerly loaded.
     */
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.user LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.user = :user ORDER BY o.orderDate DESC")
    java.util.List<Order> findByUserWithItems(@org.springframework.data.repository.query.Param("user") com.ecommerce.model.User user);

    /**
     * Gets paginated orders for a specific customer.
     */
    org.springframework.data.domain.Page<Order> findByUserUserId(int userId, org.springframework.data.domain.Pageable pageable);

    /**
     * Finds orders by their current status.
     */
    java.util.List<Order> findByStatus(String status);

    /**
     * Finds orders within a date range.
     */
    java.util.List<Order> findByOrderDateBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    /**
     * Loads an Order together with all its OrderItems and Product details in one query.
     */
    @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.orderId = :orderId")
    java.util.Optional<Order> findOrderWithItems(@org.springframework.data.repository.query.Param("orderId") int orderId);

    /**
     * Gets paginated orders for a user, ordered by date descending.
     */
    @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o LEFT JOIN FETCH o.user WHERE o.user.userId = :userId ORDER BY o.orderDate DESC")
    org.springframework.data.domain.Page<Order> getUserOrderHistory(@org.springframework.data.repository.query.Param("userId") int userId, org.springframework.data.domain.Pageable pageable);

    /**
     * Generates a weekly sales report (Native SQL).
     */
    @org.springframework.data.jpa.repository.Query(value = "SELECT date_trunc('week', order_date) as week, SUM(total_amount) as total " +
            "FROM orders GROUP BY week ORDER BY week", nativeQuery = true)
    java.util.List<Object[]> getWeeklySalesReport();

    /**
     * Calculates total revenue for the current month (Native SQL).
     */
    @org.springframework.data.jpa.repository.Query(value = "SELECT SUM(total_amount) FROM orders " +
            "WHERE order_date >= date_trunc('month', current_date)", nativeQuery = true)
    java.lang.Double getTotalRevenueCurrentMonth();
}
