package com.petclinic.backend.controller;

import com.petclinic.backend.service.ProgressIndicatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for progress indicators
 * Provides endpoints for tracking long-running operations
 * Validates: Requirements 16.4, 16.5
 */
@RestController
@RequestMapping("/api/progress")
@Tag(name = "Progress", description = "Progress tracking APIs for long-running operations")
public class ProgressController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProgressController.class);
    
    private final ProgressIndicatorService progressIndicatorService;
    
    @Autowired
    public ProgressController(ProgressIndicatorService progressIndicatorService) {
        this.progressIndicatorService = progressIndicatorService;
    }
    
    @GetMapping("/{progressId}")
    @Operation(summary = "Get progress status", 
               description = "Retrieve current progress status for a specific operation")
    @ApiResponse(responseCode = "200", description = "Progress status retrieved successfully")
    public ResponseEntity<Map<String, Object>> getProgress(
            @Parameter(description = "Progress indicator ID")
            @PathVariable String progressId) {
        
        try {
            Map<String, Object> progress = progressIndicatorService.getProgress(progressId);
            
            if (progress.containsKey("error")) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(progress);
            
        } catch (Exception e) {
            logger.error("Error getting progress for {}: {}", progressId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get progress: " + e.getMessage()));
        }
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get all active operations", 
               description = "Retrieve progress status for all currently active operations")
    @ApiResponse(responseCode = "200", description = "Active operations retrieved successfully")
    public ResponseEntity<Map<String, Map<String, Object>>> getAllActiveProgress() {
        try {
            Map<String, Map<String, Object>> activeProgress = progressIndicatorService.getAllActiveProgress();
            return ResponseEntity.ok(activeProgress);
            
        } catch (Exception e) {
            logger.error("Error getting all active progress: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/{progressId}/cancel")
    @Operation(summary = "Cancel operation", 
               description = "Cancel a running operation")
    @ApiResponse(responseCode = "200", description = "Operation cancelled successfully")
    @ApiResponse(responseCode = "404", description = "Operation not found or already completed")
    public ResponseEntity<Map<String, Object>> cancelOperation(
            @Parameter(description = "Progress indicator ID")
            @PathVariable String progressId) {
        
        try {
            boolean cancelled = progressIndicatorService.cancelOperation(progressId);
            
            if (cancelled) {
                return ResponseEntity.ok(Map.of(
                    "message", "Operation cancelled successfully",
                    "progressId", progressId,
                    "cancelled", true
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "message", "Operation could not be cancelled",
                        "progressId", progressId,
                        "cancelled", false
                    ));
            }
            
        } catch (Exception e) {
            logger.error("Error cancelling operation {}: {}", progressId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to cancel operation: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/cleanup")
    @Operation(summary = "Cleanup old progress indicators", 
               description = "Remove completed progress indicators older than specified minutes")
    @ApiResponse(responseCode = "200", description = "Cleanup completed successfully")
    public ResponseEntity<Map<String, Object>> cleanupOldProgress(
            @Parameter(description = "Number of minutes to retain completed operations")
            @RequestParam(defaultValue = "30") int retentionMinutes) {
        
        try {
            progressIndicatorService.cleanupOldProgress(retentionMinutes);
            
            return ResponseEntity.ok(Map.of(
                "message", "Cleanup completed successfully",
                "retentionMinutes", retentionMinutes
            ));
            
        } catch (Exception e) {
            logger.error("Error during progress cleanup: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to cleanup progress indicators: " + e.getMessage()));
        }
    }
}