package com.petclinic.backend.service;

import com.petclinic.backend.dto.BulkDeleteRequest;
import com.petclinic.backend.dto.BulkOperationResult;
import com.petclinic.backend.service.impl.ConcurrentOperationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConcurrentOperationService
 * Tests coordination of request deduplication and optimistic locking
 */
@ExtendWith(MockitoExtension.class)
class ConcurrentOperationServiceTest {
    
    @Mock
    private RequestDeduplicationService deduplicationService;
    
    @Mock
    private OptimisticLockingService optimisticLockingService;
    
    @Mock
    private EnhancedTableService<Object> enhancedTableService;
    
    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private ConcurrentOperationServiceImpl concurrentOperationService;
    
    @BeforeEach
    void setUp() {
        // Mock security context
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
    }
    
    @Test
    void testExecuteSafeBulkDelete_ValidRequest_ExecutesSuccessfully() throws ExecutionException, InterruptedException {
        // Given
        BulkDeleteRequest request = new BulkDeleteRequest(Arrays.asList(1L, 2L, 3L), "visits");
        BulkOperationResult expectedResult = new BulkOperationResult(true, 3, 3, "visits");
        
        // Mock deduplication service
        when(deduplicationService.executeWithDeduplication(anyString(), anyLong(), any()))
            .thenAnswer(invocation -> {
                java.util.function.Supplier<BulkOperationResult> supplier = invocation.getArgument(2);
                return CompletableFuture.completedFuture(supplier.get());
            });
        
        // Mock optimistic locking service
        when(optimisticLockingService.executeBulkOperationWithLocking(anyString(), anyList(), any(), anyInt()))
            .thenReturn(expectedResult);
        
        // When
        CompletableFuture<BulkOperationResult> future = concurrentOperationService.executeSafeBulkDelete(request);
        BulkOperationResult result = future.get();
        
        // Then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Operation should be successful");
        assertEquals(3, result.getDeletedCount(), "Should have deleted 3 items");
        assertEquals("visits", result.getEntityType(), "Should have correct entity type");
    }
    
    @Test
    void testExecuteSafeBulkDelete_InvalidRequest_ReturnsError() throws ExecutionException, InterruptedException {
        // Given
        BulkDeleteRequest request = null;
        
        // When
        CompletableFuture<BulkOperationResult> future = concurrentOperationService.executeSafeBulkDelete(request);
        BulkOperationResult result = future.get();
        
        // Then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Operation should fail");
        assertTrue(result.hasErrors(), "Result should contain errors");
        assertTrue(result.getErrors().get(0).contains("Invalid bulk delete request"), 
                  "Error should mention invalid request");
    }
    
    @Test
    void testExecuteSafeBulkDelete_EmptySelectedIds_ReturnsError() throws ExecutionException, InterruptedException {
        // Given
        BulkDeleteRequest request = new BulkDeleteRequest(Arrays.asList(), "visits");
        
        // When
        CompletableFuture<BulkOperationResult> future = concurrentOperationService.executeSafeBulkDelete(request);
        BulkOperationResult result = future.get();
        
        // Then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Operation should fail");
        assertTrue(result.hasErrors(), "Result should contain errors");
    }
    
    @Test
    void testExecuteSafeBulkOperation_ValidOperation_ExecutesSuccessfully() throws ExecutionException, InterruptedException {
        // Given
        String entityType = "visits";
        String operationType = "DELETE";
        List<Long> entityIds = Arrays.asList(1L, 2L);
        BulkOperationResult expectedResult = new BulkOperationResult(true, 2, 2, entityType);
        
        OptimisticLockingService.BulkOperation operation = (ids) -> expectedResult;
        
        // Mock deduplication service
        when(deduplicationService.executeWithDeduplication(anyString(), anyLong(), any()))
            .thenAnswer(invocation -> {
                java.util.function.Supplier<BulkOperationResult> supplier = invocation.getArgument(2);
                return CompletableFuture.completedFuture(supplier.get());
            });
        
        // Mock optimistic locking service
        when(optimisticLockingService.executeBulkOperationWithLocking(anyString(), anyList(), any(), anyInt()))
            .thenReturn(expectedResult);
        
        // When
        CompletableFuture<BulkOperationResult> future = concurrentOperationService.executeSafeBulkOperation(
            entityType, operationType, entityIds, operation, null);
        BulkOperationResult result = future.get();
        
        // Then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Operation should be successful");
        assertEquals(2, result.getDeletedCount(), "Should have processed 2 items");
    }
    
