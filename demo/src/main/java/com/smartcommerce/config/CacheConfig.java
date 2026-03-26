package com.smartcommerce.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * Cache Configuration for Spring Cache
 * Configures in-memory caching with TTL (time-to-live) expiration
 * Uses Caffeine cache for production-grade performance and memory management
 * Prevents memory leaks from unbounded cache growth
 */
@Configuration
@EnableCaching
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

    @Bean(name = "appCacheManager")
    @Primary
    public CacheManager appCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                PRODUCTS_CACHE,
                PRODUCT_CACHE,
                CATEGORIES_CACHE,
                CATEGORY_CACHE,
                USERS_CACHE,
                USER_CACHE,
                USER_EMAIL_CACHE,
                INVENTORY_CACHE);
        
        // ✅ TTL Configuration: Prevent memory leaks
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .maximumSize(10000)
                .recordStats()                           // Track cache hit/miss metrics
        );
        
        return cacheManager;
    }
}
