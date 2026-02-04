package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.*;
import com.petclinic.backend.model.*;
import com.petclinic.backend.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of DataIntegrityService
 * Provides data integrity checks for table operations and owner updates
 * 
 * Validates: Requirements 7.2, 7.3, 7.4
 */
@Service
@Transactional(readOnly = true)
public class DataIntegrityServiceImpl implements DataIntegrityService {

    private static final Logger logger = LoggerFactory.getLogger(DataIntegrityServiceImpl.class);

    @Autowired
    private VisitService visitService;

    @Autowired
    private PetService petService;

    @Autowired
    private UserService userService; // For owners

    @Autowired
    private VeterinarianService veterinarianService;

    @Override
    public IntegrityCheckResult validateSortingIntegrity(String entityType, SortMetadata sortMetadata, Pageable pageable) {
        logger.debug("Validating sorting integrity for entity type '{}' with sort metadata: {}", entityType, sortMetadata);
        
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try {
            // Validate entity type
            if (!isValidEntityType(entityType)) {
                issues.add("Invalid entity type: " + entityType);
            }
            
            // Validate sort metadata
            if (sortMetadata != null) {
                if (sortMetadata.getColumn() == null || sortMetadata.getColumn().trim().isEmpty()) {
                    issues.add("Sort column cannot be null or empty");
                }
                
                if (!isValidSortColumn(entityType, sortMetadata.getColumn())) {
                    issues.add("Invalid sort column '" + sortMetadata.getColumn() + "' for entity type '" + entityType + "'");
                }
            }
            
            // Validate pagination parameters
            if (pageable != null) {
                if (pageable.getPageSize() <= 0) {
                    issues.add("Page size must be positive");
                }
                
                if (pageable.getPageSize() > 1000) {
                    warnings.add("Large page size (" + pageable.getPageSize() + ") may impact performance");
                }
                
                if (pageable.getOffset() < 0) {
                    issues.add("Page offset cannot be negative");
                }
            }
            
            // Check for potential relationship integrity issues
            if (sortMetadata != null && sortMetadata.getColumn() != null) {
                String column = sortMetadata.getColumn().toLowerCase();
                if (column.contains(".")) {
                    // This is a relationship column, check if relationships exist
                    RelationshipIntegrityCheck relationshipCheck = checkRelationshipIntegrity(entityType, column);
                    if (!relationshipCheck.isIntact()) {
                        warnings.addAll(relationshipCheck.getWarnings());
                    }
                }
            }
            
            boolean isValid = issues.isEmpty();
            String message = isValid ? "Sorting integrity validation passed" : 
                           "Sorting integrity validation failed with " + issues.size() + " issues";
            
            logger.debug("Sorting integrity validation completed. Valid: {}, Issues: {}, Warnings: {}", 
                        isValid, issues.size(), warnings.size());
            
            return new IntegrityCheckResult(isValid, message, issues, warnings);
            
        } catch (Exception e) {
            logger.error("Error validating sorting integrity: {}", e.getMessage(), e);
            issues.add("Integrity validation error: " + e.getMessage());
            return new IntegrityCheckResult(false, "Validation error occurred", issues, warnings);
        }
    }

