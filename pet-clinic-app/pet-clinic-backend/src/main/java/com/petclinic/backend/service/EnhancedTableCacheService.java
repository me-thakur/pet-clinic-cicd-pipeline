package com.petclinic.backend.service;

import com.petclinic.backend.dto.FilterCriteria;
import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.dto.SortMetadata;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * Service interface for caching enhanced table operations
 * Provides caching for sort/filter results and cache invalidation strategies
 * Validates: Requirements 5.3
 */
public interface EnhancedTableCacheService {
    
    /**
     * Get cached sort/filter results
     * 
     * @param entityType Entity type
     * @param pageable Pagination information
     * @param sortMetadata Sort criteria
     * @param filters Filter criteria
     * @return Cached results or null if not found
     */
    PagedResponse<Object> getCachedResults(String entityType, Pageable pageable, 
                                         SortMetadata sortMetadata, List<FilterCriteria> filters);
    
    /**
     * Cache sort/filter results
     * 
     * @param entityType Entity type
     * @param pageable Pagination information
     * @param sortMetadata Sort criteria
     * @param filters Filter criteria
     * @param results Results to cache
     */
    void cacheResults(String entityType, Pageable pageable, SortMetadata sortMetadata, 
                     List<FilterCriteria> filters, PagedResponse<Object> results);
    
    /**
     * Get cached count for filtered results
     * 
     * @param entityType Entity type
     * @param filters Filter criteria
     * @return Cached count or null if not found
     */
    Long getCachedCount(String entityType, List<FilterCriteria> filters);
    
    /**
     * Cache count for filtered results
     * 
     * @param entityType Entity type
     * @param filters Filter criteria
     * @param count Count to cache
     */
    void cacheCount(String entityType, List<FilterCriteria> filters, Long count);
    
    /**
     * Get cached filter values
     * 
     * @param entityType Entity type
     * @param column Column name
     * @return Cached filter values or null if not found
     */
    List<String> getCachedFilterValues(String entityType, String column);
    
    /**
     * Cache filter values
     * 
     * @param entityType Entity type
     * @param column Column name
     * @param values Filter values to cache
     */
    void cacheFilterValues(String entityType, String column, List<String> values);
    
    /**
     * Invalidate all caches for an entity type
     * 
     * @param entityType Entity type
     */
    void invalidateEntityCaches(String entityType);
    
    /**
     * Invalidate all table caches
     */
    void invalidateAllTableCaches();
    
    /**
     * Get cache statistics for table operations
     * 
     * @return Map of cache statistics
     */
    Map<String, Object> getTableCacheStatistics();
    
    /**
     * Get cache hit rate for table operations
     * 
     * @return Cache hit rate (0.0 to 1.0)
     */
    double getTableCacheHitRate();
    
    /**
     * Warm up caches with frequently accessed data
     * 
     * @param entityType Entity type to warm up
     */
    void warmUpEntityCache(String entityType);
    
    /**
     * Check if caching is enabled for entity type
     * 
     * @param entityType Entity type
     * @return True if caching is enabled
     */
    boolean isCachingEnabled(String entityType);
    
    /**
     * Enable/disable caching for entity type
     * 
     * @param entityType Entity type
     * @param enabled True to enable caching
     */
    void setCachingEnabled(String entityType, boolean enabled);
}