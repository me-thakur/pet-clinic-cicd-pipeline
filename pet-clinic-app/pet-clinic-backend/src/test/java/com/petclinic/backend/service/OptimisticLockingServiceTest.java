package com.petclinic.backend.service;

import com.petclinic.backend.dto.BulkOperationResult;
import com.petclinic.backend.service.impl.OptimisticLockingServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OptimisticLockingService
 * Tests optimistic locking behavior and retry logic
 */
@ExtendWith(MockitoExtension.class)
class OptimisticLockingServiceTest {
    
    @Mock
    private EntityManager entityManager;
    
    @Mock
    private Query query;
    
    @InjectMocks
    private OptimisticLockingServiceImpl optimisticLockingService;
    
    @BeforeEach
    void setUp() {
        // Reset any static state if needed
    }
    
    @Test
    void testExecuteBulkOperationWithLocking_SuccessfulOperation_ReturnsResult() {
        // Given
        String entityType = "visits";
        List<Long> entityIds = Arrays.asList(1L, 2L, 3L);
        int maxRetries = 3;
        
        OptimisticLockingService.BulkOperation operation = (ids) -> {
            BulkOperationResult result = new BulkOperationResult(true, 3, 3, entityType);
            return result;
        };
        
        // Mock entity manager for version retrieval
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(
            new Object[]{1L, 1L},
            new Object[]{2L, 1L},
            new Object[]{3L, 1L}
        ));
        
        // When
        BulkOperationResult result = optimisticLockingService.executeBulkOperationWithLocking(
            entityType, entityIds, operation, maxRetries);
        
