package com.petclinic.frontend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Frontend Search Service
 * 
 * Service class for managing search operations through the backend REST API.
 * Provides methods for global search, filtering, and search analytics.
 * 
 * Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5
 */
@Service
public class SearchService {

    private final WebClient webClient;

    @Autowired
    public SearchService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Perform global search across all entities
     */
    public Mono<Map<String, Object>> globalSearch(String query) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("query", query)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Perform global search with pagination
     */
    public Mono<Map<String, Object>> globalSearch(String query, int page, int size) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/paginated")
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Perform advanced global search with sorting
     */
    public Mono<Map<String, Object>> globalSearch(String query, int page, int size, String sortBy, String sortDirection) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/advanced")
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .queryParam("sortBy", sortBy)
                        .queryParam("sortDirection", sortDirection)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Search within a specific entity type
     */
    public Mono<Map<String, Object>> searchByEntityType(String query, String entityType, int page, int size) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/entity/{entityType}")
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build(entityType))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Get search result counts by entity type
     */
    public Mono<Map<String, Integer>> getSearchResultCounts(String query) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/counts")
                        .queryParam("query", query)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Integer>>() {});
    }

    /**
     * Get search suggestions based on partial query
     */
    public Mono<List<String>> getSearchSuggestions(String partialQuery, int maxSuggestions) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/suggestions")
                        .queryParam("partialQuery", partialQuery)
                        .queryParam("maxSuggestions", maxSuggestions)
                        .build())
                .retrieve()
                .bodyToFlux(String.class)
                .collectList();
    }

    /**
     * Get popular search terms
     */
    public Mono<List<String>> getPopularSearchTerms(int maxResults) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/popular")
                        .queryParam("maxResults", maxResults)
                        .build())
                .retrieve()
                .bodyToFlux(String.class)
                .collectList();
    }

    /**
     * Get search analytics and metrics
     */
    public Mono<Map<String, Object>> getSearchAnalytics() {
        return webClient.get()
                .uri("/search/analytics")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Advanced search with filters
     */
    public Mono<Map<String, Object>> advancedSearch(String query, Map<String, Object> filters, int page, int size) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/advanced")
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .bodyValue(filters)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Apply filters to search results
     */
    public Mono<Map<String, Object>> applyFilters(List<Map<String, Object>> filters, int page, int size) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/filters/apply")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .bodyValue(filters)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Apply filters to a specific entity type
     */
    public Mono<Map<String, Object>> applyFiltersToEntityType(List<Map<String, Object>> filters, String entityType, int page, int size) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/filters/apply/{entityType}")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build(entityType))
                .bodyValue(filters)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Get recent filter combinations
     */
    public Mono<List<List<Map<String, Object>>>> getRecentFilters(Long userId, int maxResults) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/filters/recent")
                        .queryParam("userId", userId)
                        .queryParam("maxResults", maxResults)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<List<Map<String, Object>>>>() {});
    }

    /**
     * Get popular filter combinations
     */
    public Mono<List<Map<String, Object>>> getPopularFilters(int maxResults) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/filters/popular")
                        .queryParam("maxResults", maxResults)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }

    /**
     * Save filter combination
     */
    public Mono<Map<String, Object>> saveFilterCombination(Long userId, String filterName, List<Map<String, Object>> filters) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/filters/save")
                        .queryParam("userId", userId)
                        .queryParam("filterName", filterName)
                        .build())
                .bodyValue(filters)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Get saved filter combinations
     */
    public Mono<Map<Long, String>> getSavedFilterCombinations(Long userId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/filters/saved")
                        .queryParam("userId", userId)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<Long, String>>() {});
    }

    /**
     * Load saved filter combination
     */
    public Mono<List<Map<String, Object>>> loadFilterCombination(Long id) {
        return webClient.get()
                .uri("/filters/saved/{id}", id)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }

    /**
     * Delete saved filter combination
     */
    public Mono<Map<String, String>> deleteFilterCombination(Long id, Long userId) {
        return webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/filters/saved/{id}")
                        .queryParam("userId", userId)
                        .build(id))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {});
    }

    /**
     * Get available filter fields for an entity type
     */
    public Mono<Map<String, String>> getAvailableFields(String entityType) {
        return webClient.get()
                .uri("/filters/fields/{entityType}", entityType)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {});
    }

    /**
     * Get available operators for a field type
     */
    public Mono<List<String>> getAvailableOperators(String fieldType) {
        return webClient.get()
                .uri("/filters/operators/{fieldType}", fieldType)
                .retrieve()
                .bodyToFlux(String.class)
                .collectList();
    }

    /**
     * Validate filter criteria
     */
    public Mono<Map<String, Object>> validateFilters(List<Map<String, Object>> filters) {
        return webClient.post()
                .uri("/filters/validate")
                .bodyValue(filters)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Combine search query with filters
     */
    public Mono<Map<String, Object>> combineSearchAndFilters(String query, List<Map<String, Object>> filters, int page, int size) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/filters/search")
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .bodyValue(filters)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Get filter analytics
     */
    public Mono<Map<String, Object>> getFilterAnalytics() {
        return webClient.get()
                .uri("/filters/analytics")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Export filtered results
     */
    public Mono<byte[]> exportFilteredResults(List<Map<String, Object>> filters, String format) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/filters/export")
                        .queryParam("format", format)
                        .build())
                .bodyValue(filters)
                .retrieve()
                .bodyToMono(byte[].class);
    }

    /**
     * Clear filter history for a user
     */
    public Mono<Map<String, String>> clearFilterHistory(Long userId) {
        return webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/filters/history")
                        .queryParam("userId", userId)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {});
    }
}