package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.BulkDeleteRequest;
import com.petclinic.backend.dto.BulkOperationResult;
import com.petclinic.backend.service.ConcurrentOperationService;
import com.petclinic.backend.service.EnhancedTableService;
import com.petclinic.backend.service.OptimisticLockingService;
import com.petclinic.backend.service.RequestDeduplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of concurrent operation service
 * Coordinates request deduplication and optimistic locking for safe bulk operations
 * Validates: Requirements 5.5
 */
@Service
public class ConcurrentOperationServiceImpl implements ConcurrentOperationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentOperationServiceImpl.class);
    
    // Configuration constants
    private static final long DEFAULT_DEDUPLICATION_WINDOW_MS = 5000; // 5 seconds
    private static final int DEFAULT_MAX_RETRIES = 3;
    
    @Autowired
    private RequestDeduplicationService deduplicationService;
    
    @Autowired
    private OptimisticLockingService optimisticLockingService;
    
    @Autowired
    private EnhancedTableService<Object> enhancedTableService;
    
    // Track active operations
    private final Map<String, CompletableFuture<BulkOperationResult>> activeOperations = new ConcurrentHashMap<>();
    private final Map<String, BulkOperationResult> operationResults = new ConcurrentHashMap<>();
    
    // Statistics
    private final AtomicLong totalOperations = new AtomicLong(0);
    private final AtomicLong successfulOperations = new AtomicLong(0);
    private final AtomicLong failedOperations = new AtomicLong(0);
    private final AtomicLong cancelledOperations = new AtomicLong(0);
    
    @Override
    public CompletableFuture<BulkOperationResult> executeSafeBulkDelete(BulkDeleteRequest request) {
        if (request == null || !request.isValid()) {
            CompletableFuture<BulkOperationResult> future = new CompletableFuture<>();
            BulkOperationResult result = new BulkOperationResult(false, 0, 0, request != null ? request.getEntityType() : "unknown");
            result.addError("Invalid bulk delete request");
            future.complete(result);
            return future;
        }
        
        String userId = getCurrentUserId();
        String requestKey = generateRequestKey(request.getEntityType(), "DELETE", request.getSelectedIds(), userId);
        
        logger.info("Executing safe bulk delete: {} {} items, request key: {}", 
                   request.getSelectedCount(), request.getEntityType(), requestKey);
        
        // Create the bulk operation
        OptimisticLockingService.BulkOperation operation = (entityIds) -> {
            return enhancedTableService.bulkDelete(request);
        };
        
        return executeSafeBulkOperation(
            request.getEntityType(),
            "DELETE",
            request.getSelectedIds(),
            operation,
            null // No specific version requirements for delete
        );
    }
    
    @Override
    public CompletableFuture<BulkOperationResult> executeSafeBulkOperation(
            String entityType,
            String operationType,
            List<Long> entityIds,
            OptimisticLockingService.BulkOperation operation,
            Map<Long, Long> entityVersions) {
        
        totalOperations.incrementAndGet();
        
        String userId = getCurrentUserId();
        String requestKey = generateRequestKey(entityType, operationType, entityIds, userId);
        
        logger.debug("Executing safe bulk operation: {} {} on {} items, request key: {}", 
                    operationType, entityType, entityIds.size(), requestKey);
        
        // Check if operation is already in progress
        CompletableFuture<BulkOperationResult> existingOperation = activeOperations.get(requestKey);
        if (existingOperation != null && !existingOperation.isDone()) {
            logger.info("Returning existing operation for request key: {}", requestKey);
            return existingOperation;
        }
        
        // Validate entity versions if provided
        if (entityVersions != null && !entityVersions.isEmpty()) {
            Map<Long, Long> conflicts = optimisticLockingService.validateEntityVersions(entityType, entityVersions);
            if (!conflicts.isEmpty()) {
                CompletableFuture<BulkOperationResult> future = new CompletableFuture<>();
                BulkOperationResult result = new BulkOperationResult(false, 0, entityIds.size(), entityType);
                result.addError("Version conflicts detected for entities: " + conflicts.keySet());
                future.complete(result);
                return future;
            }
        }
        
        // Execute with deduplication
        CompletableFuture<BulkOperationResult> future = deduplicationService.executeWithDeduplication(
            requestKey,
            DEFAULT_DEDUPLICATION_WINDOW_MS,
            () -> {
                try {
                    // Execute with optimistic locking
                    BulkOperationResult result = optimisticLockingService.executeBulkOperationWithLocking(
                        entityType,
                        entityIds,
                        operation,
                        DEFAULT_MAX_RETRIES
                    );
                    
                    // Update statistics
                    if (result.isSuccess()) {
                        successfulOperations.incrementAndGet();
                    } else {
                        failedOperations.incrementAndGet();
                    }
                    
                    return result;
                    
                } catch (Exception e) {
                    logger.error("Error executing bulk operation for request {}: {}", requestKey, e.getMessage(), e);
                    failedOperations.incrementAndGet();
                    
                    BulkOperationResult errorResult = new BulkOperationResult(false, 0, entityIds.size(), entityType);
                    errorResult.addError("Operation failed: " + e.getMessage());
                    return errorResult;
                }
            }
        );
        
        // Track the operation
        activeOperations.put(requestKey, future);
        
        // Clean up when operation completes
        future.whenComplete((result, throwable) -> {
            activeOperations.remove(requestKey);
            if (result != null) {
                operationResults.put(requestKey, result);
                logger.info("Bulk operation completed for request {}: {} items processed, {} successful", 
                           requestKey, result.getTotalRequested(), result.getDeletedCount());
            }
            if (throwable != null) {
                logger.error("Bulk operation failed for request {}: {}", requestKey, throwable.getMessage());
            }
        });
        
        return future;
    }
    
    @Override
    public boolean isOperationInProgress(String requestKey) {
        CompletableFuture<BulkOperationResult> operation = activeOperations.get(requestKey);
        return operation != null && !operation.isDone();
    }
    
    @Override
    public boolean cancelOperation(String requestKey) {
        CompletableFuture<BulkOperationResult> operation = activeOperations.get(requestKey);
        if (operation != null && !operation.isDone()) {
            boolean cancelled = operation.cancel(true);
            if (cancelled) {
                cancelledOperations.incrementAndGet();
                logger.info("Cancelled bulk operation for request: {}", requestKey);
            }
            return cancelled;
        }
        return false;
    }
    
    @Override
    public BulkOperationResult getOperationStatus(String requestKey) {
        // Check if operation is still active
        CompletableFuture<BulkOperationResult> activeOperation = activeOperations.get(requestKey);
        if (activeOperation != null) {
            if (activeOperation.isDone()) {
                try {
                    return activeOperation.get();
                } catch (Exception e) {
                    logger.error("Error getting operation result for {}: {}", requestKey, e.getMessage());
                    return null;
                }
            } else {
                // Operation is still in progress
                BulkOperationResult inProgress = new BulkOperationResult();
                inProgress.setOperationType("IN_PROGRESS");
                return inProgress;
            }
        }
        
        // Check completed operations
        return operationResults.get(requestKey);
    }
    
    @Override
    public Map<String, Object> getConcurrentOperationStatistics() {
        Map<String, Object> stats = Map.of(
            "totalOperations", totalOperations.get(),
            "successfulOperations", successfulOperations.get(),
            "failedOperations", failedOperations.get(),
            "cancelledOperations", cancelledOperations.get(),
            "activeOperations", activeOperations.size(),
            "cachedResults", operationResults.size(),
            "successRate", totalOperations.get() > 0 ? 
                (double) successfulOperations.get() / totalOperations.get() * 100.0 : 0.0
        );
        
        // Add deduplication and optimistic locking statistics
        Map<String, Object> allStats = new java.util.HashMap<>(stats);
        allStats.put("deduplication", deduplicationService.getDeduplicationStatistics());
        allStats.put("optimisticLocking", optimisticLockingService.getOptimisticLockingStatistics());
        
        return allStats;
    }
    
    @Override
    public String generateRequestKey(String entityType, String operationType, 
                                   List<Long> entityIds, String userId) {
        try {
            // Create a unique key based on operation parameters
            StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append(entityType).append(":");
            keyBuilder.append(operationType).append(":");
            keyBuilder.append(userId != null ? userId : "anonymous").append(":");
            
            // Sort entity IDs for consistent key generation
            entityIds.stream().sorted().forEach(id -> keyBuilder.append(id).append(","));
            
            // Hash the key to keep it manageable
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(keyBuilder.toString().getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString().substring(0, 16); // Use first 16 characters
            
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error generating request key: {}", e.getMessage());
            // Fallback to simple concatenation
            return entityType + ":" + operationType + ":" + entityIds.size() + ":" + System.currentTimeMillis();
        }
    }
    
    /**
     * Get the current user ID from security context
     */
    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            logger.debug("Could not get current user ID: {}", e.getMessage());
        }
        return "anonymous";
    }
}