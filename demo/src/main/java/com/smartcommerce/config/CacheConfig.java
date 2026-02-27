package com.smartcommerce.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache Configuration for Spring Cache
 * Configures in-memory caching for products, categories, and user profiles
 * Acceptance Criteria: Cache configuration enabled using @EnableCaching
 */
@Configuration

public class CacheConfig {
    
    // Cache names as constants for type safety
    public static final String PRODUCTS_CACHE = "products";
    public static final String PRODUCT_CACHE = "product";
    public static final String CATEGORIES_CACHE = "categories";
    public static final String CATEGORY_CACHE = "category";
    public static final String USERS_CACHE = "users";
    public static final String USER_CACHE = "user";
    public static final String USER_EMAIL_CACHE = "userByEmail";
    public static final String INVENTORY_CACHE = "inventory";
    
    /**
     * Configures cache manager with named caches
     * Uses ConcurrentMapCache for simple in-memory caching
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        java.util.ArrayList<ConcurrentMapCache> caches = new java.util.ArrayList<>();
        caches.add(new ConcurrentMapCache(PRODUCTS_CACHE));
        caches.add(new ConcurrentMapCache(PRODUCT_CACHE));
        caches.add(new ConcurrentMapCache(CATEGORIES_CACHE));
        caches.add(new ConcurrentMapCache(CATEGORY_CACHE));
        caches.add(new ConcurrentMapCache(USERS_CACHE));
        caches.add(new ConcurrentMapCache(USER_CACHE));
        caches.add(new ConcurrentMapCache(USER_EMAIL_CACHE));
        caches.add(new ConcurrentMapCache(INVENTORY_CACHE));
        cacheManager.setCaches(caches);
        return cacheManager;
    }
}
