package com.petclinic.backend.controller;

import com.petclinic.backend.service.CacheManagementService;
import com.petclinic.backend.service.PerformanceMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for performance monitoring and optimization
 * Provides endpoints for performance metrics, cache management, and optimization
 * Validates: Requirements 16.1, 16.2, 16.3, 16.4, 16.5
 */
@RestController
@RequestMapping("/api/performance")
@Tag(name = "Performance", description = "Performance monitoring and optimization APIs")
public class PerformanceController {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceController.class);
    
    private final PerformanceMonitoringService performanceMonitoringService;
    private final CacheManagementService cacheManagementService;
    
    @Autowired
    public PerformanceController(PerformanceMonitoringService performanceMonitoringService,
                                CacheManagementService cacheManagementService) {
        this.performanceMonitoringService = performanceMonitoringService;
        this.cacheManagementService = cacheManagementService;
    }
    
    @GetMapping("/metrics")
    @Operation(summary = "Get current performance metrics", 
               description = "Retrieve current system performance metrics including page load times, search performance, and resource usage")
    @ApiResponse(responseCode = "200", description = "Performance metrics retrieved successfully")
    public ResponseEntity<Map<String, Object>> getCurrentMetrics() {
        try {
            long startTime = System.currentTimeMillis();
            
            Map<String, Object> metrics = performanceMonitoringService.getCurrentPerformanceMetrics();
            
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordApiPerformance("/api/performance/metrics", "GET", 
                executionTime, 200, null, null);
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            logger.error("Error retrieving current performance metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to retrieve performance metrics: " + e.getMessage()));
        }
    }
    
    @GetMapping("/metrics/range")
    @Operation(summary = "Get performance metrics for time range", 
               description = "Retrieve performance metrics for a specific time range")
    @ApiResponse(responseCode = "200", description = "Performance metrics retrieved successfully")
    public ResponseEntity<Map<String, Object>> getMetricsForRange(
            @Parameter(description = "Start time for metrics collection")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "End time for metrics collection")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        try {
            long requestStartTime = System.currentTimeMillis();
            
            Map<String, Object> metrics = performanceMonitoringService.getPerformanceMetrics(startTime, endTime);
            
            long executionTime = System.currentTimeMillis() - requestStartTime;
            performanceMonitoringService.recordApiPerformance("/api/performance/metrics/range", "GET", 
                executionTime, 200, null, null);
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            logger.error("Error retrieving performance metrics for range: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to retrieve performance metrics: " + e.getMessage()));
        }
    }
    
    @GetMapping("/alerts")
    @Operation(summary = "Get performance alerts", 
               description = "Retrieve current performance alerts and warnings")
    @ApiResponse(responseCode = "200", description = "Performance alerts retrieved successfully")
    public ResponseEntity<Map<String, Object>> getPerformanceAlerts() {
        try {
            long startTime = System.currentTimeMillis();
            
            Map<String, Object> alerts = performanceMonitoringService.getPerformanceAlerts();
            
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordApiPerformance("/api/performance/alerts", "GET", 
                executionTime, 200, null, null);
            
            return ResponseEntity.ok(alerts);
            
        } catch (Exception e) {
            logger.error("Error retrieving performance alerts: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to retrieve performance alerts: " + e.getMessage()));
        }
    }
    
    @GetMapping("/recommendations")
    @Operation(summary = "Get optimization recommendations", 
               description = "Retrieve performance optimization recommendations based on current metrics")
    @ApiResponse(responseCode = "200", description = "Optimization recommendations retrieved successfully")
    public ResponseEntity<Map<String, Object>> getOptimizationRecommendations() {
        try {
            long startTime = System.currentTimeMillis();
            
            Map<String, Object> recommendations = performanceMonitoringService.getOptimizationRecommendations();
            
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordApiPerformance("/api/performance/recommendations", "GET", 
                executionTime, 200, null, null);
            
            return ResponseEntity.ok(recommendations);
            
        } catch (Exception e) {
            logger.error("Error retrieving optimization recommendations: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to retrieve optimization recommendations: " + e.getMessage()));
        }
    }
    
    @PostMapping("/analyze")
    @Operation(summary = "Trigger performance analysis", 
               description = "Trigger comprehensive performance analysis asynchronously")
    @ApiResponse(responseCode = "202", description = "Performance analysis started")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerPerformanceAnalysis() {
        try {
            long startTime = System.currentTimeMillis();
            
            CompletableFuture<Map<String, Object>> analysisResult = 
                performanceMonitoringService.analyzePerformanceAsync();
            
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordApiPerformance("/api/performance/analyze", "POST", 
                executionTime, 202, null, null);
            
            return ResponseEntity.accepted()
                .body(Map.of(
                    "message", "Performance analysis started",
                    "status", "RUNNING",
                    "timestamp", LocalDateTime.now()
                ));
            
        } catch (Exception e) {
            logger.error("Error triggering performance analysis: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to trigger performance analysis: " + e.getMessage()));
        }
    }
    
    @GetMapping("/check/page/{pageName}")
    @Operation(summary = "Check page performance", 
               description = "Check if a specific page meets performance requirements")
    @ApiResponse(responseCode = "200", description = "Page performance status retrieved")
    public ResponseEntity<Map<String, Object>> checkPagePerformance(
            @Parameter(description = "Name of the page to check")
            @PathVariable String pageName) {
        
        try {
            long startTime = System.currentTimeMillis();
            
            boolean acceptable = performanceMonitoringService.isPagePerformanceAcceptable(pageName);
            
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordApiPerformance("/api/performance/check/page/" + pageName, "GET", 
                executionTime, 200, null, null);
            
            return ResponseEntity.ok(Map.of(
                "pageName", pageName,
                "acceptable", acceptable,
                "threshold", "2000ms",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            logger.error("Error checking page performance for {}: {}", pageName, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to check page performance: " + e.getMessage()));
        }
    }
    
    @GetMapping("/check/search/{searchType}")
    @Operation(summary = "Check search performance", 
               description = "Check if a specific search type meets performance requirements")
    @ApiResponse(responseCode = "200", description = "Search performance status retrieved")
    public ResponseEntity<Map<String, Object>> checkSearchPerformance(
            @Parameter(description = "Type of search to check")
            @PathVariable String searchType) {
        
        try {
            long startTime = System.currentTimeMillis();
            
            boolean acceptable = performanceMonitoringService.isSearchPerformanceAcceptable(searchType);
            
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordApiPerformance("/api/performance/check/search/" + searchType, "GET", 
                executionTime, 200, null, null);
            
            return ResponseEntity.ok(Map.of(
                "searchType", searchType,
                "acceptable", acceptable,
                "threshold", "3000ms",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            logger.error("Error checking search performance for {}: {}", searchType, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to check search performance: " + e.getMessage()));
        }
    }
    
    @GetMapping("/cache/statistics")
    @Operation(summary = "Get cache statistics", 
               description = "Retrieve comprehensive cache performance statistics")
    @ApiResponse(responseCode = "200", description = "Cache statistics retrieved successfully")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        try {
            long startTime = System.currentTimeMillis();
            
            Map<String, Object> statistics = cacheManagementService.getCacheStatistics();
            
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordApiPerformance("/api/performance/cache/statistics", "GET", 
                executionTime, 200, null, null);
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error retrieving cache statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to retrieve cache statistics: " + e.getMessage()));
        }
    }
    
    @GetMapping("/cache/hit-rates")
    @Operation(summary = "Get cache hit rates", 
               description = "Retrieve cache hit rates for all caches")
    @ApiResponse(responseCode = "200", description = "Cache hit rates retrieved successfully")
    public ResponseEntity<Map<String, Double>> getCacheHitRates() {
        try {
            long startTime = System.currentTimeMillis();
            
            Map<String, Double> hitRates = cacheManagementService.getCacheHitRates();
            
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordApiPerformance("/api/performance/cache/hit-rates", "GET", 
                executionTime, 200, null, null);
            
            return ResponseEntity.ok(hitRates);
            
        } catch (Exception e) {
            logger.error("Error retrieving cache hit rates: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/cache/clear")
    @Operation(summary = "Clear all caches", 
               description = "Clear all application caches (admin only)")
    @ApiResponse(responseCode = "200", description = "All caches cleared successfully")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> clearAllCaches() {
        try {
            long startTime = System.currentTimeMillis();
            
            cacheManagementService.clearAllCaches();
            
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordApiPerformance("/api/performance/cache/clear", "POST", 
                executionTime, 200, null, null);
            
            return ResponseEntity.ok(Map.of(
                "message", "All caches cleared successfully",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            logger.error("Error clearing all caches: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to clear caches: " + e.getMessage()));
        }
    }
    
    @PostMapping("/cache/clear/{cacheName}")
    @Operation(summary = "Clear specific cache", 
               description = "Clear a specific cache by name (admin only)")
    @ApiResponse(responseCode = "200", description = "Cache cleared successfully")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> clearSpecificCache(
            @Parameter(description = "Name of the cache to clear")
            @PathVariable String cacheName) {
        
        try {
            long startTime = System.currentTimeMillis();
            
            cacheManagementService.clearCache(cacheName);
            
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordApiPerformance("/api/performance/cache/clear/" + cacheName, "POST", 
                executionTime, 200, null, null);
            
            return ResponseEntity.ok(Map.of(
                "message", "Cache '" + cacheName + "' cleared successfully",
                "cacheName", cacheName,
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            logger.error("Error clearing cache {}: {}", cacheName, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to clear cache: " + e.getMessage()));
        }
    }
    
    @PostMapping("/cache/warmup")
    @Operation(summary = "Warm up caches", 
               description = "Pre-populate caches with frequently accessed data (admin only)")
    @ApiResponse(responseCode = "200", description = "Cache warm-up completed successfully")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> warmUpCaches() {
        try {
            long startTime = System.currentTimeMillis();
            
            cacheManagementService.warmUpCaches();
            
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordApiPerformance("/api/performance/cache/warmup", "POST", 
                executionTime, 200, null, null);
            
            return ResponseEntity.ok(Map.of(
                "message", "Cache warm-up completed successfully",
                "executionTime", executionTime + "ms",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            logger.error("Error warming up caches: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to warm up caches: " + e.getMessage()));
        }
    }
    
    @GetMapping("/export/{format}")
    @Operation(summary = "Export performance metrics", 
               description = "Export performance metrics in specified format (admin only)")
    @ApiResponse(responseCode = "200", description = "Metrics exported successfully")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> exportMetrics(
            @Parameter(description = "Export format (json, csv, prometheus)")
            @PathVariable String format) {
        
        try {
            long startTime = System.currentTimeMillis();
            
            String exportedData = performanceMonitoringService.exportMetrics(format);
            
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordApiPerformance("/api/performance/export/" + format, "GET", 
                executionTime, 200, null, (long) exportedData.length());
            
            String contentType = getContentTypeForFormat(format);
            
            return ResponseEntity.ok()
                .header("Content-Type", contentType)
                .header("Content-Disposition", "attachment; filename=performance-metrics." + format)
                .body(exportedData);
            
        } catch (Exception e) {
            logger.error("Error exporting metrics in format {}: {}", format, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Error exporting metrics: " + e.getMessage());
        }
    }
    
    @PostMapping("/cleanup")
    @Operation(summary = "Cleanup old metrics", 
               description = "Remove performance metrics older than specified days (admin only)")
    @ApiResponse(responseCode = "200", description = "Metrics cleanup completed successfully")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cleanupOldMetrics(
            @Parameter(description = "Number of days to retain metrics")
            @RequestParam(defaultValue = "30") int retentionDays) {
        
        try {
            long startTime = System.currentTimeMillis();
            
            performanceMonitoringService.cleanupOldMetrics(retentionDays);
            
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordApiPerformance("/api/performance/cleanup", "POST", 
                executionTime, 200, null, null);
            
            return ResponseEntity.ok(Map.of(
                "message", "Metrics cleanup completed successfully",
                "retentionDays", retentionDays,
                "executionTime", executionTime + "ms",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            logger.error("Error cleaning up old metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to cleanup metrics: " + e.getMessage()));
        }
    }
    
    @PostMapping("/frontend-metrics")
    @Operation(summary = "Receive frontend performance metrics", 
               description = "Endpoint for frontend to send client-side performance metrics")
    @ApiResponse(responseCode = "200", description = "Frontend metrics received successfully")
    public ResponseEntity<Map<String, Object>> receiveFrontendMetrics(
            @RequestBody Map<String, Object> frontendMetrics) {
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Process frontend metrics
            processFrontendMetrics(frontendMetrics);
            
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordApiPerformance("/api/performance/frontend-metrics", "POST", 
                executionTime, 200, null, null);
            
            return ResponseEntity.ok(Map.of(
                "message", "Frontend metrics received successfully",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            logger.error("Error processing frontend metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to process frontend metrics: " + e.getMessage()));
        }
    }
    
    private String getContentTypeForFormat(String format) {
        switch (format.toLowerCase()) {
            case "json":
                return "application/json";
            case "csv":
                return "text/csv";
            case "prometheus":
                return "text/plain";
            default:
                return "text/plain";
        }
    }
    
    @SuppressWarnings("unchecked")
    private void processFrontendMetrics(Map<String, Object> frontendMetrics) {
        try {
            Map<String, Object> metrics = (Map<String, Object>) frontendMetrics.get("metrics");
            if (metrics == null) return;
            
            // Process page load metrics
            List<Map<String, Object>> pageLoads = (List<Map<String, Object>>) metrics.get("pageLoads");
            if (pageLoads != null) {
                for (Map<String, Object> pageLoad : pageLoads) {
                    String pageName = (String) pageLoad.get("pageName");
                    Number loadTime = (Number) pageLoad.get("loadTime");
                    if (pageName != null && loadTime != null) {
                        performanceMonitoringService.recordPageLoadTime(pageName, loadTime.longValue(), "frontend");
                    }
                }
            }
            
            // Process search metrics
            List<Map<String, Object>> searches = (List<Map<String, Object>>) metrics.get("searches");
            if (searches != null) {
                for (Map<String, Object> search : searches) {
                    String type = (String) search.get("type");
                    Number responseTime = (Number) search.get("responseTime");
                    if (type != null && responseTime != null) {
                        performanceMonitoringService.recordSearchPerformance(type, "frontend", 0, responseTime.longValue());
                    }
                }
            }
            
            // Process filter metrics
            List<Map<String, Object>> filters = (List<Map<String, Object>>) metrics.get("filters");
            if (filters != null) {
                for (Map<String, Object> filter : filters) {
                    String filterType = (String) filter.get("filterType");
                    Number responseTime = (Number) filter.get("responseTime");
                    if (filterType != null && responseTime != null) {
                        performanceMonitoringService.recordFilterPerformance(filterType, 1, 0, responseTime.longValue());
                    }
                }
            }
            
            // Process interaction metrics
            List<Map<String, Object>> interactions = (List<Map<String, Object>>) metrics.get("interactions");
            if (interactions != null) {
                for (Map<String, Object> interaction : interactions) {
                    String type = (String) interaction.get("type");
                    String target = (String) interaction.get("target");
                    Number responseTime = (Number) interaction.get("responseTime");
                    if (type != null && responseTime != null) {
                        // Record as API performance for interactions
                        performanceMonitoringService.recordApiPerformance(
                            target != null ? target : type, "INTERACTION", 
                            responseTime.longValue(), 200, null, null);
                    }
                }
            }
            
            logger.debug("Processed frontend metrics successfully");
            
        } catch (Exception e) {
            logger.error("Error processing frontend metrics: {}", e.getMessage(), e);
        }
    }
}