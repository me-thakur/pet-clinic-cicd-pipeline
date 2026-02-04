package com.petclinic.backend.controller;

import com.petclinic.backend.service.PerformanceDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * Controller for performance dashboard views and API endpoints
 * Provides both web pages and REST endpoints for performance monitoring
 * Validates: Requirements 16.2, 16.3, 16.4, 16.5
 */
@Controller
@RequestMapping("/performance")
@Tag(name = "Performance Dashboard", description = "Performance dashboard and monitoring views")
public class PerformanceDashboardController {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceDashboardController.class);
    
    private final PerformanceDashboardService performanceDashboardService;
    
    @Autowired
    public PerformanceDashboardController(PerformanceDashboardService performanceDashboardService) {
        this.performanceDashboardService = performanceDashboardService;
    }
    
    @GetMapping("")
    @Operation(summary = "Performance dashboard page", 
               description = "Display the main performance monitoring dashboard")
    @ApiResponse(responseCode = "200", description = "Dashboard page displayed successfully")
    public String performanceDashboard() {
        return "performance-dashboard";
    }
    
    @GetMapping("/dashboard")
    @ResponseBody
    @Operation(summary = "Get dashboard data", 
               description = "Retrieve comprehensive dashboard data for the performance monitoring page")
    @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        try {
            Map<String, Object> dashboardData = performanceDashboardService.getPerformanceDashboard();
            return ResponseEntity.ok(dashboardData);
            
        } catch (Exception e) {
            logger.error("Error getting dashboard data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to retrieve dashboard data: " + e.getMessage()));
        }
    }
    
    @GetMapping("/realtime")
    @ResponseBody
    @Operation(summary = "Get real-time metrics", 
               description = "Retrieve real-time performance metrics")
    @ApiResponse(responseCode = "200", description = "Real-time metrics retrieved successfully")
    public ResponseEntity<Map<String, Object>> getRealTimeMetrics() {
        try {
            Map<String, Object> realTimeMetrics = performanceDashboardService.getRealTimeMetrics();
            return ResponseEntity.ok(realTimeMetrics);
            
        } catch (Exception e) {
            logger.error("Error getting real-time metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to retrieve real-time metrics: " + e.getMessage()));
        }
    }
}