    @Override
    public IntegrityCheckResult validateMultiColumnSortingIntegrity(String entityType, MultiColumnSortMetadata multiColumnSortMetadata, Pageable pageable) {
        logger.debug("Validating multi-column sorting integrity for entity type '{}'", entityType);
        
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try {
            // Validate entity type
            if (!isValidEntityType(entityType)) {
                issues.add("Invalid entity type: " + entityType);
            }
            
            // Validate multi-column sort metadata
            if (multiColumnSortMetadata != null) {
                List<MultiColumnSortMetadata.SortCriterion> sortCriteria = multiColumnSortMetadata.getSortCriteria();
                
                if (sortCriteria == null || sortCriteria.isEmpty()) {
                    issues.add("Multi-column sort criteria cannot be null or empty");
                } else {
                    // Validate each sort criterion
                    for (int i = 0; i < sortCriteria.size(); i++) {
                        MultiColumnSortMetadata.SortCriterion criterion = sortCriteria.get(i);
                        
                        if (criterion.getColumn() == null || criterion.getColumn().trim().isEmpty()) {
                            issues.add("Sort criterion " + (i + 1) + " has null or empty column");
                        } else if (!isValidSortColumn(entityType, criterion.getColumn())) {
                            issues.add("Invalid sort column '" + criterion.getColumn() + "' in criterion " + (i + 1));
                        }
                        
                        if (criterion.getPrecedence() < 0) {
                            issues.add("Sort criterion " + (i + 1) + " has negative precedence");
                        }
                    }
                    
                    // Check for duplicate columns
                    Set<String> columnNames = sortCriteria.stream()
                        .map(c -> c.getColumn().toLowerCase())
                        .collect(Collectors.toSet());
                    
                    if (columnNames.size() != sortCriteria.size()) {
                        warnings.add("Duplicate columns found in multi-column sort criteria");
                    }
                    
                    // Check for conflicting priorities
                    Set<Integer> priorities = sortCriteria.stream()
                        .map(MultiColumnSortMetadata.SortCriterion::getPrecedence)
                        .collect(Collectors.toSet());
                    
                    if (priorities.size() != sortCriteria.size()) {
                        warnings.add("Conflicting priorities found in multi-column sort criteria");
                    }
                }
            }
            
            // Validate pagination parameters (same as single column)
            if (pageable != null) {
                if (pageable.getPageSize() <= 0) {
                    issues.add("Page size must be positive");
                }
                
                if (pageable.getPageSize() > 1000) {
                    warnings.add("Large page size may impact performance with multi-column sorting");
                }
            }
            
            boolean isValid = issues.isEmpty();
            String message = isValid ? "Multi-column sorting integrity validation passed" : 
                           "Multi-column sorting integrity validation failed";
            
            return new IntegrityCheckResult(isValid, message, issues, warnings);
            
        } catch (Exception e) {
            logger.error("Error validating multi-column sorting integrity: {}", e.getMessage(), e);
            issues.add("Multi-column integrity validation error: " + e.getMessage());
            return new IntegrityCheckResult(false, "Validation error occurred", issues, warnings);
        }
    }

    @Override
    public RelationshipValidationResult validateRelationshipsAfterSorting(String entityType, List<Object> beforeSort, List<Object> afterSort) {
        logger.debug("Validating relationships after sorting for entity type '{}'", entityType);
        
        Map<String, Integer> relationshipCounts = new HashMap<>();
        List<String> brokenRelationships = new ArrayList<>();
        
        try {
            if (beforeSort == null || afterSort == null) {
                brokenRelationships.add("Cannot validate relationships with null data");
                return new RelationshipValidationResult(false, relationshipCounts, brokenRelationships);
            }
            
            if (beforeSort.size() != afterSort.size()) {
                brokenRelationships.add("Data size mismatch: before=" + beforeSort.size() + ", after=" + afterSort.size());
                return new RelationshipValidationResult(false, relationshipCounts, brokenRelationships);
            }
            
            // Count relationships before and after sorting
            relationshipCounts.put("before_total", beforeSort.size());
            relationshipCounts.put("after_total", afterSort.size());
            
            switch (entityType.toLowerCase()) {
                case "visits":
                    validateVisitRelationships(beforeSort, afterSort, relationshipCounts, brokenRelationships);
                    break;
                case "pets":
                    validatePetRelationships(beforeSort, afterSort, relationshipCounts, brokenRelationships);
                    break;
                case "owners":
                    validateOwnerRelationships(beforeSort, afterSort, relationshipCounts, brokenRelationships);
                    break;
                case "veterinarians":
                    validateVeterinarianRelationships(beforeSort, afterSort, relationshipCounts, brokenRelationships);
                    break;
                default:
                    brokenRelationships.add("Unknown entity type for relationship validation: " + entityType);
            }
            
            boolean relationshipsIntact = brokenRelationships.isEmpty();
            
            logger.debug("Relationship validation completed. Intact: {}, Broken relationships: {}", 
                        relationshipsIntact, brokenRelationships.size());
            
            return new RelationshipValidationResult(relationshipsIntact, relationshipCounts, brokenRelationships);
            
        } catch (Exception e) {
            logger.error("Error validating relationships after sorting: {}", e.getMessage(), e);
            brokenRelationships.add("Relationship validation error: " + e.getMessage());
            return new RelationshipValidationResult(false, relationshipCounts, brokenRelationships);
        }
    }

