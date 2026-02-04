package com.petclinic.backend.controller;

import com.petclinic.backend.dto.StandardApiResponse;
import com.petclinic.backend.service.DataSeedingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for managing database seeding operations (development/staging only)
 */
@RestController
@RequestMapping("/api/admin/data-seeding")
@CrossOrigin(origins = "${pet-clinic.cors.allowed-origins}")
@Profile({"dev", "staging"})
@Tag(name = "Data Seeding", description = "Database seeding operations for development and testing")
public class DataSeedingController {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSeedingController.class);
    
    private final DataSeedingService dataSeedingService;
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public DataSeedingController(DataSeedingService dataSeedingService, JdbcTemplate jdbcTemplate) {
        this.dataSeedingService = dataSeedingService;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Operation(summary = "Seed database with dummy data", 
               description = "Generates and inserts dummy data for development and testing purposes")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Data seeding completed successfully"),
        @ApiResponse(responseCode = "500", description = "Error during data seeding")
    })
    @PostMapping("/seed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StandardApiResponse<Map<String, Object>>> seedDatabase() {
        try {
            logger.info("Manual data seeding requested");
            
            Map<String, Object> beforeCounts = getDatabaseCounts();
            
            dataSeedingService.seedAllData();
            
            Map<String, Object> afterCounts = getDatabaseCounts();
            Map<String, Object> result = new HashMap<>();
            result.put("before", beforeCounts);
            result.put("after", afterCounts);
            result.put("message", "Database seeding completed successfully");
            
            logger.info("Manual data seeding completed successfully");
            return ResponseEntity.ok(StandardApiResponse.success("Database seeded successfully", result));
            
        } catch (Exception e) {
            logger.error("Error during manual data seeding: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(StandardApiResponse.error("DATA_SEEDING_ERROR", "Failed to seed database: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Get database statistics", 
               description = "Returns current count of records in each table")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StandardApiResponse<Map<String, Object>>> getDatabaseStatistics() {
        try {
            DataSeedingService.DataSeedingStatistics stats = dataSeedingService.getStatistics();
            Map<String, Object> result = new HashMap<>();
            result.put("owners", stats.getOwnerCount());
            result.put("pets", stats.getPetCount());
            result.put("veterinarians", stats.getVeterinarianCount());
            result.put("visits", stats.getVisitCount());
            result.put("users", stats.getUserCount());
            
            return ResponseEntity.ok(StandardApiResponse.success("Database statistics retrieved", result));
        } catch (Exception e) {
            logger.error("Error retrieving database statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(StandardApiResponse.error("STATISTICS_ERROR", "Failed to retrieve statistics: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Clear all dummy data", 
               description = "Removes all dummy data from the database (keeps core system data)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dummy data cleared successfully"),
        @ApiResponse(responseCode = "500", description = "Error during data clearing")
    })
    @DeleteMapping("/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StandardApiResponse<Map<String, Object>>> clearDummyData() {
        try {
            logger.info("Clearing dummy data requested");
            
            Map<String, Object> beforeCounts = getDatabaseCounts();
            
            dataSeedingService.clearAllData();
            
            Map<String, Object> afterCounts = getDatabaseCounts();
            Map<String, Object> result = new HashMap<>();
            result.put("before", beforeCounts);
            result.put("after", afterCounts);
            result.put("message", "Dummy data cleared successfully");
            
            logger.info("Dummy data clearing completed successfully");
            return ResponseEntity.ok(StandardApiResponse.success("Dummy data cleared successfully", result));
            
        } catch (Exception e) {
            logger.error("Error during dummy data clearing: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(StandardApiResponse.error("DATA_CLEARING_ERROR", "Failed to clear dummy data: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Reset database to initial state", 
               description = "Clears all data and re-runs initial migrations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Database reset successfully"),
        @ApiResponse(responseCode = "500", description = "Error during database reset")
    })
    @PostMapping("/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StandardApiResponse<Void>> resetDatabase() {
        try {
            logger.warn("Database reset requested - this will clear ALL data");
            
            // This is a dangerous operation, so we'll just clear the data tables
            // and let Flyway handle the schema
            jdbcTemplate.update("SET FOREIGN_KEY_CHECKS = 0");
            
            String[] tables = {
                "audit_logs", "vaccinations", "medical_records", "appointments", 
                "visits", "pets", "owners", "vet_specialties", "veterinarians", 
                "specialties", "users", "system_settings"
            };
            
            for (String table : tables) {
                try {
                    jdbcTemplate.update("TRUNCATE TABLE " + table);
                } catch (Exception e) {
                    logger.warn("Could not truncate table {}: {}", table, e.getMessage());
                }
            }
            
            jdbcTemplate.update("SET FOREIGN_KEY_CHECKS = 1");
            
            logger.info("Database reset completed successfully");
            return ResponseEntity.ok(StandardApiResponse.success("Database reset successfully - restart application to re-run migrations"));
            
        } catch (Exception e) {
            logger.error("Error during database reset: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(StandardApiResponse.error("DATABASE_RESET_ERROR", "Failed to reset database: " + e.getMessage()));
        }
    }
    
    private Map<String, Object> getDatabaseCounts() {
        Map<String, Object> counts = new HashMap<>();
        
        String[] tables = {
            "owners", "pets", "veterinarians", "specialties", "visits", 
            "appointments", "medical_records", "vaccinations", "users", 
            "system_settings", "audit_logs"
        };
        
        for (String table : tables) {
            try {
                Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
                counts.put(table, count != null ? count : 0);
            } catch (Exception e) {
                counts.put(table, "Error: " + e.getMessage());
            }
        }
        
        return counts;
    }
}