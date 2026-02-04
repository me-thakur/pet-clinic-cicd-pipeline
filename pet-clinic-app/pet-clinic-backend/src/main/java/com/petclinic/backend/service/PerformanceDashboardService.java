package com.petclinic.backend.service;

import java.util.Map;

/**
 * Service for performance dashboard and reporting
 * Validates: Requirements 16.2, 16.3, 16.4, 16.5
 */
public interface PerformanceDashboardService {
    
    /**
     * Get comprehensive performance dashboard data
     */
    Map<String, Object> getPerformanceDashboard();
    
    /**
     * Get real-time performance metrics
     */
    Map<String, Object> getRealTimeMetrics();
    
    /**
     * Get performance trends over time
     */
    Map<String, Object> getPerformanceTrends(String timeRange);
    
    /**
     * Get system health status
     */
    Map<String, Object> getSystemHealthStatus();
    
    /**
     * Get performance bottlenecks analysis
     */
    Map<String, Object> getBottlenecksAnalysis();
    
    /**
     * Get optimization recommendations
     */
    Map<String, Object> getOptimizationRecommendations();
    
    /**
     * Generate performance report
     */
    String generatePerformanceReport(String format, String timeRange);
}