package com.petclinic.backend.controller;

import com.petclinic.backend.config.FlywayConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for database migration monitoring and health checks.
 * Provides endpoints to check migration status and schema version.
 * 
 * Validates: Requirements 8.1
 */
@RestController
@RequestMapping("/api/admin/migrations")
@Tag(name = "Migration Management", description = "Database migration monitoring and health checks")
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true")
public class MigrationController {
    
    @Autowired(required = false)
    private FlywayConfig.FlywayMigrationInfo migrationInfo;
    
    /**
     * Get current database schema version and migration status.
     * Only accessible by admin users.
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get migration status",
        description = "Returns current database schema version and pending migration information"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Migration status retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "503", description = "Migration service unavailable")
    })
    public ResponseEntity<Map<String, Object>> getMigrationStatus() {
        if (migrationInfo == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "unavailable");
            response.put("message", "Migration service not available");
            return ResponseEntity.status(503).body(response);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("currentVersion", migrationInfo.getCurrentVersion());
        response.put("hasPendingMigrations", migrationInfo.hasPendingMigrations());
        response.put("status", "available");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint for migration system.
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Migration health check",
        description = "Simple health check for the migration system"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Migration system is healthy"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "503", description = "Migration system is unhealthy")
    })
    public ResponseEntity<Map<String, String>> getMigrationHealth() {
        Map<String, String> response = new HashMap<>();
        
        if (migrationInfo == null) {
            response.put("status", "DOWN");
            response.put("message", "Migration service not available");
            return ResponseEntity.status(503).body(response);
        }
        
        try {
            String version = migrationInfo.getCurrentVersion();
            response.put("status", "UP");
            response.put("version", version);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("message", "Migration health check failed: " + e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
    }
}