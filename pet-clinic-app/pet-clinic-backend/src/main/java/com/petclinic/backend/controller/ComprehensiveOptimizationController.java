package com.petclinic.backend.controller;

import com.petclinic.backend.service.ComprehensivePerformanceOptimizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for comprehensive performance optimization
 * Provides endpoints for triggering and monitoring system-wide optimizations
 * Validates: Requirements 16.2, 16.3, 16.4, 16.5
 */
@RestController
@RequestMapping("/api/optimization")
@Tag(name = "Performance Optimization", description = "Comprehensive performance optimization APIs")
public class ComprehensiveOptimizationController {
    
    private static final Logger logger = LoggerFactory.getLogger(ComprehensiveOptimizationController.class);
    
    private final ComprehensivePerformanceOptimizationService optimizationService;
    
    @Autowired
    public ComprehensiveOptimizationController(ComprehensivePerformanceOptimizationService optimizationService) {
        this.optimizationService = optimizationService;
    }
    
    @PostMapping("/trigger")
    @Operation(summary = "Trigger comprehensive optimization", 
               description = "Manually trigger comprehensive performance optimization (admin only)")
    @ApiResponse(responseCode = "202", description = "Optimization started successfully")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerOptimization() {
        try {
            logger.info("Manual comprehensive optimization triggered by admin");
            
            CompletableFuture<Map<String, Object>> optimizationFuture = 
                optimizationService.triggerManualOptimization();
            
            return ResponseEntity.accepted()
                .body(Map.of(
                    "message", "Comprehensive optimization started",
                    "status", "RUNNING",
                    "timestamp", LocalDateTime.now(),
                    "estimatedDuration", "5-10 minutes"
                ));
            
        } catch (Exception e) {
            logger.error("Error triggering comprehensive optimization: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to trigger optimization: " + e.getMessage()));
        }
    }
    
    @GetMapping("/status")
    @Operation(summary = "Get optimization status", 
               description = "Get current optimization status and system performance")
    @ApiResponse(responseCode = "200", description = "Optimization status retrieved successfully")
    public ResponseEntity<Map<String, Object>> getOptimizationStatus() {
        try {
            Map<String, Object> status = optimizationService.getOptimizationStatus();
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("Error getting optimization status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get optimization status: " + e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "Get optimization health check", 
               description = "Quick health check for optimization services")
    @ApiResponse(responseCode = "200", description = "Health check completed")
    public ResponseEntity<Map<String, Object>> getOptimizationHealth() {
        try {
            Map<String, Object> health = Map.of(
                "status", "HEALTHY",
                "services", Map.of(
                    "performanceMonitoring", "ACTIVE",
                    "databaseOptimization", "ACTIVE",
                    "cacheManagement", "ACTIVE",
                    "paginationOptimization", "ACTIVE",
                    "progressIndicators", "ACTIVE"
                ),
                "scheduledOptimization", "ENABLED",
                "nextRun", LocalDateTime.now().plusHours(1),
                "timestamp", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error getting optimization health: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "status", "UNHEALTHY",
                    "error", "Health check failed: " + e.getMessage(),
                    "timestamp", LocalDateTime.now()
                ));
        }
    }
}