        // Then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Operation should be successful");
        assertEquals(3, result.getDeletedCount(), "Should have processed 3 items");
        assertEquals(3, result.getTotalRequested(), "Should have requested 3 items");
    }
    
    @Test
    void testExecuteBulkOperationWithLocking_OptimisticLockException_RetriesAndSucceeds() {
        // Given
        String entityType = "visits";
        List<Long> entityIds = Arrays.asList(1L, 2L);
        int maxRetries = 3;
        
        // Create operation that fails once then succeeds
        final boolean[] firstCall = {true};
        OptimisticLockingService.BulkOperation operation = (ids) -> {
            if (firstCall[0]) {
                firstCall[0] = false;
                throw new OptimisticLockException("Version conflict");
            }
            return new BulkOperationResult(true, 2, 2, entityType);
        };
        
        // Mock entity manager
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(
            new Object[]{1L, 1L},
            new Object[]{2L, 1L}
        ));
        
        // When
        BulkOperationResult result = optimisticLockingService.executeBulkOperationWithLocking(
            entityType, entityIds, operation, maxRetries);
        
        // Then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Operation should succeed after retry");
        assertEquals(2, result.getDeletedCount(), "Should have processed 2 items");
    }
    
    @Test
    void testExecuteBulkOperationWithLocking_MaxRetriesExceeded_ReturnsFailure() {
        // Given
        String entityType = "visits";
        List<Long> entityIds = Arrays.asList(1L);
        int maxRetries = 2;
        
        // Operation that always fails with optimistic lock exception
        OptimisticLockingService.BulkOperation operation = (ids) -> {
            throw new OptimisticLockingFailureException("Persistent version conflict");
        };
        
        // Mock entity manager
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(new Object[]{1L, 1L}));
        
        // When
        BulkOperationResult result = optimisticLockingService.executeBulkOperationWithLocking(
            entityType, entityIds, operation, maxRetries);
        
        // Then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Operation should fail after max retries");
        assertTrue(result.hasErrors(), "Result should contain errors");
        assertTrue(result.getErrors().get(0).contains("concurrent modifications"), 
                  "Error should mention concurrent modifications");
    }
    
    @Test
    void testExecuteBulkOperationWithLocking_NonLockingException_DoesNotRetry() {
        // Given
        String entityType = "visits";
        List<Long> entityIds = Arrays.asList(1L);
        int maxRetries = 3;
        
        // Operation that fails with non-locking exception
        OptimisticLockingService.BulkOperation operation = (ids) -> {
            throw new RuntimeException("Database connection error");
        };
        
        // Mock entity manager
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(new Object[]{1L, 1L}));
        
        // When
        BulkOperationResult result = optimisticLockingService.executeBulkOperationWithLocking(
            entityType, entityIds, operation, maxRetries);
        
        // Then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Operation should fail");
        assertTrue(result.hasErrors(), "Result should contain errors");
    }
    
    @Test
    void testValidateEntityVersions_NoConflicts_ReturnsEmptyMap() {
        // Given
        String entityType = "visits";
        Map<Long, Long> entityVersions = Map.of(1L, 1L, 2L, 2L);
        
        // Mock entity manager to return matching versions
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(
            new Object[]{1L, 1L},
            new Object[]{2L, 2L}
        ));
        
        // When
        Map<Long, Long> conflicts = optimisticLockingService.validateEntityVersions(entityType, entityVersions);
        
        // Then
        assertNotNull(conflicts, "Conflicts map should not be null");
        assertTrue(conflicts.isEmpty(), "Should have no conflicts");
    }
    
    @Test
    void testValidateEntityVersions_WithConflicts_ReturnsConflictMap() {
        // Given
        String entityType = "visits";
        Map<Long, Long> entityVersions = Map.of(1L, 1L, 2L, 2L);
        
        // Mock entity manager to return different versions (conflicts)
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(
            new Object[]{1L, 2L}, // Version conflict: expected 1, actual 2
            new Object[]{2L, 2L}  // No conflict
        ));
        
        // When
        Map<Long, Long> conflicts = optimisticLockingService.validateEntityVersions(entityType, entityVersions);
        
        // Then
        assertNotNull(conflicts, "Conflicts map should not be null");
        assertEquals(1, conflicts.size(), "Should have one conflict");
        assertTrue(conflicts.containsKey(1L), "Should contain conflict for entity 1");
        assertEquals(2L, conflicts.get(1L), "Should show actual version 2 for entity 1");
    }
    
    @Test
    void testValidateEntityVersions_NullOrEmptyInput_ReturnsEmptyMap() {
        // Test null input
        Map<Long, Long> nullConflicts = optimisticLockingService.validateEntityVersions("visits", null);
        assertTrue(nullConflicts.isEmpty(), "Null input should return empty map");
        
        // Test empty input
        Map<Long, Long> emptyConflicts = optimisticLockingService.validateEntityVersions("visits", Map.of());
        assertTrue(emptyConflicts.isEmpty(), "Empty input should return empty map");
    }
    
    @Test
    void testValidateEntityVersions_UnknownEntityType_ReturnsEmptyMap() {
        // Given
        String entityType = "unknown";
        Map<Long, Long> entityVersions = Map.of(1L, 1L);
        
        // When
        Map<Long, Long> conflicts = optimisticLockingService.validateEntityVersions(entityType, entityVersions);
        
        // Then
        assertTrue(conflicts.isEmpty(), "Unknown entity type should return empty map");
    }
    
    @Test
    void testGetCurrentEntityVersions_ValidEntities_ReturnsVersionMap() {
        // Given
        String entityType = "visits";
        List<Long> entityIds = Arrays.asList(1L, 2L, 3L);
        
        // Mock entity manager
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(
            new Object[]{1L, 1L},
            new Object[]{2L, 2L},
            new Object[]{3L, 1L}
        ));
        
        // When
        Map<Long, Long> versions = optimisticLockingService.getCurrentEntityVersions(entityType, entityIds);
        
        // Then
        assertNotNull(versions, "Versions map should not be null");
        assertEquals(3, versions.size(), "Should have versions for 3 entities");
        assertEquals(1L, versions.get(1L), "Entity 1 should have version 1");
        assertEquals(2L, versions.get(2L), "Entity 2 should have version 2");
        assertEquals(1L, versions.get(3L), "Entity 3 should have version 1");
    }
    
    @Test
    void testGetCurrentEntityVersions_EmptyInput_ReturnsEmptyMap() {
        // Test null input
        Map<Long, Long> nullVersions = optimisticLockingService.getCurrentEntityVersions("visits", null);
        assertTrue(nullVersions.isEmpty(), "Null input should return empty map");
        
        // Test empty input
        Map<Long, Long> emptyVersions = optimisticLockingService.getCurrentEntityVersions("visits", Arrays.asList());
        assertTrue(emptyVersions.isEmpty(), "Empty input should return empty map");
    }
    
    @Test
    void testHandleOptimisticLockingFailure_AlwaysReturnsTrue() {
        // Given
        String entityType = "visits";
        Long entityId = 1L;
        Long expectedVersion = 1L;
        Long actualVersion = 2L;
        int attempt = 1;
        
        // When
        boolean shouldRetry = optimisticLockingService.handleOptimisticLockingFailure(
            entityType, entityId, expectedVersion, actualVersion, attempt);
        
        // Then
        assertTrue(shouldRetry, "Should always allow retry for optimistic locking failures");
    }
    
    @Test
    void testGetOptimisticLockingStatistics_ReturnsValidStatistics() {
        // Given - Execute some operations to generate statistics
        String entityType = "visits";
        List<Long> entityIds = Arrays.asList(1L);
        
        // Mock entity manager
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(new Object[]{1L, 1L}));
        
        // Execute a successful operation
        OptimisticLockingService.BulkOperation successOperation = (ids) -> 
            new BulkOperationResult(true, 1, 1, entityType);
        
        optimisticLockingService.executeBulkOperationWithLocking(entityType, entityIds, successOperation, 3);
        
        // When
        Map<String, Object> stats = optimisticLockingService.getOptimisticLockingStatistics();
        
        // Then
        assertNotNull(stats, "Statistics should not be null");
        assertTrue(stats.containsKey("totalOperations"), "Should contain total operations");
        assertTrue(stats.containsKey("optimisticLockFailures"), "Should contain lock failures");
        assertTrue(stats.containsKey("failureRate"), "Should contain failure rate");
        assertTrue(stats.containsKey("successfulRetries"), "Should contain successful retries");
        assertTrue(stats.containsKey("failedRetries"), "Should contain failed retries");
        assertTrue(stats.containsKey("retrySuccessRate"), "Should contain retry success rate");
        
        // Verify some values
        assertTrue((Long) stats.get("totalOperations") > 0, "Should have processed operations");
        assertTrue((Double) stats.get("failureRate") >= 0.0, "Failure rate should be non-negative");
    }
}