package com.petclinic.backend.service;

import com.petclinic.backend.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * Service interface for handling table sorting errors and recovery
 * Provides fallback mechanisms and error recovery for table sorting operations
 * 
 * Validates: Requirements 6.2, 2.5
 */
public interface TableSortingErrorHandlingService {

    /**
     * Execute table sorting operation with fallback mechanism
     * 
     * @param sortingOperation The sorting operation to execute
     * @param entityType Type of entity being sorted
     * @param pageable Pagination parameters
     * @param sortColumn Sort column name
     * @param sortDirection Sort direction
     * @return PagedResponse with results or fallback data
     */
    PagedResponse<Object> executeSortingWithFallback(
            SortingOperation sortingOperation,
            String entityType,
            Pageable pageable,
            String sortColumn,
            String sortDirection);

    /**
     * Check if server-side sorting is available
     * 
     * @param entityType Type of entity to check
     * @return true if server-side sorting is available, false otherwise
     */
    boolean isServerSideSortingAvailable(String entityType);

    /**
     * Get fallback data with client-side sorting instructions
     * 
     * @param entityType Type of entity
     * @param pageable Pagination parameters
     * @return PagedResponse with unsorted data and client-side sorting metadata
     */
    PagedResponse<Object> getFallbackDataWithClientSideSort(String entityType, Pageable pageable);

    /**
     * Get user-friendly error message for sorting failures
     * 
     * @param entityType Type of entity that failed to sort
     * @param error The original error
     * @return User-friendly error message
     */
    String getSortingErrorMessage(String entityType, Throwable error);

    /**
     * Log detailed sorting error information
     * 
     * @param entityType Type of entity
     * @param sortColumn Sort column
     * @param sortDirection Sort direction
     * @param error The error that occurred
     * @param attemptNumber The retry attempt number
     */
    void logSortingError(String entityType, String sortColumn, String sortDirection, 
                        Throwable error, int attemptNumber);

    /**
     * Maintain previous state on sorting failures
     * 
     * @param entityType Type of entity
     * @param previousState Previous successful state
     * @return PagedResponse with previous state maintained
     */
    PagedResponse<Object> maintainPreviousState(String entityType, Map<String, Object> previousState);

    /**
     * Functional interface for sorting operations
     */
    @FunctionalInterface
    interface SortingOperation {
        PagedResponse<Object> execute() throws Exception;
    }
}