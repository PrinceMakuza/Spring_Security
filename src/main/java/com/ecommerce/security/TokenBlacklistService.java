package com.ecommerce.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final Map<String, LocalDateTime> blacklist = new ConcurrentHashMap<>();

    /**
     * Adds a token to the blacklist with its expiration time.
     */
    public void blacklistToken(String token) {
        // We'll store it with the time it was added. 
        // In a real scenario, we'd store it until its actual JWT expiration.
        // For simplicity, we'll just keep it for an hour (same as default expiration).
        blacklist.put(token, LocalDateTime.now().plusHours(1));
    }

    /**
     * Checks if a token is blacklisted.
     */
    public boolean isBlacklisted(String token) {
        return blacklist.containsKey(token);
    }

    /**
     * Scheduled cleanup of expired blacklist entries every hour.
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now();
        blacklist.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
