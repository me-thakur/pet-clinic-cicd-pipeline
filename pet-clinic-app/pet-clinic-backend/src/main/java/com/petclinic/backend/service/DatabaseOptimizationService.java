package com.petclinic.backend.service;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;

/**
 * Service for database query optimization and performance monitoring
 * Validates: Requirements 16.1, 16.2, 16.3
 */
public interface DatabaseOptimizationService {
    
    /**
     * Optimize database queries for better performance
     */
    void optimizeQueries();
    
    /**
     * Analyze slow queries and provide recommendations
     */
    Map<String, Object> analyzeSlowQueries();
    
    /**
     * Get database performance statistics
     */
    Map<String, Object> getDatabasePerformanceStats();
    
    /**
     * Optimize pagination queries for large datasets
     */
    <T> List<T> optimizedPagination(String entityType, Pageable pageable, Map<String, Object> filters);
    
    /**
     * Create database indexes for frequently queried columns
     */
    void createOptimalIndexes();
    
    /**
     * Monitor and log slow queries
     */
    void monitorSlowQueries(String query, long executionTime, String entityType);
    
    /**
     * Get query execution plan analysis
     */
    Map<String, Object> analyzeQueryExecutionPlan(String query);
    
    /**
     * Optimize database connection pool settings
     */
    void optimizeConnectionPool();
}