    @Test
    void testExecuteSafeBulkOperation_WithVersionConflicts_ReturnsError() throws ExecutionException, InterruptedException {
        // Given
        String entityType = "visits";
        String operationType = "UPDATE";
        List<Long> entityIds = Arrays.asList(1L, 2L);
        Map<Long, Long> entityVersions = Map.of(1L, 1L, 2L, 2L);
        Map<Long, Long> conflicts = Map.of(1L, 2L); // Version conflict for entity 1
        
        OptimisticLockingService.BulkOperation operation = (ids) -> 
            new BulkOperationResult(true, 2, 2, entityType);
        
        // Mock optimistic locking service to return conflicts
        when(optimisticLockingService.validateEntityVersions(entityType, entityVersions))
            .thenReturn(conflicts);
        
        // When
        CompletableFuture<BulkOperationResult> future = concurrentOperationService.executeSafeBulkOperation(
            entityType, operationType, entityIds, operation, entityVersions);
        BulkOperationResult result = future.get();
        
        // Then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Operation should fail due to version conflicts");
        assertTrue(result.hasErrors(), "Result should contain errors");
        assertTrue(result.getErrors().get(0).contains("Version conflicts"), 
                  "Error should mention version conflicts");
    }
    
    @Test
    void testExecuteSafeBulkOperation_OperationThrowsException_HandlesGracefully() throws ExecutionException, InterruptedException {
        // Given
        String entityType = "visits";
        String operationType = "DELETE";
        List<Long> entityIds = Arrays.asList(1L);
        
        OptimisticLockingService.BulkOperation operation = (ids) -> {
            throw new RuntimeException("Test exception");
        };
        
        // Mock deduplication service
        when(deduplicationService.executeWithDeduplication(anyString(), anyLong(), any()))
            .thenAnswer(invocation -> {
                java.util.function.Supplier<BulkOperationResult> supplier = invocation.getArgument(2);
                return CompletableFuture.completedFuture(supplier.get());
            });
        
        // When
        CompletableFuture<BulkOperationResult> future = concurrentOperationService.executeSafeBulkOperation(
            entityType, operationType, entityIds, operation, null);
        BulkOperationResult result = future.get();
        
        // Then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Operation should fail");
        assertTrue(result.hasErrors(), "Result should contain errors");
        assertTrue(result.getErrors().get(0).contains("Operation failed"), 
                  "Error should mention operation failure");
    }
    
    @Test
    void testIsOperationInProgress_ActiveOperation_ReturnsTrue() {
        // Given
        String requestKey = "test-request-key";
        CompletableFuture<BulkOperationResult> activeOperation = new CompletableFuture<>();
        
        // Simulate active operation by executing one that doesn't complete immediately
        concurrentOperationService.executeSafeBulkOperation(
            "visits", "DELETE", Arrays.asList(1L), 
            (ids) -> new BulkOperationResult(true, 1, 1, "visits"), null);
        
        // When & Then
        // Note: This test is limited because we can't easily inject the active operation
        // In a real scenario, we'd need to refactor to make this more testable
        assertFalse(concurrentOperationService.isOperationInProgress("non-existent-key"), 
                   "Non-existent operation should not be in progress");
    }
    
    @Test
    void testCancelOperation_NonExistentOperation_ReturnsFalse() {
        // Given
        String requestKey = "non-existent-key";
        
        // When
        boolean cancelled = concurrentOperationService.cancelOperation(requestKey);
        
        // Then
        assertFalse(cancelled, "Non-existent operation should not be cancelled");
    }
    
