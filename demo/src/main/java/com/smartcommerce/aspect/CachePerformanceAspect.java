package com.smartcommerce.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Aspect for monitoring cache performance
 * Tracks cache hits, misses, and performance improvements
 * User Story: Performance improvements measured and reported
 */
@Aspect
@Component
public class CachePerformanceAspect {

    private static final Logger logger = LoggerFactory.getLogger(CachePerformanceAspect.class);

    /**
     * Monitor @Cacheable method executions
     * Logs cache hits/misses and execution times
     */
    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object monitorCacheable(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            logger.info("CACHE READ - Method: {} | ExecutionTime: {}ms", methodName, executionTime);

            return result;
        } catch (Throwable ex) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.warn("CACHE MISS or ERROR - Method: {} | ExecutionTime: {}ms", methodName, executionTime);
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
}
