package com.petclinic.frontend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Simple Frontend Integration Test
 * 
 * This test validates basic frontend functionality and web interface integration.
 * It tests that the frontend application starts correctly and basic endpoints are accessible.
 * 
 * **Validates: Requirements 6 (Mobile-Responsive UI)**
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SimpleFrontendIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testApplicationStartsSuccessfully() {
        // Test that the application context loads successfully
        // This validates that all beans are properly configured
        assert mockMvc != null;
    }

    @Test
    void testHomePageAccessible() throws Exception {
        // Test that the home page is accessible
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    void testOwnersPageAccessible() throws Exception {
        // Test that the owners page is accessible
        mockMvc.perform(get("/owners"))
                .andExpect(status().isOk());
    }

    @Test
    void testPetsPageAccessible() throws Exception {
        // Test that the pets page is accessible
        mockMvc.perform(get("/pets"))
                .andExpect(status().isOk());
    }

    @Test
    void testVeterinariansPageAccessible() throws Exception {
        // Test that the veterinarians page is accessible
        mockMvc.perform(get("/veterinarians"))
                .andExpect(status().isOk());
    }

    @Test
    void testDashboardPageAccessible() throws Exception {
        // Test that the dashboard page is accessible
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    void testSearchPageAccessible() throws Exception {
        // Test that the search page is accessible
        mockMvc.perform(get("/search"))
                .andExpect(status().isOk());
    }

    @Test
    void testMobileUserAgentHandling() throws Exception {
        // Test mobile user agent handling
        mockMvc.perform(get("/owners")
                .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)"))
                .andExpect(status().isOk());
    }

    @Test
    void testStaticResourcesAccessible() throws Exception {
        // Test that static resources are accessible
        mockMvc.perform(get("/css/bootstrap.min.css"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/js/bootstrap.min.js"))
                .andExpect(status().isOk());
    }

    @Test
    void testErrorPageHandling() throws Exception {
        // Test error page handling for non-existent pages
        mockMvc.perform(get("/nonexistent"))
                .andExpect(status().isNotFound());
    }
}