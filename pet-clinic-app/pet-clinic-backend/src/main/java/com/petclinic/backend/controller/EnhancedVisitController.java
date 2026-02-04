package com.petclinic.backend.controller;

import com.petclinic.backend.dto.*;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.service.EnhancedVisitService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * Enhanced visits controller with server-side sorting, filtering, and bulk operations
 * Extends the base visit functionality with enhanced table capabilities
 * Validates: Requirements 3.1, 3.2
 */
@RestController
@RequestMapping("/api/v1/visits")
@CrossOrigin(origins = "${pet-clinic.cors.allowed-origins}")
@Validated
public class EnhancedVisitController {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedVisitController.class);
    
    @Autowired
    private EnhancedVisitService enhancedVisitService;

    /**
     * Get visits with enhanced table functionality (sort, filter, pagination)
     * GET /api/v1/visits?page=0&size=10&sortBy=visitDate&sortDir=desc&status=completed
     * 
     * @param page Page number (0-based)
     * @param size Page size (1-100)
     * @param sortBy Sort column name
     * @param sortDir Sort direction (asc/desc)
     * @param status Visit completion status filter (completed/pending)
     * @param visitType Visit type filter
     * @param filters Additional filter parameters
     * @return Paginated response with sort and filter metadata
     */
    @GetMapping
    public ResponseEntity<PagedResponse<Visit>> getVisitsWithEnhancements(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String visitType,
            @RequestParam Map<String, String> filters) {
        
        logger.debug("Getting visits with enhancements: page={}, size={}, sortBy={}, sortDir={}, status={}, visitType={}, filters={}", 
                    page, size, sortBy, sortDir, status, visitType, filters);
        
        try {
            // Create pageable with sort
            Pageable pageable = createPageable(page, size, sortBy, sortDir);
            
            // Parse filters including status and visitType
            List<FilterCriteria> filterCriteria = parseVisitFilters(filters, status, visitType);
            
            // Create sort metadata
            SortMetadata sortMetadata = createSortMetadata(sortBy, sortDir);
            
            // Create filter metadata
            List<FilterMetadata> filterMetadata = createFilterMetadata(filterCriteria);
            
            // Get data from service
            PagedResponse<Visit> response = enhancedVisitService.findVisitsWithSortAndFilter(
                pageable, sortMetadata, filterCriteria);
            
            // Set metadata
            response.setSortMetadata(sortMetadata);
            response.setActiveFilters(filterMetadata);
            
            logger.debug("Retrieved {} visits with {} total elements", 
                        response.getContent().size(), 
                        response.getPage().getTotalElements());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters for visits: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error retrieving visits with sort/filter: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get available visit status values for filter dropdown
     * GET /api/v1/visits/filter-values/status
     * 
     * @return List of available status values
     */
    @GetMapping("/filter-values/status")
    public ResponseEntity<List<String>> getVisitStatusValues() {
        logger.debug("Getting available visit status values");
        
        try {
            // Visit status is based on completion: completed or pending
            List<String> statusValues = enhancedVisitService.getAvailableVisitStatusValues();
            
            logger.debug("Retrieved {} status values", statusValues.size());
            return ResponseEntity.ok(statusValues);
            
        } catch (Exception e) {
            logger.error("Error getting visit status values: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get available visit type values for filter dropdown
     * GET /api/v1/visits/filter-values/visitType
     * 
     * @return List of available visit type values
     */
    @GetMapping("/filter-values/visitType")
    public ResponseEntity<List<String>> getVisitTypeValues() {
        logger.debug("Getting available visit type values");
        
        try {
            List<String> visitTypeValues = enhancedVisitService.getAvailableVisitTypeValues();
            
            logger.debug("Retrieved {} visit type values", visitTypeValues.size());
            return ResponseEntity.ok(visitTypeValues);
            
        } catch (Exception e) {
            logger.error("Error getting visit type values: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Bulk delete visits
     * DELETE /api/v1/visits/bulk
     * 
     * @param request Bulk delete request
     * @return Bulk operation result
     */
    @DeleteMapping("/bulk")
    public ResponseEntity<BulkOperationResult> bulkDeleteVisits(
            @Valid @RequestBody BulkDeleteRequest request) {
        
        logger.debug("Bulk delete visits request: {}", request);
        
        try {
            // Validate request
            if (!request.isValid()) {
                logger.warn("Invalid bulk delete request: {}", request);
                return ResponseEntity.badRequest().build();
            }
            
            // Set entity type
            request.setEntityType("visits");
            
            // Execute bulk delete
            BulkOperationResult result = enhancedVisitService.bulkDeleteVisits(request);
            
            logger.info("Bulk delete completed for visits: deleted {}/{} items", 
                       result.getDeletedCount(), result.getTotalRequested());
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid bulk delete request for visits: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error during bulk delete for visits: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Validate sort parameters for visits
     * GET /api/v1/visits/validate-sort?column={column}&direction={direction}
     * 
     * @param column Sort column
     * @param direction Sort direction
     * @return Validation result
     */
    @GetMapping("/validate-sort")
    public ResponseEntity<Map<String, Object>> validateVisitSort(
            @RequestParam String column,
            @RequestParam String direction) {
        
        logger.debug("Validating visit sort: column={}, direction={}", column, direction);
        
        try {
            boolean isValidColumn = isValidVisitSortColumn(column);
            boolean isValidDirection = isValidSortDirection(direction);
            
            boolean isValid = isValidColumn && isValidDirection;
            
            Map<String, Object> result = Map.of(
                "valid", isValid,
                "validColumn", isValidColumn,
                "validDirection", isValidDirection,
                "message", isValid ? "Sort parameters are valid" : "Invalid sort parameters"
            );
            
            logger.debug("Visit sort validation: {}", result);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error validating visit sort: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Helper Methods

    /**
     * Validate visit-specific sort columns
     */
    private boolean isValidVisitSortColumn(String column) {
        if (column == null) return false;
        
        String col = column.toLowerCase();
        return col.equals("id") || col.equals("visitdate") || col.equals("visittype") || 
               col.equals("diagnosis") || col.equals("treatment") || col.equals("cost") ||
               col.equals("pet.name") || col.equals("veterinarian.lastname") ||
               col.equals("duration") || col.equals("notes");
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
     * Parse visit-specific filter parameters
     */
    private List<FilterCriteria> parseVisitFilters(Map<String, String> filters, String status, String visitType) {
        List<FilterCriteria> filterCriteria = new ArrayList<>();
        
        // Remove pagination and sort parameters
        filters.remove("page");
        filters.remove("size");
        filters.remove("sortBy");
        filters.remove("sortDir");
        filters.remove("status");
        filters.remove("visitType");
        
        // Add status filter if provided
        if (status != null && !status.trim().isEmpty()) {
            FilterCriteria statusCriteria = new FilterCriteria("status", "equals", status, "visits");
            filterCriteria.add(statusCriteria);
        }
        
        // Add visit type filter if provided
        if (visitType != null && !visitType.trim().isEmpty()) {
            FilterCriteria typeCriteria = new FilterCriteria("visitType", "equals", visitType, "visits");
            filterCriteria.add(typeCriteria);
        }
        
        // Add other filters
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            if (value != null && !value.trim().isEmpty()) {
                FilterCriteria criteria = new FilterCriteria(key, "equals", value, "visits");
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