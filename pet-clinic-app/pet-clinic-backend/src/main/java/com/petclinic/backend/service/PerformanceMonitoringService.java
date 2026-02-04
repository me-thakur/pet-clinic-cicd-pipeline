package com.petclinic.backend.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for performance monitoring and optimization
 * Provides metrics collection, performance analysis, and alerting
 * Validates: Requirements 16.1, 16.2, 16.3, 16.4, 16.5
 */
public interface PerformanceMonitoringService {
    
    /**
     * Record page load time metric
     * @param pageName Name of the page
     * @param loadTimeMs Load time in milliseconds
     * @param userId User ID (optional)
     */
    void recordPageLoadTime(String pageName, long loadTimeMs, String userId);
    
    /**
     * Record search operation performance
     * @param searchType Type of search (global, entity-specific, etc.)
     * @param queryComplexity Complexity indicator (simple, medium, complex)
     * @param resultCount Number of results returned
     * @param executionTimeMs Execution time in milliseconds
     */
    void recordSearchPerformance(String searchType, String queryComplexity, 
                                int resultCount, long executionTimeMs);
    
    /**
     * Record filter operation performance
     * @param entityType Entity being filtered
     * @param filterCount Number of active filters
     * @param resultCount Number of results after filtering
     * @param executionTimeMs Execution time in milliseconds
     */
    void recordFilterPerformance(String entityType, int filterCount, 
                                int resultCount, long executionTimeMs);
    
    /**
     * Record database query performance
     * @param queryType Type of query (SELECT, INSERT, UPDATE, DELETE)
     * @param entityType Entity involved in query
     * @param executionTimeMs Execution time in milliseconds
     * @param recordCount Number of records affected
     */
    void recordDatabaseQueryPerformance(String queryType, String entityType, 
                                       long executionTimeMs, int recordCount);
    
    /**
     * Record API endpoint performance
     * @param endpoint API endpoint path
     * @param httpMethod HTTP method
     * @param responseTimeMs Response time in milliseconds
     * @param statusCode HTTP status code
     * @param requestSize Request size in bytes (optional)
     * @param responseSize Response size in bytes (optional)
     */
    void recordApiPerformance(String endpoint, String httpMethod, long responseTimeMs, 
                             int statusCode, Long requestSize, Long responseSize);
    
    /**
     * Record cache performance metrics
     * @param cacheName Name of the cache
     * @param operation Cache operation (hit, miss, eviction, etc.)
     * @param executionTimeMs Time taken for cache operation
     */
    void recordCachePerformance(String cacheName, String operation, long executionTimeMs);
    
    /**
     * Get current performance metrics summary
     * @return Map containing current performance metrics
     */
    Map<String, Object> getCurrentPerformanceMetrics();
    
    /**
     * Get performance metrics for a specific time range
     * @param startTime Start time for metrics collection
     * @param endTime End time for metrics collection
     * @return Map containing performance metrics for the specified range
     */
    Map<String, Object> getPerformanceMetrics(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Get performance alerts for current system state
     * @return Map containing active performance alerts
     */
    Map<String, Object> getPerformanceAlerts();
    
    /**
     * Check if page load time meets requirements (2 seconds)
     * @param pageName Name of the page to check
     * @return true if page meets performance requirements
     */
    boolean isPagePerformanceAcceptable(String pageName);
    
    /**
     * Check if search operation meets requirements (3 seconds)
     * @param searchType Type of search to check
     * @return true if search meets performance requirements
     */
    boolean isSearchPerformanceAcceptable(String searchType);
    
    /**
     * Get performance optimization recommendations
     * @return Map containing optimization recommendations
     */
    Map<String, Object> getOptimizationRecommendations();
    
    /**
     * Trigger performance analysis asynchronously
     * @return CompletableFuture containing analysis results
     */
    CompletableFuture<Map<String, Object>> analyzePerformanceAsync();
    
    /**
     * Clear performance metrics older than specified days
     * @param retentionDays Number of days to retain metrics
     */
    void cleanupOldMetrics(int retentionDays);
    
    /**
     * Export performance metrics to external monitoring system
     * @param exportFormat Format for export (json, csv, prometheus)
     * @return Exported metrics data
     */
    String exportMetrics(String exportFormat);
}