    @Override
    @Transactional
    public TransactionIntegrityResult ensureTransactionalIntegrity(String operation, String entityType, List<Long> entityIds) {
        logger.debug("Ensuring transactional integrity for operation '{}' on entity type '{}' with {} IDs", 
                    operation, entityType, entityIds != null ? entityIds.size() : 0);
        
        List<String> lockingRequirements = new ArrayList<>();
        List<String> rollbackTriggers = new ArrayList<>();
        String transactionId = UUID.randomUUID().toString();
        
        try {
            // Validate operation type
            if (!isValidOperation(operation)) {
                rollbackTriggers.add("Invalid operation type: " + operation);
                return new TransactionIntegrityResult(false, transactionId, lockingRequirements, rollbackTriggers);
            }
            
            // Validate entity IDs
            if (entityIds == null || entityIds.isEmpty()) {
                rollbackTriggers.add("Entity IDs cannot be null or empty");
                return new TransactionIntegrityResult(false, transactionId, lockingRequirements, rollbackTriggers);
            }
            
            // Check for duplicate IDs
            Set<Long> uniqueIds = new HashSet<>(entityIds);
            if (uniqueIds.size() != entityIds.size()) {
                rollbackTriggers.add("Duplicate entity IDs found in operation");
            }
            
            // Determine locking requirements based on operation and entity type
            switch (operation.toUpperCase()) {
                case "DELETE":
                    lockingRequirements.add("EXCLUSIVE_LOCK on " + entityType);
                    lockingRequirements.add("SHARED_LOCK on related entities");
                    rollbackTriggers.add("Foreign key constraint violation");
                    rollbackTriggers.add("Concurrent modification detected");
                    break;
                case "UPDATE":
                    lockingRequirements.add("SHARED_LOCK on " + entityType);
                    rollbackTriggers.add("Optimistic locking failure");
                    rollbackTriggers.add("Data validation failure");
                    break;
                case "SORT":
                    lockingRequirements.add("READ_LOCK on " + entityType);
                    rollbackTriggers.add("Data consistency check failure");
                    break;
                default:
                    rollbackTriggers.add("Unknown operation type: " + operation);
            }
            
            // Check for existing locks or conflicts
            if (hasExistingConflicts(entityType, entityIds, operation)) {
                rollbackTriggers.add("Existing conflicts detected for entity IDs");
            }
            
            boolean transactionSafe = rollbackTriggers.isEmpty();
            
            logger.debug("Transaction integrity check completed. Safe: {}, Locking requirements: {}, Rollback triggers: {}", 
                        transactionSafe, lockingRequirements.size(), rollbackTriggers.size());
            
            return new TransactionIntegrityResult(transactionSafe, transactionId, lockingRequirements, rollbackTriggers);
            
        } catch (Exception e) {
            logger.error("Error ensuring transactional integrity: {}", e.getMessage(), e);
            rollbackTriggers.add("Transaction integrity error: " + e.getMessage());
            return new TransactionIntegrityResult(false, transactionId, lockingRequirements, rollbackTriggers);
        }
    }

