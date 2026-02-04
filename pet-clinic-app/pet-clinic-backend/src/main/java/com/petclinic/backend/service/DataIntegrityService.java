package com.petclinic.backend.service;

import com.petclinic.backend.dto.*;
import com.petclinic.backend.model.*;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * Service interface for data integrity checks during table operations
 * Ensures proper relationships are maintained and transactional integrity is preserved
 * 
 * Validates: Requirements 7.2, 7.3, 7.4
 */
public interface DataIntegrityService {

    /**
     * Validate data integrity before table sorting operations
     * 
     * @param entityType The entity type being sorted
     * @param sortMetadata Sort criteria to validate
     * @param pageable Pagination parameters
     * @return IntegrityCheckResult indicating if sorting can proceed safely
     */
    IntegrityCheckResult validateSortingIntegrity(String entityType, SortMetadata sortMetadata, Pageable pageable);

    /**
     * Validate data integrity for multi-column sorting operations
     * 
     * @param entityType The entity type being sorted
     * @param multiColumnSortMetadata Multi-column sort criteria
     * @param pageable Pagination parameters
     * @return IntegrityCheckResult indicating if sorting can proceed safely
     */
    IntegrityCheckResult validateMultiColumnSortingIntegrity(String entityType, MultiColumnSortMetadata multiColumnSortMetadata, Pageable pageable);

    /**
     * Ensure proper relationships are maintained during sorting
     * 
     * @param entityType The entity type
     * @param beforeSort Data before sorting
     * @param afterSort Data after sorting
     * @return RelationshipValidationResult indicating if relationships are intact
     */
    RelationshipValidationResult validateRelationshipsAfterSorting(String entityType, List<Object> beforeSort, List<Object> afterSort);

    /**
     * Add transactional integrity for data operations
     * 
     * @param operation The operation being performed
     * @param entityType The entity type
     * @param entityIds IDs of entities being modified
     * @return TransactionIntegrityResult with transaction safety information
     */
    TransactionIntegrityResult ensureTransactionalIntegrity(String operation, String entityType, List<Long> entityIds);

    /**
     * Implement data validation during owner updates
     * 
     * @param ownerId The owner ID being updated
     * @param updateData The update data
     * @return OwnerUpdateValidationResult indicating if update is safe
     */
    OwnerUpdateValidationResult validateOwnerUpdate(Long ownerId, Map<String, Object> updateData);

    /**
     * Validate referential integrity before bulk operations
     * 
     * @param entityType The entity type
     * @param entityIds IDs of entities to be modified
     * @param operation The operation type (DELETE, UPDATE, etc.)
     * @return ReferentialIntegrityResult indicating if operation is safe
     */
    ReferentialIntegrityResult validateReferentialIntegrity(String entityType, List<Long> entityIds, String operation);

    /**
     * Check for orphaned records after operations
     * 
     * @param entityType The entity type that was modified
     * @param modifiedIds IDs that were modified
     * @return OrphanCheckResult indicating if any orphaned records exist
     */
    OrphanCheckResult checkForOrphanedRecords(String entityType, List<Long> modifiedIds);

    /**
     * Validate data consistency across related entities
     * 
     * @param entityType The primary entity type
     * @param entityId The primary entity ID
     * @return ConsistencyValidationResult indicating consistency status
     */
    ConsistencyValidationResult validateDataConsistency(String entityType, Long entityId);

    /**
     * Result of integrity checks
     */
    class IntegrityCheckResult {
        private final boolean valid;
        private final String message;
        private final List<String> issues;
        private final List<String> warnings;
        private final long timestamp;

