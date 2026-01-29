package com.petclinic.backend.controller;

import com.petclinic.backend.dto.FilterCriteria;
import com.petclinic.backend.dto.FilterResult;
import com.petclinic.backend.dto.SearchResult;
import com.petclinic.backend.service.FilterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * REST controller for advanced filtering functionality
 * Validates: Requirements 4.2, 4.5
 */
@RestController
@RequestMapping("/api/filters")
@CrossOrigin(origins = "*")
public class FilterController {
    
    private static final Logger logger = LoggerFactory.getLogger(FilterController.class);
    
    @Autowired
    private FilterService filterService;
    
    /**
     * Apply filters to search results
     * POST /api/filters/apply
     */
    @PostMapping("/apply")
    public ResponseEntity<FilterResult> applyFilters(
            @Valid @RequestBody List<FilterCriteria> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        
        try {
            logger.info("Applying {} filters with pagination: page={}, size={}", filters.size(), page, size);
            
            FilterResult result = filterService.applyFilters(filters, page, size, sortBy, sortDirection);
            
            logger.info("Filter application completed: {} results found in {}ms", 
                       result.getTotalResults(), result.getExecutionTimeMs());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error applying filters: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    /**
     * Apply filters to a specific entity type
     * POST /api/filters/apply/{entityType}
     */
    @PostMapping("/apply/{entityType}")
    public ResponseEntity<Page<SearchResult>> applyFiltersToEntityType(
            @PathVariable String entityType,
            @Valid @RequestBody List<FilterCriteria> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            logger.info("Applying {} filters to entity type '{}' with pagination: page={}, size={}", 
                       filters.size(), entityType, page, size);
            
            Page<SearchResult> result = filterService.applyFiltersToEntityType(filters, entityType, page, size);
            
            logger.info("Entity-specific filter application completed: {} results found", result.getTotalElements());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error applying filters to entity type '{}': {}", entityType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    /**
     * Get recent filter combinations
     * GET /api/filters/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<List<List<FilterCriteria>>> getRecentFilters(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "10") int maxResults) {
        
        try {
            logger.info("Getting recent filter combinations for user {} (max: {})", userId, maxResults);
            
            List<List<FilterCriteria>> recentFilters = filterService.getRecentFilterCombinations(userId, maxResults);
            
            return ResponseEntity.ok(recentFilters);
            
        } catch (Exception e) {
            logger.error("Error getting recent filters: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get popular filter combinations
     * GET /api/filters/popular
     */
    @GetMapping("/popular")
    public ResponseEntity<List<Map<String, Object>>> getPopularFilters(
            @RequestParam(defaultValue = "10") int maxResults) {
        
        try {
            logger.info("Getting popular filter combinations (max: {})", maxResults);
            
            List<Map<String, Object>> popularFilters = filterService.getPopularFilterCombinations(maxResults);
            
            return ResponseEntity.ok(popularFilters);
            
        } catch (Exception e) {
            logger.error("Error getting popular filters: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Save filter combination for reuse
     * POST /api/filters/save
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveFilterCombination(
            @RequestParam(required = false) Long userId,
            @RequestParam String filterName,
            @Valid @RequestBody List<FilterCriteria> filters) {
        
        try {
            logger.info("Saving filter combination '{}' for user {} with {} filters", filterName, userId, filters.size());
            
            Long filterId = filterService.saveFilterCombination(userId, filterName, filters);
            
            Map<String, Object> response = Map.of(
                "filterId", filterId,
                "filterName", filterName,
                "message", "Filter combination saved successfully"
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            logger.error("Error saving filter combination: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get saved filter combinations
     * GET /api/filters/saved
     */
    @GetMapping("/saved")
    public ResponseEntity<Map<Long, String>> getSavedFilterCombinations(
            @RequestParam(required = false) Long userId) {
        
        try {
            logger.info("Getting saved filter combinations for user {}", userId);
            
            Map<Long, String> savedFilters = filterService.getSavedFilterCombinations(userId);
            
            return ResponseEntity.ok(savedFilters);
            
        } catch (Exception e) {
            logger.error("Error getting saved filter combinations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Load saved filter combination
     * GET /api/filters/saved/{id}
     */
    @GetMapping("/saved/{id}")
    public ResponseEntity<List<FilterCriteria>> loadFilterCombination(@PathVariable Long id) {
        
        try {
            logger.info("Loading filter combination with ID {}", id);
            
            List<FilterCriteria> filters = filterService.loadFilterCombination(id);
            
            if (filters.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(filters);
            
        } catch (Exception e) {
            logger.error("Error loading filter combination {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Delete saved filter combination
     * DELETE /api/filters/saved/{id}
     */
    @DeleteMapping("/saved/{id}")
    public ResponseEntity<Map<String, String>> deleteFilterCombination(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        
        try {
            logger.info("Deleting filter combination {} for user {}", id, userId);
            
            boolean deleted = filterService.deleteFilterCombination(id, userId);
            
            if (deleted) {
                Map<String, String> response = Map.of("message", "Filter combination deleted successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> response = Map.of("message", "Filter combination not found or access denied");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error deleting filter combination {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get available filter fields for an entity type
     * GET /api/filters/fields/{entityType}
     */
    @GetMapping("/fields/{entityType}")
    public ResponseEntity<Map<String, String>> getAvailableFields(@PathVariable String entityType) {
        
        try {
            logger.info("Getting available fields for entity type '{}'", entityType);
            
            Map<String, String> fields = filterService.getAvailableFields(entityType);
            
            if (fields.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(fields);
            
        } catch (Exception e) {
            logger.error("Error getting available fields for entity type '{}': {}", entityType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get available operators for a field type
     * GET /api/filters/operators/{fieldType}
     */
    @GetMapping("/operators/{fieldType}")
    public ResponseEntity<List<String>> getAvailableOperators(@PathVariable String fieldType) {
        
        try {
            logger.info("Getting available operators for field type '{}'", fieldType);
            
            List<String> operators = filterService.getAvailableOperators(fieldType);
            
            return ResponseEntity.ok(operators);
            
        } catch (Exception e) {
            logger.error("Error getting available operators for field type '{}': {}", fieldType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Validate filter criteria
     * POST /api/filters/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateFilters(@Valid @RequestBody List<FilterCriteria> filters) {
        
        try {
            logger.info("Validating {} filter criteria", filters.size());
            
            List<String> validationErrors = filterService.validateFilters(filters);
            
            Map<String, Object> response = Map.of(
                "valid", validationErrors.isEmpty(),
                "errors", validationErrors,
                "filterCount", filters.size()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error validating filters: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Combine search query with filters
     * POST /api/filters/search
     */
    @PostMapping("/search")
    public ResponseEntity<FilterResult> combineSearchAndFilters(
            @RequestParam String query,
            @Valid @RequestBody List<FilterCriteria> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            logger.info("Combining search query '{}' with {} filters", query, filters.size());
            
            FilterResult result = filterService.combineSearchAndFilters(query, filters, page, size);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error combining search and filters: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    /**
     * Get filter analytics
     * GET /api/filters/analytics
     */
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getFilterAnalytics() {
        
        try {
            logger.info("Getting filter analytics");
            
            Map<String, Object> analytics = filterService.getFilterAnalytics();
            
            return ResponseEntity.ok(analytics);
            
        } catch (Exception e) {
            logger.error("Error getting filter analytics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Export filtered results
     * POST /api/filters/export
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportFilteredResults(
            @Valid @RequestBody List<FilterCriteria> filters,
            @RequestParam(defaultValue = "csv") String format) {
        
        try {
            logger.info("Exporting filtered results with {} filters in {} format", filters.size(), format);
            
            byte[] exportData = filterService.exportFilteredResults(filters, format);
            
            String filename = "filtered_results." + format.toLowerCase();
            MediaType mediaType = "csv".equalsIgnoreCase(format) ? 
                    MediaType.parseMediaType("text/csv") : 
                    MediaType.TEXT_PLAIN;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(exportData);
            
        } catch (Exception e) {
            logger.error("Error exporting filtered results: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Clear filter history for a user
     * DELETE /api/filters/history
     */
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, String>> clearFilterHistory(@RequestParam(required = false) Long userId) {
        
        try {
            logger.info("Clearing filter history for user {}", userId);
            
            filterService.clearFilterHistory(userId);
            
            Map<String, String> response = Map.of("message", "Filter history cleared successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error clearing filter history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}