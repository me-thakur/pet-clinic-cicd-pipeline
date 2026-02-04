package com.petclinic.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive error logging with debugging information
 * Requirements: 15.1, 15.2, 15.3, 15.4, 15.5
 */
@Service
public class ErrorLoggingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ErrorLoggingService.class);
    private static final Logger errorLogger = LoggerFactory.getLogger("ERROR_AUDIT");
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Error tracking
    private final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastErrorTimes = new ConcurrentHashMap<>();
    
    /**
     * Error severity levels
     */
    public enum ErrorSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    /**
     * Error context information
     */
    public static class ErrorContext {
        private String errorId;
        private LocalDateTime timestamp;
        private String userId;
        private String sessionId;
        private String requestId;
        private String userAgent;
        private String ipAddress;
        private String requestUri;
        private String httpMethod;
        private Map<String, String> requestHeaders;
        private Map<String, Object> additionalContext;
        
        public ErrorContext() {
            this.errorId = UUID.randomUUID().toString();
            this.timestamp = LocalDateTime.now();
            this.additionalContext = new HashMap<>();
        }
        
        // Getters and setters
        public String getErrorId() { return errorId; }
        public void setErrorId(String errorId) { this.errorId = errorId; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        public String getRequestUri() { return requestUri; }
        public void setRequestUri(String requestUri) { this.requestUri = requestUri; }
        public String getHttpMethod() { return httpMethod; }
        public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
        public Map<String, String> getRequestHeaders() { return requestHeaders; }
        public void setRequestHeaders(Map<String, String> requestHeaders) { this.requestHeaders = requestHeaders; }
        public Map<String, Object> getAdditionalContext() { return additionalContext; }
        public void setAdditionalContext(Map<String, Object> additionalContext) { this.additionalContext = additionalContext; }
        
        public void addContext(String key, Object value) {
            this.additionalContext.put(key, value);
        }
    }
    
    /**
     * Log error with comprehensive context
     */
    public String logError(Throwable error, ErrorSeverity severity, HttpServletRequest request) {
        ErrorContext context = buildErrorContext(request);
        return logError(error, severity, context);
    }
    
    /**
     * Log error with custom context
     */
    public String logError(Throwable error, ErrorSeverity severity, ErrorContext context) {
        try {
            // Set MDC for structured logging
            setMDCContext(context);
            
            // Create error log entry
            Map<String, Object> errorLog = createErrorLogEntry(error, severity, context);
            
            // Log based on severity
            logBySeverity(severity, error, errorLog);
            
            // Update error statistics
            updateErrorStatistics(error, severity);
            
            // Log to audit logger
            errorLogger.info("ERROR_AUDIT: {}", objectMapper.writeValueAsString(errorLog));
            
            return context.getErrorId();
            
        } catch (Exception e) {
            logger.error("Failed to log error properly", e);
            return "LOGGING_FAILED_" + UUID.randomUUID().toString();
        } finally {
            // Clear MDC
            MDC.clear();
        }
    }
    
    /**
     * Build error context from HTTP request
     */
    private ErrorContext buildErrorContext(HttpServletRequest request) {
        ErrorContext context = new ErrorContext();
        
        if (request != null) {
            context.setRequestUri(request.getRequestURI());
            context.setHttpMethod(request.getMethod());
            context.setUserAgent(request.getHeader("User-Agent"));
            context.setIpAddress(getClientIpAddress(request));
            context.setSessionId(request.getSession(false) != null ? request.getSession().getId() : null);
            context.setRequestId(request.getHeader("X-Request-ID"));
            
            // Extract user information if available
            if (request.getUserPrincipal() != null) {
                context.setUserId(request.getUserPrincipal().getName());
            }
            
            // Capture important headers (excluding sensitive ones)
            Map<String, String> headers = new HashMap<>();
            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (isSafeHeader(headerName)) {
                    headers.put(headerName, request.getHeader(headerName));
                }
            }
            context.setRequestHeaders(headers);
            
            // Add request parameters (excluding sensitive ones)
            Map<String, Object> requestParams = new HashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                if (isSafeParameter(key)) {
                    requestParams.put(key, values.length == 1 ? values[0] : values);
                }
            });
            context.addContext("requestParameters", requestParams);
        }
        
        return context;
    }
    
    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle comma-separated IPs
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Check if header is safe to log
     */
    private boolean isSafeHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        return !lowerName.contains("authorization") &&
               !lowerName.contains("cookie") &&
               !lowerName.contains("password") &&
               !lowerName.contains("token") &&
               !lowerName.contains("secret");
    }
    
    /**
     * Check if parameter is safe to log
     */
    private boolean isSafeParameter(String paramName) {
        String lowerName = paramName.toLowerCase();
        return !lowerName.contains("password") &&
               !lowerName.contains("token") &&
               !lowerName.contains("secret") &&
               !lowerName.contains("key") &&
               !lowerName.contains("credential");
    }
    
    /**
     * Set MDC context for structured logging
     */
    private void setMDCContext(ErrorContext context) {
        MDC.put("errorId", context.getErrorId());
        MDC.put("timestamp", context.getTimestamp().toString());
        if (context.getUserId() != null) MDC.put("userId", context.getUserId());
        if (context.getSessionId() != null) MDC.put("sessionId", context.getSessionId());
        if (context.getRequestId() != null) MDC.put("requestId", context.getRequestId());
        if (context.getRequestUri() != null) MDC.put("requestUri", context.getRequestUri());
        if (context.getHttpMethod() != null) MDC.put("httpMethod", context.getHttpMethod());
        if (context.getIpAddress() != null) MDC.put("ipAddress", context.getIpAddress());
    }
    
    /**
     * Create comprehensive error log entry
     */
    private Map<String, Object> createErrorLogEntry(Throwable error, ErrorSeverity severity, ErrorContext context) {
        Map<String, Object> logEntry = new HashMap<>();
        
        // Basic error information
        logEntry.put("errorId", context.getErrorId());
        logEntry.put("timestamp", context.getTimestamp());
        logEntry.put("severity", severity.name());
        logEntry.put("errorType", error.getClass().getSimpleName());
        logEntry.put("errorMessage", error.getMessage());
        
        // Stack trace (limited for non-critical errors)
        if (severity == ErrorSeverity.CRITICAL || severity == ErrorSeverity.HIGH) {
            logEntry.put("stackTrace", getStackTraceString(error));
        } else {
            logEntry.put("stackTrace", getStackTraceString(error, 5)); // Limit to 5 lines
        }
        
        // Request context
        if (context.getRequestUri() != null) {
            Map<String, Object> requestInfo = new HashMap<>();
            requestInfo.put("uri", context.getRequestUri());
            requestInfo.put("method", context.getHttpMethod());
            requestInfo.put("userAgent", context.getUserAgent());
            requestInfo.put("ipAddress", context.getIpAddress());
            requestInfo.put("headers", context.getRequestHeaders());
            logEntry.put("request", requestInfo);
        }
        
        // User context
        if (context.getUserId() != null || context.getSessionId() != null) {
            Map<String, Object> userInfo = new HashMap<>();
            if (context.getUserId() != null) userInfo.put("userId", context.getUserId());
            if (context.getSessionId() != null) userInfo.put("sessionId", context.getSessionId());
            logEntry.put("user", userInfo);
        }
        
        // Additional context
        if (!context.getAdditionalContext().isEmpty()) {
            logEntry.put("additionalContext", context.getAdditionalContext());
        }
        
        // System information
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("osName", System.getProperty("os.name"));
        systemInfo.put("osVersion", System.getProperty("os.version"));
        systemInfo.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        systemInfo.put("maxMemory", Runtime.getRuntime().maxMemory());
        systemInfo.put("freeMemory", Runtime.getRuntime().freeMemory());
        logEntry.put("system", systemInfo);
        
        return logEntry;
    }
    
    /**
     * Get stack trace as string
     */
    private String getStackTraceString(Throwable error) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        error.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Get limited stack trace as string
     */
    private String getStackTraceString(Throwable error, int maxLines) {
        String fullTrace = getStackTraceString(error);
        String[] lines = fullTrace.split("\n");
        
        if (lines.length <= maxLines) {
            return fullTrace;
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxLines; i++) {
            sb.append(lines[i]).append("\n");
        }
        sb.append("... (").append(lines.length - maxLines).append(" more lines)");
        
        return sb.toString();
    }
    
    /**
     * Log based on severity level
     */
    private void logBySeverity(ErrorSeverity severity, Throwable error, Map<String, Object> errorLog) {
        String message = "Error ID: {} - {} - {}";
        Object[] args = {
            errorLog.get("errorId"),
            errorLog.get("errorType"),
            errorLog.get("errorMessage")
        };
        
        switch (severity) {
            case CRITICAL:
                logger.error(message, args, error);
                break;
            case HIGH:
                logger.error(message, args, error);
                break;
            case MEDIUM:
                logger.warn(message, args);
                break;
            case LOW:
                logger.info(message, args);
                break;
        }
    }
    
    /**
     * Update error statistics
     */
    private void updateErrorStatistics(Throwable error, ErrorSeverity severity) {
        String errorKey = error.getClass().getSimpleName() + "_" + severity.name();
        
        errorCounts.computeIfAbsent(errorKey, k -> new AtomicLong(0)).incrementAndGet();
        lastErrorTimes.put(errorKey, LocalDateTime.now());
    }
    
    /**
     * Get error statistics
     */
    public Map<String, Object> getErrorStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        Map<String, Long> counts = new HashMap<>();
        errorCounts.forEach((key, value) -> counts.put(key, value.get()));
        stats.put("errorCounts", counts);
        
        Map<String, LocalDateTime> lastTimes = new HashMap<>(lastErrorTimes);
        stats.put("lastErrorTimes", lastTimes);
        
        // Calculate total errors
        long totalErrors = errorCounts.values().stream().mapToLong(AtomicLong::get).sum();
        stats.put("totalErrors", totalErrors);
        
        return stats;
    }
    
    /**
     * Clear error statistics (for testing)
     */
    public void clearStatistics() {
        errorCounts.clear();
        lastErrorTimes.clear();
    }
    
    /**
     * Log performance issue
     */
    public void logPerformanceIssue(String operation, long duration, long threshold, ErrorContext context) {
        context.addContext("operation", operation);
        context.addContext("duration", duration);
        context.addContext("threshold", threshold);
        context.addContext("performanceIssue", true);
        
        RuntimeException performanceError = new RuntimeException(
            String.format("Performance issue: %s took %dms (threshold: %dms)", operation, duration, threshold)
        );
        
        logError(performanceError, ErrorSeverity.MEDIUM, context);
    }
    
    /**
     * Log security issue
     */
    public void logSecurityIssue(String securityEvent, String details, ErrorContext context) {
        context.addContext("securityEvent", securityEvent);
        context.addContext("securityDetails", details);
        context.addContext("securityIssue", true);
        
        RuntimeException securityError = new RuntimeException(
            String.format("Security issue: %s - %s", securityEvent, details)
        );
        
        logError(securityError, ErrorSeverity.HIGH, context);
    }
    
    /**
     * Log business logic violation
     */
    public void logBusinessLogicViolation(String rule, String violation, ErrorContext context) {
        context.addContext("businessRule", rule);
        context.addContext("violation", violation);
        context.addContext("businessLogicViolation", true);
        
        RuntimeException businessError = new RuntimeException(
            String.format("Business logic violation: %s - %s", rule, violation)
        );
        
        logError(businessError, ErrorSeverity.MEDIUM, context);
    }
}