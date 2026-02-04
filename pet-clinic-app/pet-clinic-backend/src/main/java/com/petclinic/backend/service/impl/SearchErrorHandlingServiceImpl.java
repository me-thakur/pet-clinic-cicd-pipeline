package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.service.SearchErrorHandlingService;
import com.petclinic.backend.service.VisitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of SearchErrorHandlingService
 * Provides comprehensive error handling, retry mechanisms, and user-friendly error messages
 * for search operations
 * 
 * Validates: Requirements 6.1, 6.5
 */
@Service
public class SearchErrorHandlingServiceImpl implements SearchErrorHandlingService {

    private static final Logger logger = LoggerFactory.getLogger(SearchErrorHandlingServiceImpl.class);
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second
    private static final long BACKOFF_MULTIPLIER = 2;

    @Autowired
    private VisitService visitService;

    @Override
    public PagedResponse<Visit> executeSearchWithRetry(
            SearchOperation searchOperation, 
            String searchType, 
            String searchText, 
            Pageable pageable) {
        
        logger.debug("Executing {} search with retry mechanism for text: '{}'", searchType, searchText);
        
        Exception lastException = null;
        long delay = RETRY_DELAY_MS;
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                logger.debug("Attempt {} of {} for {} search", attempt, MAX_RETRY_ATTEMPTS, searchType);
                
                // Execute the search operation
                PagedResponse<Visit> result = searchOperation.execute();
                
                if (attempt > 1) {
                    logger.info("Search operation succeeded on attempt {} for {} search", attempt, searchType);
                }
                
                return result;
                
            } catch (Exception e) {
                lastException = e;
                logDetailedError(searchType, searchText, e, attempt);
                
                // Don't retry on the last attempt
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    break;
                }
                
                // Check if error is retryable
                if (!isRetryableError(e)) {
                    logger.warn("Non-retryable error encountered for {} search, stopping retries", searchType);
                    break;
                }
                
                // Wait before retry with exponential backoff
                try {
                    logger.debug("Waiting {}ms before retry attempt {} for {} search", delay, attempt + 1, searchType);
                    Thread.sleep(delay);
                    delay *= BACKOFF_MULTIPLIER;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Retry delay interrupted for {} search", searchType);
                    break;
                }
            }
        }
        
        // All retries failed, return empty result with error information
        logger.error("All retry attempts failed for {} search with text: '{}'", searchType, searchText);
        return createErrorPagedResponse(pageable, searchType, lastException);
    }

    @Override
    public boolean isSearchServiceAvailable() {
        try {
            // Simple health check - try to get visit count
            visitService.findAll();
            return true;
        } catch (Exception e) {
            logger.warn("Search service availability check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getUserFriendlyErrorMessage(String searchType, Throwable error) {
        if (error == null) {
            return String.format("Search service is temporarily unavailable. Please try your %s search again in a few moments.", searchType);
        }
        
        // Categorize errors and provide appropriate messages
        if (error instanceof TimeoutException) {
            return String.format("The %s search is taking longer than expected. Please try again with a more specific search term.", searchType);
        }
        
        if (error instanceof SQLException) {
            return "We're experiencing database connectivity issues. Please try your search again in a few moments.";
        }
        
        if (error instanceof IllegalArgumentException) {
            return String.format("Invalid search parameters provided for %s search. Please check your search criteria and try again.", searchType);
        }
        
        if (error.getMessage() != null && error.getMessage().toLowerCase().contains("connection")) {
            return "We're experiencing connectivity issues. Please check your internet connection and try again.";
        }
        
        if (error.getMessage() != null && error.getMessage().toLowerCase().contains("timeout")) {
            return String.format("The %s search timed out. Please try again with a more specific search term.", searchType);
        }
        
        // Generic error message
        return String.format("We encountered an issue with your %s search. Please try again, and if the problem persists, contact support.", searchType);
    }

    @Override
    public void logDetailedError(String searchType, String searchText, Throwable error, int attemptNumber) {
        logger.error("Search error details - Type: {}, Text: '{}', Attempt: {}, Error: {}, Message: {}", 
                    searchType, searchText, attemptNumber, error.getClass().getSimpleName(), error.getMessage());
        
        // Log stack trace for debugging (only on first attempt to avoid spam)
        if (attemptNumber == 1) {
            logger.debug("Full stack trace for {} search error:", searchType, error);
        }
        
        // Log additional context
        logger.error("Search context - Service available: {}, Thread: {}, Timestamp: {}", 
                    isSearchServiceAvailable(), Thread.currentThread().getName(), System.currentTimeMillis());
    }

    /**
     * Determine if an error is retryable
     */
    private boolean isRetryableError(Exception e) {
        // Don't retry validation errors or illegal arguments
        if (e instanceof IllegalArgumentException) {
            return false;
        }
        
        // Don't retry security-related errors
        if (e instanceof SecurityException) {
            return false;
        }
        
        // Retry database connection issues, timeouts, and temporary failures
        if (e instanceof SQLException || 
            e instanceof TimeoutException ||
            (e.getMessage() != null && (
                e.getMessage().toLowerCase().contains("connection") ||
                e.getMessage().toLowerCase().contains("timeout") ||
                e.getMessage().toLowerCase().contains("temporary")
            ))) {
            return true;
        }
        
        // Default to retrying unknown exceptions
        return true;
    }

    /**
     * Create an error response with empty results
     */
    private PagedResponse<Visit> createErrorPagedResponse(Pageable pageable, String searchType, Exception error) {
        Page<Visit> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        PagedResponse<Visit> response = new PagedResponse<>(emptyPage);
        
        // Add error information to the response
        response.setError(true);
        response.setErrorMessage(getUserFriendlyErrorMessage(searchType, error));
        response.setErrorType("SEARCH_SERVICE_UNAVAILABLE");
        response.setRetryable(isRetryableError(error));
        
        return response;
    }
}