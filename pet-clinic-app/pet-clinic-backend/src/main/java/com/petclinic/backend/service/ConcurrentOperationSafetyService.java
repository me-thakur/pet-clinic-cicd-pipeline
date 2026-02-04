package com.petclinic.backend.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for concurrent operation safety
 * Provides proper locking mechanisms, optimistic locking, and conflict resolution
 * 
 * Validates: Requirements 7.5
 */
public interface ConcurrentOperationSafetyService {

    /**
     * Add proper locking mechanisms for concurrent operations
     * 
     * @param entityType The entity type being locked
     * @param entityIds IDs of entities to lock
     * @param lockType Type of lock (READ, WRITE, EXCLUSIVE)
     * @param operation The operation requiring the lock
     * @return LockResult indicating if lock was acquired successfully
     */
    LockResult acquireLock(String entityType, List<Long> entityIds, LockType lockType, String operation);

    /**
     * Release locks for entities
     * 
     * @param lockId The lock ID to release
     * @return boolean indicating if lock was released successfully
     */
    boolean releaseLock(String lockId);

    /**
     * Implement optimistic locking for data updates
     * 
     * @param entityType The entity type being updated
     * @param entityId The entity ID
     * @param expectedVersion The expected version for optimistic locking
     * @param updateData The data to update
     * @return OptimisticLockResult indicating success or version conflict
     */
    OptimisticLockResult performOptimisticUpdate(String entityType, Long entityId, Long expectedVersion, Map<String, Object> updateData);

    /**
     * Add conflict resolution for concurrent modifications
     * 
     * @param entityType The entity type with conflicts
     * @param entityId The entity ID with conflicts
     * @param conflictingOperations List of conflicting operations
     * @return ConflictResolutionResult with resolution strategy
     */
    ConflictResolutionResult resolveConflicts(String entityType, Long entityId, List<ConcurrentOperation> conflictingOperations);

    /**
     * Check for concurrent operation conflicts
     * 
     * @param entityType The entity type
     * @param entityIds IDs to check for conflicts
     * @param operation The operation being attempted
     * @return ConflictDetectionResult indicating if conflicts exist
     */
    ConflictDetectionResult detectConflicts(String entityType, List<Long> entityIds, String operation);

    /**
     * Execute operation with concurrent safety guarantees
     * 
     * @param operation The operation to execute safely
     * @return CompletableFuture with the operation result
     */
    CompletableFuture<SafeOperationResult> executeSafely(SafeOperation operation);

    /**
     * Monitor concurrent operations for deadlock detection
     * 
     * @return DeadlockMonitoringResult with current deadlock status
     */
    DeadlockMonitoringResult monitorForDeadlocks();

    /**
     * Get current lock status for entities
     * 
     * @param entityType The entity type
     * @param entityIds IDs to check lock status for
     * @return LockStatusResult with current lock information
     */
    LockStatusResult getLockStatus(String entityType, List<Long> entityIds);

    /**
     * Lock types for concurrent operations
     */
    enum LockType {
        READ,           // Shared read lock
        WRITE,          // Exclusive write lock
        EXCLUSIVE,      // Exclusive lock for all operations
        OPTIMISTIC      // Optimistic locking (version-based)
    }

    /**
     * Result of lock acquisition
     */
    class LockResult {
        private final boolean acquired;
        private final String lockId;
        private final long acquisitionTime;
        private final long expirationTime;
        private final List<String> conflicts;
        private final String message;

        public LockResult(boolean acquired, String lockId, long acquisitionTime, long expirationTime, List<String> conflicts, String message) {
            this.acquired = acquired;
            this.lockId = lockId;
            this.acquisitionTime = acquisitionTime;
            this.expirationTime = expirationTime;
            this.conflicts = conflicts;
            this.message = message;
        }

        public boolean isAcquired() { return acquired; }
        public String getLockId() { return lockId; }
        public long getAcquisitionTime() { return acquisitionTime; }
        public long getExpirationTime() { return expirationTime; }
        public List<String> getConflicts() { return conflicts; }
        public String getMessage() { return message; }
    }

