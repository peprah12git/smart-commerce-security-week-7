package com.smartcommerce.aspect;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Aspect for monitoring cache performance.
 * Tracks cache hits, misses, and performance improvements.
 *
 * Uses @Order(Ordered.HIGHEST_PRECEDENCE) to ensure this aspect wraps AROUND
 * Spring's CacheInterceptor. Without this, on a cache hit the CacheInterceptor
 * short-circuits (returns the cached value without calling proceed()), so this
 * aspect would never be invoked for cache hits.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CachePerformanceAspect {

    private static final Logger logger = LoggerFactory.getLogger(CachePerformanceAspect.class);

    // Thread-safe counters using AtomicInteger to track cache performance metrics
    // Prevents race conditions when multiple threads update statistics simultaneously
    private static final AtomicInteger cacheHitCount = new AtomicInteger(0);
    private static final AtomicInteger cacheMissCount = new AtomicInteger(0);

    /**
     * Threshold in milliseconds — if execution completes faster than this,
     * we consider it a cache hit (Spring returned the cached value without
     * hitting the database).
     */
    private static final long CACHE_HIT_THRESHOLD_MS = 5;

    /**
     * Monitor @Cacheable method executions.
     * Determines cache hit/miss by measuring execution time:
     * - A cache hit returns almost instantly (Spring's CacheInterceptor
     * short-circuits and returns the cached value).
     * - A cache miss requires actual method execution (DB query, etc.)
     * and takes measurably longer.
     */
    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object monitorCacheable(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();

        // Get cache name from annotation for logging
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        String cacheName = "unknown";
        if (cacheable != null) {
            if (cacheable.cacheNames().length > 0) {
                cacheName = cacheable.cacheNames()[0];
            } else if (cacheable.value().length > 0) {
                cacheName = cacheable.value()[0];
            }
        }

        Object result;
        try {
            // proceed() calls through to Spring's CacheInterceptor.
            // On a cache hit, CacheInterceptor returns the cached value very fast.
            // On a cache miss, the actual method runs (DB call, etc.).
            result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            if (executionTime < CACHE_HIT_THRESHOLD_MS) {
                cacheHitCount.incrementAndGet();    
                logger.info("CACHE HIT - Method: {} | Cache: {} | ExecutionTime: {}ms | TotalHits: {}",
                        methodName, cacheName, executionTime, cacheHitCount.get());
            } else {
                cacheMissCount.incrementAndGet();
                logger.info("CACHE MISS - Method: {} | Cache: {} | ExecutionTime: {}ms | TotalMisses: {}",
                        methodName, cacheName, executionTime, cacheMissCount.get());
            }
            return result;
        } catch (Throwable ex) {
            cacheMissCount.incrementAndGet();
            logger.warn("CACHE ERROR/MISS - Method: {} | Cache: {} | ExecutionTime: {}ms | TotalMisses: {}",
                    methodName, cacheName, System.currentTimeMillis() - startTime, cacheMissCount.get());
            throw ex;
        }
    }

    /**
     * Monitor @CacheEvict method executions
     */
    @Around("@annotation(org.springframework.cache.annotation.CacheEvict) || " +
            "@annotation(org.springframework.cache.annotation.Caching)")
    public Object monitorCacheEvict(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();

        Object result = joinPoint.proceed();
        long executionTime = System.currentTimeMillis() - startTime;

        logger.info("CACHE EVICTION - Method: {} | ExecutionTime: {}ms", methodName, executionTime);
        return result;
    }

    /**
     * Monitor @CachePut method executions
     */
    @Around("@annotation(org.springframework.cache.annotation.CachePut)")
    public Object monitorCachePut(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();

        Object result = joinPoint.proceed();
        long executionTime = System.currentTimeMillis() - startTime;

        logger.info("CACHE UPDATE - Method: {} | ExecutionTime: {}ms", methodName, executionTime);
        return result;
    }

    /**
     * Utility method to get cache hit/miss stats
     * Thread-safe retrieval of counter values
     */
    public static String getCacheStats() {
        return String.format("Cache Hits: %d | Cache Misses: %d", cacheHitCount.get(), cacheMissCount.get());
    }
}