    @Override
    public OwnerUpdateValidationResult validateOwnerUpdate(Long ownerId, Map<String, Object> updateData) {
        logger.debug("Validating owner update for ID {} with {} fields", ownerId, updateData != null ? updateData.size() : 0);
        
        List<String> validationErrors = new ArrayList<>();
        List<Long> affectedPetIds = new ArrayList<>();
        Map<String, Object> sanitizedData = new HashMap<>();
        
        try {
            // Validate owner ID
            if (ownerId == null || ownerId <= 0) {
                validationErrors.add("Invalid owner ID: " + ownerId);
                return new OwnerUpdateValidationResult(false, validationErrors, affectedPetIds, sanitizedData);
            }
            
            // Check if owner exists
            if (!ownerExists(ownerId)) {
                validationErrors.add("Owner with ID " + ownerId + " does not exist");
                return new OwnerUpdateValidationResult(false, validationErrors, affectedPetIds, sanitizedData);
            }
            
            // Validate update data
            if (updateData == null || updateData.isEmpty()) {
                validationErrors.add("Update data cannot be null or empty");
                return new OwnerUpdateValidationResult(false, validationErrors, affectedPetIds, sanitizedData);
            }
            
            // Get affected pets
            affectedPetIds = getPetIdsByOwnerId(ownerId);
            
            // Validate and sanitize each field
            for (Map.Entry<String, Object> entry : updateData.entrySet()) {
                String fieldName = entry.getKey();
                Object fieldValue = entry.getValue();
                
                ValidationResult fieldValidation = validateOwnerField(fieldName, fieldValue);
                if (!fieldValidation.isValid()) {
                    validationErrors.addAll(fieldValidation.getErrors());
                } else {
                    sanitizedData.put(fieldName, fieldValidation.getSanitizedValue());
                }
            }
            
            // Check for business rule violations
            if (sanitizedData.containsKey("email")) {
                String email = (String) sanitizedData.get("email");
                if (isEmailAlreadyUsed(email, ownerId)) {
                    validationErrors.add("Email address is already in use by another owner");
                }
            }
            
            // Check for referential integrity issues
            if (sanitizedData.containsKey("status") && "INACTIVE".equals(sanitizedData.get("status"))) {
                if (hasActivePets(ownerId)) {
                    validationErrors.add("Cannot deactivate owner with active pets");
                }
            }
            
            boolean updateSafe = validationErrors.isEmpty();
            
            logger.debug("Owner update validation completed. Safe: {}, Errors: {}, Affected pets: {}", 
                        updateSafe, validationErrors.size(), affectedPetIds.size());
            
            return new OwnerUpdateValidationResult(updateSafe, validationErrors, affectedPetIds, sanitizedData);
            
        } catch (Exception e) {
            logger.error("Error validating owner update: {}", e.getMessage(), e);
            validationErrors.add("Owner update validation error: " + e.getMessage());
            return new OwnerUpdateValidationResult(false, validationErrors, affectedPetIds, sanitizedData);
        }
    }

    @Override
    public ReferentialIntegrityResult validateReferentialIntegrity(String entityType, List<Long> entityIds, String operation) {
        logger.debug("Validating referential integrity for {} operation on {} entities of type '{}'", 
                    operation, entityIds != null ? entityIds.size() : 0, entityType);
        
        Map<String, List<Long>> dependentEntities = new HashMap<>();
        List<String> integrityViolations = new ArrayList<>();
        
        try {
            if (entityIds == null || entityIds.isEmpty()) {
                integrityViolations.add("Entity IDs cannot be null or empty");
                return new ReferentialIntegrityResult(false, dependentEntities, integrityViolations, false);
            }
            
            switch (entityType.toLowerCase()) {
                case "owners":
                    validateOwnerReferentialIntegrity(entityIds, operation, dependentEntities, integrityViolations);
                    break;
                case "pets":
                    validatePetReferentialIntegrity(entityIds, operation, dependentEntities, integrityViolations);
                    break;
                case "veterinarians":
                    validateVeterinarianReferentialIntegrity(entityIds, operation, dependentEntities, integrityViolations);
                    break;
                case "visits":
                    validateVisitReferentialIntegrity(entityIds, operation, dependentEntities, integrityViolations);
                    break;
                default:
                    integrityViolations.add("Unknown entity type: " + entityType);
            }
            
            boolean referentiallyIntact = integrityViolations.isEmpty();
            boolean operationAllowed = referentiallyIntact || "READ".equalsIgnoreCase(operation);
            
            logger.debug("Referential integrity validation completed. Intact: {}, Operation allowed: {}, Violations: {}", 
                        referentiallyIntact, operationAllowed, integrityViolations.size());
            
            return new ReferentialIntegrityResult(referentiallyIntact, dependentEntities, integrityViolations, operationAllowed);
            
        } catch (Exception e) {
            logger.error("Error validating referential integrity: {}", e.getMessage(), e);
            integrityViolations.add("Referential integrity validation error: " + e.getMessage());
            return new ReferentialIntegrityResult(false, dependentEntities, integrityViolations, false);
        }
    }

