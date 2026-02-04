package com.petclinic.backend.controller;

import com.petclinic.backend.dto.*;
import com.petclinic.backend.service.EnhancedTableService;
import com.petclinic.backend.service.EnhancedTableCacheService;
import com.petclinic.backend.service.ConcurrentOperationService;
import com.petclinic.backend.service.TableSortingErrorHandlingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Generic enhanced table controller providing sort, filter, and bulk operations
 * Implements server-side sorting and filtering with pagination support
 * Validates: Requirements 2.1, 2.4
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "${pet-clinic.cors.allowed-origins}")
@Validated
public class EnhancedTableController {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedTableController.class);
    
    private final EnhancedTableService<Object> enhancedTableService;
    private final EnhancedTableCacheService cacheService;
    private final ConcurrentOperationService concurrentOperationService;
    private final TableSortingErrorHandlingService tableSortingErrorHandlingService;
    
    public EnhancedTableController(EnhancedTableService<Object> enhancedTableService,
                                 EnhancedTableCacheService cacheService,
                                 ConcurrentOperationService concurrentOperationService,
                                 TableSortingErrorHandlingService tableSortingErrorHandlingService) {
        this.enhancedTableService = enhancedTableService;
        this.cacheService = cacheService;
        this.concurrentOperationService = concurrentOperationService;
        this.tableSortingErrorHandlingService = tableSortingErrorHandlingService;
    }

    /**
     * Generic endpoint for getting entities with sort and filter support
     * GET /api/v1/{entityType}?page=0&size=10&sortBy=id&sortDir=asc&filters=...
     * GET /api/v1/{entityType}?page=0&size=10&multiSort=column1:asc,column2:desc&filters=...
     * 
     * @param entityType Entity type (visits, owners, pets, veterinarians)
     * @param page Page number (0-based)
     * @param size Page size (1-100)
     * @param sortBy Sort column name (single column sorting)
     * @param sortDir Sort direction (asc/desc) (single column sorting)
     * @param multiSort Multi-column sort specification (column1:asc,column2:desc)
     * @param filters Filter parameters as request parameters
     * @return Paginated response with sort and filter metadata
     */
    @GetMapping("/{entityType}")
    public ResponseEntity<PagedResponse<Object>> getEntities(
            @PathVariable String entityType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) String multiSort,
            @RequestParam Map<String, String> filters) {
        
        logger.debug("Getting {} with pagination: page={}, size={}, sortBy={}, sortDir={}, multiSort={}, filters={}", 
                    entityType, page, size, sortBy, sortDir, multiSort, filters);
        
        try {
            // Validate entity type
            if (!isValidEntityType(entityType)) {
                logger.warn("Invalid entity type requested: {}", entityType);
                return ResponseEntity.badRequest().build();
            }
            
            // Validate sort parameters before processing
            if (sortBy != null && !isValidColumn(entityType, sortBy)) {
                logger.warn("Invalid sort column for {}: {}", entityType, sortBy);
                return ResponseEntity.badRequest().build();
            }
            
            if (sortDir != null && !isValidSortDirection(sortDir)) {
                logger.warn("Invalid sort direction: {}", sortDir);
                return ResponseEntity.badRequest().build();
            }
            
            // Create pageable with sort (prioritize multi-column sort if provided)
            Pageable pageable;
            SortMetadata sortMetadata = null;
            MultiColumnSortMetadata multiColumnSortMetadata = null;
            
            if (multiSort != null && !multiSort.trim().isEmpty()) {
                // Parse multi-column sort
                multiColumnSortMetadata = parseMultiColumnSort(multiSort, entityType);
                if (multiColumnSortMetadata.getSortCriteria().isEmpty()) {
                    logger.warn("Invalid multi-column sort specification: {}", multiSort);
                    return ResponseEntity.badRequest().build();
                }
                pageable = createPageableWithMultiSort(page, size, multiColumnSortMetadata);
                sortMetadata = multiColumnSortMetadata.toPrimarySortMetadata(); // For backward compatibility
            } else {
                // Single column sort
                pageable = createPageable(page, size, sortBy, sortDir);
                sortMetadata = createSortMetadata(sortBy, sortDir);
            }
            
            // Parse filters
            List<FilterCriteria> filterCriteria = parseFilters(filters, entityType);
            
            // Create filter metadata
            List<FilterMetadata> filterMetadata = createFilterMetadata(filterCriteria);
            
            // Get data from service with error handling
            PagedResponse<Object> response;
            if (multiColumnSortMetadata != null && !multiColumnSortMetadata.getSortCriteria().isEmpty()) {
                // Use multi-column sort service method with error handling
                final String primaryColumn = multiColumnSortMetadata.getSortCriteria().get(0).getColumn();
                final String primaryDirection = multiColumnSortMetadata.getSortCriteria().get(0).isAscending() ? "asc" : "desc";
                final String finalEntityType = entityType;
                final Pageable finalPageable = pageable;
                final MultiColumnSortMetadata finalMultiColumnSortMetadata = multiColumnSortMetadata;
                final List<FilterCriteria> finalFilterCriteria = filterCriteria;
                response = tableSortingErrorHandlingService.executeSortingWithFallback(
                    () -> enhancedTableService.findWithMultiColumnSortAndFilter(
                        finalEntityType, finalPageable, finalMultiColumnSortMetadata, finalFilterCriteria),
                    entityType,
                    pageable,
                    primaryColumn,
                    primaryDirection
                );
            } else {
                // Use single-column sort service method with error handling
                final String finalSortBy = sortBy;
                final String finalSortDir = sortDir;
                final String finalEntityType = entityType;
                final Pageable finalPageable = pageable;
                final SortMetadata finalSortMetadata = sortMetadata;
                final List<FilterCriteria> finalFilterCriteria = filterCriteria;
                response = tableSortingErrorHandlingService.executeSortingWithFallback(
                    () -> enhancedTableService.findWithSortAndFilter(
                        finalEntityType, finalPageable, finalSortMetadata, finalFilterCriteria),
                    entityType,
                    pageable,
                    finalSortBy,
                    finalSortDir
                );
            }
            
            // Set metadata
            response.setSortMetadata(sortMetadata);
            if (multiColumnSortMetadata != null) {
                response.setMultiColumnSortMetadata(multiColumnSortMetadata);
            }
            response.setActiveFilters(filterMetadata);
            
            // Check if there was a sorting error and handle appropriately
            if (response.isError()) {
                logger.warn("Table sorting failed for {}: {}", entityType, response.getErrorMessage());
                
                // Return appropriate HTTP status based on error type
                if ("SORTING_FALLBACK".equals(response.getErrorType())) {
                    // Client-side fallback - return 200 OK with warning
                    return ResponseEntity.ok(response);
                } else if ("CACHED_FALLBACK".equals(response.getErrorType())) {
                    // Using cached data - return 200 OK with warning
                    return ResponseEntity.ok(response);
                } else {
                    // Service unavailable - return 503
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
                }
            }
            
            logger.debug("Retrieved {} {} with {} total elements", 
                        response.getContent().size(), entityType, 
                        response.getPage().getTotalElements());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters for {}: {}", entityType, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error retrieving {} with sort/filter: {}", entityType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Bulk delete endpoint with concurrent operation safety
     * DELETE /api/v1/{entityType}/bulk
     * 
     * @param entityType Entity type
     * @param request Bulk delete request
     * @return Bulk operation result
     */
    @DeleteMapping("/{entityType}/bulk")
    public CompletableFuture<ResponseEntity<BulkOperationResult>> bulkDelete(
            @PathVariable String entityType,
            @Valid @RequestBody BulkDeleteRequest request) {
        
        logger.debug("Bulk delete request for {}: {}", entityType, request);
        
        try {
            // Validate entity type
            if (!isValidEntityType(entityType)) {
                logger.warn("Invalid entity type for bulk delete: {}", entityType);
                BulkOperationResult errorResult = new BulkOperationResult(false, 0, 0, entityType);
                errorResult.addError("Invalid entity type: " + entityType);
                return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(errorResult));
            }
            
            // Validate request
            if (!request.isValid()) {
                logger.warn("Invalid bulk delete request: {}", request);
                BulkOperationResult errorResult = new BulkOperationResult(false, 0, 0, entityType);
                errorResult.addError("Invalid bulk delete request");
                return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(errorResult));
            }
            
            // Set entity type in request if not already set
            if (request.getEntityType() == null) {
                request.setEntityType(entityType);
            }
            
            // Execute bulk delete with concurrent safety
            return concurrentOperationService.executeSafeBulkDelete(request)
                .thenApply(result -> {
                    logger.info("Bulk delete completed for {}: deleted {}/{} items", 
                               entityType, result.getDeletedCount(), result.getTotalRequested());
                    return ResponseEntity.ok(result);
                })
                .exceptionally(throwable -> {
                    logger.error("Error during bulk delete for {}: {}", entityType, throwable.getMessage(), throwable);
                    BulkOperationResult errorResult = new BulkOperationResult(false, 0, 
                                                                             request.getSelectedCount(), entityType);
                    errorResult.addError("Bulk delete failed: " + throwable.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
                });
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid bulk delete request for {}: {}", entityType, e.getMessage());
            BulkOperationResult errorResult = new BulkOperationResult(false, 0, 0, entityType);
            errorResult.addError("Invalid request: " + e.getMessage());
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(errorResult));
        } catch (Exception e) {
            logger.error("Error during bulk delete for {}: {}", entityType, e.getMessage(), e);
            BulkOperationResult errorResult = new BulkOperationResult(false, 0, 0, entityType);
            errorResult.addError("Internal server error: " + e.getMessage());
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult));
        }
    }

    /**
     * Get available filter values for a specific column
     * GET /api/v1/{entityType}/filter-values/{column}
     * 
     * @param entityType Entity type
     * @param column Column name
     * @return List of available filter values
     */
    @GetMapping("/{entityType}/filter-values/{column}")
    public ResponseEntity<List<String>> getFilterValues(
            @PathVariable String entityType,
            @PathVariable String column) {
        
        logger.debug("Getting filter values for {}.{}", entityType, column);
        
        try {
            // Validate entity type
            if (!isValidEntityType(entityType)) {
                logger.warn("Invalid entity type for filter values: {}", entityType);
                return ResponseEntity.badRequest().build();
            }
            
            // Validate column
            if (!isValidColumn(entityType, column)) {
                logger.warn("Invalid column for {}: {}", entityType, column);
                return ResponseEntity.badRequest().build();
            }
            
            List<String> filterValues = enhancedTableService.getAvailableFilterValues(entityType, column);
            
            logger.debug("Retrieved {} filter values for {}.{}", filterValues.size(), entityType, column);
            return ResponseEntity.ok(filterValues);
            
        } catch (Exception e) {
            logger.error("Error getting filter values for {}.{}: {}", entityType, column, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get bulk operation progress
     * GET /api/v1/{entityType}/bulk-progress/{operationId}
     * 
     * @param entityType Entity type
     * @param operationId Operation ID for tracking
     * @return Current progress of bulk operation
     */
    @GetMapping("/{entityType}/bulk-progress/{operationId}")
    public ResponseEntity<Map<String, Object>> getBulkProgress(
            @PathVariable String entityType,
            @PathVariable String operationId) {
        
        logger.debug("Getting bulk operation progress for {}: {}", entityType, operationId);
        
        try {
            // Validate entity type
            if (!isValidEntityType(entityType)) {
                logger.warn("Invalid entity type for progress check: {}", entityType);
                return ResponseEntity.badRequest().build();
            }
            
            // In a real implementation, this would check a progress tracking store
            // For now, return a simple response indicating the feature is available
            Map<String, Object> progress = Map.of(
                "operationId", operationId,
                "entityType", entityType,
                "status", "completed",
                "message", "Progress tracking available for large bulk operations"
            );
            
            return ResponseEntity.ok(progress);
            
        } catch (Exception e) {
            logger.error("Error getting bulk progress for {}: {}", entityType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    /**
     * Validate sort parameters
     * GET /api/v1/{entityType}/validate-sort?column={column}&direction={direction}
     * 
     * @param entityType Entity type
     * @param column Sort column
     * @param direction Sort direction
     * @return Validation result
     */
    @GetMapping("/{entityType}/validate-sort")
    public ResponseEntity<Map<String, Object>> validateSort(
            @PathVariable String entityType,
            @RequestParam String column,
            @RequestParam String direction) {
        
        logger.debug("Validating sort for {}: column={}, direction={}", entityType, column, direction);
        
        try {
            boolean isValidEntity = isValidEntityType(entityType);
            boolean isValidColumn = isValidColumn(entityType, column);
            boolean isValidDirection = isValidSortDirection(direction);
            
            boolean isValid = isValidEntity && isValidColumn && isValidDirection;
            
            Map<String, Object> result = Map.of(
                "valid", isValid,
                "validEntity", isValidEntity,
                "validColumn", isValidColumn,
                "validDirection", isValidDirection,
                "message", isValid ? "Sort parameters are valid" : "Invalid sort parameters"
            );
            
            logger.debug("Sort validation for {}: {}", entityType, result);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error validating sort for {}: {}", entityType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get table cache statistics
     * GET /api/v1/{entityType}/cache/statistics
     * 
     * @param entityType Entity type
     * @return Cache statistics
     */
    @GetMapping("/{entityType}/cache/statistics")
    public ResponseEntity<Map<String, Object>> getCacheStatistics(@PathVariable String entityType) {
        logger.debug("Getting cache statistics for {}", entityType);
        
        try {
            if (!isValidEntityType(entityType)) {
                return ResponseEntity.badRequest().build();
            }
            
            Map<String, Object> statistics = cacheService.getTableCacheStatistics();
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error getting cache statistics for {}: {}", entityType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Clear cache for entity type
     * POST /api/v1/{entityType}/cache/clear
     * 
     * @param entityType Entity type
     * @return Success message
     */
    @PostMapping("/{entityType}/cache/clear")
    public ResponseEntity<Map<String, String>> clearEntityCache(@PathVariable String entityType) {
        logger.info("Clearing cache for entity type: {}", entityType);
        
        try {
            if (!isValidEntityType(entityType)) {
                return ResponseEntity.badRequest().build();
            }
            
            cacheService.invalidateEntityCaches(entityType);
            
            Map<String, String> response = Map.of(
                "status", "success",
                "message", "Cache cleared for entity type: " + entityType
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error clearing cache for {}: {}", entityType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Warm up cache for entity type
     * POST /api/v1/{entityType}/cache/warm-up
     * 
     * @param entityType Entity type
     * @return Success message
     */
    @PostMapping("/{entityType}/cache/warm-up")
    public ResponseEntity<Map<String, String>> warmUpEntityCache(@PathVariable String entityType) {
        logger.info("Warming up cache for entity type: {}", entityType);
        
        try {
            if (!isValidEntityType(entityType)) {
                return ResponseEntity.badRequest().build();
            }
            
            cacheService.warmUpEntityCache(entityType);
            
            Map<String, String> response = Map.of(
                "status", "success",
                "message", "Cache warmed up for entity type: " + entityType
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error warming up cache for {}: {}", entityType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get concurrent operation statistics
     * GET /api/v1/{entityType}/concurrent/statistics
     * 
     * @param entityType Entity type
     * @return Concurrent operation statistics
     */
    @GetMapping("/{entityType}/concurrent/statistics")
    public ResponseEntity<Map<String, Object>> getConcurrentOperationStatistics(@PathVariable String entityType) {
        logger.debug("Getting concurrent operation statistics for {}", entityType);
        
        try {
            if (!isValidEntityType(entityType)) {
                return ResponseEntity.badRequest().build();
            }
            
            Map<String, Object> statistics = concurrentOperationService.getConcurrentOperationStatistics();
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error getting concurrent operation statistics for {}: {}", entityType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check operation status
     * GET /api/v1/{entityType}/concurrent/status/{requestKey}
     * 
     * @param entityType Entity type
     * @param requestKey Request key for the operation
     * @return Operation status
     */
    @GetMapping("/{entityType}/concurrent/status/{requestKey}")
    public ResponseEntity<BulkOperationResult> getOperationStatus(
            @PathVariable String entityType,
            @PathVariable String requestKey) {
        
        logger.debug("Getting operation status for {}: {}", entityType, requestKey);
        
        try {
            if (!isValidEntityType(entityType)) {
                return ResponseEntity.badRequest().build();
            }
            
            BulkOperationResult status = concurrentOperationService.getOperationStatus(requestKey);
            if (status != null) {
                return ResponseEntity.ok(status);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error getting operation status for {} {}: {}", entityType, requestKey, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Cancel an operation
     * POST /api/v1/{entityType}/concurrent/cancel/{requestKey}
     * 
     * @param entityType Entity type
     * @param requestKey Request key for the operation
     * @return Success message
     */
    @PostMapping("/{entityType}/concurrent/cancel/{requestKey}")
    public ResponseEntity<Map<String, Object>> cancelOperation(
            @PathVariable String entityType,
            @PathVariable String requestKey) {
        
        logger.info("Cancelling operation for {}: {}", entityType, requestKey);
        
        try {
            if (!isValidEntityType(entityType)) {
                return ResponseEntity.badRequest().build();
            }
            
            boolean cancelled = concurrentOperationService.cancelOperation(requestKey);
            
            Map<String, Object> response = Map.of(
                "cancelled", cancelled,
                "requestKey", requestKey,
                "message", cancelled ? "Operation cancelled successfully" : "Operation could not be cancelled"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error cancelling operation for {} {}: {}", entityType, requestKey, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Enable/disable caching for entity type
     * POST /api/v1/{entityType}/cache/toggle?enabled={enabled}
     * 
     * @param entityType Entity type
     * @param enabled True to enable caching
     * @return Success message
     */
    @PostMapping("/{entityType}/cache/toggle")
    public ResponseEntity<Map<String, Object>> toggleEntityCaching(
            @PathVariable String entityType,
            @RequestParam boolean enabled) {
        
        logger.info("Setting caching {} for entity type: {}", enabled ? "enabled" : "disabled", entityType);
        
        try {
            if (!isValidEntityType(entityType)) {
                return ResponseEntity.badRequest().build();
            }
            
            cacheService.setCachingEnabled(entityType, enabled);
            
            Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Caching " + (enabled ? "enabled" : "disabled") + " for entity type: " + entityType,
                "cachingEnabled", enabled
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error toggling caching for {}: {}", entityType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Helper Methods

    /**
     * Validate entity type
     */
    private boolean isValidEntityType(String entityType) {
        if (entityType == null) return false;
        
        String type = entityType.toLowerCase();
        return type.equals("visits") || type.equals("owners") || 
               type.equals("pets") || type.equals("veterinarians");
    }

    /**
     * Validate column name for entity type
     */
    private boolean isValidColumn(String entityType, String column) {
        if (entityType == null || column == null) return false;
        
        // Basic validation - in a real implementation, this would check against entity metadata
        String type = entityType.toLowerCase();
        String col = column.toLowerCase();
        
        switch (type) {
            case "visits":
                return col.equals("id") || col.equals("visitdate") || col.equals("visittype") || 
                       col.equals("diagnosis") || col.equals("treatment") || col.equals("cost") ||
                       col.equals("pet.name") || col.equals("veterinarian.lastname");
            case "owners":
                return col.equals("id") || col.equals("firstname") || col.equals("lastname") || 
                       col.equals("address") || col.equals("city") || col.equals("telephone");
            case "pets":
                return col.equals("id") || col.equals("name") || col.equals("birthdate") || 
                       col.equals("pettype") || col.equals("owner.lastname");
            case "veterinarians":
                return col.equals("id") || col.equals("firstname") || col.equals("lastname") || 
                       col.equals("specialties");
            default:
                return false;
        }
    }

    /**
     * Validate sort direction
     */
    private boolean isValidSortDirection(String direction) {
        return direction != null && 
               (direction.equalsIgnoreCase("asc") || direction.equalsIgnoreCase("desc"));
    }

    /**
     * Create Pageable with sort
     */
    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        if (sortBy != null && sortDir != null && isValidSortDirection(sortDir)) {
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
            Sort sort = Sort.by(direction, sortBy);
            return PageRequest.of(page, size, sort);
        } else {
            return PageRequest.of(page, size);
        }
    }
    
    /**
     * Create Pageable with multi-column sort
     */
    private Pageable createPageableWithMultiSort(int page, int size, MultiColumnSortMetadata multiSort) {
        if (multiSort == null || multiSort.getSortCriteria().isEmpty()) {
            return PageRequest.of(page, size);
        }
        
        List<Sort.Order> orders = new ArrayList<>();
        for (MultiColumnSortMetadata.SortCriterion criterion : multiSort.getSortCriteria()) {
            Sort.Direction direction = criterion.isAscending() ? Sort.Direction.ASC : Sort.Direction.DESC;
            orders.add(new Sort.Order(direction, criterion.getColumn()));
        }
        
        Sort sort = Sort.by(orders);
        return PageRequest.of(page, size, sort);
    }
    
    /**
     * Parse multi-column sort specification
     * Format: "column1:asc,column2:desc,column3:asc"
     */
    private MultiColumnSortMetadata parseMultiColumnSort(String multiSort, String entityType) {
        MultiColumnSortMetadata metadata = new MultiColumnSortMetadata();
        
        if (multiSort == null || multiSort.trim().isEmpty()) {
            return metadata;
        }
        
        String[] sortSpecs = multiSort.split(",");
        for (int i = 0; i < sortSpecs.length && i < 5; i++) { // Limit to 5 sort criteria
            String spec = sortSpecs[i].trim();
            String[] parts = spec.split(":");
            
            if (parts.length == 2) {
                String column = parts[0].trim();
                String direction = parts[1].trim();
                
                // Validate column and direction
                if (isValidColumn(entityType, column) && isValidSortDirection(direction)) {
                    metadata.addSecondarySortCriterion(column, direction);
                } else {
                    logger.warn("Invalid sort specification: {} for entity type: {}", spec, entityType);
                    // Don't add invalid criteria, but continue processing others
                }
            } else {
                logger.warn("Invalid sort specification format: {}", spec);
            }
        }
        
        return metadata;
    }

    /**
     * Parse filter parameters
     */
    private List<FilterCriteria> parseFilters(Map<String, String> filters, String entityType) {
        List<FilterCriteria> filterCriteria = new ArrayList<>();
        
        // Remove pagination and sort parameters
        filters.remove("page");
        filters.remove("size");
        filters.remove("sortBy");
        filters.remove("sortDir");
        
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            if (value != null && !value.trim().isEmpty()) {
                // Default to equals operator for simple filters
                FilterCriteria criteria = new FilterCriteria(key, "equals", value, entityType);
                filterCriteria.add(criteria);
            }
        }
        
        return filterCriteria;
    }

    /**
     * Create sort metadata
     */
    private SortMetadata createSortMetadata(String sortBy, String sortDir) {
        if (sortBy != null && sortDir != null) {
            return new SortMetadata(sortBy, sortDir, true);
        }
        return null;
    }

    /**
     * Create filter metadata
     */
    private List<FilterMetadata> createFilterMetadata(List<FilterCriteria> filterCriteria) {
        List<FilterMetadata> filterMetadata = new ArrayList<>();
        
        for (FilterCriteria criteria : filterCriteria) {
            FilterMetadata metadata = new FilterMetadata(
                criteria.getField(), 
                criteria.getOperator(), 
                criteria.getValue()
            );
            filterMetadata.add(metadata);
        }
        
        return filterMetadata;
    }
}