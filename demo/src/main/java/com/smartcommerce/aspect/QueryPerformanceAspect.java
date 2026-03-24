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
    private static final long SLOW_QUERY_THRESHOLD = 100;

    /**
     * Monitor ALL repository and service layer queries with one pointcut
     */
    @Around("execution(* com.smartcommerce.repositories..*(..))" +
            " || execution(* com.smartcommerce.service..*(..))")
    public Object monitorQueryPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();

        try {
            return joinPoint.proceed();
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;

            if (executionTime > SLOW_QUERY_THRESHOLD) {
                logger.warn("SLOW QUERY - {}: {}ms", methodName, executionTime);
            }
        }
    }
}