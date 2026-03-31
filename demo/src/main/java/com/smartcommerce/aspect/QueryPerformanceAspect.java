package com.smartcommerce.aspect;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Aspect for monitoring query performance and recording execution times
 * User Story 3.2: Query optimization with execution time tracking
 */
@Aspect
@Component
public class QueryPerformanceAspect {

    private static final Logger logger = LoggerFactory.getLogger(QueryPerformanceAspect.class);
    private static final long SLOW_QUERY_THRESHOLD = 500; // raise from 100ms to 500ms

    /**
     * Monitor repository layer only — where actual DB queries happen
     * Removing service layer avoids double-counting and self-measuring overhead
     */
    @Around("execution(* com.smartcommerce.repositories..*(..))")
    public Object monitorQueryPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        try {
            return joinPoint.proceed();
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;

            if (executionTime > SLOW_QUERY_THRESHOLD) {
                // Compute method name only when actually logging
                logger.warn("SLOW QUERY - {}: {}ms",
                        joinPoint.getSignature().toShortString(),
                        executionTime);
            }
        }
    }
}