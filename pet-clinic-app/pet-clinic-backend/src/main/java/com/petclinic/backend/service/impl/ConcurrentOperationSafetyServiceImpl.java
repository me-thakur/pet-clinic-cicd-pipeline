package com.petclinic.backend.service.impl;

import com.petclinic.backend.service.ConcurrentOperationSafetyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Implementation of ConcurrentOperationSafetyService
 * Provides proper locking mechanisms, optimistic locking, and conflict resolution
 * 
 * Validates: Requirements 7.5
 */
@Service
public class ConcurrentOperationSafetyServiceImpl implements ConcurrentOperationSafetyService {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentOperationSafetyServiceImpl.class);

    // Lock management
    private final Map<String, LockInfo> activeLocks = new ConcurrentHashMap<>();
    private final Map<String, ReentrantReadWriteLock> entityLocks = new ConcurrentHashMap<>();
    private final Map<String, Long> entityVersions = new ConcurrentHashMap<>();
    
    // Operation tracking
    private final Map<String, ConcurrentOperation> activeOperations = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // Deadlock detection
    private final ScheduledExecutorService deadlockMonitor = Executors.newScheduledThreadPool(1);
    
    // Lock timeout configuration
    private static final long DEFAULT_LOCK_TIMEOUT_MS = 30000; // 30 seconds
    private static final long LOCK_CLEANUP_INTERVAL_MS = 60000; // 1 minute

    public ConcurrentOperationSafetyServiceImpl() {
        // Start periodic lock cleanup
        deadlockMonitor.scheduleAtFixedRate(this::cleanupExpiredLocks, 
                                          LOCK_CLEANUP_INTERVAL_MS, 
                                          LOCK_CLEANUP_INTERVAL_MS, 
                                          TimeUnit.MILLISECONDS);
    }

    @Override
    public LockResult acquireLock(String entityType, List<Long> entityIds, LockType lockType, String operation) {
        logger.debug("Acquiring {} lock for {} entities of type '{}' for operation '{}'", 
                    lockType, entityIds.size(), entityType, operation);
        
        String lockId = generateLockId(entityType, entityIds, lockType);
        long acquisitionTime = System.currentTimeMillis();
        long expirationTime = acquisitionTime + DEFAULT_LOCK_TIMEOUT_MS;
        List<String> conflicts = new ArrayList<>();
        
        try {
            // Check for existing conflicts
            ConflictDetectionResult conflictCheck = detectConflicts(entityType, entityIds, operation);
            if (conflictCheck.hasConflicts() && conflictCheck.getSeverity() == ConflictSeverity.CRITICAL) {
                conflicts.addAll(conflictCheck.getConflictDetails().values());
                return new LockResult(false, null, acquisitionTime, 0, conflicts, "Critical conflicts detected");
            }
            
            // Attempt to acquire locks for all entities
            boolean allLocksAcquired = true;
            List<String> acquiredEntityLocks = new ArrayList<>();
            
            for (Long entityId : entityIds) {
                String entityKey = entityType + ":" + entityId;
                ReentrantReadWriteLock entityLock = entityLocks.computeIfAbsent(entityKey, k -> new ReentrantReadWriteLock());
                
                boolean lockAcquired = false;
                try {
                    switch (lockType) {
                        case READ:
                            lockAcquired = entityLock.readLock().tryLock(5, TimeUnit.SECONDS);
                            break;
                        case WRITE:
                        case EXCLUSIVE:
                            lockAcquired = entityLock.writeLock().tryLock(5, TimeUnit.SECONDS);
                            break;
                        case OPTIMISTIC:
                            // Optimistic locks don't require actual locking at this stage
                            lockAcquired = true;
                            break;
                    }
                    
                    if (lockAcquired) {
                        acquiredEntityLocks.add(entityKey);
                    } else {
                        allLocksAcquired = false;
                        conflicts.add("Failed to acquire " + lockType + " lock for entity " + entityId);
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    allLocksAcquired = false;
                    conflicts.add("Lock acquisition interrupted for entity " + entityId);
                    break;
                }
            }
            
            if (allLocksAcquired) {
                // Store lock information
                LockInfo lockInfo = new LockInfo(lockId, lockType, operation, acquisitionTime, expirationTime, getCurrentUserId());
                activeLocks.put(lockId, lockInfo);
                
                logger.debug("Successfully acquired {} lock '{}' for {} entities", lockType, lockId, entityIds.size());
                return new LockResult(true, lockId, acquisitionTime, expirationTime, conflicts, "Lock acquired successfully");
            } else {
                // Release any partially acquired locks
                releaseEntityLocks(acquiredEntityLocks, lockType);
                return new LockResult(false, null, acquisitionTime, 0, conflicts, "Failed to acquire all required locks");
            }
            
        } catch (Exception e) {
            logger.error("Error acquiring lock: {}", e.getMessage(), e);
            conflicts.add("Lock acquisition error: " + e.getMessage());
            return new LockResult(false, null, acquisitionTime, 0, conflicts, "Lock acquisition failed");
        }
    }

    @Override
    public boolean releaseLock(String lockId) {
        logger.debug("Releasing lock '{}'", lockId);
        
        try {
            LockInfo lockInfo = activeLocks.remove(lockId);
            if (lockInfo == null) {
                logger.warn("Attempted to release non-existent lock '{}'", lockId);
                return false;
            }
            
            // Parse entity information from lock ID
            String[] parts = lockId.split(":");
            if (parts.length >= 3) {
                String entityType = parts[1];
                String entityIdsStr = parts[2];
                List<Long> entityIds = Arrays.stream(entityIdsStr.split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
                
                // Release entity locks
                List<String> entityKeys = entityIds.stream()
                    .map(id -> entityType + ":" + id)
                    .collect(Collectors.toList());
                
                releaseEntityLocks(entityKeys, lockInfo.getLockType());
            }
            
            logger.debug("Successfully released lock '{}'", lockId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error releasing lock '{}': {}", lockId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public OptimisticLockResult performOptimisticUpdate(String entityType, Long entityId, Long expectedVersion, Map<String, Object> updateData) {
        logger.debug("Performing optimistic update for {} entity {} with expected version {}", 
                    entityType, entityId, expectedVersion);
        
        String entityKey = entityType + ":" + entityId;
        
        try {
            // Get current version
            Long currentVersion = entityVersions.get(entityKey);
            if (currentVersion == null) {
                currentVersion = 1L; // Initialize version if not exists
                entityVersions.put(entityKey, currentVersion);
            }
            
            // Check for version conflict
            if (!Objects.equals(currentVersion, expectedVersion)) {
                logger.warn("Version conflict detected for {} entity {}: expected {}, current {}", 
                           entityType, entityId, expectedVersion, currentVersion);
                
                Map<String, Object> conflictingData = getCurrentEntityData(entityType, entityId);
                return new OptimisticLockResult(false, currentVersion, expectedVersion, true, 
                                              conflictingData, "Version conflict detected");
            }
            
            // Perform the update
            boolean updateSuccessful = performEntityUpdate(entityType, entityId, updateData);
            
            if (updateSuccessful) {
                // Increment version
                Long newVersion = currentVersion + 1;
                entityVersions.put(entityKey, newVersion);
                
                logger.debug("Optimistic update successful for {} entity {}, new version: {}", 
                           entityType, entityId, newVersion);
                
                return new OptimisticLockResult(true, newVersion, expectedVersion, false, 
                                              null, "Update successful");
            } else {
                return new OptimisticLockResult(false, currentVersion, expectedVersion, false, 
                                              null, "Update failed");
            }
            
        } catch (Exception e) {
            logger.error("Error performing optimistic update: {}", e.getMessage(), e);
            return new OptimisticLockResult(false, null, expectedVersion, false, 
                                          null, "Update error: " + e.getMessage());
        }
    }

    @Override
    public ConflictResolutionResult resolveConflicts(String entityType, Long entityId, List<ConcurrentOperation> conflictingOperations) {
        logger.debug("Resolving conflicts for {} entity {} with {} conflicting operations", 
                    entityType, entityId, conflictingOperations.size());
        
        List<String> resolutionSteps = new ArrayList<>();
        List<ConcurrentOperation> rejectedOperations = new ArrayList<>();
        
        try {
            // Determine resolution strategy based on operation types and timing
            ResolutionStrategy strategy = determineResolutionStrategy(conflictingOperations);
            resolutionSteps.add("Selected resolution strategy: " + strategy);
            
            Map<String, Object> resolvedData = new HashMap<>();
            boolean resolved = false;
            
            switch (strategy) {
                case FIRST_WINS:
                    resolved = resolveFirstWins(conflictingOperations, resolvedData, rejectedOperations, resolutionSteps);
                    break;
                case LAST_WINS:
                    resolved = resolveLastWins(conflictingOperations, resolvedData, rejectedOperations, resolutionSteps);
                    break;
                case MERGE:
                    resolved = resolveMerge(conflictingOperations, resolvedData, rejectedOperations, resolutionSteps);
                    break;
                case RETRY:
                    resolved = resolveRetry(conflictingOperations, resolvedData, rejectedOperations, resolutionSteps);
                    break;
                case USER_DECISION:
                    resolutionSteps.add("Conflict requires user intervention");
                    resolved = false;
                    break;
                case ABORT_ALL:
                    rejectedOperations.addAll(conflictingOperations);
                    resolutionSteps.add("All conflicting operations aborted");
                    resolved = true;
                    break;
            }
            
            logger.debug("Conflict resolution completed. Resolved: {}, Strategy: {}, Rejected operations: {}", 
                        resolved, strategy, rejectedOperations.size());
            
            return new ConflictResolutionResult(resolved, strategy, resolvedData, resolutionSteps, rejectedOperations);
            
        } catch (Exception e) {
            logger.error("Error resolving conflicts: {}", e.getMessage(), e);
            resolutionSteps.add("Conflict resolution error: " + e.getMessage());
            return new ConflictResolutionResult(false, ResolutionStrategy.ABORT_ALL, 
                                              new HashMap<>(), resolutionSteps, conflictingOperations);
        }
    }

    @Override
    public ConflictDetectionResult detectConflicts(String entityType, List<Long> entityIds, String operation) {
        logger.debug("Detecting conflicts for {} operation on {} entities of type '{}'", 
                    operation, entityIds.size(), entityType);
        
        List<ConcurrentOperation> conflictingOperations = new ArrayList<>();
        Map<String, String> conflictDetails = new HashMap<>();
        
        try {
            // Check for conflicting operations on the same entities
            for (Long entityId : entityIds) {
                String entityKey = entityType + ":" + entityId;
                
                // Check active locks
                for (LockInfo lockInfo : activeLocks.values()) {
                    if (isConflictingLock(lockInfo, entityType, entityId, operation)) {
                        conflictDetails.put("lock_conflict_" + entityId, 
                                          "Entity " + entityId + " is locked by operation: " + lockInfo.getOperation());
                    }
                }
                
                // Check active operations
                for (ConcurrentOperation activeOp : activeOperations.values()) {
                    if (isConflictingOperation(activeOp, entityType, entityId, operation)) {
                        conflictingOperations.add(activeOp);
                        conflictDetails.put("operation_conflict_" + entityId, 
                                          "Conflicting operation: " + activeOp.getOperationType());
                    }
                }
            }
            
            // Determine conflict severity
            ConflictSeverity severity = determineConflictSeverity(conflictingOperations, operation);
            
            boolean hasConflicts = !conflictingOperations.isEmpty() || !conflictDetails.isEmpty();
            
            logger.debug("Conflict detection completed. Has conflicts: {}, Severity: {}, Conflicting operations: {}", 
                        hasConflicts, severity, conflictingOperations.size());
            
            return new ConflictDetectionResult(hasConflicts, conflictingOperations, conflictDetails, severity);
            
        } catch (Exception e) {
            logger.error("Error detecting conflicts: {}", e.getMessage(), e);
            conflictDetails.put("detection_error", "Conflict detection error: " + e.getMessage());
            return new ConflictDetectionResult(true, conflictingOperations, conflictDetails, ConflictSeverity.CRITICAL);
        }
    }

    @Override
    public CompletableFuture<SafeOperationResult> executeSafely(SafeOperation operation) {
        logger.debug("Executing operation '{}' safely", operation.getOperationId());
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            List<String> warnings = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            String lockId = null;
            
            try {
                // Acquire required locks
                LockResult lockResult = acquireLock(operation.getEntityType(), operation.getEntityIds(), 
                                                  operation.getRequiredLockType(), operation.getOperationType());
                
                if (!lockResult.isAcquired()) {
                    errors.add("Failed to acquire required locks: " + lockResult.getMessage());
                    return new SafeOperationResult(false, null, warnings, errors, 
                                                 System.currentTimeMillis() - startTime, operation.getOperationId());
                }
                
                lockId = lockResult.getLockId();
                warnings.addAll(lockResult.getConflicts());
                
                // Execute the operation with timeout
                Object result = executeWithTimeout(operation, operation.getTimeoutMillis());
                
                long executionTime = System.currentTimeMillis() - startTime;
                logger.debug("Safe operation '{}' completed successfully in {}ms", 
                           operation.getOperationId(), executionTime);
                
                return new SafeOperationResult(true, result, warnings, errors, executionTime, operation.getOperationId());
                
            } catch (TimeoutException e) {
                errors.add("Operation timed out after " + operation.getTimeoutMillis() + "ms");
                return new SafeOperationResult(false, null, warnings, errors, 
                                             System.currentTimeMillis() - startTime, operation.getOperationId());
            } catch (Exception e) {
                logger.error("Error executing safe operation '{}': {}", operation.getOperationId(), e.getMessage(), e);
                errors.add("Operation execution error: " + e.getMessage());
                return new SafeOperationResult(false, null, warnings, errors, 
                                             System.currentTimeMillis() - startTime, operation.getOperationId());
            } finally {
                // Always release locks
                if (lockId != null) {
                    releaseLock(lockId);
                }
            }
        }, executorService);
    }

    @Override
    public DeadlockMonitoringResult monitorForDeadlocks() {
        logger.debug("Monitoring for deadlocks");
        
        List<String> deadlockedOperations = new ArrayList<>();
        Map<String, List<String>> lockChains = new HashMap<>();
        List<String> resolutionRecommendations = new ArrayList<>();
        
        try {
            // Simple deadlock detection based on circular wait conditions
            Map<String, Set<String>> waitGraph = buildWaitGraph();
            
            // Detect cycles in the wait graph
            Set<String> visited = new HashSet<>();
            Set<String> recursionStack = new HashSet<>();
            
            for (String node : waitGraph.keySet()) {
                if (detectCycle(node, waitGraph, visited, recursionStack, new ArrayList<>())) {
                    deadlockedOperations.add(node);
                }
            }
            
            // Build lock chains for analysis
            for (String operation : deadlockedOperations) {
                List<String> chain = buildLockChain(operation, waitGraph);
                lockChains.put(operation, chain);
            }
            
            // Generate resolution recommendations
            if (!deadlockedOperations.isEmpty()) {
                resolutionRecommendations.add("Deadlock detected involving " + deadlockedOperations.size() + " operations");
                resolutionRecommendations.add("Consider implementing lock ordering to prevent deadlocks");
                resolutionRecommendations.add("Abort oldest operation to break deadlock cycle");
            }
            
            boolean deadlockDetected = !deadlockedOperations.isEmpty();
            
            logger.debug("Deadlock monitoring completed. Deadlock detected: {}, Affected operations: {}", 
                        deadlockDetected, deadlockedOperations.size());
            
            return new DeadlockMonitoringResult(deadlockDetected, deadlockedOperations, lockChains, resolutionRecommendations);
            
        } catch (Exception e) {
            logger.error("Error monitoring for deadlocks: {}", e.getMessage(), e);
            resolutionRecommendations.add("Deadlock monitoring error: " + e.getMessage());
            return new DeadlockMonitoringResult(false, deadlockedOperations, lockChains, resolutionRecommendations);
        }
    }

    @Override
    public LockStatusResult getLockStatus(String entityType, List<Long> entityIds) {
        logger.debug("Getting lock status for {} entities of type '{}'", entityIds.size(), entityType);
        
        Map<Long, LockInfo> entityLocks = new HashMap<>();
        List<String> expiredLocks = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        
        try {
            for (Long entityId : entityIds) {
                String entityKey = entityType + ":" + entityId;
                
                // Find locks for this entity
                for (Map.Entry<String, LockInfo> entry : activeLocks.entrySet()) {
                    String lockId = entry.getKey();
                    LockInfo lockInfo = entry.getValue();
                    
                    if (lockId.contains(entityKey)) {
                        if (lockInfo.getExpirationTime() < currentTime) {
                            expiredLocks.add(lockId);
                        } else {
                            entityLocks.put(entityId, lockInfo);
                        }
                    }
                }
            }
            
            int totalActiveLocks = activeLocks.size();
            
            logger.debug("Lock status retrieved. Entity locks: {}, Total active: {}, Expired: {}", 
                        entityLocks.size(), totalActiveLocks, expiredLocks.size());
            
            return new LockStatusResult(entityLocks, totalActiveLocks, expiredLocks);
            
        } catch (Exception e) {
            logger.error("Error getting lock status: {}", e.getMessage(), e);
            return new LockStatusResult(entityLocks, 0, expiredLocks);
        }
    }

    // Private helper methods

    private String generateLockId(String entityType, List<Long> entityIds, LockType lockType) {
        String entityIdsStr = entityIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        return lockType + ":" + entityType + ":" + entityIdsStr + ":" + System.currentTimeMillis();
    }

    private String getCurrentUserId() {
        // In a real system, this would get the current user from security context
        return "system";
    }

    private void releaseEntityLocks(List<String> entityKeys, LockType lockType) {
        for (String entityKey : entityKeys) {
            ReentrantReadWriteLock entityLock = entityLocks.get(entityKey);
            if (entityLock != null) {
                try {
                    switch (lockType) {
                        case READ:
                            entityLock.readLock().unlock();
                            break;
                        case WRITE:
                        case EXCLUSIVE:
                            entityLock.writeLock().unlock();
                            break;
                        case OPTIMISTIC:
                            // No actual lock to release for optimistic locking
                            break;
                    }
                } catch (Exception e) {
                    logger.warn("Error releasing lock for entity '{}': {}", entityKey, e.getMessage());
                }
            }
        }
    }

    private void cleanupExpiredLocks() {
        long currentTime = System.currentTimeMillis();
        List<String> expiredLockIds = new ArrayList<>();
        
        for (Map.Entry<String, LockInfo> entry : activeLocks.entrySet()) {
            if (entry.getValue().getExpirationTime() < currentTime) {
                expiredLockIds.add(entry.getKey());
            }
        }
        
        for (String lockId : expiredLockIds) {
            logger.debug("Cleaning up expired lock: {}", lockId);
            releaseLock(lockId);
        }
        
        if (!expiredLockIds.isEmpty()) {
            logger.info("Cleaned up {} expired locks", expiredLockIds.size());
        }
    }

    private Map<String, Object> getCurrentEntityData(String entityType, Long entityId) {
        // Simplified - in real system would fetch actual entity data
        Map<String, Object> data = new HashMap<>();
        data.put("entityType", entityType);
        data.put("entityId", entityId);
        data.put("lastModified", System.currentTimeMillis());
        return data;
    }

    private boolean performEntityUpdate(String entityType, Long entityId, Map<String, Object> updateData) {
        // Simplified - in real system would perform actual entity update
        logger.debug("Performing update for {} entity {} with data: {}", entityType, entityId, updateData.keySet());
        return true;
    }

    private ResolutionStrategy determineResolutionStrategy(List<ConcurrentOperation> conflictingOperations) {
        // Simplified strategy determination
        if (conflictingOperations.size() <= 2) {
            return ResolutionStrategy.LAST_WINS;
        } else if (conflictingOperations.stream().anyMatch(op -> "DELETE".equals(op.getOperationType()))) {
            return ResolutionStrategy.USER_DECISION;
        } else {
            return ResolutionStrategy.MERGE;
        }
    }

    private boolean resolveFirstWins(List<ConcurrentOperation> operations, Map<String, Object> resolvedData, 
                                   List<ConcurrentOperation> rejectedOperations, List<String> resolutionSteps) {
        if (operations.isEmpty()) return false;
        
        ConcurrentOperation firstOperation = operations.stream()
            .min(Comparator.comparing(ConcurrentOperation::getTimestamp))
            .orElse(null);
        
        if (firstOperation != null) {
            resolvedData.putAll(firstOperation.getOperationData());
            operations.stream()
                .filter(op -> !op.equals(firstOperation))
                .forEach(rejectedOperations::add);
            resolutionSteps.add("Applied first operation: " + firstOperation.getOperationId());
            return true;
        }
        
        return false;
    }

    private boolean resolveLastWins(List<ConcurrentOperation> operations, Map<String, Object> resolvedData, 
                                  List<ConcurrentOperation> rejectedOperations, List<String> resolutionSteps) {
        if (operations.isEmpty()) return false;
        
        ConcurrentOperation lastOperation = operations.stream()
            .max(Comparator.comparing(ConcurrentOperation::getTimestamp))
            .orElse(null);
        
        if (lastOperation != null) {
            resolvedData.putAll(lastOperation.getOperationData());
            operations.stream()
                .filter(op -> !op.equals(lastOperation))
                .forEach(rejectedOperations::add);
            resolutionSteps.add("Applied last operation: " + lastOperation.getOperationId());
            return true;
        }
        
        return false;
    }

    private boolean resolveMerge(List<ConcurrentOperation> operations, Map<String, Object> resolvedData, 
                               List<ConcurrentOperation> rejectedOperations, List<String> resolutionSteps) {
        // Simplified merge - in real system would have sophisticated merge logic
        for (ConcurrentOperation operation : operations) {
            resolvedData.putAll(operation.getOperationData());
        }
        resolutionSteps.add("Merged " + operations.size() + " operations");
        return true;
    }

    private boolean resolveRetry(List<ConcurrentOperation> operations, Map<String, Object> resolvedData, 
                               List<ConcurrentOperation> rejectedOperations, List<String> resolutionSteps) {
        // Mark all operations for retry
        rejectedOperations.addAll(operations);
        resolutionSteps.add("Marked " + operations.size() + " operations for retry");
        return true;
    }

    private boolean isConflictingLock(LockInfo lockInfo, String entityType, Long entityId, String operation) {
        // Simplified conflict detection
        return lockInfo.getLockType() == LockType.EXCLUSIVE || 
               (lockInfo.getLockType() == LockType.WRITE && "WRITE".equals(operation));
    }

    private boolean isConflictingOperation(ConcurrentOperation activeOp, String entityType, Long entityId, String operation) {
        return activeOp.getEntityType().equals(entityType) && 
               activeOp.getEntityId().equals(entityId) &&
               isConflictingOperationType(activeOp.getOperationType(), operation);
    }

    private boolean isConflictingOperationType(String activeOpType, String newOpType) {
        // Simplified conflict rules
        return ("DELETE".equals(activeOpType) || "DELETE".equals(newOpType)) ||
               ("UPDATE".equals(activeOpType) && "UPDATE".equals(newOpType));
    }

    private ConflictSeverity determineConflictSeverity(List<ConcurrentOperation> conflictingOperations, String operation) {
        if (conflictingOperations.isEmpty()) {
            return ConflictSeverity.LOW;
        }
        
        boolean hasDeleteOperation = conflictingOperations.stream()
            .anyMatch(op -> "DELETE".equals(op.getOperationType())) || "DELETE".equals(operation);
        
        if (hasDeleteOperation) {
            return ConflictSeverity.CRITICAL;
        } else if (conflictingOperations.size() > 3) {
            return ConflictSeverity.HIGH;
        } else {
            return ConflictSeverity.MEDIUM;
        }
    }

    private Object executeWithTimeout(SafeOperation operation, long timeoutMillis) throws Exception {
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
            try {
                return operation.execute();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService);
        
        return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    private Map<String, Set<String>> buildWaitGraph() {
        // Simplified wait graph construction
        Map<String, Set<String>> waitGraph = new HashMap<>();
        
        for (LockInfo lockInfo : activeLocks.values()) {
            waitGraph.computeIfAbsent(lockInfo.getOwnerId(), k -> new HashSet<>());
        }
        
        return waitGraph;
    }

    private boolean detectCycle(String node, Map<String, Set<String>> graph, Set<String> visited, 
                              Set<String> recursionStack, List<String> path) {
        if (recursionStack.contains(node)) {
            return true; // Cycle detected
        }
        
        if (visited.contains(node)) {
            return false;
        }
        
        visited.add(node);
        recursionStack.add(node);
        path.add(node);
        
        Set<String> neighbors = graph.get(node);
        if (neighbors != null) {
            for (String neighbor : neighbors) {
                if (detectCycle(neighbor, graph, visited, recursionStack, path)) {
                    return true;
                }
            }
        }
        
        recursionStack.remove(node);
        path.remove(path.size() - 1);
        return false;
    }

    private List<String> buildLockChain(String operation, Map<String, Set<String>> waitGraph) {
        List<String> chain = new ArrayList<>();
        chain.add(operation);
        
        Set<String> dependencies = waitGraph.get(operation);
        if (dependencies != null && !dependencies.isEmpty()) {
            chain.addAll(dependencies);
        }
        
        return chain;
    }
}