package com.ecommerce.service;

import com.ecommerce.model.CartItem;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repository.*;
import com.ecommerce.util.DataEventBus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CartService manages the shopping cart business logic.
 * Note: DataEventBus.publish() should be called from the caller (Controller) 
 * to ensure transaction commit before UI refresh.
 */
@Service
public class CartService {
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    
    private final ConcurrentMap<Integer, List<CartItem>> userCarts = new ConcurrentHashMap<>();

    public CartService(CartItemRepository cartItemRepository, 
                       ProductRepository productRepository,
                       UserRepository userRepository, 
                       OrderRepository orderRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public void addToCart(int userId, int productId, int quantity) {
        System.out.println("DEBUG: Service.addToCart - User: " + userId + ", Product: " + productId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product == null) throw new RuntimeException("Product cannot be null");
        
        if (product.getStockQuantity() < quantity) {
            throw new RuntimeException("Not enough stock available. Remaining: " + product.getStockQuantity());
        }

        List<CartItem> cart = userCarts.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>(cartItemRepository.findByUser(user)));
        Optional<CartItem> matchingItem = cart.stream()
                .filter(item -> item.getProduct().getProductId() == productId)
                .findFirst();

        if (matchingItem.isPresent()) {
            CartItem item = matchingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            CartItem item = new CartItem();
            item.setUser(user);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPrice(product.getPrice());
            cartItemRepository.save(item);
            cart.add(item);
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);
        System.out.println("DEBUG: Service.addToCart - Success. New stock: " + product.getStockQuantity());
    }

    @Transactional(readOnly = true)
    public List<CartItem> getCartItems(int userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return new CopyOnWriteArrayList<>();
        
        List<CartItem> dbItems = cartItemRepository.findByUser(user);
        userCarts.put(userId, new CopyOnWriteArrayList<>(dbItems));
        return userCarts.get(userId);
    }

    @Transactional
    public void updateQuantity(int cartItemId, int quantity) {
        cartItemRepository.findById(cartItemId).ifPresent(item -> {
            Product product = item.getProduct();
            int diff = quantity - item.getQuantity();
            
            if (product.getStockQuantity() < diff) {
                throw new RuntimeException("Not enough stock available");
            }

            if (quantity <= 0) {
                removeFromCart(cartItemId);
            } else {
                product.setStockQuantity(product.getStockQuantity() - diff);
                item.setQuantity(quantity);
                cartItemRepository.save(item);
                productRepository.save(product);
            }
        });
    }

    @Transactional
    public void removeFromCart(int cartItemId) {
        cartItemRepository.findById(cartItemId).ifPresent(item -> {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
            
            int userId = item.getUser().getUserId();
            List<CartItem> cart = userCarts.get(userId);
            if (cart != null) {
                cart.removeIf(i -> i.getCartItemId() == cartItemId);
            }
            
            cartItemRepository.delete(item);
        });
    }

    @Transactional
    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public boolean checkout(int userId) {
        System.out.println("DEBUG: Service.checkout - User: " + userId);
        List<CartItem> items = getCartItems(userId);
        if (items.isEmpty()) {
            System.out.println("DEBUG: Service.checkout - Failed (Cart empty)");
            return false;
        }

        User user = userRepository.findById(userId).orElseThrow();
        double totalAmount = items.stream().mapToDouble(CartItem::getSubtotal).sum();
        processCheckout(user, items, totalAmount);
        
        cartItemRepository.deleteByUser(user);
        userCarts.remove(userId);
        System.out.println("DEBUG: Service.checkout - Success");
        return true;
    }

    @Transactional
    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public void checkoutSingleItem(int cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        
        User user = cartItem.getUser();
        processCheckout(user, List.of(cartItem), cartItem.getSubtotal());
        
        int userId = user.getUserId();
        List<CartItem> cart = userCarts.get(userId);
        if (cart != null) cart.removeIf(i -> i.getCartItemId() == cartItemId);
        
        cartItemRepository.delete(cartItem);
    }

    private void processCheckout(User user, List<CartItem> items, double totalAmount) {
        Order order = new Order();
        order.setUser(user);
        order.setTotalAmount(totalAmount);
        order.setStatus("COMPLETED");
        order = orderRepository.save(order);

        for (CartItem cartItem : items) {
            Product product = cartItem.getProduct();
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            order.getItems().add(orderItem);
        }
        orderRepository.save(order);
    }
}
