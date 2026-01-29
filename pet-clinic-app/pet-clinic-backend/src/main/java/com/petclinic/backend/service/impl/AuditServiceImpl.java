package com.petclinic.backend.service.impl;

import com.petclinic.backend.model.AuditLog;
import com.petclinic.backend.repository.AuditLogRepository;
import com.petclinic.backend.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of AuditService for audit logging operations
 */
@Service
@Transactional
public class AuditServiceImpl implements AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void logDataAccess(String username, String action, String resource, Long resourceId) {
        logDataAccess(username, action, resource, resourceId, null, null, null);
    }

    @Override
    public void logDataAccess(String username, String action, String resource, Long resourceId, String details) {
        logDataAccess(username, action, resource, resourceId, details, null, null);
    }

    @Override
    public void logDataAccess(String username, String action, String resource, Long resourceId, 
                             String details, String ipAddress, String userAgent) {
        try {
            AuditLog auditLog = new AuditLog(username, action, resource, resourceId, details);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            
            auditLogRepository.save(auditLog);
            
            logger.info("Audit log created: user={}, action={}, resource={}, resourceId={}", 
                       username, action, resource, resourceId);
        } catch (Exception e) {
            logger.error("Failed to create audit log: user={}, action={}, resource={}, resourceId={}", 
                        username, action, resource, resourceId, e);
            // Don't throw exception to avoid disrupting business operations
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByUser(String username) {
        return auditLogRepository.findByUsernameOrderByTimestampDesc(username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByResource(String resource, Long resourceId) {
        return auditLogRepository.findByResourceAndResourceIdOrderByTimestampDesc(resource, resourceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findByTimestampBetween(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByAction(String action) {
        return auditLogRepository.findByActionOrderByTimestampDesc(action);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByUser(String username, Pageable pageable) {
        return auditLogRepository.findByUsernameOrderByTimestampDesc(username, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getRecentAuditLogs(String resource, Long resourceId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return auditLogRepository.findRecentByResource(resource, resourceId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countAuditLogsByAction(String action, LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.countByActionAndTimestampBetween(action, startDate, endDate);
    }

    @Override
    public void deleteOldAuditLogs(LocalDateTime cutoffDate) {
        try {
            List<AuditLog> oldLogs = auditLogRepository.findByTimestampBetween(
                LocalDateTime.of(2000, 1, 1, 0, 0), cutoffDate);
            
            if (!oldLogs.isEmpty()) {
                auditLogRepository.deleteAll(oldLogs);
                logger.info("Deleted {} old audit logs before {}", oldLogs.size(), cutoffDate);
            }
        } catch (Exception e) {
            logger.error("Failed to delete old audit logs before {}", cutoffDate, e);
        }
    }
}