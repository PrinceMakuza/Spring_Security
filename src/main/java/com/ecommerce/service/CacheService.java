package com.ecommerce.service;

import com.ecommerce.model.Product;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * CacheService manages an in-memory HashMap cache for search results.
 * Provides O(1) hashing for speed as per requirements.
 */
@Service
public class CacheService {
    private Map<String, List<Product>> searchCache = new HashMap<>();
    
    // Performance metrics (kept for reporting)
    private int cacheHits = 0;
    private int cacheMisses = 0;

    public List<Product> get(String key) { 
        List<Product> result = searchCache.get(key);
        if (result != null) cacheHits++;
        else cacheMisses++;
        return result; 
    }

    public void put(String key, List<Product> products) { 
        // Real-world safeguard: prevent unbounded growth
        if (searchCache.size() > 500) searchCache.clear();
        searchCache.put(key, products); 
    }

    public void invalidate() { searchCache.clear(); }

    // Analytics (for performance_report.txt)
    public int getHits() { return cacheHits; }
    public int getMisses() { return cacheMisses; }
}
