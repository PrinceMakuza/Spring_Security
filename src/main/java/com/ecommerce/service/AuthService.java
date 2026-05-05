package com.ecommerce.service;

import com.ecommerce.model.User;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.util.UserContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * AuthService handles authentication and secure password management.
 */
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticates a user and returns a token.
     */
    public String login(String email, String password) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Incorrect password.");
        }
        
        // Context populating will be handled by JwtAuthenticationFilter for subsequent requests
        return null; // Token generation will be handled in Controller or here
    }

    /**
     * Registers a new user with an encoded password.
     */
    @Transactional
    public User register(String name, String email, String password, String role, String location) {
        User user = new User();
        user.setName(name);
        user.setEmail(email.toLowerCase());
        user.setRole(role != null ? role : "CUSTOMER");
        user.setPassword(passwordEncoder.encode(password));
        user.setLocation(location);
        return userRepository.save(user);
    }

    public void logout() {
        // UserContext.clear() is fine, but we'll also blacklist the token in the controller
        UserContext.clear();
    }

    /**
     * Updates the current user's profile and hashes the new password if provided.
     */
    @Transactional
    public User updateProfile(int userId, String name, String email, String location, String plainPassword) {
        return userRepository.findById(userId).map(user -> {
            user.setName(name);
            user.setEmail(email.toLowerCase());
            user.setLocation(location);
            
            if (plainPassword != null && !plainPassword.trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(plainPassword));
            }
            
            // Update local context if it's the current user
            if (UserContext.getCurrentUserId() == userId) {
                UserContext.setCurrentUserName(name);
                UserContext.setCurrentUserEmail(user.getEmail());
                UserContext.setCurrentUserLocation(location);
            }
            
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("User not found"));
    }
}
