package com.petclinic.frontend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class for validating export routing configuration
 * Validates: Requirements 3.1, 3.3
 */
@Component
public class RoutingValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(RoutingValidator.class);
    
    private static final List<String> VALID_REPORT_TYPES = Arrays.asList("visits", "revenue", "dashboard");
    private static final List<String> VALID_FORMATS = Arrays.asList("pdf", "csv");
    
    /**
     * Validate export endpoint routing configuration
     */
    public boolean validateExportEndpointRouting(String reportType, String format) {
        logger.debug("Validating export endpoint routing - reportType: {}, format: {}", reportType, format);
        
        boolean isValid = true;
        
        // Validate report type
        if (!isValidReportType(reportType)) {
            logger.error("Invalid report type: {}. Valid types: {}", reportType, VALID_REPORT_TYPES);
            isValid = false;
        }
        
        // Validate format
        if (!isValidFormat(format)) {
            logger.error("Invalid format: {}. Valid formats: {}", format, VALID_FORMATS);
            isValid = false;
        }
        
        // Validate frontend endpoint path
        String frontendPath = "/dashboard/export/" + reportType;
        if (!isValidFrontendPath(frontendPath)) {
            logger.error("Invalid frontend path: {}", frontendPath);
            isValid = false;
        }
        
        // Validate backend endpoint path
        String backendPath = "/reports/export/" + reportType;
        if (!isValidBackendPath(backendPath)) {
            logger.error("Invalid backend path: {}", backendPath);
            isValid = false;
        }
        
        if (isValid) {
            logger.info("Export endpoint routing validation passed - reportType: {}, format: {}", reportType, format);
        } else {
            logger.error("Export endpoint routing validation failed - reportType: {}, format: {}", reportType, format);
        }
        
        return isValid;
    }
    
    /**
     * Validate report type
     */
    public boolean isValidReportType(String reportType) {
        if (reportType == null || reportType.trim().isEmpty()) {
            return false;
        }
        return VALID_REPORT_TYPES.contains(reportType.toLowerCase().trim());
    }
    
    /**
     * Validate format
     */
    public boolean isValidFormat(String format) {
        if (format == null || format.trim().isEmpty()) {
            return false;
        }
        return VALID_FORMATS.contains(format.toLowerCase().trim());
    }
    
    /**
     * Validate frontend path
     */
    public boolean isValidFrontendPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        
        // Frontend paths should start with /dashboard/export/
        if (!path.startsWith("/dashboard/export/")) {
            return false;
        }
        
        // Extract report type from path
        String reportType = path.substring("/dashboard/export/".length());
        return isValidReportType(reportType);
    }
    
    /**
     * Validate backend path
     */
    public boolean isValidBackendPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        
        // Backend paths should start with /reports/export/
        if (!path.startsWith("/reports/export/")) {
            return false;
        }
        
        // Extract report type from path
        String reportType = path.substring("/reports/export/".length());
        return isValidReportType(reportType);
    }
    
    /**
     * Get frontend export URL
     */
    public String getFrontendExportUrl(String reportType) {
        if (!isValidReportType(reportType)) {
            throw new IllegalArgumentException("Invalid report type: " + reportType);
        }
        return "/dashboard/export/" + reportType.toLowerCase();
    }
    
    /**
     * Get backend export URL
     */
    public String getBackendExportUrl(String reportType) {
        if (!isValidReportType(reportType)) {
            throw new IllegalArgumentException("Invalid report type: " + reportType);
        }
        return "/reports/export/" + reportType.toLowerCase();
    }
    
    /**
     * Get all valid report types
     */
    public List<String> getValidReportTypes() {
        return VALID_REPORT_TYPES;
    }
    
    /**
     * Get all valid formats
     */
    public List<String> getValidFormats() {
        return VALID_FORMATS;
    }
    
    /**
     * Log routing configuration for debugging
     */
    public void logRoutingConfiguration() {
        logger.info("Export Routing Configuration:");
        logger.info("  Valid Report Types: {}", VALID_REPORT_TYPES);
        logger.info("  Valid Formats: {}", VALID_FORMATS);
        logger.info("  Frontend Path Pattern: /dashboard/export/{reportType}");
        logger.info("  Backend Path Pattern: /reports/export/{reportType}");
        
        for (String reportType : VALID_REPORT_TYPES) {
            logger.info("  Frontend URL for {}: {}", reportType, getFrontendExportUrl(reportType));
            logger.info("  Backend URL for {}: {}", reportType, getBackendExportUrl(reportType));
        }
    }
}