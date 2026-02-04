package com.petclinic.backend.service;

import com.petclinic.backend.dto.BulkDeleteRequest;
import com.petclinic.backend.dto.BulkOperationResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for coordinating concurrent operations with safety mechanisms
 * Combines request deduplication and optimistic locking for bulk operations
 * Validates: Requirements 5.5
 */
public interface ConcurrentOperationService {
    
    /**
     * Execute a bulk delete operation with concurrent safety
     * @param request Bulk delete request
     * @return CompletableFuture with the operation result
     */
    CompletableFuture<BulkOperationResult> executeSafeBulkDelete(BulkDeleteRequest request);
    
    /**
     * Execute a bulk operation with concurrent safety
     * @param entityType Type of entity
     * @param operationType Type of operation (DELETE, UPDATE, etc.)
     * @param entityIds List of entity IDs
     * @param operation The operation to execute
     * @param entityVersions Optional map of entity versions for optimistic locking
     * @return CompletableFuture with the operation result
     */
    CompletableFuture<BulkOperationResult> executeSafeBulkOperation(
        String entityType,
        String operationType,
        java.util.List<Long> entityIds,
        OptimisticLockingService.BulkOperation operation,
        Map<Long, Long> entityVersions
    );
    
    /**
     * Check if a bulk operation is currently in progress
     * @param requestKey Unique key for the operation
     * @return true if operation is in progress, false otherwise
     */
    boolean isOperationInProgress(String requestKey);
    
    /**
     * Cancel a bulk operation if possible
     * @param requestKey Unique key for the operation
     * @return true if operation was cancelled, false otherwise
     */
    boolean cancelOperation(String requestKey);
    
    /**
     * Get the status of a bulk operation
     * @param requestKey Unique key for the operation
     * @return Operation status or null if not found
     */
    BulkOperationResult getOperationStatus(String requestKey);
    
    /**
     * Get concurrent operation statistics
     * @return Map containing operation statistics
     */
    Map<String, Object> getConcurrentOperationStatistics();
    
    /**
     * Generate a unique request key for deduplication
     * @param entityType Type of entity
     * @param operationType Type of operation
     * @param entityIds List of entity IDs
     * @param userId User ID performing the operation
     * @return Unique request key
     */
    String generateRequestKey(String entityType, String operationType, 
                             java.util.List<Long> entityIds, String userId);
}