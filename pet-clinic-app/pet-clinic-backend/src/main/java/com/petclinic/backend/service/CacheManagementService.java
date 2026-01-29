package com.petclinic.backend.service;

import java.util.Map;

/**
 * Service interface for cache management operations
 * Provides cache invalidation strategies and cache statistics
 * Validates: Requirements 10.3
 */
public interface CacheManagementService {
    
    /**
     * Clear all caches
     */
    void clearAllCaches();
    
    /**
     * Clear specific cache by name
     * @param cacheName Name of the cache to clear
     */
    void clearCache(String cacheName);
    
    /**
     * Clear caches related to a specific entity type
     * @param entityType Entity type (Pet, Veterinarian, Visit, etc.)
     */
    void clearEntityCaches(String entityType);
    
    /**
     * Get cache statistics
     * @return Map containing cache statistics
     */
    Map<String, Object> getCacheStatistics();
    
    /**
     * Get cache hit rates
     * @return Map of cache names to hit rates
     */
    Map<String, Double> getCacheHitRates();
    
    /**
     * Warm up caches with frequently accessed data
     */
    void warmUpCaches();
    
    /**
     * Invalidate search result caches
     */
    void invalidateSearchCaches();
    
    /**
     * Invalidate statistics caches
     */
    void invalidateStatisticsCaches();
    
    /**
     * Invalidate dashboard metrics caches
     */
    void invalidateDashboardCaches();
}