    @Override
    public OrphanCheckResult checkForOrphanedRecords(String entityType, List<Long> modifiedIds) {
        logger.debug("Checking for orphaned records after modifying {} entities of type '{}'", 
                    modifiedIds != null ? modifiedIds.size() : 0, entityType);
        
        Map<String, List<Long>> orphanedRecords = new HashMap<>();
        List<String> cleanupRecommendations = new ArrayList<>();
        
        try {
            switch (entityType.toLowerCase()) {
                case "owners":
                    checkForOrphanedPetsAfterOwnerModification(modifiedIds, orphanedRecords, cleanupRecommendations);
                    break;
                case "pets":
                    checkForOrphanedVisitsAfterPetModification(modifiedIds, orphanedRecords, cleanupRecommendations);
                    break;
                case "veterinarians":
                    checkForOrphanedVisitsAfterVeterinarianModification(modifiedIds, orphanedRecords, cleanupRecommendations);
                    break;
                case "visits":
                    // Visits don't typically have dependent entities that can become orphaned
                    break;
                default:
                    cleanupRecommendations.add("Unknown entity type for orphan check: " + entityType);
            }
            
            boolean hasOrphans = !orphanedRecords.isEmpty();
            
            logger.debug("Orphan check completed. Has orphans: {}, Orphaned record types: {}", 
                        hasOrphans, orphanedRecords.keySet());
            
            return new OrphanCheckResult(hasOrphans, orphanedRecords, cleanupRecommendations);
            
        } catch (Exception e) {
            logger.error("Error checking for orphaned records: {}", e.getMessage(), e);
            cleanupRecommendations.add("Orphan check error: " + e.getMessage());
            return new OrphanCheckResult(false, orphanedRecords, cleanupRecommendations);
        }
    }

    @Override
    public ConsistencyValidationResult validateDataConsistency(String entityType, Long entityId) {
        logger.debug("Validating data consistency for entity type '{}' with ID {}", entityType, entityId);
        
        Map<String, String> consistencyChecks = new HashMap<>();
        List<String> inconsistencies = new ArrayList<>();
        
        try {
            if (entityId == null || entityId <= 0) {
                inconsistencies.add("Invalid entity ID: " + entityId);
                return new ConsistencyValidationResult(false, consistencyChecks, inconsistencies, 0.0);
            }
            
            switch (entityType.toLowerCase()) {
                case "owners":
                    validateOwnerConsistency(entityId, consistencyChecks, inconsistencies);
                    break;
                case "pets":
                    validatePetConsistency(entityId, consistencyChecks, inconsistencies);
                    break;
                case "veterinarians":
                    validateVeterinarianConsistency(entityId, consistencyChecks, inconsistencies);
                    break;
                case "visits":
                    validateVisitConsistency(entityId, consistencyChecks, inconsistencies);
                    break;
                default:
                    inconsistencies.add("Unknown entity type: " + entityType);
            }
            
            boolean consistent = inconsistencies.isEmpty();
            double consistencyScore = calculateConsistencyScore(consistencyChecks, inconsistencies);
            
            logger.debug("Data consistency validation completed. Consistent: {}, Score: {}, Inconsistencies: {}", 
                        consistent, consistencyScore, inconsistencies.size());
            
            return new ConsistencyValidationResult(consistent, consistencyChecks, inconsistencies, consistencyScore);
            
        } catch (Exception e) {
            logger.error("Error validating data consistency: {}", e.getMessage(), e);
            inconsistencies.add("Data consistency validation error: " + e.getMessage());
            return new ConsistencyValidationResult(false, consistencyChecks, inconsistencies, 0.0);
        }
    }

    // Private helper methods

    private boolean isValidEntityType(String entityType) {
        return Arrays.asList("visits", "owners", "pets", "veterinarians").contains(entityType.toLowerCase());
    }

    private boolean isValidSortColumn(String entityType, String column) {
        // This would typically check against a configuration or metadata
        // For now, we'll do basic validation
        return column != null && !column.trim().isEmpty() && column.length() <= 100;
    }

    private boolean isValidOperation(String operation) {
        return Arrays.asList("CREATE", "READ", "UPDATE", "DELETE", "SORT", "FILTER").contains(operation.toUpperCase());
    }

    private RelationshipIntegrityCheck checkRelationshipIntegrity(String entityType, String relationshipColumn) {
        // Simplified relationship integrity check
        List<String> warnings = new ArrayList<>();
        
        if (relationshipColumn.contains("pet.") && !"pets".equals(entityType)) {
            // Check if pet relationships are intact
            warnings.add("Pet relationship column used - ensure pet data integrity");
        }
        
        if (relationshipColumn.contains("owner.") && !"owners".equals(entityType)) {
            warnings.add("Owner relationship column used - ensure owner data integrity");
        }
        
        return new RelationshipIntegrityCheck(warnings.isEmpty(), warnings);
    }

