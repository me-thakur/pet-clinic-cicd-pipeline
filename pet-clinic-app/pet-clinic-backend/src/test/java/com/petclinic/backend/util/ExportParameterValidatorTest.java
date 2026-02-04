package com.petclinic.backend.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ExportParameterValidator
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
    @DisplayName("Should reject null report type")
    void testNullReportType() {
        // Given
        String reportType = null;
        String format = "pdf";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateExportParameters(reportType, format, startDate, endDate);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("reportType"));
        assertTrue(result.getErrors().get("reportType").contains("Report type is required"));
    }
    
    @Test
    @DisplayName("Should reject empty report type")
    void testEmptyReportType() {
        // Given
        String reportType = "  ";
        String format = "pdf";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateExportParameters(reportType, format, startDate, endDate);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("reportType"));
        assertTrue(result.getErrors().get("reportType").contains("Report type is required"));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"invalid", "INVALID", "report", "export", "data"})
    @DisplayName("Should reject invalid report types")
    void testInvalidReportTypes(String reportType) {
        // Given
        String format = "pdf";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateExportParameters(reportType, format, startDate, endDate);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("reportType"));
        assertTrue(result.getErrors().get("reportType").contains("Invalid report type"));
        assertTrue(result.getErrors().get("reportType").contains("visits, revenue, dashboard"));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"visits", "VISITS", "revenue", "REVENUE", "dashboard", "DASHBOARD"})
    @DisplayName("Should accept valid report types (case insensitive)")
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
    
    @Test
    @DisplayName("Should reject null format")
    void testNullFormat() {
        // Given
        String reportType = "visits";
        String format = null;
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateExportParameters(reportType, format, startDate, endDate);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("format"));
        assertTrue(result.getErrors().get("format").contains("Format is required"));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"invalid", "INVALID", "doc", "xlsx", "txt"})
    @DisplayName("Should reject invalid formats")
    void testInvalidFormats(String format) {
        // Given
        String reportType = "visits";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateExportParameters(reportType, format, startDate, endDate);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("format"));
        assertTrue(result.getErrors().get("format").contains("Invalid format"));
        assertTrue(result.getErrors().get("format").contains("pdf, csv"));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"pdf", "PDF", "csv", "CSV"})
    @DisplayName("Should accept valid formats (case insensitive)")
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
    @DisplayName("Should reject null start date")
    void testNullStartDate() {
        // Given
        String reportType = "visits";
        String format = "pdf";
        LocalDate startDate = null;
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateExportParameters(reportType, format, startDate, endDate);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("dateRange"));
        assertTrue(result.getErrors().get("dateRange").contains("Start date is required"));
    }
    
    @Test
    @DisplayName("Should reject null end date")
    void testNullEndDate() {
        // Given
        String reportType = "visits";
        String format = "pdf";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = null;
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateExportParameters(reportType, format, startDate, endDate);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("dateRange"));
        assertTrue(result.getErrors().get("dateRange").contains("End date is required"));
    }
    
    @Test
    @DisplayName("Should reject start date after end date")
    void testStartDateAfterEndDate() {
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
        assertTrue(result.getErrors().get("dateRange").contains("Start date"));
        assertTrue(result.getErrors().get("dateRange").contains("must be before or equal to end date"));
    }
    
    @Test
    @DisplayName("Should reject date range too large")
    void testDateRangeTooLarge() {
        // Given
        String reportType = "visits";
        String format = "pdf";
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31); // More than 365 days
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateExportParameters(reportType, format, startDate, endDate);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("dateRange"));
        assertTrue(result.getErrors().get("dateRange").contains("Date range is too large"));
        assertTrue(result.getErrors().get("dateRange").contains("Maximum allowed: 365 days"));
    }
    
    @Test
    @DisplayName("Should reject dates too far in future")
    void testDatesTooFarInFuture() {
        // Given
        String reportType = "visits";
        String format = "pdf";
        LocalDate startDate = LocalDate.now().plusDays(60);
        LocalDate endDate = LocalDate.now().plusDays(90);
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateExportParameters(reportType, format, startDate, endDate);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("dateRange"));
        assertTrue(result.getErrors().get("dateRange").contains("cannot be more than 30 days in the future"));
    }
    
    @Test
    @DisplayName("Should validate filter parameters successfully")
    void testValidFilterParameters() {
        // Given
        Long veterinarianId = 1L;
        String species = "Dog";
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateFilterParameters(veterinarianId, species);
        
        // Then
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }
    
    @Test
    @DisplayName("Should reject negative veterinarian ID")
    void testNegativeVeterinarianId() {
        // Given
        Long veterinarianId = -1L;
        String species = "Dog";
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateFilterParameters(veterinarianId, species);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("veterinarianId"));
        assertTrue(result.getErrors().get("veterinarianId").contains("must be a positive number"));
    }
    
    @Test
    @DisplayName("Should reject zero veterinarian ID")
    void testZeroVeterinarianId() {
        // Given
        Long veterinarianId = 0L;
        String species = "Dog";
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateFilterParameters(veterinarianId, species);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("veterinarianId"));
        assertTrue(result.getErrors().get("veterinarianId").contains("must be a positive number"));
    }
    
    @Test
    @DisplayName("Should reject species name too short")
    void testSpeciesNameTooShort() {
        // Given
        Long veterinarianId = 1L;
        String species = "D";
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateFilterParameters(veterinarianId, species);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("species"));
        assertTrue(result.getErrors().get("species").contains("must be at least 2 characters long"));
    }
    
    @Test
    @DisplayName("Should reject species name too long")
    void testSpeciesNameTooLong() {
        // Given
        Long veterinarianId = 1L;
        String species = "A".repeat(51); // 51 characters
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateFilterParameters(veterinarianId, species);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("species"));
        assertTrue(result.getErrors().get("species").contains("must be less than 50 characters long"));
    }
    
    @Test
    @DisplayName("Should reject species name with invalid characters")
    void testSpeciesNameInvalidCharacters() {
        // Given
        Long veterinarianId = 1L;
        String species = "Dog123";
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateFilterParameters(veterinarianId, species);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("species"));
        assertTrue(result.getErrors().get("species").contains("can only contain letters, spaces, and hyphens"));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"Dog", "Cat", "Golden Retriever", "German-Shepherd", "Maine Coon"})
    @DisplayName("Should accept valid species names")
    void testValidSpeciesNames(String species) {
        // Given
        Long veterinarianId = 1L;
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateFilterParameters(veterinarianId, species);
        
        // Then
        assertTrue(result.isValid());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"true", "TRUE", "false", "FALSE", "1", "0", "yes", "YES", "no", "NO"})
    @DisplayName("Should accept valid boolean parameter values")
    void testValidBooleanParameters(String booleanStr) {
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateBooleanParameter(booleanStr, "testParam");
        
        // Then
        assertTrue(result.isValid());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"invalid", "maybe", "2", "-1", "on", "off"})
    @DisplayName("Should reject invalid boolean parameter values")
    void testInvalidBooleanParameters(String booleanStr) {
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateBooleanParameter(booleanStr, "testParam");
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Invalid testParam value"));
        assertTrue(result.getErrorMessage().contains("Expected: true/false, 1/0, or yes/no"));
    }
    
    @Test
    @DisplayName("Should parse boolean parameters correctly")
    void testParseBooleanParameter() {
        // Test true values
        assertTrue(validator.parseBooleanParameter("true", false));
        assertTrue(validator.parseBooleanParameter("TRUE", false));
        assertTrue(validator.parseBooleanParameter("1", false));
        assertTrue(validator.parseBooleanParameter("yes", false));
        assertTrue(validator.parseBooleanParameter("YES", false));
        
        // Test false values
        assertFalse(validator.parseBooleanParameter("false", true));
        assertFalse(validator.parseBooleanParameter("FALSE", true));
        assertFalse(validator.parseBooleanParameter("0", true));
        assertFalse(validator.parseBooleanParameter("no", true));
        assertFalse(validator.parseBooleanParameter("NO", true));
        
        // Test default values
        assertTrue(validator.parseBooleanParameter(null, true));
        assertFalse(validator.parseBooleanParameter("", false));
        assertTrue(validator.parseBooleanParameter("  ", true));
    }
    
    @Test
    @DisplayName("Should normalize report type correctly")
    void testNormalizeReportType() {
        assertEquals("visits", validator.getNormalizedReportType("VISITS"));
        assertEquals("revenue", validator.getNormalizedReportType("Revenue"));
        assertEquals("dashboard", validator.getNormalizedReportType("  dashboard  "));
        assertNull(validator.getNormalizedReportType(null));
    }
    
    @Test
    @DisplayName("Should normalize format correctly")
    void testNormalizeFormat() {
        assertEquals("pdf", validator.getNormalizedFormat("PDF"));
        assertEquals("csv", validator.getNormalizedFormat("Csv"));
        assertEquals("pdf", validator.getNormalizedFormat("  pdf  "));
        assertNull(validator.getNormalizedFormat(null));
    }
    
    @Test
    @DisplayName("Should create proper error response body")
    void testCreateErrorResponseBody() {
        // Given
        ExportParameterValidator.ValidationResult result = 
            validator.validateExportParameters(null, "invalid", null, null);
        
        // When
        String errorBody = validator.createErrorResponseBody(result);
        
        // Then
        assertNotNull(errorBody);
        assertTrue(errorBody.contains("\"status\":\"error\""));
        assertTrue(errorBody.contains("\"message\":"));
        assertTrue(errorBody.contains("\"errors\":"));
        assertTrue(errorBody.contains("reportType"));
        assertTrue(errorBody.contains("format"));
        assertTrue(errorBody.contains("dateRange"));
    }
    
    @Test
    @DisplayName("Should handle multiple validation errors")
    void testMultipleValidationErrors() {
        // Given - all invalid parameters
        String reportType = "invalid";
        String format = "invalid";
        LocalDate startDate = LocalDate.of(2024, 1, 31);
        LocalDate endDate = LocalDate.of(2024, 1, 1); // Start after end
        
        // When
        ExportParameterValidator.ValidationResult result = 
            validator.validateExportParameters(reportType, format, startDate, endDate);
        
        // Then
        assertFalse(result.isValid());
        assertEquals(3, result.getErrors().size());
        assertTrue(result.getErrors().containsKey("reportType"));
        assertTrue(result.getErrors().containsKey("format"));
        assertTrue(result.getErrors().containsKey("dateRange"));
        
        String formattedMessage = result.getFormattedErrorMessage();
        assertTrue(formattedMessage.contains("reportType"));
        assertTrue(formattedMessage.contains("format"));
        assertTrue(formattedMessage.contains("dateRange"));
    }
}