        public IntegrityCheckResult(boolean valid, String message, List<String> issues, List<String> warnings) {
            this.valid = valid;
            this.message = message;
            this.issues = issues;
            this.warnings = warnings;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public List<String> getIssues() { return issues; }
        public List<String> getWarnings() { return warnings; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Result of relationship validation
     */
    class RelationshipValidationResult {
        private final boolean relationshipsIntact;
        private final Map<String, Integer> relationshipCounts;
        private final List<String> brokenRelationships;
        private final long timestamp;

        public RelationshipValidationResult(boolean relationshipsIntact, Map<String, Integer> relationshipCounts, List<String> brokenRelationships) {
            this.relationshipsIntact = relationshipsIntact;
            this.relationshipCounts = relationshipCounts;
            this.brokenRelationships = brokenRelationships;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean areRelationshipsIntact() { return relationshipsIntact; }
        public Map<String, Integer> getRelationshipCounts() { return relationshipCounts; }
        public List<String> getBrokenRelationships() { return brokenRelationships; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Result of transaction integrity checks
     */
    class TransactionIntegrityResult {
        private final boolean transactionSafe;
        private final String transactionId;
        private final List<String> lockingRequirements;
        private final List<String> rollbackTriggers;
        private final long timestamp;

        public TransactionIntegrityResult(boolean transactionSafe, String transactionId, List<String> lockingRequirements, List<String> rollbackTriggers) {
            this.transactionSafe = transactionSafe;
            this.transactionId = transactionId;
            this.lockingRequirements = lockingRequirements;
            this.rollbackTriggers = rollbackTriggers;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isTransactionSafe() { return transactionSafe; }
        public String getTransactionId() { return transactionId; }
        public List<String> getLockingRequirements() { return lockingRequirements; }
        public List<String> getRollbackTriggers() { return rollbackTriggers; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Result of owner update validation
     */
    class OwnerUpdateValidationResult {
        private final boolean updateSafe;
        private final List<String> validationErrors;
        private final List<Long> affectedPetIds;
        private final Map<String, Object> sanitizedData;
        private final long timestamp;

        public OwnerUpdateValidationResult(boolean updateSafe, List<String> validationErrors, List<Long> affectedPetIds, Map<String, Object> sanitizedData) {
            this.updateSafe = updateSafe;
            this.validationErrors = validationErrors;
            this.affectedPetIds = affectedPetIds;
            this.sanitizedData = sanitizedData;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isUpdateSafe() { return updateSafe; }
        public List<String> getValidationErrors() { return validationErrors; }
        public List<Long> getAffectedPetIds() { return affectedPetIds; }
        public Map<String, Object> getSanitizedData() { return sanitizedData; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Result of referential integrity checks
     */
    class ReferentialIntegrityResult {
        private final boolean referentiallyIntact;
        private final Map<String, List<Long>> dependentEntities;
        private final List<String> integrityViolations;
        private final boolean operationAllowed;
        private final long timestamp;

        public ReferentialIntegrityResult(boolean referentiallyIntact, Map<String, List<Long>> dependentEntities, List<String> integrityViolations, boolean operationAllowed) {
            this.referentiallyIntact = referentiallyIntact;
            this.dependentEntities = dependentEntities;
            this.integrityViolations = integrityViolations;
            this.operationAllowed = operationAllowed;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isReferentiallyIntact() { return referentiallyIntact; }
        public Map<String, List<Long>> getDependentEntities() { return dependentEntities; }
        public List<String> getIntegrityViolations() { return integrityViolations; }
        public boolean isOperationAllowed() { return operationAllowed; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Result of orphan record checks
     */
    class OrphanCheckResult {
        private final boolean hasOrphans;
        private final Map<String, List<Long>> orphanedRecords;
        private final List<String> cleanupRecommendations;
        private final long timestamp;

        public OrphanCheckResult(boolean hasOrphans, Map<String, List<Long>> orphanedRecords, List<String> cleanupRecommendations) {
            this.hasOrphans = hasOrphans;
            this.orphanedRecords = orphanedRecords;
            this.cleanupRecommendations = cleanupRecommendations;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean hasOrphans() { return hasOrphans; }
        public Map<String, List<Long>> getOrphanedRecords() { return orphanedRecords; }
        public List<String> getCleanupRecommendations() { return cleanupRecommendations; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Result of consistency validation
     */
    class ConsistencyValidationResult {
        private final boolean consistent;
        private final Map<String, String> consistencyChecks;
        private final List<String> inconsistencies;
        private final double consistencyScore;
        private final long timestamp;

        public ConsistencyValidationResult(boolean consistent, Map<String, String> consistencyChecks, List<String> inconsistencies, double consistencyScore) {
            this.consistent = consistent;
            this.consistencyChecks = consistencyChecks;
            this.inconsistencies = inconsistencies;
            this.consistencyScore = consistencyScore;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isConsistent() { return consistent; }
        public Map<String, String> getConsistencyChecks() { return consistencyChecks; }
        public List<String> getInconsistencies() { return inconsistencies; }
        public double getConsistencyScore() { return consistencyScore; }
        public long getTimestamp() { return timestamp; }
    }
}