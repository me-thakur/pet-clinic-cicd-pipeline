package com.petclinic.backend.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for comprehensive performance optimization
 * Coordinates all performance optimization activities
 * Validates: Requirements 16.2, 16.3, 16.4, 16.5
 */
public interface ComprehensivePerformanceOptimizationService {
    
    /**
     * Perform comprehensive performance optimization
     * @return CompletableFuture with optimization results
     */
    CompletableFuture<Map<String, Object>> performComprehensiveOptimization();
    
    /**
     * Trigger manual optimization
     * @return CompletableFuture with optimization results
     */
    CompletableFuture<Map<String, Object>> triggerManualOptimization();
    
    /**
     * Get current optimization status
     * @return Map containing optimization status information
     */
    Map<String, Object> getOptimizationStatus();
}