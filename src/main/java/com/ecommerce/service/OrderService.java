package com.ecommerce.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import com.ecommerce.model.Order;
import com.ecommerce.model.User;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * OrderService manages order-related business logic.
 */
@Service
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final com.ecommerce.repository.ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository, com.ecommerce.repository.ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "orders")
    public List<Order> getAllOrders() {
        return orderRepository.findAllWithUser();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "{#page, #size, #sortBy, #direction}")
    public org.springframework.data.domain.Page<Order> getAllOrders(int page, int size, String sortBy, String direction) {
        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by(
            "desc".equalsIgnoreCase(direction) ? org.springframework.data.domain.Sort.Direction.DESC : org.springframework.data.domain.Sort.Direction.ASC,
            sortBy
        );
        return orderRepository.findAllWithUser(org.springframework.data.domain.PageRequest.of(page, size, sort));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "'user_' + #userId")
    public List<Order> getUserOrders(int userId) {
        com.ecommerce.model.User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepository.findByUserWithItems(user);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "order", key = "#orderId")
    public Order getOrderDetails(int orderId) {
        return orderRepository.findOrderWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    /**
     * Creates a new order with stock validation and decrement.
     * READ_COMMITTED isolation prevents dirty reads.
     */
    @Transactional(propagation = Propagation.REQUIRED,
                   isolation = Isolation.READ_COMMITTED,
                   rollbackFor = Exception.class)
    @Caching(evict = {
        @CacheEvict(value = "orders", allEntries = true),
        @CacheEvict(value = "users", allEntries = true),
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "product", allEntries = true)
    })
    public Order createOrder(int userId, List<Integer> productIds, List<Integer> quantities) {
        com.ecommerce.model.User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = new Order();
        order.setUser(user);
        order.setStatus("PENDING");
        
        double totalAmount = 0;
        List<com.ecommerce.model.OrderItem> items = new java.util.ArrayList<>();

        for (int i = 0; i < productIds.size(); i++) {
            int productId = productIds.get(i);
            int quantity = quantities.get(i);

            com.ecommerce.model.Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

            // Stock validation
            if (product.getStockQuantity() < quantity) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName() + 
                        ". Available: " + product.getStockQuantity() + ", Requested: " + quantity);
            }

            // Decrement stock
            product.setStockQuantity(product.getStockQuantity() - quantity);
            productRepository.save(product);

            com.ecommerce.model.OrderItem item = new com.ecommerce.model.OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPrice(product.getPrice());
            items.add(item);

            totalAmount += product.getPrice() * quantity;
        }

        order.setItems(items);
        order.setTotalAmount(totalAmount);

        // Evict user order history cache (if any)
        // Note: Actual cache eviction will be handled by @CacheEvict in future step or here manually via CacheManager if needed.
        
        return orderRepository.save(order);
    }

    /**
     * Updates order status in a new transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Caching(evict = {
        @CacheEvict(value = "orders", allEntries = true),
        @CacheEvict(value = "order", key = "#orderId")
    })
    public Order updateOrderStatus(int orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        
        return orderRepository.save(order);
    }
}
