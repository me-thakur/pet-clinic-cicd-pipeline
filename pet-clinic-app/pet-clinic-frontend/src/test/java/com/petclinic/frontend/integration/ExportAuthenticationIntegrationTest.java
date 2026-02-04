package com.petclinic.frontend.integration;

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
 * Integration test for export authentication flow
 * Validates: Requirements 2.1, 2.4
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ExportAuthenticationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testExportWithoutAuthentication_ShouldRedirectToLogin() {
        String url = "http://localhost:" + port + "/dashboard/export/visits?startDate=2024-01-01&endDate=2024-01-31&format=pdf";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Should redirect to login page (3xx status)
        assertTrue(response.getStatusCode().is3xxRedirection(), 
                  "Unauthenticated export request should redirect to login");
        
        String location = response.getHeaders().getLocation().toString();
        assertTrue(location.contains("/login"), 
                  "Should redirect to login page, but redirected to: " + location);
    }

    @Test
    public void testExportWithBasicAuth_ShouldReturnUnauthorizedForInvalidCredentials() {
        String url = "http://localhost:" + port + "/dashboard/export/visits?startDate=2024-01-01&endDate=2024-01-31&format=pdf";
        
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("invalid", "credentials")
                .getForEntity(url, String.class);
        
        // Should return 401 or redirect to login
        assertTrue(response.getStatusCode().is4xxClientError() || response.getStatusCode().is3xxRedirection(),
                  "Invalid credentials should result in authentication failure");
    }

    @Test
    public void testExportWithValidAuth_ShouldAttemptBackendCall() {
        String url = "http://localhost:" + port + "/dashboard/export/visits?startDate=2024-01-01&endDate=2024-01-31&format=pdf";
        
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("admin", "admin123")
                .getForEntity(url, String.class);
        
        // With valid frontend auth, should attempt to call backend
        // May fail with 500 if backend is not available, but should not be 401/403 from frontend
        assertFalse(response.getStatusCode() == HttpStatus.UNAUTHORIZED,
                   "Valid frontend credentials should not result in 401 from frontend");
        assertFalse(response.getStatusCode() == HttpStatus.FORBIDDEN,
                   "Valid frontend credentials should not result in 403 from frontend");
        
        // The request should either succeed or fail with backend-related errors
        assertTrue(response.getStatusCode().is2xxSuccessful() || 
                  response.getStatusCode().is5xxServerError(),
                  "With valid auth, should either succeed or fail with backend error, got: " + response.getStatusCode());
    }

    @Test
    public void testHealthEndpoint_ShouldBeAccessible() {
        String url = "http://localhost:" + port + "/actuator/health";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                    "Health endpoint should be accessible without authentication");
    }
}