package com.petclinic.frontend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class to verify export endpoint routing
 * Validates: Requirements 3.1, 3.3
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ExportRoutingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testVisitExportEndpointRouting() throws Exception {
        // Test GET request to /dashboard/export/visits
        mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31")
                .param("format", "pdf"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Type"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testRevenueExportEndpointRouting() throws Exception {
        // Test GET request to /dashboard/export/revenue
        mockMvc.perform(get("/dashboard/export/revenue")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31")
                .param("format", "pdf"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Type"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testDashboardExportEndpointRouting() throws Exception {
        // Test GET request to /dashboard/export/dashboard
        mockMvc.perform(get("/dashboard/export/dashboard")
                .param("format", "pdf"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Type"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testPostExportEndpointRouting() throws Exception {
        // Test POST request to /dashboard/export/visits (requires CSRF token)
        mockMvc.perform(post("/dashboard/export/visits")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31")
                .param("format", "pdf")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Type"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testInvalidReportTypeRouting() throws Exception {
        // Test invalid report type
        mockMvc.perform(get("/dashboard/export/invalid")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31")
                .param("format", "pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"Invalid report type. Supported types: visits, revenue, dashboard\"}"));
    }

    @Test
    public void testUnauthenticatedExportRequest() throws Exception {
        // Test unauthenticated request
        mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31")
                .param("format", "pdf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testParameterBinding() throws Exception {
        // Test parameter binding and validation
        mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31")
                .param("format", "csv")
                .param("includeTrends", "true"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Type"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testMissingRequiredParameters() throws Exception {
        // Test missing required parameters
        mockMvc.perform(get("/dashboard/export/visits")
                .param("format", "pdf"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testInvalidDateFormat() throws Exception {
        // Test invalid date format
        mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", "invalid-date")
                .param("endDate", "2024-01-31")
                .param("format", "pdf"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testEndpointAccessibility() throws Exception {
        // Test that all expected endpoints are accessible
        String[] reportTypes = {"visits", "revenue", "dashboard"};
        
        for (String reportType : reportTypes) {
            mockMvc.perform(get("/dashboard/export/" + reportType)
                    .param("startDate", "2024-01-01")
                    .param("endDate", "2024-01-31")
                    .param("format", "pdf"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Content-Type"));
        }
    }
}