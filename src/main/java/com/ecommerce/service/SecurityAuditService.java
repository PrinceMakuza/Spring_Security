package com.ecommerce.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SecurityAuditService tracks security-related events for auditing and reporting.
 * Demonstrates DSA concepts using ConcurrentHashMap for O(1) lookups and Atomic counters.
 */
@Service
public class SecurityAuditService {

    // Tracks successful logins per user
    private final Map<String, AtomicInteger> loginSuccessMap = new ConcurrentHashMap<>();
    
    // Tracks failed login attempts per IP or username
    private final Map<String, AtomicInteger> loginFailureMap = new ConcurrentHashMap<>();
    
    // Tracks access frequency per endpoint
    private final Map<String, AtomicInteger> endpointHitMap = new ConcurrentHashMap<>();

    public void recordLoginSuccess(String username) {
        loginSuccessMap.computeIfAbsent(username, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void recordLoginFailure(String usernameOrIp) {
        loginFailureMap.computeIfAbsent(usernameOrIp, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void recordEndpointHit(String endpoint) {
        endpointHitMap.computeIfAbsent(endpoint, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public Map<String, Integer> getLoginSuccessStats() {
        return convertToMap(loginSuccessMap);
    }

    public Map<String, Integer> getLoginFailureStats() {
        return convertToMap(loginFailureMap);
    }

    public Map<String, Integer> getEndpointHitStats() {
        return convertToMap(endpointHitMap);
    }

    private Map<String, Integer> convertToMap(Map<String, AtomicInteger> source) {
        Map<String, Integer> target = new java.util.HashMap<>();
        source.forEach((k, v) -> target.put(k, v.get()));
        return target;
    }
}
