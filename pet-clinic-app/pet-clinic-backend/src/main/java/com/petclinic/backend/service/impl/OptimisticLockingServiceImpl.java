package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.BulkOperationResult;
import com.petclinic.backend.model.BaseEntity;
import com.petclinic.backend.service.OptimisticLockingService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of optimistic locking service for bulk operations
 * Handles version conflicts and provides retry logic
 * Validates: Requirements 5.5
 */
@Service
@Transactional
public class OptimisticLockingServiceImpl implements OptimisticLockingService {
    
    private static final Logger logger = LoggerFactory.getLogger(OptimisticLockingServiceImpl.class);
    
    @PersistenceContext
    private EntityManager entityManager;
    
    // Statistics
    private final AtomicLong totalOperations = new AtomicLong(0);
    private final AtomicLong optimisticLockFailures = new AtomicLong(0);
    private final AtomicLong successfulRetries = new AtomicLong(0);
    private final AtomicLong failedRetries = new AtomicLong(0);
    
    @Override
    public BulkOperationResult executeBulkOperationWithLocking(
            String entityType,
            List<Long> entityIds,
            BulkOperation operation,
            int maxRetries) {
        
        totalOperations.incrementAndGet();
        logger.debug("Executing bulk operation with optimistic locking: {} entities, max retries: {}", 
                    entityIds.size(), maxRetries);
        
        BulkOperationResult result = null;
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                // Get current entity versions before operation
                Map<Long, Long> preOperationVersions = getCurrentEntityVersions(entityType, entityIds);
                logger.debug("Pre-operation versions for attempt {}: {}", attempt, preOperationVersions);
                
                // Execute the operation
                result = operation.execute(entityIds);
                
                // If we get here, the operation succeeded
                if (attempt > 1) {
                    successfulRetries.incrementAndGet();
                    logger.info("Bulk operation succeeded on attempt {} after optimistic lock failures", attempt);
                }
                
                return result;
                
            } catch (OptimisticLockException | OptimisticLockingFailureException e) {
                optimisticLockFailures.incrementAndGet();
                lastException = e;
                
                logger.warn("Optimistic locking failure on attempt {} for {} operation: {}", 
                           attempt, entityType, e.getMessage());
                
                if (attempt <= maxRetries) {
                    // Wait a bit before retrying to reduce contention
                    try {
                        Thread.sleep(50 * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    
                    // Clear the persistence context to avoid stale entities
                    entityManager.clear();
                    
                    logger.debug("Retrying bulk operation (attempt {} of {})", attempt + 1, maxRetries + 1);
                } else {
                    failedRetries.incrementAndGet();
                    logger.error("Bulk operation failed after {} attempts due to optimistic locking conflicts", 
                                maxRetries + 1);
                }
                
            } catch (Exception e) {
                logger.error("Bulk operation failed with non-locking exception on attempt {}: {}", 
                            attempt, e.getMessage(), e);
                lastException = e;
                break; // Don't retry for non-locking exceptions
            }
        }
        
        // If we get here, all attempts failed
        if (result == null) {
            result = new BulkOperationResult(false, 0, entityIds.size(), entityType);
        }
        
        result.addError("Bulk operation failed after " + (maxRetries + 1) + " attempts due to concurrent modifications");
        if (lastException != null) {
            result.addError("Last error: " + lastException.getMessage());
        }
        
        return result;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> validateEntityVersions(String entityType, Map<Long, Long> entityVersions) {
        Map<Long, Long> conflicts = new HashMap<>();
        
        if (entityVersions == null || entityVersions.isEmpty()) {
            return conflicts;
        }
        
        String entityClassName = getEntityClassName(entityType);
        if (entityClassName == null) {
            logger.warn("Unknown entity type for version validation: {}", entityType);
            return conflicts;
        }
        
        try {
            String jpql = "SELECT e.id, e.version FROM " + entityClassName + " e WHERE e.id IN :ids";
            Query query = entityManager.createQuery(jpql);
            query.setParameter("ids", entityVersions.keySet());
            
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();
            
            for (Object[] row : results) {
                Long entityId = (Long) row[0];
                Long currentVersion = (Long) row[1];
                Long expectedVersion = entityVersions.get(entityId);
                
                if (expectedVersion != null && !expectedVersion.equals(currentVersion)) {
                    conflicts.put(entityId, currentVersion);
                    logger.debug("Version conflict detected for {} {}: expected {}, actual {}", 
                                entityType, entityId, expectedVersion, currentVersion);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error validating entity versions for {}: {}", entityType, e.getMessage(), e);
        }
        
        return conflicts;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getCurrentEntityVersions(String entityType, List<Long> entityIds) {
        Map<Long, Long> versions = new HashMap<>();
        
        if (entityIds == null || entityIds.isEmpty()) {
            return versions;
        }
        
        String entityClassName = getEntityClassName(entityType);
        if (entityClassName == null) {
            logger.warn("Unknown entity type for version retrieval: {}", entityType);
            return versions;
        }
        
        try {
            String jpql = "SELECT e.id, e.version FROM " + entityClassName + " e WHERE e.id IN :ids";
            Query query = entityManager.createQuery(jpql);
            query.setParameter("ids", entityIds);
            
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();
            
            for (Object[] row : results) {
                Long entityId = (Long) row[0];
                Long version = (Long) row[1];
                versions.put(entityId, version);
            }
            
            logger.debug("Retrieved versions for {} {}: {}", entityIds.size(), entityType, versions);
            
        } catch (Exception e) {
            logger.error("Error retrieving entity versions for {}: {}", entityType, e.getMessage(), e);
        }
        
        return versions;
    }
    
    @Override
    public boolean handleOptimisticLockingFailure(
            String entityType,
            Long entityId,
            Long expectedVersion,
            Long actualVersion,
            int attempt) {
        
        logger.warn("Optimistic locking failure for {} {}: expected version {}, actual version {}, attempt {}", 
                   entityType, entityId, expectedVersion, actualVersion, attempt);
        
        // Always allow retry for optimistic locking failures
        // The calling code will determine if max retries have been exceeded
        return true;
    }
    
    @Override
    public Map<String, Object> getOptimisticLockingStatistics() {
        long total = totalOperations.get();
        long failures = optimisticLockFailures.get();
        
        return Map.of(
            "totalOperations", total,
            "optimisticLockFailures", failures,
            "failureRate", total > 0 ? (double) failures / total * 100.0 : 0.0,
            "successfulRetries", successfulRetries.get(),
            "failedRetries", failedRetries.get(),
            "retrySuccessRate", failures > 0 ? 
                (double) successfulRetries.get() / failures * 100.0 : 0.0
        );
    }
    
    /**
     * Get the entity class name for JPQL queries
     */
    private String getEntityClassName(String entityType) {
        switch (entityType.toLowerCase()) {
            case "visits":
                return "Visit";
            case "owners":
                return "User"; // Assuming owners are stored as User entities
            case "pets":
                return "Pet";
            case "veterinarians":
                return "Veterinarian";
            default:
                return null;
        }
    }
}