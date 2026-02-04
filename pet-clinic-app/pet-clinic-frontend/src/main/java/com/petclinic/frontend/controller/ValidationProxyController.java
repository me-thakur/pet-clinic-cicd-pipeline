package com.petclinic.frontend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Proxy controller to forward validation requests to the backend API
 * This allows the frontend JavaScript to make validation calls that are proxied to the backend
 */
@RestController
@RequestMapping("/api/validation")
public class ValidationProxyController {

    @Value("${pet-clinic.backend.url:http://localhost:9090}")
    private String backendUrl;

    private final RestTemplate restTemplate;

    public ValidationProxyController() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Health check endpoint for validation service
     */
    @GetMapping("/owners/health")
    public ResponseEntity<?> checkValidationHealth() {
        try {
            String url = backendUrl + "/api/validation/owners/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                "status", "ERROR",
                "message", "Validation service error: " + e.getMessage()
            ));
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", "UNAVAILABLE",
                "message", "Unable to connect to validation service: " + e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "ERROR",
                "message", "Unexpected error: " + e.getMessage()
            ));
        }
    }

    /**
     * Validate specific fields
     */
    @PostMapping("/owners/validate-fields")
    public ResponseEntity<?> validateFields(@RequestBody Map<String, Object> ownerData,
                                          @RequestParam(required = false) String fields,
                                          HttpServletRequest request) {
        try {
            String url = backendUrl + "/api/validation/owners/validate-fields";
            if (fields != null && !fields.isEmpty()) {
                url += "?fields=" + fields;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Requested-With", "XMLHttpRequest");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(ownerData, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                "valid", false,
                "error", "Validation service error: " + e.getMessage()
            ));
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "valid", false,
                "error", "Unable to connect to validation service",
                "fallback", true,
                "fallbackReason", "CONNECTION_ERROR"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "valid", false,
                "error", "Unexpected validation error: " + e.getMessage()
            ));
        }
    }

    /**
     * Validate complete owner form
     */
    @PostMapping("/owners/validate")
    public ResponseEntity<?> validateOwner(@RequestBody Map<String, Object> ownerData,
                                         HttpServletRequest request) {
        try {
            String url = backendUrl + "/api/validation/owners/validate";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Requested-With", "XMLHttpRequest");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(ownerData, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                "valid", false,
                "error", "Validation service error: " + e.getMessage()
            ));
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "valid", false,
                "error", "Unable to connect to validation service",
                "fallback", true,
                "fallbackReason", "CONNECTION_ERROR"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "valid", false,
                "error", "Unexpected validation error: " + e.getMessage()
            ));
        }
    }

    /**
     * Validate owner for update (with ID)
     */
    @PostMapping("/owners/validate/{ownerId}")
    public ResponseEntity<?> validateOwnerUpdate(@PathVariable Long ownerId,
                                               @RequestBody Map<String, Object> ownerData,
                                               HttpServletRequest request) {
        try {
            String url = backendUrl + "/api/validation/owners/validate/" + ownerId;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Requested-With", "XMLHttpRequest");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(ownerData, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                "valid", false,
                "error", "Validation service error: " + e.getMessage()
            ));
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "valid", false,
                "error", "Unable to connect to validation service",
                "fallback", true,
                "fallbackReason", "CONNECTION_ERROR"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "valid", false,
                "error", "Unexpected validation error: " + e.getMessage()
            ));
        }
    }
}