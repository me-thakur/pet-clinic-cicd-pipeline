package com.petclinic.frontend.controller;

import com.petclinic.frontend.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SearchController
 * Tests the frontend search controller functionality
 * 
 * Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SearchController Tests")
class SearchControllerTest {

    @Mock
    private SearchService searchService;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private SearchController searchController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(searchController).build();
    }

    @Test
    @DisplayName("Should show search page without query")
    void testShowSearchPage_NoQuery() {
        // Given
        List<String> popularTerms = List.of("dog", "cat", "vaccination");
        when(searchService.getPopularSearchTerms(5)).thenReturn(Mono.just(popularTerms));

        // When
        String result = searchController.showSearchPage(null, 0, 20, model);

        // Then
        assertEquals("search/global", result);
        verify(searchService).getPopularSearchTerms(5);
        verify(model).addAttribute("popularTerms", popularTerms);
        verify(model, never()).addAttribute(eq("results"), any());
        verify(model, never()).addAttribute(eq("query"), any());
    }

    @Test
    @DisplayName("Should show search page with query and results")
    void testShowSearchPage_WithQuery() {
        // Given
        String query = "Buddy";
        List<String> popularTerms = List.of("dog", "cat", "vaccination");
        Map<String, Object> searchResults = Map.of(
            "totalResults", 5,
            "results", List.of(),
            "executionTimeMs", 150L
        );

        when(searchService.getPopularSearchTerms(5)).thenReturn(Mono.just(popularTerms));
        when(searchService.globalSearch(query, 0, 20)).thenReturn(Mono.just(searchResults));

        // When
        String result = searchController.showSearchPage(query, 0, 20, model);

        // Then
        assertEquals("search/global", result);
        verify(searchService).getPopularSearchTerms(5);
        verify(searchService).globalSearch(query, 0, 20);
        verify(model).addAttribute("popularTerms", popularTerms);
        verify(model).addAttribute("results", searchResults);
        verify(model).addAttribute("query", query);
    }

    @Test
    @DisplayName("Should handle search service error gracefully")
    void testShowSearchPage_ServiceError() {
        // Given
        when(searchService.getPopularSearchTerms(5)).thenReturn(Mono.error(new RuntimeException("Service error")));

        // When
        String result = searchController.showSearchPage(null, 0, 20, model);

        // Then
        assertEquals("search/global", result);
        verify(model).addAttribute(eq("error"), contains("Error loading search page"));
    }

    @Test
    @DisplayName("Should perform search and redirect")
    void testPerformSearch_ValidQuery() {
        // Given
        String query = "Buddy";

        // When
        String result = searchController.performSearch(query, 0, 20, redirectAttributes);

        // Then
        assertEquals("redirect:/search?query=Buddy&page=0&size=20", result);
        verify(redirectAttributes, never()).addFlashAttribute(eq("error"), any());
    }

    @Test
    @DisplayName("Should handle empty query in perform search")
    void testPerformSearch_EmptyQuery() {
        // Given
        String query = "";

        // When
        String result = searchController.performSearch(query, 0, 20, redirectAttributes);

        // Then
        assertEquals("redirect:/search", result);
        verify(redirectAttributes).addFlashAttribute("error", "Please enter a search query");
    }

    @Test
    @DisplayName("Should handle null query in perform search")
    void testPerformSearch_NullQuery() {
        // Given
        String query = null;

        // When
        String result = searchController.performSearch(query, 0, 20, redirectAttributes);

        // Then
        assertEquals("redirect:/search", result);
        verify(redirectAttributes).addFlashAttribute("error", "Please enter a search query");
    }

    @Test
    @DisplayName("Should handle quick search redirect")
    void testQuickSearch() {
        // Given
        String query = "vaccination";

        // When
        String result = searchController.quickSearch(query);

        // Then
        assertEquals("redirect:/search?query=vaccination", result);
    }

    @Test
    @DisplayName("Should return search suggestions")
    void testGetSearchSuggestions() {
        // Given
        String partialQuery = "Bud";
        List<String> suggestions = List.of("Buddy", "Buddha", "Budgie");
        when(searchService.getSearchSuggestions(partialQuery, 8)).thenReturn(Mono.just(suggestions));

        // When
        List<String> result = searchController.getSearchSuggestions(partialQuery, 8);

        // Then
        assertEquals(suggestions, result);
        verify(searchService).getSearchSuggestions(partialQuery, 8);
    }

    @Test
    @DisplayName("Should return empty list when suggestions service fails")
    void testGetSearchSuggestions_ServiceError() {
        // Given
        String partialQuery = "Bud";
        when(searchService.getSearchSuggestions(partialQuery, 8)).thenReturn(Mono.error(new RuntimeException("Service error")));

        // When
        List<String> result = searchController.getSearchSuggestions(partialQuery, 8);

        // Then
        assertTrue(result.isEmpty());
        verify(searchService).getSearchSuggestions(partialQuery, 8);
    }

    @Test
    @DisplayName("Should search by entity type")
    void testSearchByEntityType() {
        // Given
        String entityType = "Pet";
        String query = "Buddy";
        Map<String, Object> searchResults = Map.of(
            "totalResults", 3,
            "results", List.of(),
            "executionTimeMs", 120L
        );

        when(searchService.searchByEntityType(query, entityType, 0, 20)).thenReturn(Mono.just(searchResults));

        // When
        String result = searchController.searchByEntityType(entityType, query, 0, 20, model);

        // Then
        assertEquals("search/global", result);
        verify(searchService).searchByEntityType(query, entityType, 0, 20);
        verify(model).addAttribute("results", searchResults);
        verify(model).addAttribute("query", query);
        verify(model).addAttribute("entityType", entityType);
        verify(model).addAttribute("entityTypeFilter", true);
    }

    @Test
    @DisplayName("Should handle entity type search error")
    void testSearchByEntityType_ServiceError() {
        // Given
        String entityType = "Pet";
        String query = "Buddy";
        when(searchService.searchByEntityType(query, entityType, 0, 20)).thenReturn(Mono.error(new RuntimeException("Service error")));

        // When
        String result = searchController.searchByEntityType(entityType, query, 0, 20, model);

        // Then
        assertEquals("search/global", result);
        verify(model).addAttribute(eq("error"), contains("Error searching Pet"));
    }

    @Test
    @DisplayName("Should perform advanced search with filters")
    void testAdvancedSearch() {
        // Given
        String query = "Buddy";
        String entityType = "Pet";
        String species = "Dog";
        Map<String, Object> searchResults = Map.of(
            "totalResults", 2,
            "results", List.of(),
            "executionTimeMs", 180L
        );

        when(searchService.advancedSearch(eq(query), any(Map.class), eq(0), eq(20))).thenReturn(Mono.just(searchResults));

        // When
        String result = searchController.advancedSearch(query, entityType, null, null, species, null, null, null, 0, 20, model, redirectAttributes);

        // Then
        assertEquals("search/global", result);
        verify(searchService).advancedSearch(eq(query), any(Map.class), eq(0), eq(20));
        verify(model).addAttribute("results", searchResults);
        verify(model).addAttribute("query", query);
        verify(model).addAttribute(eq("appliedFilters"), any(Map.class));
    }

    @Test
    @DisplayName("Should handle advanced search error")
    void testAdvancedSearch_ServiceError() {
        // Given
        String query = "Buddy";
        when(searchService.advancedSearch(eq(query), any(Map.class), eq(0), eq(20))).thenReturn(Mono.error(new RuntimeException("Service error")));

        // When
        String result = searchController.advancedSearch(query, null, null, null, null, null, null, null, 0, 20, model, redirectAttributes);

        // Then
        assertEquals("redirect:/search?query=Buddy", result);
        verify(redirectAttributes).addFlashAttribute(eq("error"), contains("Error performing advanced search"));
    }

    @Test
    @DisplayName("Should handle export search results")
    void testExportSearchResults() {
        // Given
        String query = "Buddy";
        String format = "csv";

        // When
        String result = searchController.exportSearchResults(query, format, null, null, null, null, null, redirectAttributes);

        // Then
        assertEquals("redirect:/search?query=Buddy", result);
        verify(redirectAttributes).addFlashAttribute(eq("success"), contains("Export request submitted"));
    }

    @Test
    @DisplayName("Should get search analytics")
    void testGetSearchAnalytics() {
        // Given
        Map<String, Object> analytics = Map.of(
            "totalSearches", 100,
            "uniqueQueries", 75,
            "popularTerms", List.of("dog", "cat", "vaccination")
        );

        when(searchService.getSearchAnalytics()).thenReturn(Mono.just(analytics));

        // When
        String result = searchController.getSearchAnalytics(model);

        // Then
        assertEquals("search/analytics", result);
        verify(searchService).getSearchAnalytics();
        verify(model).addAttribute("analytics", analytics);
    }

    @Test
    @DisplayName("Should handle analytics service error")
    void testGetSearchAnalytics_ServiceError() {
        // Given
        when(searchService.getSearchAnalytics()).thenReturn(Mono.error(new RuntimeException("Service error")));

        // When
        String result = searchController.getSearchAnalytics(model);

        // Then
        assertEquals("search/analytics", result);
        verify(model).addAttribute(eq("error"), contains("Error loading search analytics"));
    }

    @Test
    @DisplayName("Should handle search with pagination parameters")
    void testShowSearchPage_WithPagination() throws Exception {
        // Given
        String query = "test";
        List<String> popularTerms = List.of("dog", "cat");
        Map<String, Object> searchResults = Map.of("totalResults", 50);

        when(searchService.getPopularSearchTerms(5)).thenReturn(Mono.just(popularTerms));
        when(searchService.globalSearch(query, 2, 10)).thenReturn(Mono.just(searchResults));

        // When & Then
        mockMvc.perform(get("/search")
                .param("query", query)
                .param("page", "2")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("search/global"));

        verify(searchService).globalSearch(query, 2, 10);
    }

    @Test
    @DisplayName("Should handle POST search request")
    void testPerformSearch_PostRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/search")
                .param("query", "Buddy")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/search?query=Buddy&page=0&size=20"));
    }

    @Test
    @DisplayName("Should handle empty POST search request")
    void testPerformSearch_EmptyPostRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/search")
                .param("query", "")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/search"));
    }
}