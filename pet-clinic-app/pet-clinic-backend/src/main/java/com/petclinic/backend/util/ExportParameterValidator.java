package com.petclinic.backend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for validating export request parameters
 * Provides comprehensive validation with descriptive error messages
 * Validates: Requirements 3.2, 3.4, 3.5
 */
@Component
public class ExportParameterValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(ExportParameterValidator.class);
    
    // Valid report types
    private static final List<String> VALID_REPORT_TYPES = Arrays.asList(
        "visits", "revenue", "dashboard"
    );
    
    // Valid formats
    private static final List<String> VALID_FORMATS = Arrays.asList(
        "pdf", "csv"
    );
    
    // Maximum date range in days (1 year)
    private static final long MAX_DATE_RANGE_DAYS = 365;
    
    /**
     * Validation result containing validation status and error details
     */
    public static class ValidationResult {
        private final boolean valid;
        private final Map<String, String> errors;
        private final String errorMessage;
        
        public ValidationResult(boolean valid) {
            this.valid = valid;
            this.errors = new HashMap<>();
            this.errorMessage = null;
        }
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errors = new HashMap<>();
            this.errorMessage = errorMessage;
        }
        
        public ValidationResult(boolean valid, Map<String, String> errors) {
            this.valid = valid;
            this.errors = errors;
            this.errorMessage = null;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public Map<String, String> getErrors() {
            return errors;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public String getFormattedErrorMessage() {
            if (errorMessage != null) {
                return errorMessage;
            }
            
            if (!errors.isEmpty()) {
                StringBuilder sb = new StringBuilder("Validation errors: ");
                errors.forEach((field, error) -> sb.append(field).append(": ").append(error).append("; "));
                return sb.toString();
            }
            
            return "Unknown validation error";
        }
    }
    
    /**
     * Validate all export parameters
     */
    public ValidationResult validateExportParameters(String reportType, String format, 
                                                   LocalDate startDate, LocalDate endDate) {
        logger.debug("Validating export parameters - reportType: {}, format: {}, dateRange: {} to {}", 
                    reportType, format, startDate, endDate);
        
        Map<String, String> errors = new HashMap<>();
        
        // Validate report type
        ValidationResult reportTypeResult = validateReportType(reportType);
        if (!reportTypeResult.isValid()) {
            errors.put("reportType", reportTypeResult.getErrorMessage());
        }
        
        // Validate format
        ValidationResult formatResult = validateFormat(format);
        if (!formatResult.isValid()) {
            errors.put("format", formatResult.getErrorMessage());
        }
        
        // Validate date range
        ValidationResult dateResult = validateDateRange(startDate, endDate);
        if (!dateResult.isValid()) {
            errors.put("dateRange", dateResult.getErrorMessage());
        }
        
        if (errors.isEmpty()) {
            logger.debug("Export parameters validation successful");
            return new ValidationResult(true);
        } else {
            logger.warn("Export parameters validation failed: {}", errors);
            return new ValidationResult(false, errors);
        }
    }
    
    /**
     * Validate report type parameter
     */
    public ValidationResult validateReportType(String reportType) {
        if (reportType == null || reportType.trim().isEmpty()) {
            return new ValidationResult(false, "Report type is required. Supported types: " + String.join(", ", VALID_REPORT_TYPES));
        }
        
        String normalizedType = reportType.trim().toLowerCase();
        if (!VALID_REPORT_TYPES.contains(normalizedType)) {
            return new ValidationResult(false, "Invalid report type '" + reportType + "'. Supported types: " + String.join(", ", VALID_REPORT_TYPES));
        }
        
        return new ValidationResult(true);
    }
    
    /**
     * Validate format parameter
     */
    public ValidationResult validateFormat(String format) {
        if (format == null || format.trim().isEmpty()) {
            return new ValidationResult(false, "Format is required. Supported formats: " + String.join(", ", VALID_FORMATS));
        }
        
        String normalizedFormat = format.trim().toLowerCase();
        if (!VALID_FORMATS.contains(normalizedFormat)) {
            return new ValidationResult(false, "Invalid format '" + format + "'. Supported formats: " + String.join(", ", VALID_FORMATS));
        }
        
        return new ValidationResult(true);
    }
    
    /**
     * Validate date range parameters
     */
    public ValidationResult validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            return new ValidationResult(false, "Start date is required. Format: YYYY-MM-DD (e.g., 2024-01-01)");
        }
        
        if (endDate == null) {
            return new ValidationResult(false, "End date is required. Format: YYYY-MM-DD (e.g., 2024-01-31)");
        }
        
        if (startDate.isAfter(endDate)) {
            return new ValidationResult(false, "Start date (" + startDate + ") must be before or equal to end date (" + endDate + ")");
        }
        
        // Check if date range is too large
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > MAX_DATE_RANGE_DAYS) {
            return new ValidationResult(false, "Date range is too large (" + daysBetween + " days). Maximum allowed: " + MAX_DATE_RANGE_DAYS + " days");
        }
        
        // Check if dates are not too far in the future
        LocalDate maxFutureDate = LocalDate.now().plusDays(30);
        if (startDate.isAfter(maxFutureDate) || endDate.isAfter(maxFutureDate)) {
            return new ValidationResult(false, "Dates cannot be more than 30 days in the future");
        }
        
        return new ValidationResult(true);
    }
    
    /**
     * Validate optional filter parameters
     */
    public ValidationResult validateFilterParameters(Long veterinarianId, String species) {
        Map<String, String> errors = new HashMap<>();
        
        // Validate veterinarian ID if provided
        if (veterinarianId != null && veterinarianId <= 0) {
            errors.put("veterinarianId", "Veterinarian ID must be a positive number");
        }
        
        // Validate species if provided
        if (species != null && !species.trim().isEmpty()) {
            String normalizedSpecies = species.trim();
            if (normalizedSpecies.length() < 2) {
                errors.put("species", "Species name must be at least 2 characters long");
            }
            if (normalizedSpecies.length() > 50) {
                errors.put("species", "Species name must be less than 50 characters long");
            }
            // Check for valid characters (letters, spaces, hyphens)
            if (!normalizedSpecies.matches("^[a-zA-Z\\s\\-]+$")) {
                errors.put("species", "Species name can only contain letters, spaces, and hyphens");
            }
        }
        
        if (errors.isEmpty()) {
            return new ValidationResult(true);
        } else {
            return new ValidationResult(false, errors);
        }
    }
    
    /**
     * Parse and validate date string
     */
    public ValidationResult validateDateString(String dateStr, String fieldName) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return new ValidationResult(false, fieldName + " is required. Format: YYYY-MM-DD (e.g., 2024-01-01)");
        }
        
        try {
            LocalDate.parse(dateStr.trim());
            return new ValidationResult(true);
        } catch (DateTimeParseException e) {
            return new ValidationResult(false, "Invalid " + fieldName + " format '" + dateStr + "'. Expected format: YYYY-MM-DD (e.g., 2024-01-01)");
        }
    }
    
    /**
     * Validate boolean parameter
     */
    public ValidationResult validateBooleanParameter(String booleanStr, String fieldName) {
        if (booleanStr == null || booleanStr.trim().isEmpty()) {
            return new ValidationResult(true); // Optional parameter
        }
        
        String normalized = booleanStr.trim().toLowerCase();
        if (!Arrays.asList("true", "false", "1", "0", "yes", "no").contains(normalized)) {
            return new ValidationResult(false, "Invalid " + fieldName + " value '" + booleanStr + "'. Expected: true/false, 1/0, or yes/no");
        }
        
        return new ValidationResult(true);
    }
    
    /**
     * Get normalized report type
     */
    public String getNormalizedReportType(String reportType) {
        if (reportType == null) {
            return null;
        }
        return reportType.trim().toLowerCase();
    }
    
    /**
     * Get normalized format
     */
    public String getNormalizedFormat(String format) {
        if (format == null) {
            return null;
        }
        return format.trim().toLowerCase();
    }
    
    /**
     * Parse boolean parameter with default value
     */
    public boolean parseBooleanParameter(String booleanStr, boolean defaultValue) {
        if (booleanStr == null || booleanStr.trim().isEmpty()) {
            return defaultValue;
        }
        
        String normalized = booleanStr.trim().toLowerCase();
        return Arrays.asList("true", "1", "yes").contains(normalized);
    }
    
    /**
     * Create error response body for validation failures
     */
    public String createErrorResponseBody(ValidationResult result) {
        if (result.isValid()) {
            return "{\"status\":\"success\"}";
        }
        
        StringBuilder json = new StringBuilder();
        json.append("{\"status\":\"error\",\"message\":\"")
            .append(escapeJsonString(result.getFormattedErrorMessage()))
            .append("\"");
        
        if (!result.getErrors().isEmpty()) {
            json.append(",\"errors\":{");
            boolean first = true;
            for (Map.Entry<String, String> entry : result.getErrors().entrySet()) {
                if (!first) {
                    json.append(",");
                }
                json.append("\"").append(escapeJsonString(entry.getKey())).append("\":\"")
                    .append(escapeJsonString(entry.getValue())).append("\"");
                first = false;
            }
            json.append("}");
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Escape string for JSON
     */
    private String escapeJsonString(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}