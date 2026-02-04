package com.petclinic.backend.service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for handling request deduplication to prevent rapid successive requests
 * from causing concurrent modification issues
 * Validates: Requirements 5.5
 */
public interface RequestDeduplicationService {
    
    /**
     * Check if a request is a duplicate of a recent request
     * @param requestKey Unique key identifying the request
     * @param windowMs Time window in milliseconds to check for duplicates
     * @return true if request is a duplicate, false otherwise
     */
    boolean isDuplicateRequest(String requestKey, long windowMs);
    
    /**
     * Register a request to prevent duplicates
     * @param requestKey Unique key identifying the request
     * @param ttlMs Time to live for the request record in milliseconds
     */
    void registerRequest(String requestKey, long ttlMs);
    
    /**
     * Execute a request with deduplication protection
     * @param requestKey Unique key identifying the request
     * @param windowMs Time window to check for duplicates
     * @param operation The operation to execute
     * @param <T> Return type of the operation
     * @return CompletableFuture with the result or existing result if duplicate
     */
    <T> CompletableFuture<T> executeWithDeduplication(String requestKey, long windowMs, 
                                                      java.util.function.Supplier<T> operation);
    
    /**
     * Clear expired request records
     */
    void cleanupExpiredRequests();
    
    /**
     * Get statistics about request deduplication
     * @return Map containing deduplication statistics
     */
    java.util.Map<String, Object> getDeduplicationStatistics();
    
    /**
     * Clear all request records (for testing purposes)
     */
    void clearAllRequests();
}