package com.smartcommerce.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Cache Configuration for Spring Cache
 * Configures in-memory caching for products, categories, and user profiles
 * Acceptance Criteria: Cache configuration enabled using @EnableCaching
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
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                PRODUCTS_CACHE,
                PRODUCT_CACHE,
                CATEGORIES_CACHE,
                CATEGORY_CACHE,
                USERS_CACHE,
                USER_CACHE,
                USER_EMAIL_CACHE,
                INVENTORY_CACHE);
        return cacheManager;
    }
}
