package com.petclinic.backend.service.impl;

import com.petclinic.backend.service.DataSanitizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Implementation of DataSanitizationService for comprehensive data cleaning and validation
 * 
 * Validates: Requirements 17.3, 17.4
 */
@Service
public class DataSanitizationServiceImpl implements DataSanitizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSanitizationServiceImpl.class);
    
    // Patterns for detecting malicious content
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)<script[^>]*>.*?</script>|javascript:|on\\w+\\s*=|<iframe|<object|<embed|<link|<meta",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|sp_|xp_|--|;|'|\")",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[1-9]?[0-9]{7,15}$"
    );
    
    private static final Pattern DIRECTORY_TRAVERSAL_PATTERN = Pattern.compile(
        "(\\.\\./|\\.\\.\\\\|%2e%2e%2f|%2e%2e%5c)",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public String sanitizeString(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove null bytes
        String sanitized = input.replace("\0", "");
        
        // Remove or escape potentially dangerous characters
        sanitized = escapeSpecialCharacters(sanitized);
        
        // Check for malicious content
        if (containsMaliciousContent(sanitized)) {
            logger.warn("Potentially malicious content detected and sanitized: {}", 
                       sanitized.substring(0, Math.min(50, sanitized.length())));
            // Remove detected malicious patterns
            sanitized = XSS_PATTERN.matcher(sanitized).replaceAll("");
        }
        
        // Trim whitespace
        sanitized = sanitized.trim();
        
        // Limit length to prevent buffer overflow attacks
        if (sanitized.length() > 10000) {
            sanitized = sanitized.substring(0, 10000);
            logger.warn("Input truncated due to excessive length");
        }
        
        return sanitized;
    }
    
    @Override
    public String sanitizeHtml(String htmlContent) {
        if (htmlContent == null) {
            return null;
        }
        
        // Remove script tags and other dangerous elements
        String sanitized = htmlContent;
        sanitized = XSS_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove event handlers
        sanitized = sanitized.replaceAll("(?i)on\\w+\\s*=\\s*[\"'][^\"']*[\"']", "");
        
        // Remove javascript: URLs
        sanitized = sanitized.replaceAll("(?i)javascript:", "");
        
        // Remove data: URLs (except for images)
        sanitized = sanitized.replaceAll("(?i)data:(?!image/)", "");
        
        return sanitized.trim();
    }
    
    @Override
    public String sanitizeSql(String sqlInput) {
        if (sqlInput == null) {
            return null;
        }
        
        // Escape single quotes
        String sanitized = sqlInput.replace("'", "''");
        
        // Remove or escape dangerous SQL keywords
        if (SQL_INJECTION_PATTERN.matcher(sanitized).find()) {
            logger.warn("Potential SQL injection attempt detected and sanitized");
            // For safety, reject input with SQL injection patterns
            return null;
        }
        
        return sanitized.trim();
    }
    
    @Override
    public String sanitizeEmail(String email) {
        if (email == null) {
            return null;
        }
        
        String sanitized = sanitizeString(email).toLowerCase();
        
        if (!EMAIL_PATTERN.matcher(sanitized).matches()) {
            logger.debug("Invalid email format: {}", sanitized);
            return null;
        }
        
        // Additional checks for email-specific attacks
        if (sanitized.contains("..") || sanitized.startsWith(".") || sanitized.endsWith(".")) {
            logger.debug("Email contains invalid dot patterns: {}", sanitized);
            return null;
        }
        
        return sanitized;
    }
    
    @Override
    public String sanitizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        
        // Remove all non-digit characters except + at the beginning
        String sanitized = phoneNumber.replaceAll("[^+0-9]", "");
        
        // Ensure + is only at the beginning
        if (sanitized.contains("+")) {
            if (!sanitized.startsWith("+")) {
                sanitized = sanitized.replace("+", "");
            } else {
                // Remove any + after the first one
                sanitized = "+" + sanitized.substring(1).replace("+", "");
            }
        }
        
        if (!PHONE_PATTERN.matcher(sanitized).matches()) {
            logger.debug("Invalid phone number format: {}", sanitized);
            return null;
        }
        
        return sanitized;
    }
    
    @Override
    public Map<String, Object> sanitizeParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return null;
        }
        
        Map<String, Object> sanitized = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = sanitizeString(entry.getKey());
            Object value = entry.getValue();
            
            if (key != null) {
                if (value instanceof String) {
                    sanitized.put(key, sanitizeString((String) value));
                } else if (value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nestedMap = (Map<String, Object>) value;
                    sanitized.put(key, sanitizeParameters(nestedMap));
                } else {
                    // For non-string values, keep as-is but log for monitoring
                    sanitized.put(key, value);
                }
            }
        }
        
        return sanitized;
    }
    
    @Override
    public boolean containsMaliciousContent(String input) {
        if (input == null) {
            return false;
        }
        
        String lowerInput = input.toLowerCase();
        
        // Check for XSS patterns
        if (XSS_PATTERN.matcher(input).find()) {
            return true;
        }
        
        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(input).find()) {
            return true;
        }
        
        // Check for directory traversal
        if (DIRECTORY_TRAVERSAL_PATTERN.matcher(input).find()) {
            return true;
        }
        
        // Check for other suspicious patterns
        if (lowerInput.contains("eval(") || 
            lowerInput.contains("expression(") ||
            lowerInput.contains("vbscript:") ||
            lowerInput.contains("data:text/html") ||
            lowerInput.contains("base64,")) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        
        // Remove directory traversal patterns
        String sanitized = DIRECTORY_TRAVERSAL_PATTERN.matcher(fileName).replaceAll("");
        
        // Remove or replace dangerous characters
        sanitized = sanitized.replaceAll("[<>:\"|?*\\\\]", "_");
        
        // Remove leading/trailing dots and spaces
        sanitized = sanitized.replaceAll("^[.\\s]+|[.\\s]+$", "");
        
        // Prevent reserved Windows filenames
        String[] reservedNames = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", 
                                 "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", 
                                 "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};
        
        String upperSanitized = sanitized.toUpperCase();
        for (String reserved : reservedNames) {
            if (upperSanitized.equals(reserved) || upperSanitized.startsWith(reserved + ".")) {
                sanitized = "_" + sanitized;
                break;
            }
        }
        
        // Limit filename length
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }
        
        return sanitized.isEmpty() ? "sanitized_file" : sanitized;
    }
    
    @Override
    public Long sanitizeNumericInput(String numericInput, Long minValue, Long maxValue) {
        if (numericInput == null) {
            return null;
        }
        
        // Remove non-digit characters except minus sign at the beginning
        String sanitized = numericInput.replaceAll("[^-0-9]", "");
        
        // Ensure minus is only at the beginning
        if (sanitized.contains("-")) {
            if (!sanitized.startsWith("-")) {
                sanitized = sanitized.replace("-", "");
            } else {
                sanitized = "-" + sanitized.substring(1).replace("-", "");
            }
        }
        
        try {
            Long value = Long.parseLong(sanitized);
            
            // Check bounds
            if (minValue != null && value < minValue) {
                logger.debug("Numeric input {} below minimum {}", value, minValue);
                return null;
            }
            
            if (maxValue != null && value > maxValue) {
                logger.debug("Numeric input {} above maximum {}", value, maxValue);
                return null;
            }
            
            return value;
            
        } catch (NumberFormatException e) {
            logger.debug("Invalid numeric input: {}", sanitized);
            return null;
        }
    }
    
    @Override
    public String escapeSpecialCharacters(String input) {
        if (input == null) {
            return null;
        }
        
        // Escape HTML entities
        String escaped = input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;");
        
        return escaped;
    }
}