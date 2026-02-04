package com.petclinic.backend.service;

import com.petclinic.backend.model.User;
import org.springframework.security.core.Authentication;

import java.util.Map;

/**
 * Service interface for security audit logging and access control validation
 * Provides comprehensive audit trail for all security-sensitive operations
 * 
 * Validates: Requirements 17.1, 17.2, 17.4, 17.5
 */
public interface SecurityAuditService {
    
    /**
     * Log security-sensitive operation with full context
     * 
     * @param operation The operation being performed (e.g., "BULK_DELETE", "REPORT_EXPORT")
     * @param resource The resource being accessed (e.g., "owners", "reports")
     * @param resourceIds The IDs of resources being accessed
     * @param authentication The user's authentication context
     * @param additionalContext Additional context information
     */
    void logSecurityOperation(String operation, String resource, Object resourceIds, 
                            Authentication authentication, Map<String, Object> additionalContext);
    
    /**
     * Log access denied events for security monitoring
     * 
     * @param operation The attempted operation
     * @param resource The resource that was denied access
     * @param authentication The user's authentication context
     * @param reason The reason for denial
     */
    void logAccessDenied(String operation, String resource, Authentication authentication, String reason);
    
    /**
     * Log privilege escalation attempts
     * 
     * @param attemptedRole The role the user attempted to access
     * @param authentication The user's authentication context
     * @param resource The resource they tried to access
     */
    void logPrivilegeEscalationAttempt(String attemptedRole, Authentication authentication, String resource);
    
    /**
     * Validate if user has required role for operation
     * 
     * @param authentication The user's authentication context
     * @param requiredRole The role required for the operation
     * @return true if user has the required role
     */
    boolean hasRequiredRole(Authentication authentication, String requiredRole);
    
    /**
     * Validate if user can perform bulk operations
     * 
     * @param authentication The user's authentication context
     * @param entityType The type of entity for bulk operation
     * @param operationType The type of bulk operation (DELETE, UPDATE, etc.)
     * @return true if user can perform the bulk operation
     */
    boolean canPerformBulkOperation(Authentication authentication, String entityType, String operationType);
    
    /**
     * Validate if user can access reports
     * 
     * @param authentication The user's authentication context
     * @param reportType The type of report being accessed
     * @return true if user can access the report
     */
    boolean canAccessReports(Authentication authentication, String reportType);
    
    /**
     * Sanitize error message to prevent information leakage
     * 
     * @param originalMessage The original error message
     * @param authentication The user's authentication context
     * @return Sanitized error message appropriate for the user's role
     */
    String sanitizeErrorMessage(String originalMessage, Authentication authentication);
    
    /**
     * Log failed authentication attempts for security monitoring
     * 
     * @param username The attempted username
     * @param ipAddress The IP address of the attempt
     * @param userAgent The user agent string
     * @param failureReason The reason for authentication failure
     */
    void logAuthenticationFailure(String username, String ipAddress, String userAgent, String failureReason);
    
    /**
     * Log successful authentication for audit trail
     * 
     * @param username The authenticated username
     * @param ipAddress The IP address of the login
     * @param userAgent The user agent string
     */
    void logAuthenticationSuccess(String username, String ipAddress, String userAgent);
}