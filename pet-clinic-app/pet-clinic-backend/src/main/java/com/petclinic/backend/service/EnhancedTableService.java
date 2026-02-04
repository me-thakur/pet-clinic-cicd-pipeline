package com.petclinic.backend.service;

import com.petclinic.backend.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Enhanced table service interface providing generic sort, filter, and bulk operations
 * Supports server-side sorting and filtering with pagination
 * Validates: Requirements 2.2, 2.3
 * 
 * @param <T> Entity type
 */
public interface EnhancedTableService<T> {
    
    /**
     * Find entities with sort and filter support
     * 
     * @param entityType Entity type name (visits, owners, pets, veterinarians)
     * @param pageable Pagination and basic sort information
     * @param sortMetadata Enhanced sort metadata
     * @param filters List of filter criteria
     * @return Paginated response with sorted and filtered results
     */
    PagedResponse<T> findWithSortAndFilter(
        String entityType,
        Pageable pageable,
        SortMetadata sortMetadata,
        List<FilterCriteria> filters
    );
    
    /**
     * Find entities with multi-column sort and filter support
     * 
     * @param entityType Entity type name (visits, owners, pets, veterinarians)
     * @param pageable Pagination and basic sort information
     * @param multiColumnSortMetadata Multi-column sort metadata
     * @param filters List of filter criteria
     * @return Paginated response with sorted and filtered results
     */
    PagedResponse<T> findWithMultiColumnSortAndFilter(
        String entityType,
        Pageable pageable,
        MultiColumnSortMetadata multiColumnSortMetadata,
        List<FilterCriteria> filters
    );
    
    /**
     * Execute bulk delete operation
     * 
     * @param request Bulk delete request with IDs and context
     * @return Result of bulk delete operation
     */
    BulkOperationResult bulkDelete(BulkDeleteRequest request);
    
    /**
     * Get available filter values for a specific column
     * 
     * @param entityType Entity type name
     * @param column Column name
     * @return List of available values for filtering
     */
    List<String> getAvailableFilterValues(String entityType, String column);
    
    /**
     * Validate sort criteria for entity type
     * 
     * @param entityType Entity type name
     * @param sortMetadata Sort metadata to validate
     * @return true if sort criteria is valid, false otherwise
     */
    boolean validateSortCriteria(String entityType, SortMetadata sortMetadata);
    
    /**
     * Validate filter criteria for entity type
     * 
     * @param entityType Entity type name
     * @param filterCriteria Filter criteria to validate
     * @return true if filter criteria is valid, false otherwise
     */
    boolean validateFilterCriteria(String entityType, FilterCriteria filterCriteria);
    
    /**
     * Get sortable columns for entity type
     * 
     * @param entityType Entity type name
     * @return List of sortable column names
     */
    List<String> getSortableColumns(String entityType);
    
    /**
     * Validate multi-column sort criteria for entity type
     * 
     * @param entityType Entity type name
     * @param multiColumnSortMetadata Multi-column sort metadata to validate
     * @return true if all sort criteria are valid, false otherwise
     */
    boolean validateMultiColumnSortCriteria(String entityType, MultiColumnSortMetadata multiColumnSortMetadata);
    
    /**
     * Get filterable columns for entity type
     * 
     * @param entityType Entity type name
     * @return List of filterable column names
     */
    List<String> getFilterableColumns(String entityType);
}