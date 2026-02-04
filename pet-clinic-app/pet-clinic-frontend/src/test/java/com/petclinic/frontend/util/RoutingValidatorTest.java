package com.petclinic.frontend.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for RoutingValidator
 * Validates: Requirements 3.1, 3.3
 */
public class RoutingValidatorTest {

    private RoutingValidator routingValidator;

    @BeforeEach
    public void setUp() {
        routingValidator = new RoutingValidator();
    }

    @Test
    public void testValidReportTypes() {
        assertTrue(routingValidator.isValidReportType("visits"));
        assertTrue(routingValidator.isValidReportType("revenue"));
        assertTrue(routingValidator.isValidReportType("dashboard"));
        
        // Test case insensitive
        assertTrue(routingValidator.isValidReportType("VISITS"));
        assertTrue(routingValidator.isValidReportType("Revenue"));
        assertTrue(routingValidator.isValidReportType("DASHBOARD"));
    }

    @Test
    public void testInvalidReportTypes() {
        assertFalse(routingValidator.isValidReportType("invalid"));
        assertFalse(routingValidator.isValidReportType(""));
        assertFalse(routingValidator.isValidReportType(null));
        assertFalse(routingValidator.isValidReportType("   "));
    }

    @Test
    public void testValidFormats() {
        assertTrue(routingValidator.isValidFormat("pdf"));
        assertTrue(routingValidator.isValidFormat("csv"));
        
        // Test case insensitive
        assertTrue(routingValidator.isValidFormat("PDF"));
        assertTrue(routingValidator.isValidFormat("CSV"));
    }

    @Test
    public void testInvalidFormats() {
        assertFalse(routingValidator.isValidFormat("invalid"));
        assertFalse(routingValidator.isValidFormat(""));
        assertFalse(routingValidator.isValidFormat(null));
        assertFalse(routingValidator.isValidFormat("   "));
    }

    @Test
    public void testValidFrontendPaths() {
        assertTrue(routingValidator.isValidFrontendPath("/dashboard/export/visits"));
        assertTrue(routingValidator.isValidFrontendPath("/dashboard/export/revenue"));
        assertTrue(routingValidator.isValidFrontendPath("/dashboard/export/dashboard"));
    }

    @Test
    public void testInvalidFrontendPaths() {
        assertFalse(routingValidator.isValidFrontendPath("/dashboard/export/invalid"));
        assertFalse(routingValidator.isValidFrontendPath("/export/visits"));
        assertFalse(routingValidator.isValidFrontendPath("/dashboard/visits"));
        assertFalse(routingValidator.isValidFrontendPath(""));
        assertFalse(routingValidator.isValidFrontendPath(null));
    }

    @Test
    public void testValidBackendPaths() {
        assertTrue(routingValidator.isValidBackendPath("/reports/export/visits"));
        assertTrue(routingValidator.isValidBackendPath("/reports/export/revenue"));
        assertTrue(routingValidator.isValidBackendPath("/reports/export/dashboard"));
    }

    @Test
    public void testInvalidBackendPaths() {
        assertFalse(routingValidator.isValidBackendPath("/reports/export/invalid"));
        assertFalse(routingValidator.isValidBackendPath("/export/visits"));
        assertFalse(routingValidator.isValidBackendPath("/reports/visits"));
        assertFalse(routingValidator.isValidBackendPath(""));
        assertFalse(routingValidator.isValidBackendPath(null));
    }

    @Test
    public void testGetFrontendExportUrl() {
        assertEquals("/dashboard/export/visits", routingValidator.getFrontendExportUrl("visits"));
        assertEquals("/dashboard/export/revenue", routingValidator.getFrontendExportUrl("revenue"));
        assertEquals("/dashboard/export/dashboard", routingValidator.getFrontendExportUrl("dashboard"));
        
        // Test case insensitive
        assertEquals("/dashboard/export/visits", routingValidator.getFrontendExportUrl("VISITS"));
    }

    @Test
    public void testGetFrontendExportUrlInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            routingValidator.getFrontendExportUrl("invalid");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            routingValidator.getFrontendExportUrl(null);
        });
    }

    @Test
    public void testGetBackendExportUrl() {
        assertEquals("/reports/export/visits", routingValidator.getBackendExportUrl("visits"));
        assertEquals("/reports/export/revenue", routingValidator.getBackendExportUrl("revenue"));
        assertEquals("/reports/export/dashboard", routingValidator.getBackendExportUrl("dashboard"));
        
        // Test case insensitive
        assertEquals("/reports/export/visits", routingValidator.getBackendExportUrl("VISITS"));
    }

    @Test
    public void testGetBackendExportUrlInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            routingValidator.getBackendExportUrl("invalid");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            routingValidator.getBackendExportUrl(null);
        });
    }

    @Test
    public void testValidateExportEndpointRouting() {
        // Valid combinations
        assertTrue(routingValidator.validateExportEndpointRouting("visits", "pdf"));
        assertTrue(routingValidator.validateExportEndpointRouting("revenue", "csv"));
        assertTrue(routingValidator.validateExportEndpointRouting("dashboard", "pdf"));
        
        // Invalid report type
        assertFalse(routingValidator.validateExportEndpointRouting("invalid", "pdf"));
        
        // Invalid format
        assertFalse(routingValidator.validateExportEndpointRouting("visits", "invalid"));
        
        // Both invalid
        assertFalse(routingValidator.validateExportEndpointRouting("invalid", "invalid"));
    }

    @Test
    public void testGetValidReportTypes() {
        var validTypes = routingValidator.getValidReportTypes();
        assertEquals(3, validTypes.size());
        assertTrue(validTypes.contains("visits"));
        assertTrue(validTypes.contains("revenue"));
        assertTrue(validTypes.contains("dashboard"));
    }

    @Test
    public void testGetValidFormats() {
        var validFormats = routingValidator.getValidFormats();
        assertEquals(2, validFormats.size());
        assertTrue(validFormats.contains("pdf"));
        assertTrue(validFormats.contains("csv"));
    }
}