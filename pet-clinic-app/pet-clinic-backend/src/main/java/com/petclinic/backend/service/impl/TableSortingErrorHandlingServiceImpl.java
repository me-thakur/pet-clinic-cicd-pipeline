package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.dto.SortMetadata;
import com.petclinic.backend.service.EnhancedTableService;
import com.petclinic.backend.service.TableSortingErrorHandlingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of TableSortingErrorHandlingService
 * Provides comprehensive error handling, fallback mechanisms, and state maintenance
 * for table sorting operations
 * 
 * Validates: Requirements 6.2, 2.5
 */
@Service
public class TableSortingErrorHandlingServiceImpl implements TableSortingErrorHandlingService {

    private static final Logger logger = LoggerFactory.getLogger(TableSortingErrorHandlingServiceImpl.class);
    
    private static final int MAX_RETRY_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 500; // 0.5 seconds
    
    // Cache for previous successful states
    private final Map<String, PagedResponse<Object>> previousStateCache = new ConcurrentHashMap<>();
    
    @Autowired
    private EnhancedTableService<Object> enhancedTableService;

    @Override
    public PagedResponse<Object> executeSortingWithFallback(
            SortingOperation sortingOperation,
            String entityType,
            Pageable pageable,
            String sortColumn,
            String sortDirection) {
        
        logger.debug("Executing sorting with fallback for {}: column={}, direction={}", 
                    entityType, sortColumn, sortDirection);
        
        Exception lastException = null;
        
        // Try server-side sorting with retry
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                logger.debug("Attempt {} of {} for server-side sorting of {}", attempt, MAX_RETRY_ATTEMPTS, entityType);
                
                PagedResponse<Object> result = sortingOperation.execute();
                
                if (attempt > 1) {
                    logger.info("Server-side sorting succeeded on attempt {} for {}", attempt, entityType);
                }
                
                // Cache successful result
                cacheSuccessfulState(entityType, result);
                
                return result;
                
            } catch (Exception e) {
                lastException = e;
                logSortingError(entityType, sortColumn, sortDirection, e, attempt);
                
                // Don't retry on the last attempt
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    break;
                }
                
                // Check if error is retryable
                if (!isRetryableError(e)) {
                    logger.warn("Non-retryable error encountered for {} sorting, stopping retries", entityType);
                    break;
                }
                
