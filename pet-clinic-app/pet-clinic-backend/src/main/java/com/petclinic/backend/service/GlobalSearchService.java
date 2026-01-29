package com.petclinic.backend.service;

import com.petclinic.backend.dto.SearchResult;
import com.petclinic.backend.dto.SearchResultSummary;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * Service interface for global search functionality across all entities
 * Provides cross-entity searching, highlighting, and result aggregation
 * Validates: Requirements 4.1, 4.3, 4.4
 */
public interface GlobalSearchService {
    
    /**
     * Perform global search across all entities
     * @param query Search query string
     * @return SearchResultSummary with results from all entity types
     */
    SearchResultSummary globalSearch(String query);
    
    /**
     * Perform global search with pagination
     * @param query Search query string
     * @param page Page number (0-based)
     * @param size Page size
     * @return SearchResultSummary with paginated results
     */
    SearchResultSummary globalSearch(String query, int page, int size);
    
    /**
     * Perform global search with sorting and pagination
     * @param query Search query string
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Field to sort by (relevance, date, name)
     * @param sortDirection Sort direction (asc, desc)
     * @return SearchResultSummary with sorted and paginated results
     */
    SearchResultSummary globalSearch(String query, int page, int size, String sortBy, String sortDirection);
    
    /**
     * Search pets by various criteria with highlighting
     * @param query Search query string
     * @return List of SearchResult objects for pets
     */
    List<SearchResult> searchPets(String query);
    
    /**
     * Search visits by various criteria with highlighting
     * @param query Search query string
     * @return List of SearchResult objects for visits
     */
    List<SearchResult> searchVisits(String query);
    
    /**
     * Search veterinarians by various criteria with highlighting
     * @param query Search query string
     * @return List of SearchResult objects for veterinarians
     */
    List<SearchResult> searchVeterinarians(String query);
    
    /**
     * Search owners by various criteria with highlighting
     * @param query Search query string
     * @return List of SearchResult objects for owners
     */
    List<SearchResult> searchOwners(String query);
    
    /**
     * Get search suggestions based on partial query
     * @param partialQuery Partial search query
     * @param maxSuggestions Maximum number of suggestions to return
     * @return List of search suggestions
     */
    List<String> getSearchSuggestions(String partialQuery, int maxSuggestions);
    
    /**
     * Get search result counts by entity type
     * @param query Search query string
     * @return Map of entity type to result count
     */
    Map<String, Integer> getSearchResultCounts(String query);
    
    /**
     * Search within a specific entity type
     * @param query Search query string
     * @param entityType Entity type to search (Pet, Visit, Veterinarian, Owner)
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of SearchResult objects for the specified entity type
     */
    Page<SearchResult> searchByEntityType(String query, String entityType, int page, int size);
    
    /**
     * Advanced search with multiple filters
     * @param query Search query string
     * @param filters Map of filter criteria
     * @param page Page number (0-based)
     * @param size Page size
     * @return SearchResultSummary with filtered results
     */
    SearchResultSummary advancedSearch(String query, Map<String, Object> filters, int page, int size);
    
    /**
     * Get recent search queries for a user (if authentication is implemented)
     * @param userId User ID (optional, can be null for anonymous searches)
     * @param maxResults Maximum number of recent queries to return
     * @return List of recent search queries
     */
    List<String> getRecentSearches(Long userId, int maxResults);
    
    /**
     * Save search query to history (if authentication is implemented)
     * @param userId User ID (optional, can be null for anonymous searches)
     * @param query Search query to save
     */
    void saveSearchQuery(Long userId, String query);
    
    /**
     * Get popular search terms
     * @param maxResults Maximum number of popular terms to return
     * @return List of popular search terms
     */
    List<String> getPopularSearchTerms(int maxResults);
    
    /**
     * Clear search history for a user
     * @param userId User ID
     */
    void clearSearchHistory(Long userId);
    
    /**
     * Get search analytics and metrics
     * @return Map containing search analytics data
     */
    Map<String, Object> getSearchAnalytics();
}