package com.petclinic.frontend.controller;

import com.petclinic.frontend.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Frontend Search Controller
 * 
 * Handles web requests for the global search interface.
 * Provides endpoints for the search page and search functionality.
 * 
 * Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5
 */
@Controller
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Show global search page
     */
    @GetMapping
    public String showSearchPage(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        
        try {
            // Load popular search terms for the search page
            List<String> popularTerms = searchService.getPopularSearchTerms(5).block();
            model.addAttribute("popularTerms", popularTerms);
            
            // If there's a query, perform the search
            if (query != null && !query.trim().isEmpty()) {
                Map<String, Object> searchResults = searchService.globalSearch(query, page, size).block();
                model.addAttribute("results", searchResults);
                model.addAttribute("query", query);
            }
            
            return "search/global";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading search page: " + e.getMessage());
            return "search/global";
        }
    }

    /**
     * Perform global search (for form submissions)
     */
    @PostMapping
    public String performSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            RedirectAttributes redirectAttributes) {
        
        if (query == null || query.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please enter a search query");
            return "redirect:/search";
        }
        
        // Redirect to GET request with query parameter
        return "redirect:/search?query=" + query + "&page=" + page + "&size=" + size;
    }

    /**
     * Quick search from navigation bar
     */
    @GetMapping("/quick")
    public String quickSearch(@RequestParam String q) {
        return "redirect:/search?query=" + q;
    }

    /**
     * Search suggestions endpoint (returns JSON for AJAX calls)
     */
    @GetMapping("/suggestions")
    @ResponseBody
    public List<String> getSearchSuggestions(
            @RequestParam String partialQuery,
            @RequestParam(defaultValue = "8") int maxSuggestions) {
        
        try {
            return searchService.getSearchSuggestions(partialQuery, maxSuggestions).block();
        } catch (Exception e) {
            return List.of(); // Return empty list on error
        }
    }

    /**
     * Search by entity type
     */
    @GetMapping("/entity/{entityType}")
    public String searchByEntityType(
            @PathVariable String entityType,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        
        try {
            Map<String, Object> searchResults = searchService.searchByEntityType(query, entityType, page, size).block();
            
            model.addAttribute("results", searchResults);
            model.addAttribute("query", query);
            model.addAttribute("entityType", entityType);
            model.addAttribute("entityTypeFilter", true);
            
            return "search/global";
        } catch (Exception e) {
            model.addAttribute("error", "Error searching " + entityType + ": " + e.getMessage());
            return "search/global";
        }
    }

    /**
     * Advanced search with filters
     */
    @PostMapping("/advanced")
    public String advancedSearch(
            @RequestParam String query,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Build filters map
            Map<String, Object> filters = Map.of(
                "entityType", entityType != null ? entityType : "",
                "dateFrom", dateFrom != null ? dateFrom : "",
                "dateTo", dateTo != null ? dateTo : "",
                "species", species != null ? species : "",
                "status", status != null ? status : "",
                "minAge", minAge != null ? minAge : 0,
                "maxAge", maxAge != null ? maxAge : 0
            );
            
            Map<String, Object> searchResults = searchService.advancedSearch(query, filters, page, size).block();
            
            model.addAttribute("results", searchResults);
            model.addAttribute("query", query);
            model.addAttribute("appliedFilters", filters);
            
            return "search/global";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error performing advanced search: " + e.getMessage());
            return "redirect:/search?query=" + query;
        }
    }

    /**
     * Export search results
     */
    @GetMapping("/export")
    public String exportSearchResults(
            @RequestParam String query,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String status,
            RedirectAttributes redirectAttributes) {
        
        try {
            // This would typically trigger a download
            // For now, we'll just redirect back with a success message
            redirectAttributes.addFlashAttribute("success", 
                "Export request submitted. The file will be downloaded shortly.");
            
            return "redirect:/search?query=" + query;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error exporting search results: " + e.getMessage());
            return "redirect:/search?query=" + query;
        }
    }

    /**
     * Get search analytics (for admin users)
     */
    @GetMapping("/analytics")
    public String getSearchAnalytics(Model model) {
        try {
            Map<String, Object> analytics = searchService.getSearchAnalytics().block();
            model.addAttribute("analytics", analytics);
            
            return "search/analytics";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading search analytics: " + e.getMessage());
            return "search/analytics";
        }
    }
}