                // Wait before retry
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Retry delay interrupted for {} sorting", entityType);
                    break;
                }
            }
        }
        
        // Server-side sorting failed, fall back to client-side sorting
        logger.warn("Server-side sorting failed for {}, falling back to client-side sorting", entityType);
        return getFallbackDataWithClientSideSort(entityType, pageable);
    }

    @Override
    public boolean isServerSideSortingAvailable(String entityType) {
        try {
            // Simple health check - try to get a small page without sorting
            Pageable testPageable = PageRequest.of(0, 1);
            enhancedTableService.findWithSortAndFilter(entityType, testPageable, null, List.of());
            return true;
        } catch (Exception e) {
            logger.warn("Server-side sorting availability check failed for {}: {}", entityType, e.getMessage());
            return false;
        }
    }

    @Override
    public PagedResponse<Object> getFallbackDataWithClientSideSort(String entityType, Pageable pageable) {
        logger.info("Providing fallback data with client-side sorting instructions for {}", entityType);
        
        try {
            // Get unsorted data from the service
            Pageable unsortedPageable = PageRequest.of(0, pageable.getPageSize() * 10); // Get more data for client sorting
            PagedResponse<Object> unsortedData = enhancedTableService.findWithSortAndFilter(
                entityType, unsortedPageable, null, List.of());
            
            // Add client-side sorting metadata
            unsortedData.setError(true);
            unsortedData.setErrorMessage("Server-side sorting is temporarily unavailable. Data will be sorted on your device.");
            unsortedData.setErrorType("SORTING_FALLBACK");
            unsortedData.setRetryable(true);
            
            // Add fallback sorting metadata
            SortMetadata fallbackSort = new SortMetadata();
            fallbackSort.setClientSideFallback(true);
            fallbackSort.setFallbackMessage("Sorting will be performed on your device due to server issues.");
            unsortedData.setSortMetadata(fallbackSort);
            
            logger.debug("Provided {} unsorted records for client-side sorting of {}", 
                        unsortedData.getContent().size(), entityType);
            
            return unsortedData;
            
        } catch (Exception e) {
            logger.error("Failed to get fallback data for {}: {}", entityType, e.getMessage(), e);
            
            // Return previous state if available
            PagedResponse<Object> previousState = previousStateCache.get(entityType);
            if (previousState != null) {
                logger.info("Returning cached previous state for {}", entityType);
                PagedResponse<Object> cachedResponse = cloneResponse(previousState);
                cachedResponse.setError(true);
                cachedResponse.setErrorMessage("Using cached data due to server issues. Please refresh to try again.");
                cachedResponse.setErrorType("CACHED_FALLBACK");
                return cachedResponse;
            }
            
            // Last resort - return empty response with error
            return createEmptyErrorResponse(pageable, entityType, e);
        }
    }

    @Override
    public String getSortingErrorMessage(String entityType, Throwable error) {
        if (error == null) {
            return String.format("Table sorting is temporarily unavailable for %s. Data will be sorted on your device.", entityType);
        }
        
        // Categorize errors and provide appropriate messages
        if (error instanceof TimeoutException) {
            return String.format("Sorting %s is taking longer than expected. We've switched to device-based sorting for better performance.", entityType);
        }
        
        if (error instanceof SQLException) {
            return "We're experiencing database issues. Your data will be sorted on your device while we resolve this.";
        }
        
        if (error instanceof IllegalArgumentException) {
            return String.format("Invalid sorting parameters for %s. Please check your sort criteria.", entityType);
        }
        
        if (error.getMessage() != null && error.getMessage().toLowerCase().contains("connection")) {
            return "We're experiencing connectivity issues. Your data will be sorted on your device.";
        }
        
        // Generic fallback message
        return String.format("Server-side sorting is temporarily unavailable for %s. We've switched to device-based sorting.", entityType);
    }

    @Override
    public void logSortingError(String entityType, String sortColumn, String sortDirection, 
                               Throwable error, int attemptNumber) {
        logger.error("Sorting error details - Entity: {}, Column: {}, Direction: {}, Attempt: {}, Error: {}, Message: {}", 
                    entityType, sortColumn, sortDirection, attemptNumber, 
                    error.getClass().getSimpleName(), error.getMessage());
        
        // Log stack trace for debugging (only on first attempt to avoid spam)
        if (attemptNumber == 1) {
            logger.debug("Full stack trace for {} sorting error:", entityType, error);
        }
        
        // Log additional context
        logger.error("Sorting context - Service available: {}, Thread: {}, Timestamp: {}", 
                    isServerSideSortingAvailable(entityType), Thread.currentThread().getName(), 
                    System.currentTimeMillis());
    }

    @Override
    public PagedResponse<Object> maintainPreviousState(String entityType, Map<String, Object> previousState) {
        logger.info("Maintaining previous state for {} due to sorting failure", entityType);
        
        PagedResponse<Object> cachedResponse = previousStateCache.get(entityType);
        if (cachedResponse != null) {
            PagedResponse<Object> response = cloneResponse(cachedResponse);
            response.setError(true);
            response.setErrorMessage("Maintaining previous view due to sorting issues. Please try again.");
            response.setErrorType("STATE_MAINTAINED");
            response.setRetryable(true);
            return response;
        }
        
        // If no previous state available, return empty response
        return createEmptyErrorResponse(PageRequest.of(0, 10), entityType, 
                                      new RuntimeException("No previous state available"));
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
     * Cache successful sorting state
     */
    private void cacheSuccessfulState(String entityType, PagedResponse<Object> response) {
        try {
            // Only cache if response is successful (no error)
            if (!response.isError()) {
                previousStateCache.put(entityType, cloneResponse(response));
                logger.debug("Cached successful state for {}", entityType);
            }
        } catch (Exception e) {
            logger.warn("Failed to cache state for {}: {}", entityType, e.getMessage());
        }
    }

    /**
     * Clone a PagedResponse for caching
     */
    private PagedResponse<Object> cloneResponse(PagedResponse<Object> original) {
        PagedResponse<Object> clone = new PagedResponse<>();
        clone.setContent(original.getContent());
        clone.setPage(original.getPage());
        clone.setSortMetadata(original.getSortMetadata());
        clone.setMultiColumnSortMetadata(original.getMultiColumnSortMetadata());
        clone.setActiveFilters(original.getActiveFilters());
        return clone;
    }

    /**
     * Create an empty error response
     */
    private PagedResponse<Object> createEmptyErrorResponse(Pageable pageable, String entityType, Exception error) {
        Page<Object> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        PagedResponse<Object> response = new PagedResponse<>(emptyPage);
        
        response.setError(true);
        response.setErrorMessage(getSortingErrorMessage(entityType, error));
        response.setErrorType("SORTING_SERVICE_UNAVAILABLE");
        response.setRetryable(true);
        
        return response;
    }
}