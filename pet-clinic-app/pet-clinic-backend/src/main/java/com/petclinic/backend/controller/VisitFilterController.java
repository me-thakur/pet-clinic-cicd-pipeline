package com.petclinic.backend.controller;

import com.petclinic.backend.dto.FilterCriteria;
import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.service.VisitFilterService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for visit filtering functionality
 * Provides comprehensive filtering capabilities for visits
 * 
 * Validates: Requirements 13.1, 13.2, 13.3, 13.4, 13.5
 */
@RestController
@RequestMapping("/api/visits/filter")
@CrossOrigin(origins = "${pet-clinic.cors.allowed-origins}")
@Validated
public class VisitFilterController {

    private static final Logger logger = LoggerFactory.getLogger(VisitFilterController.class);

    @Autowired
    private VisitFilterService visitFilterService;

    /**
     * Apply multiple filters to visits
     * POST /api/visits/filter/apply
     * 
     * @param filterRequest Map containing filter criteria and pagination
     * @return Paginated filtered visits with metadata
     */
    @PostMapping("/apply")
    public ResponseEntity<PagedResponse<Visit>> applyFilters(@RequestBody Map<String, Object> filterRequest) {
        logger.debug("Applying visit filters: {}", filterRequest);

        try {
            // Extract pagination parameters
            int page = (Integer) filterRequest.getOrDefault("page", 0);
            int size = Math.min((Integer) filterRequest.getOrDefault("size", 10), 100);
            String sortBy = (String) filterRequest.get("sortBy");
            String sortDir = (String) filterRequest.getOrDefault("sortDir", "desc");

            // Create pageable
            Pageable pageable = createPageable(page, size, sortBy, sortDir);

            // Extract and parse filters
            List<FilterCriteria> filters = parseFilters(filterRequest);

            // Apply filters
            PagedResponse<Visit> response = visitFilterService.applyFilters(filters, pageable);

            logger.debug("Applied {} filters, returned {} visits", filters.size(), 
                        response.getContent().size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error applying visit filters: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Filter visits by date range
     * GET /api/visits/filter/date-range?startDate={startDate}&endDate={endDate}&page=0&size=10
     * 
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDir Sort direction
     * @return Paginated visits within date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<PagedResponse<Visit>> filterByDateRange(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        logger.debug("Filtering visits by date range: {} to {}, page: {}, size: {}", 
                    startDate, endDate, page, size);

        try {
            Pageable pageable = createPageable(page, size, sortBy, sortDir);
            PagedResponse<Visit> response = visitFilterService.filterByDateRange(startDate, endDate, pageable);

            logger.debug("Date range filter returned {} visits", response.getContent().size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error filtering visits by date range: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Filter visits by visit type
     * GET /api/visits/filter/visit-type?type={type}&page=0&size=10
     * 
     * @param type Visit type to filter by
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDir Sort direction
     * @return Paginated visits of specified type
     */
    @GetMapping("/visit-type")
    public ResponseEntity<PagedResponse<Visit>> filterByVisitType(
            @RequestParam String type,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        logger.debug("Filtering visits by visit type: {}, page: {}, size: {}", type, page, size);

        try {
            Pageable pageable = createPageable(page, size, sortBy, sortDir);
            PagedResponse<Visit> response = visitFilterService.filterByVisitType(type, pageable);

            logger.debug("Visit type filter returned {} visits", response.getContent().size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error filtering visits by visit type: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Filter visits by completion status
     * GET /api/visits/filter/status?status={status}&page=0&size=10
     * 
     * @param status Status to filter by (completed, pending, cancelled, emergency)
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDir Sort direction
     * @return Paginated visits with specified status
     */
    @GetMapping("/status")
    public ResponseEntity<PagedResponse<Visit>> filterByStatus(
            @RequestParam String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        logger.debug("Filtering visits by status: {}, page: {}, size: {}", status, page, size);

        try {
            Pageable pageable = createPageable(page, size, sortBy, sortDir);
            PagedResponse<Visit> response = visitFilterService.filterByStatus(status, pageable);

            logger.debug("Status filter returned {} visits", response.getContent().size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error filtering visits by status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Filter visits by pet
     * GET /api/visits/filter/pet?petId={petId}&page=0&size=10
     * 
     * @param petId Pet ID to filter by
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDir Sort direction
     * @return Paginated visits for specified pet
     */
    @GetMapping("/pet")
    public ResponseEntity<PagedResponse<Visit>> filterByPet(
            @RequestParam Long petId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        logger.debug("Filtering visits by pet ID: {}, page: {}, size: {}", petId, page, size);

        try {
            Pageable pageable = createPageable(page, size, sortBy, sortDir);
            PagedResponse<Visit> response = visitFilterService.filterByPet(petId, pageable);

            logger.debug("Pet filter returned {} visits", response.getContent().size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error filtering visits by pet: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Filter visits by veterinarian
     * GET /api/visits/filter/veterinarian?veterinarianId={veterinarianId}&page=0&size=10
     * 
     * @param veterinarianId Veterinarian ID to filter by
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDir Sort direction
     * @return Paginated visits for specified veterinarian
     */
    @GetMapping("/veterinarian")
    public ResponseEntity<PagedResponse<Visit>> filterByVeterinarian(
            @RequestParam Long veterinarianId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        logger.debug("Filtering visits by veterinarian ID: {}, page: {}, size: {}", 
                    veterinarianId, page, size);

        try {
            Pageable pageable = createPageable(page, size, sortBy, sortDir);
            PagedResponse<Visit> response = visitFilterService.filterByVeterinarian(veterinarianId, pageable);

            logger.debug("Veterinarian filter returned {} visits", response.getContent().size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error filtering visits by veterinarian: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get available filter values for dropdown population
     * GET /api/visits/filter/values/{filterType}
     * 
     * @param filterType Type of filter (status, visitType, pets, veterinarians)
     * @return List of available values for the filter
     */
    @GetMapping("/values/{filterType}")
    public ResponseEntity<List<String>> getFilterValues(@PathVariable String filterType) {
        logger.debug("Getting filter values for type: {}", filterType);

        try {
            List<String> values = visitFilterService.getAvailableFilterValues(filterType);
            
            logger.debug("Retrieved {} values for filter type: {}", values.size(), filterType);
            return ResponseEntity.ok(values);

        } catch (Exception e) {
            logger.error("Error getting filter values for type {}: {}", filterType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get filter statistics for result highlighting
     * GET /api/visits/filter/statistics
     * 
     * @return Map of filter statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getFilterStatistics() {
        logger.debug("Getting visit filter statistics");

        try {
            Map<String, Object> statistics = visitFilterService.getFilterStatistics(new ArrayList<>());
            
            logger.debug("Retrieved filter statistics with {} entries", statistics.size());
            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            logger.error("Error getting filter statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Clear all active filters
     * DELETE /api/visits/filter/clear
     * 
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDir Sort direction
     * @return Paginated list of all visits
     */
    @DeleteMapping("/clear")
    public ResponseEntity<PagedResponse<Visit>> clearAllFilters(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        logger.debug("Clearing all visit filters, page: {}, size: {}", page, size);

        try {
            Pageable pageable = createPageable(page, size, sortBy, sortDir);
            PagedResponse<Visit> response = visitFilterService.clearAllFilters(pageable);

            logger.debug("Cleared all filters, returned {} visits", response.getContent().size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error clearing all filters: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Validate filter criteria
     * POST /api/visits/filter/validate
     * 
     * @param criteria Filter criteria to validate
     * @return Validation result
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateFilter(@RequestBody FilterCriteria criteria) {
        logger.debug("Validating filter criteria: {}", criteria);

        try {
            boolean isValid = visitFilterService.validateFilterCriteria(criteria);
            
            Map<String, Object> result = Map.of(
                "valid", isValid,
                "field", criteria.getField(),
                "value", criteria.getValue(),
                "message", isValid ? "Filter criteria is valid" : "Invalid filter criteria"
            );

            logger.debug("Filter validation result: {}", result);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error validating filter criteria: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Helper Methods

    /**
     * Create Pageable with sort
     */
    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        if (sortBy != null && sortDir != null) {
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
            Sort sort = Sort.by(direction, sortBy);
            return PageRequest.of(page, size, sort);
        } else {
            // Default sort by visitDate descending
            Sort sort = Sort.by(Sort.Direction.DESC, "visitDate");
            return PageRequest.of(page, size, sort);
        }
    }

    /**
     * Parse filters from request map
     */
    @SuppressWarnings("unchecked")
    private List<FilterCriteria> parseFilters(Map<String, Object> filterRequest) {
        List<FilterCriteria> filters = new ArrayList<>();

        // Extract individual filter parameters
        String status = (String) filterRequest.get("status");
        String visitType = (String) filterRequest.get("visitType");
        Object petIdObj = filterRequest.get("petId");
        Object veterinarianIdObj = filterRequest.get("veterinarianId");
        String startDate = (String) filterRequest.get("startDate");
        String endDate = (String) filterRequest.get("endDate");

        // Add filters if present
        if (status != null && !status.trim().isEmpty()) {
            filters.add(new FilterCriteria("status", "equals", status, "visits"));
        }

        if (visitType != null && !visitType.trim().isEmpty()) {
            filters.add(new FilterCriteria("visitType", "equals", visitType, "visits"));
        }

        if (petIdObj != null) {
            String petId = petIdObj.toString();
            if (!petId.trim().isEmpty()) {
                filters.add(new FilterCriteria("petId", "equals", petId, "visits"));
            }
        }

        if (veterinarianIdObj != null) {
            String veterinarianId = veterinarianIdObj.toString();
            if (!veterinarianId.trim().isEmpty()) {
                filters.add(new FilterCriteria("veterinarianId", "equals", veterinarianId, "visits"));
            }
        }

        if (startDate != null && !startDate.trim().isEmpty()) {
            filters.add(new FilterCriteria("startDate", "greaterThanOrEqual", startDate, "visits"));
        }

        if (endDate != null && !endDate.trim().isEmpty()) {
            filters.add(new FilterCriteria("endDate", "lessThanOrEqual", endDate, "visits"));
        }

        // Handle filters array if present
        Object filtersObj = filterRequest.get("filters");
        if (filtersObj instanceof List) {
            List<Map<String, Object>> filterMaps = (List<Map<String, Object>>) filtersObj;
            for (Map<String, Object> filterMap : filterMaps) {
                String field = (String) filterMap.get("field");
                String operator = (String) filterMap.get("operator");
                Object valueObj = filterMap.get("value");
                
                if (field != null && valueObj != null) {
                    String value = valueObj.toString();
                    filters.add(new FilterCriteria(field, operator != null ? operator : "equals", value, "visits"));
                }
            }
        }

        return filters;
    }
}