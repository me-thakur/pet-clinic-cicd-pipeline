package com.petclinic.backend.service;

import com.petclinic.backend.dto.FilterCriteria;
import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.model.Visit;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service interface for visit filtering functionality
 * Provides comprehensive filtering capabilities for visits
 * 
 * Validates: Requirements 13.1, 13.2, 13.3, 13.4, 13.5
 */
public interface VisitFilterService {

    /**
     * Apply multiple filters to visits with pagination
     * 
     * @param filters List of filter criteria to apply
     * @param pageable Pagination parameters
     * @return Paginated filtered visits with metadata
     */
    PagedResponse<Visit> applyFilters(List<FilterCriteria> filters, Pageable pageable);

    /**
     * Filter visits by date range
     * 
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param pageable Pagination parameters
     * @return Paginated visits within date range
     */
    PagedResponse<Visit> filterByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Filter visits by visit type
     * 
     * @param visitType Visit type to filter by
     * @param pageable Pagination parameters
     * @return Paginated visits of specified type
     */
    PagedResponse<Visit> filterByVisitType(String visitType, Pageable pageable);

    /**
     * Filter visits by completion status
     * 
     * @param status Status to filter by (completed, pending, cancelled, emergency)
     * @param pageable Pagination parameters
     * @return Paginated visits with specified status
     */
    PagedResponse<Visit> filterByStatus(String status, Pageable pageable);

    /**
     * Filter visits by pet
     * 
     * @param petId Pet ID to filter by
     * @param pageable Pagination parameters
     * @return Paginated visits for specified pet
     */
    PagedResponse<Visit> filterByPet(Long petId, Pageable pageable);

    /**
     * Filter visits by veterinarian
     * 
     * @param veterinarianId Veterinarian ID to filter by
     * @param pageable Pagination parameters
     * @return Paginated visits for specified veterinarian
     */
    PagedResponse<Visit> filterByVeterinarian(Long veterinarianId, Pageable pageable);

    /**
     * Get available filter values for dropdown population
     * 
     * @param filterType Type of filter (status, visitType, etc.)
     * @return List of available values for the filter
     */
    List<String> getAvailableFilterValues(String filterType);

    /**
     * Get filter statistics for result highlighting
     * 
     * @param filters Applied filters
     * @return Map of filter statistics
     */
    Map<String, Object> getFilterStatistics(List<FilterCriteria> filters);

    /**
     * Validate filter criteria
     * 
     * @param criteria Filter criteria to validate
     * @return true if valid, false otherwise
     */
    boolean validateFilterCriteria(FilterCriteria criteria);

    /**
     * Clear all active filters and return all visits
     * 
     * @param pageable Pagination parameters
     * @return Paginated list of all visits
     */
    PagedResponse<Visit> clearAllFilters(Pageable pageable);

    /**
     * Get active filter summary for display
     * 
     * @param filters List of active filters
     * @return Map containing filter summary information
     */
    Map<String, Object> getActiveFilterSummary(List<FilterCriteria> filters);
}