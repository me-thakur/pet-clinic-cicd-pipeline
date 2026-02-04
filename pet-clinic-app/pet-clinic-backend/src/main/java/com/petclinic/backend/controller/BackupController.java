package com.petclinic.backend.controller;

import com.petclinic.backend.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for database backup and restore operations.
 * Provides endpoints for creating backups, restoring from backups,
 * and managing backup files.
 */
@RestController
@RequestMapping("/api/backup")
@Tag(name = "Backup Management", description = "Database backup and restore operations")
@PreAuthorize("hasRole('ADMIN')")
public class BackupController {
    
    private static final Logger logger = LoggerFactory.getLogger(BackupController.class);
    
    private final BackupService backupService;
    
    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }
    
    @PostMapping("/create")
    @Operation(summary = "Create database backup", 
               description = "Creates a backup of the current database state")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Backup created successfully"),
        @ApiResponse(responseCode = "500", description = "Backup creation failed")
    })
    public ResponseEntity<Map<String, Object>> createBackup(
            @Parameter(description = "Optional name for the backup")
            @RequestParam(required = false) String backupName) {
        
        logger.info("Creating database backup with name: {}", backupName);
        
        File backupFile = backupService.createBackup(backupName);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Backup created successfully");
        response.put("backupFile", backupFile.getName());
        response.put("backupSize", backupService.getBackupSize(backupFile));
        response.put("timestamp", backupService.getBackupTimestamp(backupFile));
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/restore")
    @Operation(summary = "Restore database from backup", 
               description = "Restores the database from an uploaded backup file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Database restored successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid backup file"),
        @ApiResponse(responseCode = "500", description = "Restore operation failed")
    })
    public ResponseEntity<Map<String, Object>> restoreFromUpload(
            @Parameter(description = "Backup file to restore from")
            @RequestParam("file") MultipartFile file) throws IOException {
        
        logger.info("Restoring database from uploaded file: {}", file.getOriginalFilename());
        
        // Save uploaded file temporarily
        File tempFile = File.createTempFile("restore_", ".sql");
        file.transferTo(tempFile);
        
        try {
            backupService.restoreFromBackup(tempFile);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Database restored successfully");
            response.put("restoredFrom", file.getOriginalFilename());
            
            return ResponseEntity.ok(response);
            
        } finally {
            // Clean up temporary file
            tempFile.delete();
        }
    }
    
    @PostMapping("/restore/{backupFileName}")
    @Operation(summary = "Restore from existing backup", 
               description = "Restores the database from an existing backup file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Database restored successfully"),
        @ApiResponse(responseCode = "404", description = "Backup file not found"),
        @ApiResponse(responseCode = "500", description = "Restore operation failed")
    })
    public ResponseEntity<Map<String, Object>> restoreFromExisting(
            @Parameter(description = "Name of the backup file to restore from")
            @PathVariable String backupFileName) {
        
        logger.info("Restoring database from existing backup: {}", backupFileName);
        
        List<File> backups = backupService.listBackups();
        File backupFile = backups.stream()
                .filter(f -> f.getName().equals(backupFileName))
                .findFirst()
                .orElse(null);
        
        if (backupFile == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Backup file not found: " + backupFileName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        backupService.restoreFromBackup(backupFile);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Database restored successfully");
        response.put("restoredFrom", backupFileName);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/list")
    @Operation(summary = "List available backups", 
               description = "Returns a list of all available backup files")
    @ApiResponse(responseCode = "200", description = "Backup list retrieved successfully")
    public ResponseEntity<List<Map<String, Object>>> listBackups() {
        
        List<File> backups = backupService.listBackups();
        
        List<Map<String, Object>> backupInfo = backups.stream()
                .map(file -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("fileName", file.getName());
                    info.put("size", backupService.getBackupSize(file));
                    info.put("timestamp", backupService.getBackupTimestamp(file));
                    info.put("valid", backupService.validateBackup(file));
                    return info;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(backupInfo);
    }
    
    @GetMapping("/download/{backupFileName}")
    @Operation(summary = "Download backup file", 
               description = "Downloads a backup file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Backup file downloaded successfully"),
        @ApiResponse(responseCode = "404", description = "Backup file not found")
    })
    public ResponseEntity<Resource> downloadBackup(
            @Parameter(description = "Name of the backup file to download")
            @PathVariable String backupFileName) {
        
        List<File> backups = backupService.listBackups();
        File backupFile = backups.stream()
                .filter(f -> f.getName().equals(backupFileName))
                .findFirst()
                .orElse(null);
        
        if (backupFile == null || !backupFile.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        Resource resource = new FileSystemResource(backupFile);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                       "attachment; filename=\"" + backupFile.getName() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate, private")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .header("X-Content-Type-Options", "nosniff")
                .header("X-Download-Options", "noopen")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
    
    @DeleteMapping("/{backupFileName}")
    @Operation(summary = "Delete backup file", 
               description = "Deletes a backup file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Backup file deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Backup file not found")
    })
    public ResponseEntity<Map<String, Object>> deleteBackup(
            @Parameter(description = "Name of the backup file to delete")
            @PathVariable String backupFileName) {
        
        List<File> backups = backupService.listBackups();
        File backupFile = backups.stream()
                .filter(f -> f.getName().equals(backupFileName))
                .findFirst()
                .orElse(null);
        
        Map<String, Object> response = new HashMap<>();
        
        if (backupFile == null) {
            response.put("success", false);
            response.put("message", "Backup file not found: " + backupFileName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        boolean deleted = backupService.deleteBackup(backupFile);
        
        if (deleted) {
            response.put("success", true);
            response.put("message", "Backup file deleted successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Failed to delete backup file");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/validate/{backupFileName}")
    @Operation(summary = "Validate backup file", 
               description = "Validates the integrity of a backup file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Backup validation completed"),
        @ApiResponse(responseCode = "404", description = "Backup file not found")
    })
    public ResponseEntity<Map<String, Object>> validateBackup(
            @Parameter(description = "Name of the backup file to validate")
            @PathVariable String backupFileName) {
        
        List<File> backups = backupService.listBackups();
        File backupFile = backups.stream()
                .filter(f -> f.getName().equals(backupFileName))
                .findFirst()
                .orElse(null);
        
        Map<String, Object> response = new HashMap<>();
        
        if (backupFile == null) {
            response.put("success", false);
            response.put("message", "Backup file not found: " + backupFileName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        boolean isValid = backupService.validateBackup(backupFile);
        
        response.put("fileName", backupFileName);
        response.put("valid", isValid);
        response.put("message", isValid ? "Backup file is valid" : "Backup file is invalid or corrupted");
        
        return ResponseEntity.ok(response);
    }
}