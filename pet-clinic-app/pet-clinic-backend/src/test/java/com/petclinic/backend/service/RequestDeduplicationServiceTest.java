package com.petclinic.backend.service;

import com.petclinic.backend.service.impl.RequestDeduplicationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RequestDeduplicationService
 * Tests request deduplication logic and concurrent operation handling
 */
@ExtendWith(MockitoExtension.class)
class RequestDeduplicationServiceTest {
    
    private RequestDeduplicationService deduplicationService;
    
    @BeforeEach
    void setUp() {
        deduplicationService = new RequestDeduplicationServiceImpl();
    }
    
    @Test
    void testIsDuplicateRequest_NewRequest_ReturnsFalse() {
        // Given
        String requestKey = "test-request-1";
        long windowMs = 5000;
        
        // When
        boolean isDuplicate = deduplicationService.isDuplicateRequest(requestKey, windowMs);
        
        // Then
        assertFalse(isDuplicate, "New request should not be considered duplicate");
    }
    
    @Test
    void testIsDuplicateRequest_WithinWindow_ReturnsTrue() {
        // Given
        String requestKey = "test-request-2";
        long windowMs = 5000;
        
        // When
        deduplicationService.registerRequest(requestKey, windowMs);
        boolean isDuplicate = deduplicationService.isDuplicateRequest(requestKey, windowMs);
        
        // Then
        assertTrue(isDuplicate, "Request within window should be considered duplicate");
    }
    
    @Test
    void testIsDuplicateRequest_OutsideWindow_ReturnsFalse() throws InterruptedException {
        // Given
        String requestKey = "test-request-3";
        long windowMs = 100; // Short window for testing
        
        // When
        deduplicationService.registerRequest(requestKey, windowMs);
        Thread.sleep(windowMs + 50); // Wait longer than window
        boolean isDuplicate = deduplicationService.isDuplicateRequest(requestKey, windowMs);
        
        // Then
        assertFalse(isDuplicate, "Request outside window should not be considered duplicate");
    }
    
    @Test
    void testIsDuplicateRequest_NullKey_ReturnsFalse() {
        // Given
        String requestKey = null;
        long windowMs = 5000;
        
        // When
        boolean isDuplicate = deduplicationService.isDuplicateRequest(requestKey, windowMs);
        
        // Then
        assertFalse(isDuplicate, "Null request key should not be considered duplicate");
    }
    
    @Test
    void testRegisterRequest_ValidRequest_RegistersSuccessfully() {
        // Given
        String requestKey = "test-request-4";
        long ttlMs = 5000;
        
        // When
        deduplicationService.registerRequest(requestKey, ttlMs);
        
        // Then
        assertTrue(deduplicationService.isDuplicateRequest(requestKey, ttlMs), 
                  "Registered request should be found");
    }
    
    @Test
    void testRegisterRequest_NullKey_HandlesGracefully() {
        // Given
        String requestKey = null;
        long ttlMs = 5000;
        
        // When & Then
        assertDoesNotThrow(() -> deduplicationService.registerRequest(requestKey, ttlMs),
                          "Null request key should be handled gracefully");
    }
    
    @Test
    void testExecuteWithDeduplication_NewRequest_ExecutesOperation() throws ExecutionException, InterruptedException {
        // Given
        String requestKey = "test-request-5";
        long windowMs = 5000;
        AtomicInteger executionCount = new AtomicInteger(0);
        
        // When
        CompletableFuture<String> result = deduplicationService.executeWithDeduplication(
            requestKey, 
            windowMs, 
            () -> {
                executionCount.incrementAndGet();
                return "success";
            }
        );
        
        // Then
        assertEquals("success", result.get(), "Operation should execute and return result");
        assertEquals(1, executionCount.get(), "Operation should execute exactly once");
    }
    
    @Test
    void testExecuteWithDeduplication_DuplicateRequest_RejectsExecution() {
        // Given
        String requestKey = "test-request-6";
        long windowMs = 5000;
        AtomicInteger executionCount = new AtomicInteger(0);
        
        // Register the request first
        deduplicationService.registerRequest(requestKey, windowMs);
        
        // When
        CompletableFuture<String> result = deduplicationService.executeWithDeduplication(
            requestKey, 
            windowMs, 
            () -> {
                executionCount.incrementAndGet();
                return "success";
            }
        );
        
        // Then
        assertTrue(result.isCompletedExceptionally(), "Duplicate request should be rejected");
        assertEquals(0, executionCount.get(), "Operation should not execute for duplicate request");
    }
    
