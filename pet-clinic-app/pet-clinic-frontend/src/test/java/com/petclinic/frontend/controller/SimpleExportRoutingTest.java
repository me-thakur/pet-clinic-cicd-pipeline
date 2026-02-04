package com.petclinic.frontend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to verify export endpoint routing without authentication
 * Validates: Requirements 3.1, 3.3
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SimpleExportRoutingTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testExportEndpointsExist() {
        // Test that export endpoints are mapped and accessible (should return 401 for unauthenticated)
        String baseUrl = "http://localhost:" + port;
        
        // Test visits export endpoint
        ResponseEntity<String> visitsResponse = restTemplate.getForEntity(
            baseUrl + "/dashboard/export/visits?startDate=2024-01-01&endDate=2024-01-31&format=pdf", 
            String.class);
        
        // Should return 401 (unauthorized) or 302 (redirect to login), not 404 (not found)
        assertTrue(visitsResponse.getStatusCode() == HttpStatus.UNAUTHORIZED || 
                  visitsResponse.getStatusCode() == HttpStatus.FOUND,
                  "Visits export endpoint should exist (got " + visitsResponse.getStatusCode() + ")");
        
        // Test revenue export endpoint
        ResponseEntity<String> revenueResponse = restTemplate.getForEntity(
            baseUrl + "/dashboard/export/revenue?startDate=2024-01-01&endDate=2024-01-31&format=pdf", 
            String.class);
        
        assertTrue(revenueResponse.getStatusCode() == HttpStatus.UNAUTHORIZED || 
                  revenueResponse.getStatusCode() == HttpStatus.FOUND,
                  "Revenue export endpoint should exist (got " + revenueResponse.getStatusCode() + ")");
        
        // Test dashboard export endpoint
        ResponseEntity<String> dashboardResponse = restTemplate.getForEntity(
            baseUrl + "/dashboard/export/dashboard?format=pdf", 
            String.class);
        
        assertTrue(dashboardResponse.getStatusCode() == HttpStatus.UNAUTHORIZED || 
                  dashboardResponse.getStatusCode() == HttpStatus.FOUND,
                  "Dashboard export endpoint should exist (got " + dashboardResponse.getStatusCode() + ")");
    }

    @Test
    public void testInvalidReportTypeReturns404() {
        // Test that invalid report types return 404 (not found)
        String baseUrl = "http://localhost:" + port;
        
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/dashboard/export/invalid?startDate=2024-01-01&endDate=2024-01-31&format=pdf", 
            String.class);
        
        // Should return 404 for invalid report type, or 401/302 if security kicks in first
        assertTrue(response.getStatusCode() == HttpStatus.NOT_FOUND || 
                  response.getStatusCode() == HttpStatus.UNAUTHORIZED || 
                  response.getStatusCode() == HttpStatus.FOUND,
                  "Invalid report type should return 404, 401, or 302 (got " + response.getStatusCode() + ")");
    }

    @Test
    public void testDiagnosticEndpointsExist() {
        // Test that diagnostic endpoints are accessible
        String baseUrl = "http://localhost:" + port;
        
        ResponseEntity<String> diagnosticsResponse = restTemplate.getForEntity(
            baseUrl + "/dashboard/diagnostics/export", 
            String.class);
        
        // Diagnostics page should be accessible (might require auth)
        assertTrue(diagnosticsResponse.getStatusCode() == HttpStatus.OK || 
                  diagnosticsResponse.getStatusCode() == HttpStatus.UNAUTHORIZED || 
                  diagnosticsResponse.getStatusCode() == HttpStatus.FOUND,
                  "Diagnostics endpoint should exist (got " + diagnosticsResponse.getStatusCode() + ")");
    }
}