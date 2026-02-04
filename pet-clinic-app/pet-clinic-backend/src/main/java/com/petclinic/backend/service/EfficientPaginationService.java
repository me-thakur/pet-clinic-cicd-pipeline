package com.petclinic.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Map;

/**
 * Service for efficient pagination and data loading
 * Validates: Requirements 16.1, 16.2, 16.3
 */
public interface EfficientPaginationService {
    
    /**
     * Get paginated results with optimized queries
     */
    <T> Page<T> getPaginatedResults(String entityType, Pageable pageable, Map<String, Object> filters);
    
    /**
     * Get paginated results with cursor-based pagination for large datasets
     */
    <T> Map<String, Object> getCursorPaginatedResults(String entityType, String cursor, int limit, Map<String, Object> filters);
    
    /**
     * Preload next page data for better user experience
     */
    <T> void preloadNextPage(String entityType, Pageable currentPage, Map<String, Object> filters);
    
    /**
     * Get pagination metadata and performance statistics
     */
    Map<String, Object> getPaginationMetadata(String entityType, Pageable pageable, Map<String, Object> filters);
    
    /**
     * Optimize pagination query based on usage patterns
     */
    void optimizePaginationQuery(String entityType, Map<String, Object> queryPattern);
    
    /**
     * Get recommended page size based on data characteristics
     */
    int getRecommendedPageSize(String entityType, Map<String, Object> filters);
}