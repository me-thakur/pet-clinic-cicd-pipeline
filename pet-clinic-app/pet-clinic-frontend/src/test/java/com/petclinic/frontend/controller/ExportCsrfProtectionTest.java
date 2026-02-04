package com.petclinic.frontend.controller;

import com.petclinic.frontend.config.SecurityConfig;
import com.petclinic.frontend.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for CSRF protection on export endpoints
 * Validates: Requirements 2.2, 2.3
 */
@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
public class ExportCsrfProtectionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @MockBean
    private com.petclinic.frontend.service.DashboardService dashboardService;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetExportWithoutCsrfToken_ShouldSucceed() throws Exception {
        // Mock successful export response
        byte[] mockPdfData = "Mock PDF content".getBytes();
        ResponseEntity<byte[]> mockResponse = ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"visits-report.pdf\"")
                .body(mockPdfData);

        when(reportService.exportVisitStatistics(any(LocalDate.class), any(LocalDate.class), eq("pdf")))
                .thenReturn(Mono.just(mockResponse));

        // GET requests should not require CSRF tokens
        mockMvc.perform(get("/dashboard/export/visits")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("format", "pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf;charset=UTF-8"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetExportWithCsrfToken_ShouldSucceed() throws Exception {
        // Mock successful export response
        byte[] mockPdfData = "Mock PDF content".getBytes();
        ResponseEntity<byte[]> mockResponse = ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"visits-report.pdf\"")
                .body(mockPdfData);

        when(reportService.exportVisitStatistics(any(LocalDate.class), any(LocalDate.class), eq("pdf")))
                .thenReturn(Mono.just(mockResponse));

        // GET requests should work with CSRF tokens too
        mockMvc.perform(get("/dashboard/export/visits")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("format", "pdf")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf;charset=UTF-8"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testPostExportWithoutCsrfToken_ShouldFail() throws Exception {
        // POST requests without CSRF token should be rejected
        mockMvc.perform(post("/dashboard/export/visits")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("format", "pdf"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testPostExportWithCsrfToken_ShouldSucceed() throws Exception {
        // Mock successful export response
        byte[] mockPdfData = "Mock PDF content".getBytes();
        ResponseEntity<byte[]> mockResponse = ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"visits-report.pdf\"")
                .body(mockPdfData);

        when(reportService.exportVisitStatistics(any(LocalDate.class), any(LocalDate.class), eq("pdf")))
                .thenReturn(Mono.just(mockResponse));

        // POST requests with CSRF token should succeed
        mockMvc.perform(post("/dashboard/export/visits")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("format", "pdf")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf;charset=UTF-8"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testCsrfTokenAvailableInResponse() throws Exception {
        // Test that CSRF token is available in cookie
        mockMvc.perform(get("/dashboard/reports"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testMultipleExportTypesWithCsrf() throws Exception {
        // Mock responses for different export types
        byte[] mockPdfData = "Mock PDF content".getBytes();
        byte[] mockCsvData = "Mock CSV content".getBytes();
        
        ResponseEntity<byte[]> pdfResponse = ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .body(mockPdfData);
        
        ResponseEntity<byte[]> csvResponse = ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .body(mockCsvData);

        when(reportService.exportVisitStatistics(any(LocalDate.class), any(LocalDate.class), eq("pdf")))
                .thenReturn(Mono.just(pdfResponse));
        when(reportService.exportRevenueReport(any(LocalDate.class), any(LocalDate.class), eq("csv"), eq(false)))
                .thenReturn(Mono.just(csvResponse));
        when(reportService.exportDashboardMetrics(eq("pdf"), any(LocalDate.class)))
                .thenReturn(Mono.just(pdfResponse));

        // Test visits export
        mockMvc.perform(get("/dashboard/export/visits")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("format", "pdf"))
                .andExpect(status().isOk());

        // Test revenue export
        mockMvc.perform(get("/dashboard/export/revenue")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("format", "csv")
                        .param("includeTrends", "false"))
                .andExpect(status().isOk());

        // Test dashboard export
        mockMvc.perform(get("/dashboard/export/dashboard")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("format", "pdf"))
                .andExpect(status().isOk());
    }

    @Test
    public void testExportWithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        // Unauthenticated requests should redirect to login regardless of CSRF
        mockMvc.perform(get("/dashboard/export/visits")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("format", "pdf"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testInvalidCsrfToken_ShouldFail() throws Exception {
        // POST request with invalid CSRF token should fail
        mockMvc.perform(post("/dashboard/export/visits")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("format", "pdf")
                        .header("X-CSRF-TOKEN", "invalid-token"))
                .andExpect(status().isForbidden());
    }
}