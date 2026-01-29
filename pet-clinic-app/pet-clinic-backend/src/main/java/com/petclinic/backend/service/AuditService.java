package com.petclinic.backend.service;

import com.petclinic.backend.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for audit logging operations
 */
public interface AuditService {

    /**
     * Log an audit event for data access
     */
    void logDataAccess(String username, String action, String resource, Long resourceId);

    /**
     * Log an audit event with additional details
     */
    void logDataAccess(String username, String action, String resource, Long resourceId, String details);

    /**
     * Log an audit event with full context information
     */
    void logDataAccess(String username, String action, String resource, Long resourceId, 
                      String details, String ipAddress, String userAgent);

    /**
     * Get audit logs for a specific user
     */
    List<AuditLog> getAuditLogsByUser(String username);

    /**
     * Get audit logs for a specific resource
     */
    List<AuditLog> getAuditLogsByResource(String resource, Long resourceId);

    /**
     * Get audit logs within a date range
     */
    List<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get audit logs by action type
     */
    List<AuditLog> getAuditLogsByAction(String action);

    /**
     * Get paginated audit logs
     */
    Page<AuditLog> getAuditLogs(Pageable pageable);

    /**
     * Get paginated audit logs for a specific user
     */
    Page<AuditLog> getAuditLogsByUser(String username, Pageable pageable);

    /**
     * Get recent audit logs for a resource
     */
    List<AuditLog> getRecentAuditLogs(String resource, Long resourceId, int limit);

    /**
     * Count audit logs by action within date range
     */
    Long countAuditLogsByAction(String action, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Delete old audit logs (for maintenance)
     */
    void deleteOldAuditLogs(LocalDateTime cutoffDate);
}