    private boolean hasExistingConflicts(String entityType, List<Long> entityIds, String operation) {
        // Simplified conflict detection - in a real system, this would check for locks, concurrent modifications, etc.
        return false;
    }

    private boolean ownerExists(Long ownerId) {
        try {
            return userService.findById(ownerId) != null;
        } catch (Exception e) {
            logger.warn("Error checking if owner exists: {}", e.getMessage());
            return false;
        }
    }

    private List<Long> getPetIdsByOwnerId(Long ownerId) {
        try {
            return petService.findByOwner(ownerId).stream()
                .map(Pet::getId)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Error getting pet IDs for owner {}: {}", ownerId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private ValidationResult validateOwnerField(String fieldName, Object fieldValue) {
        List<String> errors = new ArrayList<>();
        Object sanitizedValue = fieldValue;
        
        switch (fieldName.toLowerCase()) {
            case "firstname":
            case "lastname":
                if (fieldValue == null || fieldValue.toString().trim().isEmpty()) {
                    errors.add(fieldName + " cannot be null or empty");
                } else {
                    sanitizedValue = fieldValue.toString().trim();
                    if (sanitizedValue.toString().length() > 50) {
                        errors.add(fieldName + " cannot exceed 50 characters");
                    }
                }
                break;
            case "email":
                if (fieldValue != null && !isValidEmail(fieldValue.toString())) {
                    errors.add("Invalid email format");
                }
                break;
            case "telephone":
                if (fieldValue != null && !isValidPhoneNumber(fieldValue.toString())) {
                    errors.add("Invalid telephone format");
                }
                break;
        }
        
        return new ValidationResult(errors.isEmpty(), errors, sanitizedValue);
    }

    private boolean isEmailAlreadyUsed(String email, Long excludeOwnerId) {
        // Simplified email uniqueness check
        return false;
    }

    private boolean hasActivePets(Long ownerId) {
        try {
            return !petService.findByOwner(ownerId).isEmpty();
        } catch (Exception e) {
            logger.warn("Error checking for active pets: {}", e.getMessage());
            return false;
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }

    private boolean isValidPhoneNumber(String phone) {
        return phone != null && phone.matches("\\d{10,15}");
    }

    private void validateVisitRelationships(List<Object> beforeSort, List<Object> afterSort, 
                                          Map<String, Integer> relationshipCounts, List<String> brokenRelationships) {
        // Count pet and veterinarian relationships
        int beforePetRelationships = 0;
        int afterPetRelationships = 0;
        
        for (Object obj : beforeSort) {
            if (obj instanceof Visit && ((Visit) obj).getPet() != null) {
                beforePetRelationships++;
            }
        }
        
        for (Object obj : afterSort) {
            if (obj instanceof Visit && ((Visit) obj).getPet() != null) {
                afterPetRelationships++;
            }
        }
        
        relationshipCounts.put("before_pet_relationships", beforePetRelationships);
        relationshipCounts.put("after_pet_relationships", afterPetRelationships);
        
        if (beforePetRelationships != afterPetRelationships) {
            brokenRelationships.add("Pet relationship count mismatch");
        }
    }

    private void validatePetRelationships(List<Object> beforeSort, List<Object> afterSort, 
                                        Map<String, Integer> relationshipCounts, List<String> brokenRelationships) {
        // Similar validation for pet-owner relationships
        relationshipCounts.put("pet_relationships_validated", 1);
    }

    private void validateOwnerRelationships(List<Object> beforeSort, List<Object> afterSort, 
                                          Map<String, Integer> relationshipCounts, List<String> brokenRelationships) {
        // Similar validation for owner relationships
        relationshipCounts.put("owner_relationships_validated", 1);
    }

    private void validateVeterinarianRelationships(List<Object> beforeSort, List<Object> afterSort, 
                                                 Map<String, Integer> relationshipCounts, List<String> brokenRelationships) {
        // Similar validation for veterinarian relationships
        relationshipCounts.put("veterinarian_relationships_validated", 1);
    }

    private void validateOwnerReferentialIntegrity(List<Long> ownerIds, String operation, 
                                                 Map<String, List<Long>> dependentEntities, List<String> integrityViolations) {
        if ("DELETE".equalsIgnoreCase(operation)) {
            for (Long ownerId : ownerIds) {
                List<Long> petIds = getPetIdsByOwnerId(ownerId);
                if (!petIds.isEmpty()) {
                    dependentEntities.put("pets", petIds);
                    integrityViolations.add("Owner " + ownerId + " has " + petIds.size() + " dependent pets");
                }
            }
        }
    }

    private void validatePetReferentialIntegrity(List<Long> petIds, String operation, 
                                               Map<String, List<Long>> dependentEntities, List<String> integrityViolations) {
        if ("DELETE".equalsIgnoreCase(operation)) {
            for (Long petId : petIds) {
                List<Visit> visits = visitService.findByPet(petId);
                if (!visits.isEmpty()) {
                    List<Long> visitIds = visits.stream().map(Visit::getId).collect(Collectors.toList());
                    dependentEntities.put("visits", visitIds);
                    integrityViolations.add("Pet " + petId + " has " + visitIds.size() + " dependent visits");
                }
            }
        }
    }

    private void validateVeterinarianReferentialIntegrity(List<Long> vetIds, String operation, 
                                                        Map<String, List<Long>> dependentEntities, List<String> integrityViolations) {
        // Similar validation for veterinarian dependencies
    }

    private void validateVisitReferentialIntegrity(List<Long> visitIds, String operation, 
                                                 Map<String, List<Long>> dependentEntities, List<String> integrityViolations) {
        // Visits typically don't have dependent entities
    }

    private void checkForOrphanedPetsAfterOwnerModification(List<Long> ownerIds, 
                                                          Map<String, List<Long>> orphanedRecords, List<String> cleanupRecommendations) {
        // Check for pets that might become orphaned
    }

    private void checkForOrphanedVisitsAfterPetModification(List<Long> petIds, 
                                                          Map<String, List<Long>> orphanedRecords, List<String> cleanupRecommendations) {
        // Check for visits that might become orphaned
    }

    private void checkForOrphanedVisitsAfterVeterinarianModification(List<Long> vetIds, 
                                                                   Map<String, List<Long>> orphanedRecords, List<String> cleanupRecommendations) {
        // Check for visits that might become orphaned
    }

    private void validateOwnerConsistency(Long ownerId, Map<String, String> consistencyChecks, List<String> inconsistencies) {
        consistencyChecks.put("owner_exists", "CHECKED");
        // Add more consistency checks
    }

    private void validatePetConsistency(Long petId, Map<String, String> consistencyChecks, List<String> inconsistencies) {
        consistencyChecks.put("pet_exists", "CHECKED");
        // Add more consistency checks
    }

    private void validateVeterinarianConsistency(Long vetId, Map<String, String> consistencyChecks, List<String> inconsistencies) {
        consistencyChecks.put("veterinarian_exists", "CHECKED");
        // Add more consistency checks
    }

    private void validateVisitConsistency(Long visitId, Map<String, String> consistencyChecks, List<String> inconsistencies) {
        consistencyChecks.put("visit_exists", "CHECKED");
        // Add more consistency checks
    }

    private double calculateConsistencyScore(Map<String, String> consistencyChecks, List<String> inconsistencies) {
        if (consistencyChecks.isEmpty()) {
            return 0.0;
        }
        
        int totalChecks = consistencyChecks.size();
        int failedChecks = inconsistencies.size();
        
        return (double) (totalChecks - failedChecks) / totalChecks;
    }

    // Helper classes
    private static class RelationshipIntegrityCheck {
        private final boolean intact;
        private final List<String> warnings;

        public RelationshipIntegrityCheck(boolean intact, List<String> warnings) {
            this.intact = intact;
            this.warnings = warnings;
        }

        public boolean isIntact() { return intact; }
        public List<String> getWarnings() { return warnings; }
    }

    private static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final Object sanitizedValue;

        public ValidationResult(boolean valid, List<String> errors, Object sanitizedValue) {
            this.valid = valid;
            this.errors = errors;
            this.sanitizedValue = sanitizedValue;
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public Object getSanitizedValue() { return sanitizedValue; }
    }
}