package com.petclinic.backend.service;

import com.petclinic.backend.dto.*;
import com.petclinic.backend.model.Visit;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Enhanced visit service interface extending base VisitService with enhanced table functionality
 * Provides server-side sorting, filtering, and bulk operations for visits
 * Validates: Requirements 3.2, 3.4
 */
public interface EnhancedVisitService extends VisitService {
    
    /**
     * Find visits with sort and filter support
     * 
     * @param pageable Pagination and basic sort information
     * @param sortMetadata Enhanced sort metadata
     * @param filters List of filter criteria
     * @return Paginated response with sorted and filtered visits
     */
    PagedResponse<Visit> findVisitsWithSortAndFilter(
        Pageable pageable,
        SortMetadata sortMetadata,
        List<FilterCriteria> filters
    );
    
    /**
     * Get available visit status values for filter dropdown
     * Based on visit completion status (completed/pending)
     * 
     * @return List of available status values
     */
    List<String> getAvailableVisitStatusValues();
    
    /**
     * Get available visit type values for filter dropdown
     * 
     * @return List of available visit type values
     */
    List<String> getAvailableVisitTypeValues();
    
    /**
     * Find visits by completion status
     * 
     * @param completed true for completed visits, false for pending visits
     * @return List of visits matching the completion status
     */
    List<Visit> findVisitsByCompletionStatus(boolean completed);
    
    /**
     * Find visits by visit type
     * 
     * @param visitType Visit type to filter by
     * @return List of visits of the specified type
     */
    List<Visit> findVisitsByType(String visitType);
    
    /**
     * Execute bulk delete operation for visits
     * 
     * @param request Bulk delete request with visit IDs
     * @return Result of bulk delete operation
     */
    BulkOperationResult bulkDeleteVisits(BulkDeleteRequest request);
    
    /**
     * Get sortable columns for visits
     * 
     * @return List of sortable column names
     */
    List<String> getSortableVisitColumns();
    
    /**
     * Get filterable columns for visits
     * 
     * @return List of filterable column names
     */
    List<String> getFilterableVisitColumns();
    
    /**
     * Validate sort criteria for visits
     * 
     * @param sortMetadata Sort metadata to validate
     * @return true if sort criteria is valid, false otherwise
     */
    boolean validateVisitSortCriteria(SortMetadata sortMetadata);
    
    /**
     * Validate filter criteria for visits
     * 
     * @param filterCriteria Filter criteria to validate
     * @return true if filter criteria is valid, false otherwise
     */
    boolean validateVisitFilterCriteria(FilterCriteria filterCriteria);
}