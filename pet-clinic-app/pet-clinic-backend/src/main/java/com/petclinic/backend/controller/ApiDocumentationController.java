package com.petclinic.backend.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller providing API documentation and information endpoints.
 * Offers quick access to API documentation links and system information.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "API Documentation", description = "API documentation and information endpoints")
public class ApiDocumentationController {

    @Value("${server.port:9090}")
    private String serverPort;

    @Value("${spring.application.name:Pet Clinic Backend}")
    private String applicationName;

    /**
     * Get API documentation links and information
     * 
     * @return Map containing API documentation URLs and system information
     */
    @Operation(
        summary = "Get API documentation information",
        description = "Provides links to interactive API documentation (Swagger UI) and OpenAPI specification. " +
                     "Use these links to explore and test the Pet Clinic Management System API.",
        tags = {"API Documentation"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "API documentation information retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class)
            )
        )
    })
    @GetMapping("/docs")
    public ResponseEntity<Map<String, Object>> getApiDocumentation() {
        Map<String, Object> apiInfo = new HashMap<>();
        
        // Basic API information
        apiInfo.put("name", applicationName);
        apiInfo.put("version", "1.0.0");
        apiInfo.put("description", "Comprehensive REST API for veterinary practice management");
        
        // Documentation URLs
        Map<String, String> documentationUrls = new HashMap<>();
        documentationUrls.put("swagger-ui", "http://localhost:" + serverPort + "/swagger-ui.html");
        documentationUrls.put("openapi-json", "http://localhost:" + serverPort + "/v3/api-docs");
        documentationUrls.put("openapi-yaml", "http://localhost:" + serverPort + "/v3/api-docs.yaml");
        apiInfo.put("documentation", documentationUrls);
        
        // API groups
        Map<String, String> apiGroups = new HashMap<>();
        apiGroups.put("pet-management", "Pet CRUD operations, search, and filtering");
        apiGroups.put("veterinarian-management", "Veterinarian management and availability tracking");
        apiGroups.put("visit-management", "Visit scheduling and management");
        apiGroups.put("authentication", "User authentication and authorization");
        apiGroups.put("reporting", "Reports and analytics");
        apiInfo.put("groups", apiGroups);
        
        // Quick start information
        Map<String, String> quickStart = new HashMap<>();
        quickStart.put("authentication", "POST /api/auth/login with username/password to get JWT token");
        quickStart.put("authorization", "Include 'Authorization: Bearer <token>' header in subsequent requests");
        quickStart.put("interactive-testing", "Use Swagger UI at /swagger-ui.html for interactive API testing");
        apiInfo.put("quickStart", quickStart);
        
        return ResponseEntity.ok(apiInfo);
    }

    /**
     * Get API health and status information
     * 
     * @return API health status
     */
    @Operation(
        summary = "Get API health status",
        description = "Returns the current health status of the Pet Clinic API including version and timestamp.",
        tags = {"API Documentation"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "API health status retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class)
            )
        )
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getApiHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Pet Clinic Backend API");
        health.put("version", "1.0.0");
        health.put("timestamp", java.time.LocalDateTime.now().toString());
        health.put("documentation", "http://localhost:" + serverPort + "/swagger-ui.html");
        
        return ResponseEntity.ok(health);
    }

    /**
     * Redirect to Swagger UI (hidden from documentation)
     * 
     * @return Redirect response to Swagger UI
     */
    @Hidden
    @GetMapping("/swagger")
    public ResponseEntity<Void> redirectToSwagger() {
        return ResponseEntity.status(302)
                .header("Location", "/swagger-ui.html")
                .build();
    }
}