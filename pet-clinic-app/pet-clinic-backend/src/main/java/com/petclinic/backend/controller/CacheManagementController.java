package com.petclinic.backend.controller;

import com.petclinic.backend.service.CacheManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for cache management operations
 * Provides endpoints for cache administration and monitoring
 * Validates: Requirements 10.3
 */
@RestController
@RequestMapping("/api/admin/cache")
@Tag(name = "Cache Management", description = "Cache administration and monitoring operations")
@PreAuthorize("hasRole('ADMIN')")
public class CacheManagementController {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheManagementController.class);
    
    @Autowired
    private CacheManagementService cacheManagementService;
    
    @Operation(summary = "Get cache statistics", description = "Retrieve statistics for all caches")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cache statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        logger.debug("Getting cache statistics");
        
        Map<String, Object> statistics = cacheManagementService.getCacheStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    @Operation(summary = "Get cache hit rates", description = "Retrieve hit rates for all caches")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cache hit rates retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    @GetMapping("/hit-rates")
    public ResponseEntity<Map<String, Double>> getCacheHitRates() {
        logger.debug("Getting cache hit rates");
        
        Map<String, Double> hitRates = cacheManagementService.getCacheHitRates();
        return ResponseEntity.ok(hitRates);
    }
    
    @Operation(summary = "Clear all caches", description = "Clear all application caches")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All caches cleared successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    @PostMapping("/clear-all")
    public ResponseEntity<String> clearAllCaches() {
        logger.info("Clearing all caches via admin request");
        
        cacheManagementService.clearAllCaches();
        return ResponseEntity.ok("All caches cleared successfully");
    }
    
    @Operation(summary = "Clear specific cache", description = "Clear a specific cache by name")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cache cleared successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid cache name"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    @PostMapping("/clear/{cacheName}")
    public ResponseEntity<String> clearCache(
            @Parameter(description = "Name of the cache to clear")
            @PathVariable String cacheName) {
        logger.info("Clearing cache '{}' via admin request", cacheName);
        
        cacheManagementService.clearCache(cacheName);
        return ResponseEntity.ok("Cache '" + cacheName + "' cleared successfully");
    }
    
    @Operation(summary = "Clear entity caches", description = "Clear caches related to a specific entity type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Entity caches cleared successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid entity type"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    @PostMapping("/clear-entity/{entityType}")
    public ResponseEntity<String> clearEntityCaches(
            @Parameter(description = "Entity type (pet, veterinarian, visit, owner)")
            @PathVariable String entityType) {
        logger.info("Clearing caches for entity type '{}' via admin request", entityType);
        
        cacheManagementService.clearEntityCaches(entityType);
        return ResponseEntity.ok("Caches for entity type '" + entityType + "' cleared successfully");
    }
    
    @Operation(summary = "Warm up caches", description = "Pre-load caches with frequently accessed data")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Caches warmed up successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    @PostMapping("/warm-up")
    public ResponseEntity<String> warmUpCaches() {
        logger.info("Warming up caches via admin request");
        
        cacheManagementService.warmUpCaches();
        return ResponseEntity.ok("Caches warmed up successfully");
    }
    
    @Operation(summary = "Invalidate search caches", description = "Clear search result caches")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search caches invalidated successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    @PostMapping("/invalidate-search")
    public ResponseEntity<String> invalidateSearchCaches() {
        logger.info("Invalidating search caches via admin request");
        
        cacheManagementService.invalidateSearchCaches();
        return ResponseEntity.ok("Search caches invalidated successfully");
    }
    
    @Operation(summary = "Invalidate statistics caches", description = "Clear statistics caches")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics caches invalidated successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    @PostMapping("/invalidate-statistics")
    public ResponseEntity<String> invalidateStatisticsCaches() {
        logger.info("Invalidating statistics caches via admin request");
        
        cacheManagementService.invalidateStatisticsCaches();
        return ResponseEntity.ok("Statistics caches invalidated successfully");
    }
    
    @Operation(summary = "Invalidate dashboard caches", description = "Clear dashboard metrics caches")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dashboard caches invalidated successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    @PostMapping("/invalidate-dashboard")
    public ResponseEntity<String> invalidateDashboardCaches() {
        logger.info("Invalidating dashboard caches via admin request");
        
        cacheManagementService.invalidateDashboardCaches();
        return ResponseEntity.ok("Dashboard caches invalidated successfully");
    }
}