package com.petclinic.backend.service;

import java.util.Map;

/**
 * Service interface for data sanitization and validation
 * Provides comprehensive data cleaning to prevent injection attacks and data corruption
 * 
 * Validates: Requirements 17.3, 17.4
 */
public interface DataSanitizationService {
    
    /**
     * Sanitize string input to prevent XSS and injection attacks
     * 
     * @param input The input string to sanitize
     * @return Sanitized string safe for storage and display
     */
    String sanitizeString(String input);
    
    /**
     * Sanitize HTML content while preserving safe formatting
     * 
     * @param htmlContent The HTML content to sanitize
     * @return Sanitized HTML content with dangerous elements removed
     */
    String sanitizeHtml(String htmlContent);
    
    /**
     * Sanitize SQL input to prevent SQL injection
     * 
     * @param sqlInput The SQL input to sanitize
     * @return Sanitized SQL input
     */
    String sanitizeSql(String sqlInput);
    
    /**
     * Validate and sanitize email addresses
     * 
     * @param email The email address to validate and sanitize
     * @return Sanitized email address or null if invalid
     */
    String sanitizeEmail(String email);
    
    /**
     * Validate and sanitize phone numbers
     * 
     * @param phoneNumber The phone number to validate and sanitize
     * @return Sanitized phone number or null if invalid
     */
    String sanitizePhoneNumber(String phoneNumber);
    
    /**
     * Sanitize map of request parameters
     * 
     * @param parameters Map of parameters to sanitize
     * @return Map with sanitized values
     */
    Map<String, Object> sanitizeParameters(Map<String, Object> parameters);
    
    /**
     * Check if input contains potentially malicious content
     * 
     * @param input The input to check
     * @return true if input appears to contain malicious content
     */
    boolean containsMaliciousContent(String input);
    
    /**
     * Sanitize file names to prevent directory traversal attacks
     * 
     * @param fileName The file name to sanitize
     * @return Sanitized file name safe for file system operations
     */
    String sanitizeFileName(String fileName);
    
    /**
     * Validate and sanitize numeric input
     * 
     * @param numericInput The numeric input as string
     * @param minValue Minimum allowed value
     * @param maxValue Maximum allowed value
     * @return Sanitized numeric value or null if invalid
     */
    Long sanitizeNumericInput(String numericInput, Long minValue, Long maxValue);
    
    /**
     * Remove or escape special characters that could be used in attacks
     * 
     * @param input The input string to process
     * @return String with special characters escaped or removed
     */
    String escapeSpecialCharacters(String input);
}