    /**
     * Result of optimistic locking operation
     */
    class OptimisticLockResult {
        private final boolean successful;
        private final Long currentVersion;
        private final Long expectedVersion;
        private final boolean versionConflict;
        private final Map<String, Object> conflictingData;
        private final String message;
        private final long timestamp;

        public OptimisticLockResult(boolean successful, Long currentVersion, Long expectedVersion, 
                                  boolean versionConflict, Map<String, Object> conflictingData, String message) {
            this.successful = successful;
            this.currentVersion = currentVersion;
            this.expectedVersion = expectedVersion;
            this.versionConflict = versionConflict;
            this.conflictingData = conflictingData;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isSuccessful() { return successful; }
        public Long getCurrentVersion() { return currentVersion; }
        public Long getExpectedVersion() { return expectedVersion; }
        public boolean hasVersionConflict() { return versionConflict; }
        public Map<String, Object> getConflictingData() { return conflictingData; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Result of conflict resolution
     */
    class ConflictResolutionResult {
        private final boolean resolved;
        private final ResolutionStrategy strategy;
        private final Map<String, Object> resolvedData;
        private final List<String> resolutionSteps;
        private final List<ConcurrentOperation> rejectedOperations;
        private final long timestamp;

        public ConflictResolutionResult(boolean resolved, ResolutionStrategy strategy, Map<String, Object> resolvedData, 
                                      List<String> resolutionSteps, List<ConcurrentOperation> rejectedOperations) {
            this.resolved = resolved;
            this.strategy = strategy;
            this.resolvedData = resolvedData;
            this.resolutionSteps = resolutionSteps;
            this.rejectedOperations = rejectedOperations;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isResolved() { return resolved; }
        public ResolutionStrategy getStrategy() { return strategy; }
        public Map<String, Object> getResolvedData() { return resolvedData; }
        public List<String> getResolutionSteps() { return resolutionSteps; }
        public List<ConcurrentOperation> getRejectedOperations() { return rejectedOperations; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Result of conflict detection
     */
    class ConflictDetectionResult {
        private final boolean hasConflicts;
        private final List<ConcurrentOperation> conflictingOperations;
        private final Map<String, String> conflictDetails;
        private final ConflictSeverity severity;
        private final long timestamp;

        public ConflictDetectionResult(boolean hasConflicts, List<ConcurrentOperation> conflictingOperations, 
                                     Map<String, String> conflictDetails, ConflictSeverity severity) {
            this.hasConflicts = hasConflicts;
            this.conflictingOperations = conflictingOperations;
            this.conflictDetails = conflictDetails;
            this.severity = severity;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean hasConflicts() { return hasConflicts; }
        public List<ConcurrentOperation> getConflictingOperations() { return conflictingOperations; }
        public Map<String, String> getConflictDetails() { return conflictDetails; }
        public ConflictSeverity getSeverity() { return severity; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Result of safe operation execution
     */
    class SafeOperationResult {
        private final boolean successful;
        private final Object result;
        private final List<String> warnings;
        private final List<String> errors;
        private final long executionTime;
        private final String operationId;

        public SafeOperationResult(boolean successful, Object result, List<String> warnings, 
                                 List<String> errors, long executionTime, String operationId) {
            this.successful = successful;
            this.result = result;
            this.warnings = warnings;
            this.errors = errors;
            this.executionTime = executionTime;
            this.operationId = operationId;
        }

        public boolean isSuccessful() { return successful; }
        public Object getResult() { return result; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getErrors() { return errors; }
        public long getExecutionTime() { return executionTime; }
        public String getOperationId() { return operationId; }
    }

    /**
     * Result of deadlock monitoring
     */
    class DeadlockMonitoringResult {
        private final boolean deadlockDetected;
        private final List<String> deadlockedOperations;
        private final Map<String, List<String>> lockChains;
        private final List<String> resolutionRecommendations;
        private final long timestamp;

        public DeadlockMonitoringResult(boolean deadlockDetected, List<String> deadlockedOperations, 
                                      Map<String, List<String>> lockChains, List<String> resolutionRecommendations) {
            this.deadlockDetected = deadlockDetected;
            this.deadlockedOperations = deadlockedOperations;
            this.lockChains = lockChains;
            this.resolutionRecommendations = resolutionRecommendations;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isDeadlockDetected() { return deadlockDetected; }
        public List<String> getDeadlockedOperations() { return deadlockedOperations; }
        public Map<String, List<String>> getLockChains() { return lockChains; }
        public List<String> getResolutionRecommendations() { return resolutionRecommendations; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Result of lock status check
     */
    class LockStatusResult {
        private final Map<Long, LockInfo> entityLocks;
        private final int totalActiveLocks;
        private final List<String> expiredLocks;
        private final long timestamp;

        public LockStatusResult(Map<Long, LockInfo> entityLocks, int totalActiveLocks, List<String> expiredLocks) {
            this.entityLocks = entityLocks;
            this.totalActiveLocks = totalActiveLocks;
            this.expiredLocks = expiredLocks;
            this.timestamp = System.currentTimeMillis();
        }

        public Map<Long, LockInfo> getEntityLocks() { return entityLocks; }
        public int getTotalActiveLocks() { return totalActiveLocks; }
        public List<String> getExpiredLocks() { return expiredLocks; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Information about a specific lock
     */
    class LockInfo {
        private final String lockId;
        private final LockType lockType;
        private final String operation;
        private final long acquisitionTime;
        private final long expirationTime;
        private final String ownerId;

        public LockInfo(String lockId, LockType lockType, String operation, 
                       long acquisitionTime, long expirationTime, String ownerId) {
            this.lockId = lockId;
            this.lockType = lockType;
            this.operation = operation;
            this.acquisitionTime = acquisitionTime;
            this.expirationTime = expirationTime;
            this.ownerId = ownerId;
        }

        public String getLockId() { return lockId; }
        public LockType getLockType() { return lockType; }
        public String getOperation() { return operation; }
        public long getAcquisitionTime() { return acquisitionTime; }
        public long getExpirationTime() { return expirationTime; }
        public String getOwnerId() { return ownerId; }
    }

    /**
     * Represents a concurrent operation
     */
    class ConcurrentOperation {
        private final String operationId;
        private final String entityType;
        private final Long entityId;
        private final String operationType;
        private final Map<String, Object> operationData;
        private final long timestamp;
        private final String userId;

        public ConcurrentOperation(String operationId, String entityType, Long entityId, 
                                 String operationType, Map<String, Object> operationData, String userId) {
            this.operationId = operationId;
            this.entityType = entityType;
            this.entityId = entityId;
            this.operationType = operationType;
            this.operationData = operationData;
            this.userId = userId;
            this.timestamp = System.currentTimeMillis();
        }

        public String getOperationId() { return operationId; }
        public String getEntityType() { return entityType; }
        public Long getEntityId() { return entityId; }
        public String getOperationType() { return operationType; }
        public Map<String, Object> getOperationData() { return operationData; }
        public long getTimestamp() { return timestamp; }
        public String getUserId() { return userId; }
    }

    /**
     * Interface for safe operations
     */
    interface SafeOperation {
        String getOperationId();
        String getEntityType();
        List<Long> getEntityIds();
        String getOperationType();
        Object execute() throws Exception;
        LockType getRequiredLockType();
        long getTimeoutMillis();
    }

    /**
     * Conflict resolution strategies
     */
    enum ResolutionStrategy {
        FIRST_WINS,         // First operation wins, others are rejected
        LAST_WINS,          // Last operation wins, others are rejected
        MERGE,              // Attempt to merge conflicting changes
        USER_DECISION,      // Require user intervention
        RETRY,              // Retry conflicting operations
        ABORT_ALL           // Abort all conflicting operations
    }

    /**
     * Conflict severity levels
     */
    enum ConflictSeverity {
        LOW,                // Minor conflicts that can be auto-resolved
        MEDIUM,             // Conflicts requiring attention but not critical
        HIGH,               // Critical conflicts requiring immediate resolution
        CRITICAL            // Conflicts that could cause data corruption
    }
}