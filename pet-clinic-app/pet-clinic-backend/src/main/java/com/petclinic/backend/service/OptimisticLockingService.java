package com.petclinic.backend.service;

import com.petclinic.backend.dto.BulkOperationResult;
import com.petclinic.backend.model.BaseEntity;

import java.util.List;
import java.util.Map;

/**
 * Service for handling optimistic locking in bulk operations
 * Provides retry logic and conflict resolution for concurrent modifications
 * Validates: Requirements 5.5
 */
public interface OptimisticLockingService {
    
    /**
     * Execute a bulk operation with optimistic locking protection
     * @param entityType Type of entity being operated on
     * @param entityIds List of entity IDs to operate on
     * @param operation The bulk operation to execute
     * @param maxRetries Maximum number of retry attempts
     * @return Result of the bulk operation
     */
    BulkOperationResult executeBulkOperationWithLocking(
        String entityType,
        List<Long> entityIds,
        BulkOperation operation,
        int maxRetries
    );
    
    /**
     * Validate entity versions before bulk operation
     * @param entityType Type of entity
     * @param entityVersions Map of entity ID to expected version
     * @return Map of entity ID to current version (empty if all valid)
     */
    Map<Long, Long> validateEntityVersions(String entityType, Map<Long, Long> entityVersions);
    
    /**
     * Get current versions of entities
     * @param entityType Type of entity
     * @param entityIds List of entity IDs
     * @return Map of entity ID to current version
     */
    Map<Long, Long> getCurrentEntityVersions(String entityType, List<Long> entityIds);
    
    /**
     * Handle optimistic locking failure
     * @param entityType Type of entity
     * @param entityId ID of the entity that failed
     * @param expectedVersion Expected version
     * @param actualVersion Actual version
     * @param attempt Current attempt number
     * @return true if should retry, false if should fail
     */
    boolean handleOptimisticLockingFailure(
        String entityType,
        Long entityId,
        Long expectedVersion,
        Long actualVersion,
        int attempt
    );
    
    /**
     * Get optimistic locking statistics
     * @return Map containing locking statistics
     */
    Map<String, Object> getOptimisticLockingStatistics();
    
    /**
     * Functional interface for bulk operations
     */
    @FunctionalInterface
    interface BulkOperation {
        BulkOperationResult execute(List<Long> entityIds) throws Exception;
    }
}