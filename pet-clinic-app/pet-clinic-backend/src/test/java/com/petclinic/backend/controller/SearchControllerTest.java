package com.petclinic.backend.controller;

import com.petclinic.backend.dto.SearchResult;
import com.petclinic.backend.dto.SearchResultSummary;
import com.petclinic.backend.service.GlobalSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SearchController
 * Validates: Requirements 4.1, 4.3, 4.4
 */
@WebMvcTest(SearchController.class)
@DisplayName("SearchController Tests")
class SearchControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private GlobalSearchService globalSearchService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private SearchResultSummary testSearchSummary;
    private SearchResult testSearchResult;
    
    @BeforeEach
    void setUp() {
        // Create test search result
        testSearchResult = new SearchResult("Pet", 1L, "Buddy (Dog)", "Golden Retriever, 4 years old - Owner: John Doe");
        testSearchResult.setMatchedFields(Arrays.asList("name"));
        testSearchResult.setRelevanceScore(2.0);
        
        // Create test search summary
        Map<String, Integer> resultsByType = new HashMap<>();
        resultsByType.put("Pet", 1);
        resultsByType.put("Visit", 0);
        resultsByType.put("Veterinarian", 0);
        resultsByType.put("Owner", 0);
        
        testSearchSummary = new SearchResultSummary("Buddy", Arrays.asList(testSearchResult), resultsByType, 0, 20, 1);
    }
    
    @Test
    @DisplayName("Should perform global search successfully")
    void testGlobalSearch_Success() throws Exception {
        // Given
        String query = "Buddy";
        when(globalSearchService.globalSearch(query)).thenReturn(testSearchSummary);
        
        // When & Then
        mockMvc.perform(get("/api/search")
                .param("query", query))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.query").value(query))
                .andExpect(jsonPath("$.totalResults").value(1))
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results[0].entityType").value("Pet"))
                .andExpect(jsonPath("$.results[0].entityId").value(1))
                .andExpect(jsonPath("$.results[0].title").value("Buddy (Dog)"));
    }
    
    @Test
    @DisplayName("Should return bad request for empty query")
    void testGlobalSearch_EmptyQuery() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/search")
                .param("query", ""))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should return bad request for missing query")
    void testGlobalSearch_MissingQuery() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/search"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should perform paginated search successfully")
    void testGlobalSearchPaginated_Success() throws Exception {
        // Given
        String query = "test";
        int page = 0;
        int size = 10;
        
        when(globalSearchService.globalSearch(query, page, size)).thenReturn(testSearchSummary);
        
        // When & Then
        mockMvc.perform(get("/api/search/paginated")
                .param("query", query)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.query").value("Buddy"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }
    
    @Test
    @DisplayName("Should perform advanced search successfully")
    void testAdvancedGlobalSearch_Success() throws Exception {
        // Given
        String query = "test";
        int page = 0;
        int size = 10;
        String sortBy = "relevance";
        String sortDirection = "desc";
        
        when(globalSearchService.globalSearch(query, page, size, sortBy, sortDirection))
                .thenReturn(testSearchSummary);
        
        // When & Then
        mockMvc.perform(get("/api/search/advanced")
                .param("query", query)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .param("sortBy", sortBy)
                .param("sortDirection", sortDirection))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.query").value("Buddy"));
    }
    
    @Test
    @DisplayName("Should search by entity type successfully")
    void testSearchByEntityType_Success() throws Exception {
        // Given
        String query = "Buddy";
        String entityType = "Pet";
        int page = 0;
        int size = 10;
        
        Page<SearchResult> pageResult = new PageImpl<>(Arrays.asList(testSearchResult), 
                PageRequest.of(page, size), 1);
        
        when(globalSearchService.searchByEntityType(query, entityType, page, size))
                .thenReturn(pageResult);
        
        // When & Then
        mockMvc.perform(get("/api/search/entity/{entityType}", entityType)
                .param("query", query)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].entityType").value("Pet"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
    
    @Test
    @DisplayName("Should return not found for invalid entity type")
    void testSearchByEntityType_InvalidEntityType() throws Exception {
        // Given
        String query = "test";
        String invalidEntityType = "InvalidType";
        
        // When & Then
        mockMvc.perform(get("/api/search/entity/{entityType}", invalidEntityType)
                .param("query", query))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("Should get search result counts successfully")
    void testGetSearchResultCounts_Success() throws Exception {
        // Given
        String query = "test";
        Map<String, Integer> counts = new HashMap<>();
        counts.put("Pet", 1);
        counts.put("Visit", 0);
        counts.put("Veterinarian", 0);
        counts.put("Owner", 0);
        
        when(globalSearchService.getSearchResultCounts(query)).thenReturn(counts);
        
        // When & Then
        mockMvc.perform(get("/api/search/counts")
                .param("query", query))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.Pet").value(1))
                .andExpect(jsonPath("$.Visit").value(0))
                .andExpect(jsonPath("$.Veterinarian").value(0))
                .andExpect(jsonPath("$.Owner").value(0));
    }
    
    @Test
    @DisplayName("Should get search suggestions successfully")
    void testGetSearchSuggestions_Success() throws Exception {
        // Given
        String partialQuery = "Bu";
        int maxSuggestions = 10;
        List<String> suggestions = Arrays.asList("Buddy", "Bulldog", "Bunny");
        
        when(globalSearchService.getSearchSuggestions(partialQuery, maxSuggestions))
                .thenReturn(suggestions);
        
        // When & Then
        mockMvc.perform(get("/api/search/suggestions")
                .param("partialQuery", partialQuery)
                .param("maxSuggestions", String.valueOf(maxSuggestions)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").value("Buddy"))
                .andExpect(jsonPath("$[1]").value("Bulldog"))
                .andExpect(jsonPath("$[2]").value("Bunny"));
    }
    
    @Test
    @DisplayName("Should get popular search terms successfully")
    void testGetPopularSearchTerms_Success() throws Exception {
        // Given
        int maxResults = 5;
        List<String> popularTerms = Arrays.asList("dog", "cat", "vaccination", "checkup", "surgery");
        
        when(globalSearchService.getPopularSearchTerms(maxResults)).thenReturn(popularTerms);
        
        // When & Then
        mockMvc.perform(get("/api/search/popular")
                .param("maxResults", String.valueOf(maxResults)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0]").value("dog"));
    }
    
    @Test
    @DisplayName("Should get search analytics successfully")
    void testGetSearchAnalytics_Success() throws Exception {
        // Given
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalSearches", 100L);
        analytics.put("uniqueSearchTerms", 25);
        analytics.put("activeUsers", 10);
        analytics.put("popularTerms", Arrays.asList("dog", "cat", "vaccination"));
        
        when(globalSearchService.getSearchAnalytics()).thenReturn(analytics);
        
        // When & Then
        mockMvc.perform(get("/api/search/analytics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalSearches").value(100))
                .andExpect(jsonPath("$.uniqueSearchTerms").value(25))
                .andExpect(jsonPath("$.activeUsers").value(10))
                .andExpect(jsonPath("$.popularTerms").isArray());
    }
    
    @Test
    @DisplayName("Should perform advanced search with filters successfully")
    void testAdvancedSearchWithFilters_Success() throws Exception {
        // Given
        String query = "test";
        Map<String, Object> filters = new HashMap<>();
        filters.put("entityType", "Pet");
        filters.put("minRelevance", 1.0);
        
        when(globalSearchService.advancedSearch(eq(query), any(Map.class), eq(0), eq(20)))
                .thenReturn(testSearchSummary);
        
        // When & Then
        mockMvc.perform(post("/api/search/advanced")
                .param("query", query)
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filters)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.query").value("Buddy"));
    }
    
    @Test
    @DisplayName("Should handle service exceptions gracefully")
    void testGlobalSearch_ServiceException() throws Exception {
        // Given
        String query = "test";
        when(globalSearchService.globalSearch(query)).thenThrow(new RuntimeException("Service error"));
        
        // When & Then
        mockMvc.perform(get("/api/search")
                .param("query", query))
                .andExpect(status().isInternalServerError());
    }
    
    @Test
    @DisplayName("Should validate page and size parameters")
    void testGlobalSearchPaginated_InvalidParameters() throws Exception {
        // When & Then - Test negative page
        mockMvc.perform(get("/api/search/paginated")
                .param("query", "test")
                .param("page", "-1")
                .param("size", "10"))
                .andExpect(status().isBadRequest());
        
        // When & Then - Test size too large
        mockMvc.perform(get("/api/search/paginated")
                .param("query", "test")
                .param("page", "0")
                .param("size", "200"))
                .andExpect(status().isBadRequest());
        
        // When & Then - Test size too small
        mockMvc.perform(get("/api/search/paginated")
                .param("query", "test")
                .param("page", "0")
                .param("size", "0"))
                .andExpect(status().isBadRequest());
    }
}