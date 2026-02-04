package com.petclinic.frontend.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Frontend ExportParameterValidator
 * Validates: Requirements 3.2, 3.4, 3.5
 */
class ExportParameterValidatorTest {
    
    private ExportParameterValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new ExportParameterValidator();
    }
    
    @Test
    @DisplayName("Should validate valid export parameters successfully")
    void testValidExportParameters() {
        // Given
        String reportType = "visits";
        String format = "pdf";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateExportParameters(reportType, format, startDate, endDate);
        
        // Then
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }
    
    @Test
    @DisplayName("Should reject invalid report type with descriptive error")
    void testInvalidReportType() {
        // Given
        String reportType = "invalid";
        String format = "pdf";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateExportParameters(reportType, format, startDate, endDate);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("reportType"));
        assertTrue(result.getErrors().get("reportType").contains("Invalid report type 'invalid'"));
        assertTrue(result.getErrors().get("reportType").contains("visits, revenue, dashboard"));
    }
    
    @Test
    @DisplayName("Should reject invalid format with descriptive error")
    void testInvalidFormat() {
        // Given
        String reportType = "visits";
        String format = "doc";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateExportParameters(reportType, format, startDate, endDate);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("format"));
        assertTrue(result.getErrors().get("format").contains("Invalid format 'doc'"));
        assertTrue(result.getErrors().get("format").contains("pdf, csv"));
    }
    
    @Test
    @DisplayName("Should reject invalid date range with descriptive error")
    void testInvalidDateRange() {
        // Given
        String reportType = "visits";
        String format = "pdf";
        LocalDate startDate = LocalDate.of(2024, 1, 31);
        LocalDate endDate = LocalDate.of(2024, 1, 1);
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateExportParameters(reportType, format, startDate, endDate);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("dateRange"));
        assertTrue(result.getErrors().get("dateRange").contains("Start date (2024-01-31) must be before or equal to end date (2024-01-01)"));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"visits", "revenue", "dashboard"})
    @DisplayName("Should accept all valid report types")
    void testValidReportTypes(String reportType) {
        // Given
        String format = "pdf";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateExportParameters(reportType, format, startDate, endDate);
        
        // Then
        assertTrue(result.isValid());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"pdf", "csv"})
    @DisplayName("Should accept all valid formats")
    void testValidFormats(String format) {
        // Given
        String reportType = "visits";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateExportParameters(reportType, format, startDate, endDate);
        
        // Then
        assertTrue(result.isValid());
    }
    
    @Test
    @DisplayName("Should validate filter parameters correctly")
    void testFilterParameterValidation() {
        // Valid parameters
        ExportParameterValidator.ValidationResult result1 = 
            validator.validateFilterParameters(1L, "Dog");
        assertTrue(result1.isValid());
        
        // Invalid veterinarian ID
        ExportParameterValidator.ValidationResult result2 = 
            validator.validateFilterParameters(-1L, "Dog");
        assertFalse(result2.isValid());
        assertTrue(result2.getErrors().containsKey("veterinarianId"));
        
        // Invalid species
        ExportParameterValidator.ValidationResult result3 = 
            validator.validateFilterParameters(1L, "D");
        assertFalse(result3.isValid());
        assertTrue(result3.getErrors().containsKey("species"));
    }
    
    @Test
    @DisplayName("Should normalize parameters correctly")
    void testParameterNormalization() {
        assertEquals("visits", validator.getNormalizedReportType("VISITS"));
        assertEquals("pdf", validator.getNormalizedFormat("PDF"));
        assertNull(validator.getNormalizedReportType(null));
        assertNull(validator.getNormalizedFormat(null));
    }
    
    @Test
    @DisplayName("Should parse boolean parameters correctly")
    void testBooleanParameterParsing() {
        assertTrue(validator.parseBooleanParameter("true", false));
        assertTrue(validator.parseBooleanParameter("1", false));
        assertTrue(validator.parseBooleanParameter("yes", false));
        assertFalse(validator.parseBooleanParameter("false", true));
        assertFalse(validator.parseBooleanParameter("0", true));
        assertFalse(validator.parseBooleanParameter("no", true));
        assertTrue(validator.parseBooleanParameter(null, true));
        assertFalse(validator.parseBooleanParameter("", false));
    }
    
    @Test
    @DisplayName("Should create proper error response body")
    void testErrorResponseBodyCreation() {
        // Given invalid parameters
        ExportParameterValidator.ValidationResult result = 
            validator.validateExportParameters("invalid", "invalid", null, null);
        
        // When
        String errorBody = validator.createErrorResponseBody(result);
        
        // Then
        assertNotNull(errorBody);
        assertTrue(errorBody.contains("\"status\":\"error\""));
        assertTrue(errorBody.contains("\"message\":"));
        assertTrue(errorBody.contains("\"errors\":"));
    }
    
    @Test
    @DisplayName("Should handle edge case date ranges")
    void testEdgeCaseDateRanges() {
        String reportType = "visits";
        String format = "pdf";
        
        // Same start and end date (should be valid)
        LocalDate sameDate = LocalDate.of(2024, 1, 15);
        ExportParameterValidator.ValidationResult result1 = 
            validator.validateExportParameters(reportType, format, sameDate, sameDate);
        assertTrue(result1.isValid());
        
        // Maximum allowed range (365 days)
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31); // 365 days
        ExportParameterValidator.ValidationResult result2 = 
            validator.validateExportParameters(reportType, format, start, end);
        assertTrue(result2.isValid());
        
        // Over maximum range (366 days)
        LocalDate endOver = LocalDate.of(2025, 1, 1); // 366 days
        ExportParameterValidator.ValidationResult result3 = 
            validator.validateExportParameters(reportType, format, start, endOver);
        assertFalse(result3.isValid());
        assertTrue(result3.getErrors().containsKey("dateRange"));
        assertTrue(result3.getErrors().get("dateRange").contains("Date range is too large"));
    }
}