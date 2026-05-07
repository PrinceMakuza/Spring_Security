package com.ecommerce.service;

import com.ecommerce.dto.UserDTO;
import com.ecommerce.model.User;
import com.ecommerce.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Cacheable(value = "users", key = "{#page, #size, #sortBy, #sortDir, #name}")
    public Page<User> getAllUsers(int page, int size, String sortBy, String sortDir, String name) {
        // Map 'date' to 'createdAt' for sorting
        String sortField = sortBy.equalsIgnoreCase("date") ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        if (name != null && !name.isEmpty()) {
            return userRepository.findAll((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"), pageable);
        }
        return userRepository.findAll(pageable);
    }

    @Cacheable(value = "users", key = "#id")
    public Optional<User> getUserById(int id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public User createUser(UserDTO userDTO) {
        User user = new User();
        user.setName(userDTO.name());
        user.setEmail(userDTO.email());
        user.setRole(userDTO.role());
        // Use provided password or default if empty
        String plainPassword = (userDTO.password() != null && !userDTO.password().isEmpty()) 
            ? userDTO.password() : (userDTO.name().split("\\s+")[0] + "@123");
        user.setPassword(passwordEncoder.encode(plainPassword));
        user.setLocation(userDTO.location());
        return userRepository.save(user);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#id"),
        @CacheEvict(value = "users", allEntries = true)
    })
    public User updateUser(int id, UserDTO userDTO) {
        return userRepository.findById(id).map(user -> {
            user.setName(userDTO.name());
            user.setEmail(userDTO.email());
            user.setRole(userDTO.role());
            if (userDTO.password() != null && !userDTO.password().trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(userDTO.password()));
            }
            user.setLocation(userDTO.location());
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#id"),
        @CacheEvict(value = "users", allEntries = true)
    })
    public void deleteUser(int id) {
        userRepository.deleteById(id);
    }
}
