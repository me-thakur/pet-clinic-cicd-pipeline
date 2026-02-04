package com.petclinic.backend.service;

import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.model.Visit;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for handling search endpoint errors and recovery
 * Provides retry mechanisms and user-friendly error handling for search operations
 * 
 * Validates: Requirements 6.1, 6.5
 */
public interface SearchErrorHandlingService {

    /**
     * Execute search operation with retry mechanism and error handling
     * 
     * @param searchOperation The search operation to execute
     * @param searchType Type of search (treatment, diagnosis, description)
     * @param searchText The search text
     * @param pageable Pagination parameters
     * @return PagedResponse with results or error information
     */
    PagedResponse<Visit> executeSearchWithRetry(
            SearchOperation searchOperation, 
            String searchType, 
            String searchText, 
            Pageable pageable);

    /**
     * Check if search service is available
     * 
     * @return true if service is available, false otherwise
     */
    boolean isSearchServiceAvailable();

    /**
     * Get user-friendly error message for search failures
     * 
     * @param searchType Type of search that failed
     * @param error The original error
     * @return User-friendly error message
     */
    String getUserFriendlyErrorMessage(String searchType, Throwable error);

    /**
     * Log detailed error information for debugging
     * 
     * @param searchType Type of search that failed
     * @param searchText The search text
     * @param error The error that occurred
     * @param attemptNumber The retry attempt number
     */
    void logDetailedError(String searchType, String searchText, Throwable error, int attemptNumber);

    /**
     * Functional interface for search operations
     */
    @FunctionalInterface
    interface SearchOperation {
        PagedResponse<Visit> execute() throws Exception;
    }
}