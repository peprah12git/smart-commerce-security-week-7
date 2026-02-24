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
    
    // Threshold in milliseconds for slow query logging
    private static final long SLOW_QUERY_THRESHOLD = 100;

    /**
     * Monitor all repository method executions
     */
    @Around("execution(* com.smartcommerce.repositories..*(..))")
    public Object monitorRepositoryPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();
        
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Log all queries with execution time
            if (executionTime > SLOW_QUERY_THRESHOLD) {
                logger.warn("SLOW QUERY DETECTED - Method: {} | ExecutionTime: {}ms | Args: {}", 
                    methodName, executionTime, Arrays.toString(joinPoint.getArgs()));
            } else {
                logger.info("Query executed - Method: {} | ExecutionTime: {}ms", 
                    methodName, executionTime);
            }
        }
    }

    /**
     * Monitor service layer methods for complex operations
     */
    @Around("execution(* com.smartcommerce.service.imp.OrderServiceImp.getOrders*(..)) || " +
            "execution(* com.smartcommerce.service.imp.OrderServiceImp.getAllOrders(..))")
    public Object monitorOrderQueryPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();
        
        logger.info("=== ORDER QUERY START === Method: {} | Args: {}", 
            methodName, Arrays.toString(joinPoint.getArgs()));
        
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            
            logger.info("=== ORDER QUERY COMPLETE === Method: {} | TotalExecutionTime: {}ms", 
                methodName, executionTime);
                
            if (executionTime > SLOW_QUERY_THRESHOLD * 2) {
                logger.warn("COMPLEX QUERY PERFORMANCE ISSUE - Method: {} took {}ms", 
                    methodName, executionTime);
            }
        }
    }

    /**
     * Monitor product queries for reporting and filtering
     */
    @Around("execution(* com.smartcommerce.service.imp.ProductServiceImpl.getProducts*(..)) || " +
            "execution(* com.smartcommerce.service.imp.ProductServiceImpl.searchProducts(..))")
    public Object monitorProductQueryPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();
        
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (executionTime > SLOW_QUERY_THRESHOLD) {
                logger.warn("Product query slow - Method: {} | ExecutionTime: {}ms", 
                    methodName, executionTime);
            }
        }
    }
}