    @Test
    void testExecuteWithDeduplication_NullParameters_HandlesGracefully() throws ExecutionException, InterruptedException {
        // Given
        String requestKey = null;
        long windowMs = 5000;
        
        // When
        CompletableFuture<String> result = deduplicationService.executeWithDeduplication(
            requestKey, 
            windowMs, 
            null
        );
        
        // Then
        assertNull(result.get(), "Null parameters should return null result");
    }
    
    @Test
    void testExecuteWithDeduplication_OperationThrowsException_HandlesGracefully() {
        // Given
        String requestKey = "test-request-7";
        long windowMs = 5000;
        
        // When
        CompletableFuture<String> result = deduplicationService.executeWithDeduplication(
            requestKey, 
            windowMs, 
            () -> {
                throw new RuntimeException("Test exception");
            }
        );
        
        // Then
        // Wait a bit for the async operation to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertTrue(result.isCompletedExceptionally(), "Exception in operation should be handled");
    }
    
    @Test
    void testCleanupExpiredRequests_RemovesExpiredRequests() throws InterruptedException {
        // Given
        String requestKey = "test-request-8";
        long ttlMs = 100; // Short TTL for testing
        
        deduplicationService.registerRequest(requestKey, ttlMs);
        assertTrue(deduplicationService.isDuplicateRequest(requestKey, ttlMs), 
                  "Request should be registered");
        
        // When
        Thread.sleep(ttlMs + 50); // Wait for expiry
        deduplicationService.cleanupExpiredRequests();
        
        // Then
        assertFalse(deduplicationService.isDuplicateRequest(requestKey, ttlMs), 
                   "Expired request should be cleaned up");
    }
    
    @Test
    void testGetDeduplicationStatistics_ReturnsValidStatistics() {
        // Given
        String requestKey = "test-request-9";
        long windowMs = 5000;
        
        // When
        deduplicationService.registerRequest(requestKey, windowMs);
        deduplicationService.isDuplicateRequest(requestKey, windowMs); // This should count as duplicate
        Map<String, Object> stats = deduplicationService.getDeduplicationStatistics();
        
        // Then
        assertNotNull(stats, "Statistics should not be null");
        assertTrue(stats.containsKey("totalRequests"), "Should contain total requests");
        assertTrue(stats.containsKey("duplicateRequests"), "Should contain duplicate requests");
        assertTrue(stats.containsKey("duplicateRate"), "Should contain duplicate rate");
        assertTrue(stats.containsKey("activeRequests"), "Should contain active requests");
        
        // Verify some values
        assertTrue((Long) stats.get("totalRequests") > 0, "Should have processed requests");
        assertTrue((Long) stats.get("duplicateRequests") > 0, "Should have detected duplicates");
    }
    
    @Test
    void testClearAllRequests_ClearsAllData() {
        // Given
        String requestKey = "test-request-10";
        long windowMs = 5000;
        
        deduplicationService.registerRequest(requestKey, windowMs);
        assertTrue(deduplicationService.isDuplicateRequest(requestKey, windowMs), 
                  "Request should be registered");
        
        // When
        deduplicationService.clearAllRequests();
        
        // Then
        assertFalse(deduplicationService.isDuplicateRequest(requestKey, windowMs), 
                   "All requests should be cleared");
    }
    
    @Test
    void testConcurrentRequests_HandledCorrectly() throws InterruptedException {
        // Given
        String requestKey = "test-concurrent-request";
        long windowMs = 5000;
        AtomicInteger executionCount = new AtomicInteger(0);
        int threadCount = 10;
        
        // When
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                try {
                    CompletableFuture<String> result = deduplicationService.executeWithDeduplication(
                        requestKey, 
                        windowMs, 
                        () -> {
                            executionCount.incrementAndGet();
                            return "success";
                        }
                    );
                    // Don't wait for result to complete to test concurrency
                } catch (Exception e) {
                    // Expected for duplicate requests
                }
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then
        // Only one execution should succeed, others should be rejected as duplicates
        assertTrue(executionCount.get() <= 1, 
                  "Only one execution should succeed with concurrent requests");
    }
}