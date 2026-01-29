package com.petclinic.backend.controller;

import com.petclinic.backend.dto.SearchResult;
import com.petclinic.backend.dto.SearchResultSummary;
import com.petclinic.backend.service.GlobalSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for global search functionality
 * Provides endpoints for cross-entity searching with highlighting and analytics
 * Validates: Requirements 4.1, 4.3, 4.4
 */
@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SearchController {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    
    private final GlobalSearchService globalSearchService;
    
    @Autowired
    public SearchController(GlobalSearchService globalSearchService) {
        this.globalSearchService = globalSearchService;
    }
    
    /**
     * Perform global search across all entities
     * @param query Search query string
     * @return SearchResultSummary with results from all entity types
     */
    @GetMapping
    public ResponseEntity<SearchResultSummary> globalSearch(@RequestParam String query) {
        
        logger.info("Global search request for query: '{}'", query);
        
        try {
            if (query == null || query.trim().isEmpty()) {
                logger.warn("Empty search query provided");
                return ResponseEntity.badRequest().build();
            }
            
            SearchResultSummary results = globalSearchService.globalSearch(query);
            
            // Save search query for analytics (assuming no user context for now)
            globalSearchService.saveSearchQuery(null, query);
            
            logger.info("Global search completed for query: '{}', found {} results", 
                       query, results.getTotalResults());
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("Error performing global search for query: '{}'", query, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Perform global search with pagination
     * @param query Search query string
     * @param page Page number (0-based)
     * @param size Page size
     * @return SearchResultSummary with paginated results
     */
    @GetMapping("/paginated")
    public ResponseEntity<SearchResultSummary> globalSearchPaginated(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        
        logger.info("Paginated global search request for query: '{}', page: {}, size: {}", query, page, size);
        
        try {
            if (query == null || query.trim().isEmpty()) {
                logger.warn("Empty search query provided");
                return ResponseEntity.badRequest().build();
            }
            
            SearchResultSummary results = globalSearchService.globalSearch(query, page, size);
            
            // Save search query for analytics
            globalSearchService.saveSearchQuery(null, query);
            
            logger.info("Paginated global search completed for query: '{}', found {} results on page {}", 
                       query, results.getResults().size(), page);
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("Error performing paginated global search for query: '{}'", query, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Perform global search with sorting and pagination
     * @param query Search query string
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Field to sort by (relevance, date, name, type)
     * @param sortDirection Sort direction (asc, desc)
     * @return SearchResultSummary with sorted and paginated results
     */
    @GetMapping("/advanced")
    public ResponseEntity<SearchResultSummary> advancedGlobalSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "relevance") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        logger.info("Advanced global search request for query: '{}', page: {}, size: {}, sortBy: {}, sortDirection: {}", 
                   query, page, size, sortBy, sortDirection);
        
        try {
            if (query == null || query.trim().isEmpty()) {
                logger.warn("Empty search query provided");
                return ResponseEntity.badRequest().build();
            }
            
            SearchResultSummary results = globalSearchService.globalSearch(query, page, size, sortBy, sortDirection);
            
            // Save search query for analytics
            globalSearchService.saveSearchQuery(null, query);
            
            logger.info("Advanced global search completed for query: '{}', found {} results", 
                       query, results.getTotalResults());
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("Error performing advanced global search for query: '{}'", query, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Search within a specific entity type
     * @param query Search query string
     * @param entityType Entity type to search (Pet, Visit, Veterinarian, Owner)
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of SearchResult objects for the specified entity type
     */
    @GetMapping("/entity/{entityType}")
    public ResponseEntity<Page<SearchResult>> searchByEntityType(
            @RequestParam String query,
            @PathVariable String entityType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        
        logger.info("Entity-specific search request for query: '{}', entityType: '{}', page: {}, size: {}", 
                   query, entityType, page, size);
        
        try {
            if (query == null || query.trim().isEmpty()) {
                logger.warn("Empty search query provided");
                return ResponseEntity.badRequest().build();
            }
            
            if (!isValidEntityType(entityType)) {
                logger.warn("Invalid entity type provided: '{}'", entityType);
                return ResponseEntity.notFound().build();
            }
            
            Page<SearchResult> results = globalSearchService.searchByEntityType(query, entityType, page, size);
            
            // Save search query for analytics
            globalSearchService.saveSearchQuery(null, query);
            
            logger.info("Entity-specific search completed for query: '{}', entityType: '{}', found {} results", 
                       query, entityType, results.getTotalElements());
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("Error performing entity-specific search for query: '{}', entityType: '{}'", query, entityType, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get search result counts by entity type
     * @param query Search query string
     * @return Map of entity type to result count
     */
    @GetMapping("/counts")
    public ResponseEntity<Map<String, Integer>> getSearchResultCounts(@RequestParam String query) {
        
        logger.info("Search counts request for query: '{}'", query);
        
        try {
            if (query == null || query.trim().isEmpty()) {
                logger.warn("Empty search query provided");
                return ResponseEntity.badRequest().build();
            }
            
            Map<String, Integer> counts = globalSearchService.getSearchResultCounts(query);
            
            logger.info("Search counts completed for query: '{}', total entities with results: {}", 
                       query, counts.size());
            
            return ResponseEntity.ok(counts);
            
        } catch (Exception e) {
            logger.error("Error getting search result counts for query: '{}'", query, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get search suggestions based on partial query
     * @param partialQuery Partial search query
     * @param maxSuggestions Maximum number of suggestions to return
     * @return List of search suggestions
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSearchSuggestions(
            @RequestParam String partialQuery,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int maxSuggestions) {
        
        logger.info("Search suggestions request for partial query: '{}', maxSuggestions: {}", partialQuery, maxSuggestions);
        
        try {
            if (partialQuery == null || partialQuery.trim().isEmpty()) {
                logger.warn("Empty partial query provided");
                return ResponseEntity.badRequest().build();
            }
            
            List<String> suggestions = globalSearchService.getSearchSuggestions(partialQuery, maxSuggestions);
            
            logger.info("Search suggestions completed for partial query: '{}', found {} suggestions", 
                       partialQuery, suggestions.size());
            
            return ResponseEntity.ok(suggestions);
            
        } catch (Exception e) {
            logger.error("Error getting search suggestions for partial query: '{}'", partialQuery, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get popular search terms
     * @param maxResults Maximum number of popular terms to return
     * @return List of popular search terms
     */
    @GetMapping("/popular")
    public ResponseEntity<List<String>> getPopularSearchTerms(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int maxResults) {
        
        logger.info("Popular search terms request, maxResults: {}", maxResults);
        
        try {
            List<String> popularTerms = globalSearchService.getPopularSearchTerms(maxResults);
            
            logger.info("Popular search terms completed, found {} terms", popularTerms.size());
            
            return ResponseEntity.ok(popularTerms);
            
        } catch (Exception e) {
            logger.error("Error getting popular search terms", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get search analytics and metrics
     * @return Map containing search analytics data
     */
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getSearchAnalytics() {
        
        logger.info("Search analytics request");
        
        try {
            Map<String, Object> analytics = globalSearchService.getSearchAnalytics();
            
            logger.info("Search analytics completed");
            
            return ResponseEntity.ok(analytics);
            
        } catch (Exception e) {
            logger.error("Error getting search analytics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Advanced search with filters
     * @param query Search query string
     * @param filters Request body containing filter criteria
     * @param page Page number (0-based)
     * @param size Page size
     * @return SearchResultSummary with filtered results
     */
    @PostMapping("/advanced")
    public ResponseEntity<SearchResultSummary> advancedSearchWithFilters(
            @RequestParam String query,
            @RequestBody(required = false) Map<String, Object> filters,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        
        logger.info("Advanced search with filters request for query: '{}', filters: {}, page: {}, size: {}", 
                   query, filters, page, size);
        
        try {
            if (query == null || query.trim().isEmpty()) {
                logger.warn("Empty search query provided");
                return ResponseEntity.badRequest().build();
            }
            
            SearchResultSummary results = globalSearchService.advancedSearch(query, filters, page, size);
            
            // Save search query for analytics
            globalSearchService.saveSearchQuery(null, query);
            
            logger.info("Advanced search with filters completed for query: '{}', found {} results", 
                       query, results.getTotalResults());
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("Error performing advanced search with filters for query: '{}'", query, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Helper methods
    
    private boolean isValidEntityType(String entityType) {
        if (entityType == null) {
            return false;
        }
        
        String lowerType = entityType.toLowerCase();
        return lowerType.equals("pet") || 
               lowerType.equals("visit") || 
               lowerType.equals("veterinarian") || 
               lowerType.equals("owner");
    }
}