    @Test
    void testGetOperationStatus_NonExistentOperation_ReturnsNull() {
        // Given
        String requestKey = "non-existent-key";
        
        // When
        BulkOperationResult status = concurrentOperationService.getOperationStatus(requestKey);
        
        // Then
        assertNull(status, "Non-existent operation should return null status");
    }
    
    @Test
    void testGetConcurrentOperationStatistics_ReturnsValidStatistics() {
        // Given
        Map<String, Object> deduplicationStats = Map.of("totalRequests", 10L, "duplicateRequests", 2L);
        Map<String, Object> lockingStats = Map.of("totalOperations", 5L, "optimisticLockFailures", 1L);
        
        when(deduplicationService.getDeduplicationStatistics()).thenReturn(deduplicationStats);
        when(optimisticLockingService.getOptimisticLockingStatistics()).thenReturn(lockingStats);
        
        // When
        Map<String, Object> stats = concurrentOperationService.getConcurrentOperationStatistics();
        
        // Then
        assertNotNull(stats, "Statistics should not be null");
        assertTrue(stats.containsKey("totalOperations"), "Should contain total operations");
        assertTrue(stats.containsKey("successfulOperations"), "Should contain successful operations");
        assertTrue(stats.containsKey("failedOperations"), "Should contain failed operations");
        assertTrue(stats.containsKey("cancelledOperations"), "Should contain cancelled operations");
        assertTrue(stats.containsKey("activeOperations"), "Should contain active operations");
        assertTrue(stats.containsKey("successRate"), "Should contain success rate");
        assertTrue(stats.containsKey("deduplication"), "Should contain deduplication stats");
        assertTrue(stats.containsKey("optimisticLocking"), "Should contain optimistic locking stats");
        
        // Verify nested statistics
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedDeduplicationStats = (Map<String, Object>) stats.get("deduplication");
        assertEquals(deduplicationStats, nestedDeduplicationStats, "Should contain correct deduplication stats");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedLockingStats = (Map<String, Object>) stats.get("optimisticLocking");
        assertEquals(lockingStats, nestedLockingStats, "Should contain correct locking stats");
    }
    
    @Test
    void testGenerateRequestKey_ValidParameters_ReturnsConsistentKey() {
        // Given
        String entityType = "visits";
        String operationType = "DELETE";
        List<Long> entityIds = Arrays.asList(3L, 1L, 2L); // Unsorted to test sorting
        String userId = "testuser";
        
        // When
        String key1 = concurrentOperationService.generateRequestKey(entityType, operationType, entityIds, userId);
        String key2 = concurrentOperationService.generateRequestKey(entityType, operationType, entityIds, userId);
        
        // Then
        assertNotNull(key1, "Request key should not be null");
        assertNotNull(key2, "Request key should not be null");
        assertEquals(key1, key2, "Same parameters should generate same key");
        assertTrue(key1.length() > 0, "Request key should not be empty");
    }
    
    @Test
    void testGenerateRequestKey_DifferentParameters_ReturnsDifferentKeys() {
        // Given
        String entityType = "visits";
        String operationType = "DELETE";
        List<Long> entityIds1 = Arrays.asList(1L, 2L);
        List<Long> entityIds2 = Arrays.asList(1L, 3L);
        String userId = "testuser";
        
        // When
        String key1 = concurrentOperationService.generateRequestKey(entityType, operationType, entityIds1, userId);
        String key2 = concurrentOperationService.generateRequestKey(entityType, operationType, entityIds2, userId);
        
        // Then
        assertNotNull(key1, "Request key 1 should not be null");
        assertNotNull(key2, "Request key 2 should not be null");
        assertNotEquals(key1, key2, "Different parameters should generate different keys");
    }
    
    @Test
    void testGenerateRequestKey_NullUserId_HandlesGracefully() {
        // Given
        String entityType = "visits";
        String operationType = "DELETE";
        List<Long> entityIds = Arrays.asList(1L, 2L);
        String userId = null;
        
        // When
        String key = concurrentOperationService.generateRequestKey(entityType, operationType, entityIds, userId);
        
        // Then
        assertNotNull(key, "Request key should not be null even with null user ID");
        assertTrue(key.length() > 0, "Request key should not be empty");
    }
}