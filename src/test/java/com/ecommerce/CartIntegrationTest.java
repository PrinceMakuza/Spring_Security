package com.ecommerce;

import com.ecommerce.model.CartItem;
import com.ecommerce.model.Order;
import com.ecommerce.model.Product;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.CartService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying the full cart lifecycle:
 * 1. Add item to cart
 * 2. Verify item is visible in cart
 * 3. Verify stock was decremented
 * 4. Checkout (place order)
 * 5. Verify order was created
 * 6. Verify cart was cleared
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CartIntegrationTest {

    @Autowired private CartService cartService;
    @Autowired private ProductRepository productRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private OrderRepository orderRepository;

    // Use existing test data: user_id=2 (John Doe), product_id=1 (Laptop Pro)
    private static final int TEST_USER_ID = 2;
    private static final int TEST_PRODUCT_ID = 1;
    private static int originalStock;
    private static long originalOrderCount;

    @Test
    @org.junit.jupiter.api.Order(1)
    void step1_addItemToCart() {
        // Record initial state
        Product product = productRepository.findById(TEST_PRODUCT_ID).orElseThrow();
        originalStock = product.getStockQuantity();
        originalOrderCount = orderRepository.count();
        
        System.out.println("=== STEP 1: Adding item to cart ===");
        System.out.println("Product: " + product.getName() + ", Stock before: " + originalStock);
        
        // Add 2 units to cart
        cartService.addToCart(TEST_USER_ID, TEST_PRODUCT_ID, 2);
        
        System.out.println("✓ addToCart completed without error");
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void step2_verifyItemVisibleInCart() {
        System.out.println("\n=== STEP 2: Verifying item is visible in cart ===");
        
        List<CartItem> items = cartService.getCartItems(TEST_USER_ID);
        
        assertFalse(items.isEmpty(), "Cart should NOT be empty after adding");
        System.out.println("Cart items count: " + items.size());
        
        CartItem cartItem = items.stream()
            .filter(i -> i.getProduct().getProductId() == TEST_PRODUCT_ID)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Product not found in cart!"));
        
        assertEquals(2, cartItem.getQuantity(), "Quantity should be 2");
        assertTrue(cartItem.getUnitPrice() > 0, "Unit price should be set");
        assertNotNull(cartItem.getProductName(), "Product name should be accessible");
        
        System.out.println("✓ Cart item visible: " + cartItem.getProductName() 
            + " x" + cartItem.getQuantity() 
            + " @ $" + cartItem.getUnitPrice());
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    void step3_verifyStockDecremented() {
        System.out.println("\n=== STEP 3: Verifying stock was decremented ===");
        
        Product product = productRepository.findById(TEST_PRODUCT_ID).orElseThrow();
        int newStock = product.getStockQuantity();
        
        assertEquals(originalStock - 2, newStock, 
            "Stock should be reduced by 2 (was " + originalStock + ", now " + newStock + ")");
        
        System.out.println("✓ Stock correctly decremented: " + originalStock + " → " + newStock);
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    void step4_checkout() {
        System.out.println("\n=== STEP 4: Placing order (checkout) ===");
        
        boolean result = cartService.checkout(TEST_USER_ID);
        
        assertTrue(result, "Checkout should return true");
        System.out.println("✓ Checkout completed successfully");
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    void step5_verifyOrderCreated() {
        System.out.println("\n=== STEP 5: Verifying order was created ===");
        
        long newOrderCount = orderRepository.count();
        assertTrue(newOrderCount > originalOrderCount, 
            "Order count should increase (was " + originalOrderCount + ", now " + newOrderCount + ")");
        
        System.out.println("✓ New order created. Total orders: " + newOrderCount);
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    void step6_verifyCartCleared() {
        System.out.println("\n=== STEP 6: Verifying cart was cleared ===");
        
        List<CartItem> items = cartService.getCartItems(TEST_USER_ID);
        assertTrue(items.isEmpty(), "Cart should be empty after checkout");
        
        System.out.println("✓ Cart is empty after checkout");
        System.out.println("\n========== ALL TESTS PASSED ==========");
    }
}
