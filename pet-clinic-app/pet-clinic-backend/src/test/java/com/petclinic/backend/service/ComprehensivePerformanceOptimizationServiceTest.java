package com.petclinic.backend.service;

import com.petclinic.backend.service.impl.ComprehensivePerformanceOptimizationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ComprehensivePerformanceOptimizationService
 * Validates: Requirements 16.2, 16.3, 16.4, 16.5
 */
@ExtendWith(MockitoExtension.class)
class ComprehensivePerformanceOptimizationServiceTest {
    
    @Mock
    private PerformanceMonitoringService performanceMonitoringService;
    
    @Mock
    private DatabaseOptimizationService databaseOptimizationService;
    
    @Mock
    private EfficientPaginationService paginationService;
    
    @Mock
    private CacheManagementService cacheManagementService;
    
    @Mock
    private ProgressIndicatorService progressIndicatorService;
    
    private ComprehensivePerformanceOptimizationServiceImpl optimizationService;
    
    @BeforeEach
    void setUp() {
        optimizationService = new ComprehensivePerformanceOptimizationServiceImpl(
            performanceMonitoringService,
            databaseOptimizationService,
            paginationService,
            cacheManagementService,
            progressIndicatorService
        );
    }
    
    @Test
    void testGetOptimizationStatus() {
        // Given
        Map<String, Object> mockMetrics = Map.of("pageLoadAverage", 1200.0);
        Map<String, Object> mockCacheStats = Map.of("hitRate", 0.85);
        
        when(performanceMonitoringService.getCurrentPerformanceMetrics()).thenReturn(mockMetrics);
        when(cacheManagementService.getCacheStatistics()).thenReturn(mockCacheStats);
        
        // When
        Map<String, Object> status = optimizationService.getOptimizationStatus();
        
        // Then
        assertNotNull(status);
        assertTrue(status.containsKey("currentPerformance"));
        assertTrue(status.containsKey("systemHealth"));
        assertTrue(status.containsKey("cacheStatistics"));
        assertTrue(status.containsKey("timestamp"));
        
        verify(performanceMonitoringService).getCurrentPerformanceMetrics();
        verify(cacheManagementService).getCacheStatistics();
    }
    
    @Test
    void testTriggerManualOptimization() {
        // Given
        when(progressIndicatorService.createProgressIndicator(anyString(), anyString(), anyInt(), anyString()))
            .thenReturn("test-progress-id");
        when(performanceMonitoringService.getCurrentPerformanceMetrics())
            .thenReturn(Map.of("pageLoadAverage", 1500.0));
        when(databaseOptimizationService.analyzeSlowQueries())
            .thenReturn(Map.of("recommendations", java.util.List.of("Add index on owners.last_name")));
        
        // When
        CompletableFuture<Map<String, Object>> result = optimizationService.triggerManualOptimization();
        
        // Then
        assertNotNull(result);
        
        // Wait for completion and verify
        Map<String, Object> optimizationResult = result.join();
        assertNotNull(optimizationResult);
        assertTrue(optimizationResult.containsKey("status"));
        
        verify(progressIndicatorService).createProgressIndicator(anyString(), anyString(), anyInt(), anyString());
        verify(performanceMonitoringService).getCurrentPerformanceMetrics();
        verify(databaseOptimizationService).optimizeQueries();
    }
    
    @Test
    void testOptimizationStatusContainsRequiredFields() {
        // Given
        when(performanceMonitoringService.getCurrentPerformanceMetrics())
            .thenReturn(Map.of("pageLoadAverage", 1200.0, "searchAverage", 800.0));
        when(cacheManagementService.getCacheStatistics())
            .thenReturn(Map.of("hitRate", 0.92));
        
        // When
        Map<String, Object> status = optimizationService.getOptimizationStatus();
        
        // Then
        assertNotNull(status.get("currentPerformance"));
        assertNotNull(status.get("systemHealth"));
        assertNotNull(status.get("cacheStatistics"));
        assertNotNull(status.get("lastOptimizationRun"));
        assertNotNull(status.get("nextOptimizationRun"));
        assertNotNull(status.get("timestamp"));
    }
    
    @Test
    void testOptimizationHandlesExceptions() {
        // Given
        when(performanceMonitoringService.getCurrentPerformanceMetrics())
            .thenThrow(new RuntimeException("Service unavailable"));
        
        // When
        Map<String, Object> status = optimizationService.getOptimizationStatus();
        
        // Then
        assertNotNull(status);
        assertTrue(status.containsKey("error"));
        assertTrue(status.get("error").toString().contains("Failed to get optimization status"));
    }
}