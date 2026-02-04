package com.petclinic.backend.service.impl;

import com.petclinic.backend.service.AuditService;
import com.petclinic.backend.service.SecurityAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Implementation of SecurityAuditService for comprehensive security logging and access control
 * 
 * Validates: Requirements 17.1, 17.2, 17.3, 17.4, 17.5
 */
@Service
public class SecurityAuditServiceImpl implements SecurityAuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditServiceImpl.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    
    @Autowired
    private AuditService auditService;
    
    // Patterns for sensitive information that should be sanitized
    private static final Pattern SENSITIVE_PATTERNS = Pattern.compile(
        "(?i)(password|token|secret|key|credential|ssn|social.security|credit.card|bank.account)",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public void logSecurityOperation(String operation, String resource, Object resourceIds, 
                                   Authentication authentication, Map<String, Object> additionalContext) {
        
        String username = getUsername(authentication);
        String userRoles = getUserRoles(authentication);
        
        Map<String, Object> securityContext = new HashMap<>();
        securityContext.put("operation", operation);
        securityContext.put("resource", resource);
        securityContext.put("resourceIds", resourceIds);
        securityContext.put("username", username);
        securityContext.put("userRoles", userRoles);
        securityContext.put("timestamp", LocalDateTime.now());
        securityContext.put("ipAddress", getClientIpAddress());
        
        if (additionalContext != null) {
            // Sanitize additional context to prevent sensitive data logging
            Map<String, Object> sanitizedContext = sanitizeContext(additionalContext);
            securityContext.putAll(sanitizedContext);
        }
        
        // Log to security audit logger
        securityLogger.info("SECURITY_OPERATION: {}", securityContext);
        
        // Log to general audit service
        auditService.logDataAccess(username, operation, resource, 
                                 extractResourceId(resourceIds), 
                                 "Security operation: " + operation + " on " + resource);
        
        logger.info("Security operation logged: {} performed {} on {} by user {}", 
                   operation, resource, resourceIds, username);
    }
    
    @Override
    public void logAccessDenied(String operation, String resource, Authentication authentication, String reason) {
        String username = getUsername(authentication);
        String userRoles = getUserRoles(authentication);
        
        Map<String, Object> denialContext = new HashMap<>();
        denialContext.put("event", "ACCESS_DENIED");
        denialContext.put("operation", operation);
        denialContext.put("resource", resource);
        denialContext.put("username", username);
        denialContext.put("userRoles", userRoles);
        denialContext.put("reason", reason);
        denialContext.put("timestamp", LocalDateTime.now());
        denialContext.put("ipAddress", getClientIpAddress());
        
        // Log to security audit logger
        securityLogger.warn("ACCESS_DENIED: {}", denialContext);
        
        // Log to general audit service
        auditService.logDataAccess(username, "ACCESS_DENIED", resource, null, 
                                 "Access denied: " + reason);
        
        logger.warn("Access denied: User {} with roles {} attempted {} on {} - Reason: {}", 
                   username, userRoles, operation, resource, reason);
    }
    
    @Override
    public void logPrivilegeEscalationAttempt(String attemptedRole, Authentication authentication, String resource) {
        String username = getUsername(authentication);
        String userRoles = getUserRoles(authentication);
        
        Map<String, Object> escalationContext = new HashMap<>();
        escalationContext.put("event", "PRIVILEGE_ESCALATION_ATTEMPT");
        escalationContext.put("username", username);
        escalationContext.put("currentRoles", userRoles);
        escalationContext.put("attemptedRole", attemptedRole);
        escalationContext.put("resource", resource);
        escalationContext.put("timestamp", LocalDateTime.now());
        escalationContext.put("ipAddress", getClientIpAddress());
        
        // Log to security audit logger with high severity
        securityLogger.error("PRIVILEGE_ESCALATION_ATTEMPT: {}", escalationContext);
        
        // Log to general audit service
        auditService.logDataAccess(username, "PRIVILEGE_ESCALATION_ATTEMPT", resource, null, 
                                 "Attempted to access " + attemptedRole + " role for " + resource);
        
        logger.error("SECURITY ALERT: User {} with roles {} attempted to escalate to {} for resource {}", 
                    username, userRoles, attemptedRole, resource);
    }
    
    @Override
    public boolean hasRequiredRole(Authentication authentication, String requiredRole) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        boolean hasRole = authorities.stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + requiredRole) ||
                                 authority.getAuthority().equals(requiredRole));
        
        if (!hasRole) {
            logAccessDenied("ROLE_CHECK", requiredRole, authentication, 
                          "User does not have required role: " + requiredRole);
        }
        
        return hasRole;
    }
    
    @Override
    public boolean canPerformBulkOperation(Authentication authentication, String entityType, String operationType) {
        if (authentication == null || !authentication.isAuthenticated()) {
            logAccessDenied("BULK_" + operationType, entityType, authentication, "User not authenticated");
            return false;
        }
        
        // Check if user has USER role (minimum requirement for bulk operations)
        boolean hasUserRole = hasRequiredRole(authentication, "USER");
        
        if (!hasUserRole) {
            logAccessDenied("BULK_" + operationType, entityType, authentication, 
                          "User does not have minimum USER role for bulk operations");
            return false;
        }
        
        // For DELETE operations, require additional validation
        if ("DELETE".equals(operationType)) {
            // Log the bulk delete attempt for audit purposes
            Map<String, Object> context = new HashMap<>();
            context.put("entityType", entityType);
            context.put("operationType", operationType);
            
            logSecurityOperation("BULK_DELETE_AUTHORIZATION_CHECK", entityType, null, authentication, context);
        }
        
        return true;
    }
    
    @Override
    public boolean canAccessReports(Authentication authentication, String reportType) {
        if (authentication == null || !authentication.isAuthenticated()) {
            logAccessDenied("REPORT_ACCESS", reportType, authentication, "User not authenticated");
            return false;
        }
        
        // Reports require ADMIN role
        boolean hasAdminRole = hasRequiredRole(authentication, "ADMIN");
        
        if (!hasAdminRole) {
            logPrivilegeEscalationAttempt("ADMIN", authentication, "reports/" + reportType);
            return false;
        }
        
        // Log successful report access authorization
        Map<String, Object> context = new HashMap<>();
        context.put("reportType", reportType);
        
        logSecurityOperation("REPORT_ACCESS_AUTHORIZED", "reports", reportType, authentication, context);
        
        return true;
    }
    
    @Override
    public String sanitizeErrorMessage(String originalMessage, Authentication authentication) {
        if (originalMessage == null) {
            return "An error occurred";
        }
        
        // Check if user is admin - admins can see more detailed error messages
        boolean isAdmin = hasRequiredRole(authentication, "ADMIN");
        
        // Remove sensitive information patterns
        String sanitized = SENSITIVE_PATTERNS.matcher(originalMessage).replaceAll("[REDACTED]");
        
        // For non-admin users, provide generic messages for certain error types
        if (!isAdmin) {
            String lowerMessage = sanitized.toLowerCase();
            
            if (lowerMessage.contains("database") || lowerMessage.contains("sql") || 
                lowerMessage.contains("connection")) {
                return "A system error occurred. Please try again later.";
            }
            
            if (lowerMessage.contains("internal") || lowerMessage.contains("server")) {
                return "An internal error occurred. Please contact support if the problem persists.";
            }
            
            if (lowerMessage.contains("authentication") || lowerMessage.contains("authorization")) {
                return "Access denied. Please check your permissions.";
            }
        }
        
        return sanitized;
    }
    
    @Override
    public void logAuthenticationFailure(String username, String ipAddress, String userAgent, String failureReason) {
        Map<String, Object> failureContext = new HashMap<>();
        failureContext.put("event", "AUTHENTICATION_FAILURE");
        failureContext.put("username", username);
        failureContext.put("ipAddress", ipAddress);
        failureContext.put("userAgent", userAgent);
        failureContext.put("failureReason", failureReason);
        failureContext.put("timestamp", LocalDateTime.now());
        
        // Log to security audit logger
        securityLogger.warn("AUTHENTICATION_FAILURE: {}", failureContext);
        
        // Log to general audit service
        auditService.logDataAccess(username, "AUTHENTICATION_FAILURE", "login", null, 
                                 "Authentication failed: " + failureReason);
        
        logger.warn("Authentication failure for user {} from IP {} - Reason: {}", 
                   username, ipAddress, failureReason);
    }
    
    @Override
    public void logAuthenticationSuccess(String username, String ipAddress, String userAgent) {
        Map<String, Object> successContext = new HashMap<>();
        successContext.put("event", "AUTHENTICATION_SUCCESS");
        successContext.put("username", username);
        successContext.put("ipAddress", ipAddress);
        successContext.put("userAgent", userAgent);
        successContext.put("timestamp", LocalDateTime.now());
        
        // Log to security audit logger
        securityLogger.info("AUTHENTICATION_SUCCESS: {}", successContext);
        
        // Log to general audit service
        auditService.logDataAccess(username, "AUTHENTICATION_SUCCESS", "login", null, 
                                 "User successfully authenticated");
        
        logger.info("Successful authentication for user {} from IP {}", username, ipAddress);
    }
    
    // Helper methods
    
    private String getUsername(Authentication authentication) {
        if (authentication == null) {
            return "anonymous";
        }
        return authentication.getName() != null ? authentication.getName() : "unknown";
    }
    
    private String getUserRoles(Authentication authentication) {
        if (authentication == null) {
            return "none";
        }
        
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .reduce((a, b) -> a + "," + b)
            .orElse("none");
    }
    
    private String getClientIpAddress() {
        // In a real implementation, this would extract the IP from the HTTP request
        // For now, return a placeholder
        return "unknown";
    }
    
    private Long extractResourceId(Object resourceIds) {
        if (resourceIds instanceof Long) {
            return (Long) resourceIds;
        }
        if (resourceIds instanceof String) {
            try {
                return Long.parseLong((String) resourceIds);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private Map<String, Object> sanitizeContext(Map<String, Object> context) {
        Map<String, Object> sanitized = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Check if key or value contains sensitive information
            if (SENSITIVE_PATTERNS.matcher(key).find() || 
                (value instanceof String && SENSITIVE_PATTERNS.matcher((String) value).find())) {
                sanitized.put(key, "[REDACTED]");
            } else {
                sanitized.put(key, value);
            }
        }
        
        return sanitized;
    }
}