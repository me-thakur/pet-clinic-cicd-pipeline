package com.petclinic.backend.service.impl;

import com.petclinic.backend.service.RequestDeduplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of request deduplication service
 * Uses in-memory storage with TTL for request tracking
 * Validates: Requirements 5.5
 */
@Service
public class RequestDeduplicationServiceImpl implements RequestDeduplicationService {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestDeduplicationServiceImpl.class);
    
    // Request tracking storage
    private final Map<String, RequestRecord> activeRequests = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<?>> pendingOperations = new ConcurrentHashMap<>();
    
    // Statistics
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong duplicateRequests = new AtomicLong(0);
    private final AtomicLong cleanupOperations = new AtomicLong(0);
    
    @Override
    public boolean isDuplicateRequest(String requestKey, long windowMs) {
        if (requestKey == null) {
            return false;
        }
        
        totalRequests.incrementAndGet();
        
        RequestRecord record = activeRequests.get(requestKey);
        if (record == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceRequest = currentTime - record.timestamp;
        
        if (timeSinceRequest <= windowMs) {
            duplicateRequests.incrementAndGet();
            logger.debug("Duplicate request detected: {} ({}ms ago)", requestKey, timeSinceRequest);
            return true;
        }
        
        // Request is old enough, remove it
        activeRequests.remove(requestKey);
        return false;
    }
    
    @Override
    public void registerRequest(String requestKey, long ttlMs) {
        if (requestKey == null) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        RequestRecord record = new RequestRecord(currentTime, currentTime + ttlMs);
        activeRequests.put(requestKey, record);
        
        logger.debug("Registered request: {} (TTL: {}ms)", requestKey, ttlMs);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> executeWithDeduplication(String requestKey, long windowMs, 
                                                            java.util.function.Supplier<T> operation) {
        if (requestKey == null || operation == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        // Check if there's already a pending operation for this request
        CompletableFuture<?> existingOperation = pendingOperations.get(requestKey);
        if (existingOperation != null && !existingOperation.isDone()) {
            logger.debug("Returning existing pending operation for request: {}", requestKey);
            return (CompletableFuture<T>) existingOperation;
        }
        
        // Check for duplicate request
        if (isDuplicateRequest(requestKey, windowMs)) {
            logger.warn("Rejecting duplicate request: {}", requestKey);
            CompletableFuture<T> duplicateResult = new CompletableFuture<>();
            duplicateResult.completeExceptionally(
                new IllegalStateException("Duplicate request detected: " + requestKey));
            return duplicateResult;
        }
        
        // Register the request and execute the operation
        registerRequest(requestKey, windowMs * 2); // Keep record longer than window
        
        CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Executing operation for request: {}", requestKey);
                return operation.get();
            } catch (Exception e) {
                logger.error("Error executing operation for request {}: {}", requestKey, e.getMessage(), e);
                throw new RuntimeException("Operation failed for request: " + requestKey, e);
            }
        });
        
        // Store the pending operation
        pendingOperations.put(requestKey, future);
        
        // Clean up when operation completes
        future.whenComplete((result, throwable) -> {
            pendingOperations.remove(requestKey);
            if (throwable != null) {
                logger.error("Operation failed for request {}: {}", requestKey, throwable.getMessage());
            } else {
                logger.debug("Operation completed successfully for request: {}", requestKey);
            }
        });
        
        return future;
    }
    
    @Override
    @Scheduled(fixedRate = 60000) // Run every minute
    public void cleanupExpiredRequests() {
        long currentTime = System.currentTimeMillis();
        final int[] removedCount = {0};
        
        activeRequests.entrySet().removeIf(entry -> {
            if (entry.getValue().expiryTime <= currentTime) {
                removedCount[0]++;
                return true;
            }
            return false;
        });
        
        // Also clean up completed pending operations
        final int[] removedPendingCount = {0};
        pendingOperations.entrySet().removeIf(entry -> {
            if (entry.getValue().isDone()) {
                removedPendingCount[0]++;
                return true;
            }
            return false;
        });
        
        if (removedCount[0] > 0 || removedPendingCount[0] > 0) {
            cleanupOperations.incrementAndGet();
            logger.debug("Cleaned up {} expired requests and {} completed operations", 
                        removedCount[0], removedPendingCount[0]);
        }
    }
    
    @Override
    public Map<String, Object> getDeduplicationStatistics() {
        return Map.of(
            "totalRequests", totalRequests.get(),
            "duplicateRequests", duplicateRequests.get(),
            "duplicateRate", totalRequests.get() > 0 ? 
                (double) duplicateRequests.get() / totalRequests.get() * 100.0 : 0.0,
            "activeRequests", activeRequests.size(),
            "pendingOperations", pendingOperations.size(),
            "cleanupOperations", cleanupOperations.get()
        );
    }
    
    @Override
    public void clearAllRequests() {
        activeRequests.clear();
        pendingOperations.clear();
        logger.info("Cleared all request deduplication records");
    }
    
    /**
     * Internal class to track request records
     */
    private static class RequestRecord {
        final long timestamp;
        final long expiryTime;
        
        RequestRecord(long timestamp, long expiryTime) {
            this.timestamp = timestamp;
            this.expiryTime = expiryTime;
        }
    }
}