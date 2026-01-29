package com.petclinic.backend.repository;

import com.petclinic.backend.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for AuditLog entity operations
 */
@Repository
public interface AuditLogRepository extends BaseRepository<AuditLog, Long> {

    /**
     * Find audit logs by username
     */
    List<AuditLog> findByUsernameOrderByTimestampDesc(String username);

    /**
     * Find audit logs by resource type
     */
    List<AuditLog> findByResourceOrderByTimestampDesc(String resource);

    /**
     * Find audit logs by resource and resource ID
     */
    List<AuditLog> findByResourceAndResourceIdOrderByTimestampDesc(String resource, Long resourceId);

    /**
     * Find audit logs within a date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    List<AuditLog> findByTimestampBetween(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * Find audit logs by action type
     */
    List<AuditLog> findByActionOrderByTimestampDesc(String action);

    /**
     * Find audit logs with pagination
     */
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    /**
     * Find audit logs by username with pagination
     */
    Page<AuditLog> findByUsernameOrderByTimestampDesc(String username, Pageable pageable);

    /**
     * Find recent audit logs for a specific resource
     */
    @Query("SELECT a FROM AuditLog a WHERE a.resource = :resource AND a.resourceId = :resourceId " +
           "ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentByResource(@Param("resource") String resource, 
                                       @Param("resourceId") Long resourceId, 
                                       Pageable pageable);

    /**
     * Count audit logs by action within date range
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action = :action " +
           "AND a.timestamp BETWEEN :startDate AND :endDate")
    Long countByActionAndTimestampBetween(@Param("action") String action,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);
}