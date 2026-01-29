package com.petclinic.frontend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SearchService
 * Tests the frontend search service functionality
 * 
 * Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SearchService Tests")
class SearchServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private WebClient.UriSpec uriSpec;

    @InjectMocks
    private SearchService searchService;

    @BeforeEach
    void setUp() {
        // Setup common mock behavior
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(webClient.delete()).thenReturn(requestHeadersUriSpec);
        
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestBodyUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestBodySpec);
        
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("Should perform global search")
    void testGlobalSearch() {
        // Given
        String query = "Buddy";
        Map<String, Object> expectedResult = Map.of(
            "totalResults", 5,
            "results", List.of(),
            "executionTimeMs", 150L
        );

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(expectedResult));

        // When
        Mono<Map<String, Object>> result = searchService.globalSearch(query);

        // Then
        Map<String, Object> actualResult = result.block();
        assertEquals(expectedResult, actualResult);

        verify(webClient).get();
        verify(requestHeadersUriSpec).uri(any(java.util.function.Function.class));
        verify(responseSpec).bodyToMono(any(ParameterizedTypeReference.class));
    }

    @Test
    @DisplayName("Should perform global search with pagination")
    void testGlobalSearchWithPagination() {
        // Given
        String query = "Buddy";
        int page = 1;
        int size = 10;
        Map<String, Object> expectedResult = Map.of(
            "totalResults", 25,
            "results", List.of(),
            "currentPage", 1,
            "totalPages", 3
        );

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(expectedResult));

        // When
        Mono<Map<String, Object>> result = searchService.globalSearch(query, page, size);

        // Then
        Map<String, Object> actualResult = result.block();
        assertEquals(expectedResult, actualResult);

        verify(webClient).get();
        verify(requestHeadersUriSpec).uri(any(java.util.function.Function.class));
        verify(responseSpec).bodyToMono(any(ParameterizedTypeReference.class));
    }

    @Test
    @DisplayName("Should get search suggestions")
    void testGetSearchSuggestions() {
        // Given
        String partialQuery = "Bud";
        int maxSuggestions = 8;
        List<String> expectedSuggestions = List.of("Buddy", "Buddha", "Budgie");

        when(responseSpec.bodyToFlux(String.class))
            .thenReturn(Flux.fromIterable(expectedSuggestions));

        // When
        Mono<List<String>> result = searchService.getSearchSuggestions(partialQuery, maxSuggestions);

        // Then
        List<String> actualSuggestions = result.block();
        assertEquals(expectedSuggestions, actualSuggestions);

        verify(webClient).get();
        verify(requestHeadersUriSpec).uri(any(java.util.function.Function.class));
        verify(responseSpec).bodyToFlux(String.class);
    }

    @Test
    @DisplayName("Should get popular search terms")
    void testGetPopularSearchTerms() {
        // Given
        int maxResults = 5;
        List<String> expectedTerms = List.of("dog", "cat", "vaccination", "checkup", "surgery");

        when(responseSpec.bodyToFlux(String.class))
            .thenReturn(Flux.fromIterable(expectedTerms));

        // When
        Mono<List<String>> result = searchService.getPopularSearchTerms(maxResults);

        // Then
        List<String> actualTerms = result.block();
        assertEquals(expectedTerms, actualTerms);

        verify(webClient).get();
        verify(requestHeadersUriSpec).uri(any(java.util.function.Function.class));
        verify(responseSpec).bodyToFlux(String.class);
    }

    @Test
    @DisplayName("Should get search analytics")
    void testGetSearchAnalytics() {
        // Given
        Map<String, Object> expectedAnalytics = Map.of(
            "totalSearches", 100,
            "uniqueQueries", 75,
            "avgResultsPerSearch", 4.2,
            "popularTerms", List.of("dog", "cat", "vaccination")
        );

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(expectedAnalytics));

        // When
        Mono<Map<String, Object>> result = searchService.getSearchAnalytics();

        // Then
        Map<String, Object> actualAnalytics = result.block();
        assertEquals(expectedAnalytics, actualAnalytics);

        verify(webClient).get();
        verify(requestHeadersUriSpec).uri("/search/analytics");
        verify(responseSpec).bodyToMono(any(ParameterizedTypeReference.class));
    }

    @Test
    @DisplayName("Should perform advanced search with filters")
    void testAdvancedSearch() {
        // Given
        String query = "Buddy";
        Map<String, Object> filters = Map.of(
            "entityType", "Pet",
            "species", "Dog"
        );
        int page = 0;
        int size = 20;
        Map<String, Object> expectedResult = Map.of(
            "totalResults", 2,
            "results", List.of(),
            "appliedFilters", filters
        );

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(expectedResult));

        // When
        Mono<Map<String, Object>> result = searchService.advancedSearch(query, filters, page, size);

        // Then
        Map<String, Object> actualResult = result.block();
        assertEquals(expectedResult, actualResult);

        verify(webClient).post();
        verify(requestBodyUriSpec).uri(any(java.util.function.Function.class));
        verify(requestBodySpec).bodyValue(filters);
        verify(responseSpec).bodyToMono(any(ParameterizedTypeReference.class));
    }

    @Test
    @DisplayName("Should apply filters")
    void testApplyFilters() {
        // Given
        List<Map<String, Object>> filters = List.of(
            Map.of("field", "species", "operator", "equals", "value", "Dog")
        );
        int page = 0;
        int size = 20;
        Map<String, Object> expectedResult = Map.of(
            "totalResults", 10,
            "results", List.of()
        );

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(expectedResult));

        // When
        Mono<Map<String, Object>> result = searchService.applyFilters(filters, page, size);

        // Then
        Map<String, Object> actualResult = result.block();
        assertEquals(expectedResult, actualResult);

        verify(webClient).post();
        verify(requestBodyUriSpec).uri(any(java.util.function.Function.class));
        verify(requestBodySpec).bodyValue(filters);
        verify(responseSpec).bodyToMono(any(ParameterizedTypeReference.class));
    }

    @Test
    @DisplayName("Should get available fields for entity type")
    void testGetAvailableFields() {
        // Given
        String entityType = "Pet";
        Map<String, String> expectedFields = Map.of(
            "name", "String",
            "species", "String",
            "breed", "String",
            "age", "Integer"
        );

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(expectedFields));

        // When
        Mono<Map<String, String>> result = searchService.getAvailableFields(entityType);

        // Then
        Map<String, String> actualFields = result.block();
        assertEquals(expectedFields, actualFields);

        verify(webClient).get();
        verify(requestHeadersUriSpec).uri("/filters/fields/{entityType}", entityType);
        verify(responseSpec).bodyToMono(any(ParameterizedTypeReference.class));
    }

    @Test
    @DisplayName("Should validate filters")
    void testValidateFilters() {
        // Given
        List<Map<String, Object>> filters = List.of(
            Map.of("field", "species", "operator", "equals", "value", "Dog")
        );
        Map<String, Object> expectedValidation = Map.of(
            "valid", true,
            "errors", List.of(),
            "filterCount", 1
        );

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(expectedValidation));

        // When
        Mono<Map<String, Object>> result = searchService.validateFilters(filters);

        // Then
        Map<String, Object> actualValidation = result.block();
        assertEquals(expectedValidation, actualValidation);

        verify(webClient).post();
        verify(requestBodyUriSpec).uri("/filters/validate");
        verify(requestBodySpec).bodyValue(filters);
        verify(responseSpec).bodyToMono(any(ParameterizedTypeReference.class));
    }

    @Test
    @DisplayName("Should handle service errors gracefully")
    void testServiceError() {
        // Given
        String query = "Buddy";
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.error(new RuntimeException("Service unavailable")));

        // When & Then
        Mono<Map<String, Object>> result = searchService.globalSearch(query);
        
        assertThrows(RuntimeException.class, () -> result.block());

        verify(webClient).get();
        verify(requestHeadersUriSpec).uri(any(java.util.function.Function.class));
        verify(responseSpec).bodyToMono(any(ParameterizedTypeReference.class));
    }
}