package com.ecommerce.controller;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.model.Order;
import com.ecommerce.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<Order>>> getAllOrders() {
        return ResponseEntity.ok(new ApiResponse<>("success", "Orders retrieved", orderService.getAllOrders()));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Order>>> getUserOrders(@PathVariable int userId) {
        return ResponseEntity.ok(new ApiResponse<>("success", "User orders retrieved", orderService.getUserOrders(userId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Order>> getOrderDetails(@PathVariable int id) {
        return ResponseEntity.ok(new ApiResponse<>("success", "Order details retrieved", orderService.getOrderDetails(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Order>> createOrder(@RequestBody Map<String, Object> payload) {
        int userId = (int) payload.get("userId");
        List<Integer> productIds = (List<Integer>) payload.get("productIds");
        List<Integer> quantities = (List<Integer>) payload.get("quantities");
        
        Order order = orderService.createOrder(userId, productIds, quantities);
        return ResponseEntity.status(201).body(new ApiResponse<>("success", "Order created successfully", order));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Order>> updateStatus(@PathVariable int id, @RequestParam String status) {
        Order order = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(new ApiResponse<>("success", "Order status updated", order));
    }
}
