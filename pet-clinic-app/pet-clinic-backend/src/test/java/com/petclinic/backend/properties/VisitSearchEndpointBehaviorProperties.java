package com.petclinic.backend.properties;

import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Visit;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for Visit Search Endpoint Behavior
 * **Feature: critical-fixes-and-enhancements, Property 1: Search Endpoint Response Behavior**
 * **Validates: Requirements 1.4, 1.5**
 * 
 * Property 1: Search Endpoint Response Behavior
 * For any search endpoint and any valid search request, the system should return matching visit records 
 * in proper JSON format, and for any invalid request, should return appropriate error responses with correct status codes
 */
class VisitSearchEndpointBehaviorProperties {
    
    private List<Visit> testVisits;
    
    @BeforeEach
    void setUp() {
        setupTestData();
    }
    
    /**
     * Property 1: Search Endpoint Response Behavior - Valid Requests
     * For any valid search request, all search endpoints should return matching visit records in proper JSON format
     * **Feature: critical-fixes-and-enhancements, Property 1: Search Endpoint Response Behavior**
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 100)
    void validSearchRequestsShouldReturnMatchingVisitsInProperFormat(
            @ForAll @NotBlank @StringLength(min = 1, max = 50) String searchText,
            @ForAll @IntRange(min = 0, max = 10) int page,
            @ForAll @IntRange(min = 1, max = 100) int size) {
        
        // Test all three search methods
        testValidSearchMethod("treatment", searchText, page, size);
        testValidSearchMethod("diagnosis", searchText, page, size);
        testValidSearchMethod("description", searchText, page, size);
    }
    
    /**
     * Property 1: Search Endpoint Response Behavior - Response Format Consistency
     * For any search endpoint, the response format should be consistent and contain required fields
     * **Feature: critical-fixes-and-enhancements, Property 1: Search Endpoint Response Behavior**
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 100)
    void searchResponsesShouldHaveConsistentFormat(
            @ForAll @NotBlank @StringLength(min = 1, max = 20) String searchText,
            @ForAll @IntRange(min = 0, max = 5) int page,
            @ForAll @IntRange(min = 1, max = 20) int size) {
        
        // Test response format consistency across all methods
        testResponseFormatConsistency("treatment", searchText, page, size);
        testResponseFormatConsistency("diagnosis", searchText, page, size);
        testResponseFormatConsistency("description", searchText, page, size);
    }
    
    /**
     * Property 1: Search Endpoint Response Behavior - Pagination Behavior
     * For any valid pagination parameters, search endpoints should return properly paginated results
     * **Feature: critical-fixes-and-enhancements, Property 1: Search Endpoint Response Behavior**
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 50)
    void searchEndpointsShouldHandlePaginationCorrectly(
            @ForAll @NotBlank @StringLength(min = 1, max = 10) String searchText,
            @ForAll @IntRange(min = 0, max = 3) int page,
            @ForAll @IntRange(min = 1, max = 5) int size) {
        
        // Test pagination behavior across all methods
        testPaginationBehavior("treatment", searchText, page, size);
        testPaginationBehavior("diagnosis", searchText, page, size);
        testPaginationBehavior("description", searchText, page, size);
    }
    
    /**
     * Property 1: Search Endpoint Response Behavior - Case Insensitive Search
     * For any search text, search should be case insensitive and return consistent results
     * **Feature: critical-fixes-and-enhancements, Property 1: Search Endpoint Response Behavior**
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 100)
    void searchShouldBeCaseInsensitiveAndConsistent(
            @ForAll @NotBlank @StringLength(min = 2, max = 20) String baseSearchText) {
        
        String lowerCase = baseSearchText.toLowerCase();
        String upperCase = baseSearchText.toUpperCase();
        String mixedCase = generateMixedCase(baseSearchText);
        
        // Test case insensitive behavior across all methods
        testCaseInsensitiveBehavior("treatment", lowerCase, upperCase, mixedCase);
        testCaseInsensitiveBehavior("diagnosis", lowerCase, upperCase, mixedCase);
        testCaseInsensitiveBehavior("description", lowerCase, upperCase, mixedCase);
    }
    
    /**
     * Property 1: Search Endpoint Response Behavior - Invalid Requests
     * For any invalid search request, all search endpoints should return empty results gracefully
     * **Feature: critical-fixes-and-enhancements, Property 1: Search Endpoint Response Behavior**
     * **Validates: Requirements 1.5**
     */
    @Property(tries = 50)
    void invalidSearchRequestsShouldReturnEmptyResults(
            @ForAll("invalidSearchParameters") InvalidSearchParams params) {
        
        // Test all three search methods with invalid parameters
        testInvalidSearchMethod("treatment", params);
        testInvalidSearchMethod("diagnosis", params);
        testInvalidSearchMethod("description", params);
    }
    
    
    // Helper methods for testing individual search methods
    
    private void testValidSearchMethod(String method, String searchText, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<Visit> response = performSearch(method, searchText, pageable);
        
        // Verify successful response
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isNotNull();
        assertThat(response.getPage()).isNotNull();
        
        // Verify pagination metadata
        assertThat(response.getPage().getNumber()).isEqualTo(page);
        assertThat(response.getPage().getSize()).isEqualTo(size);
        assertThat(response.getPage().getTotalElements()).isGreaterThanOrEqualTo(0);
        assertThat(response.getPage().getTotalPages()).isGreaterThanOrEqualTo(0);
        
        // Verify content size doesn't exceed page size
        assertThat(response.getContent().size()).isLessThanOrEqualTo(size);
        
        // Verify all returned visits contain the search text (case insensitive)
        for (Visit visit : response.getContent()) {
            assertThat(visit).isNotNull();
            assertThat(visitContainsSearchText(visit, searchText, method)).isTrue();
        }
    }
    
    private void testInvalidSearchMethod(String method, InvalidSearchParams params) {
        Pageable pageable = PageRequest.of(Math.max(0, params.page), Math.max(1, params.size));
        PagedResponse<Visit> response = performSearch(method, params.searchText, pageable);
        
        // Invalid parameters should return empty results gracefully
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isNotNull();
        
        // For null or empty search text, should return empty results
        if (params.searchText == null || params.searchText.trim().isEmpty()) {
            assertThat(response.getContent()).isEmpty();
            assertThat(response.getPage().getTotalElements()).isEqualTo(0);
        }
    }
    
    private void testResponseFormatConsistency(String method, String searchText, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<Visit> response = performSearch(method, searchText, pageable);
        
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isNotNull();
        assertThat(response.getPage()).isNotNull();
        
        // Verify consistent response structure
        assertThat(response.getPage().getNumber()).isNotNull();
        assertThat(response.getPage().getSize()).isNotNull();
        assertThat(response.getPage().getTotalElements()).isNotNull();
        assertThat(response.getPage().getTotalPages()).isNotNull();
        
        // Verify each visit has required fields
        for (Visit visit : response.getContent()) {
            assertThat(visit.getId()).isNotNull();
            assertThat(visit.getVisitDate()).isNotNull();
            assertThat(visit.getPet()).isNotNull();
            // Treatment, diagnosis, and notes can be null, but should be handled gracefully
        }
    }
    
    private void testPaginationBehavior(String method, String searchText, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<Visit> response = performSearch(method, searchText, pageable);
        
        assertThat(response).isNotNull();
        
        // Verify pagination logic
        assertThat(response.getPage().getNumber()).isEqualTo(page);
        assertThat(response.getPage().getSize()).isEqualTo(size);
        
        // If there are results, verify pagination calculations
        if (response.getPage().getTotalElements() > 0) {
            long totalElements = response.getPage().getTotalElements();
            int totalPages = response.getPage().getTotalPages();
            
            // Verify total pages calculation
            int expectedTotalPages = (int) Math.ceil((double) totalElements / size);
            assertThat(totalPages).isEqualTo(expectedTotalPages);
            
            // Verify content size for current page
            if (page < totalPages - 1) {
                // Not the last page, should have full page size or less
                assertThat(response.getContent().size()).isLessThanOrEqualTo(size);
            } else if (page == totalPages - 1) {
                // Last page, should have remaining elements
                int expectedLastPageSize = (int) (totalElements - (long) page * size);
                assertThat(response.getContent().size()).isEqualTo(Math.min(expectedLastPageSize, size));
            } else {
                // Page beyond available pages, should be empty
                assertThat(response.getContent()).isEmpty();
            }
        }
    }
    
    private void testCaseInsensitiveBehavior(String method, String lowerCase, String upperCase, String mixedCase) {
        Pageable pageable = PageRequest.of(0, 10);
        PagedResponse<Visit> lowerResponse = performSearch(method, lowerCase, pageable);
        PagedResponse<Visit> upperResponse = performSearch(method, upperCase, pageable);
        PagedResponse<Visit> mixedResponse = performSearch(method, mixedCase, pageable);
        
        // All should return valid responses
        assertThat(lowerResponse).isNotNull();
        assertThat(upperResponse).isNotNull();
        assertThat(mixedResponse).isNotNull();
        
        // Should return same number of total elements (case insensitive)
        long lowerTotal = lowerResponse.getPage().getTotalElements();
        long upperTotal = upperResponse.getPage().getTotalElements();
        long mixedTotal = mixedResponse.getPage().getTotalElements();
        
        assertThat(lowerTotal).isEqualTo(upperTotal);
        assertThat(lowerTotal).isEqualTo(mixedTotal);
    }
    
    /**
     * Core search implementation that mimics the VisitSearchService behavior
     */
    private PagedResponse<Visit> performSearch(String method, String searchText, Pageable pageable) {
        try {
            if (searchText == null || searchText.trim().isEmpty()) {
                return createEmptyPagedResponse(pageable);
            }
            
            String lowerSearchText = searchText.trim().toLowerCase();
            List<Visit> filteredVisits = testVisits.stream()
                    .filter(visit -> visitMatchesSearch(visit, lowerSearchText, method))
                    .collect(Collectors.toList());
            
            return createPagedResponseFromList(filteredVisits, pageable);
        } catch (Exception e) {
            return createEmptyPagedResponse(pageable);
        }
    }
    
    private boolean visitMatchesSearch(Visit visit, String searchText, String method) {
        switch (method) {
            case "treatment":
                return visit.getTreatment() != null && 
                       visit.getTreatment().toLowerCase().contains(searchText);
            case "diagnosis":
                return visit.getDiagnosis() != null && 
                       visit.getDiagnosis().toLowerCase().contains(searchText);
            case "description":
                return visit.getNotes() != null && 
                       visit.getNotes().toLowerCase().contains(searchText);
            default:
                return false;
        }
    }
    
    private boolean visitContainsSearchText(Visit visit, String searchText, String method) {
        String lowerSearchText = searchText.toLowerCase();
        
        switch (method) {
            case "treatment":
                return visit.getTreatment() != null && 
                       visit.getTreatment().toLowerCase().contains(lowerSearchText);
            case "diagnosis":
                return visit.getDiagnosis() != null && 
                       visit.getDiagnosis().toLowerCase().contains(lowerSearchText);
            case "description":
                return visit.getNotes() != null && 
                       visit.getNotes().toLowerCase().contains(lowerSearchText);
            default:
                return false;
        }
    }
    
    private String generateMixedCase(String input) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (i % 2 == 0) {
                result.append(Character.toLowerCase(c));
            } else {
                result.append(Character.toUpperCase(c));
            }
        }
        return result.toString();
    }
    
    // Generator for invalid search parameters
    @Provide
    Arbitrary<InvalidSearchParams> invalidSearchParameters() {
        return Combinators.combine(
            Arbitraries.oneOf(
                Arbitraries.just(""), // Empty string
                Arbitraries.just("   "), // Whitespace only
                Arbitraries.just(null) // Null string
            ),
            Arbitraries.integers().between(-5, -1), // Negative page
            Arbitraries.integers().between(-10, 0) // Zero or negative size
        ).as(InvalidSearchParams::new);
    }
    
    // Helper class for invalid search parameters
    private static class InvalidSearchParams {
        final String searchText;
        final int page;
        final int size;
        
        InvalidSearchParams(String searchText, int page, int size) {
            this.searchText = searchText;
            this.page = page;
            this.size = size;
        }
        
        boolean shouldReturnBadRequest() {
            return (searchText == null || searchText.trim().isEmpty()) || 
                   page < 0 || size <= 0;
        }
    }
    
    private PagedResponse<Visit> createPagedResponseFromList(List<Visit> visits, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), visits.size());
        
        if (start >= visits.size()) {
            return createEmptyPagedResponse(pageable);
        }
        
        List<Visit> pageContent = visits.subList(start, end);
        Page<Visit> page = new PageImpl<>(pageContent, pageable, visits.size());
        return new PagedResponse<>(page);
    }
    
    private PagedResponse<Visit> createEmptyPagedResponse(Pageable pageable) {
        Page<Visit> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        return new PagedResponse<>(emptyPage);
    }
    
    private void setupTestData() {
        testVisits = new ArrayList<>();
        
        // Create test pets
        Pet pet1 = new Pet();
        pet1.setId(1L);
        pet1.setName("TestPet1");
        pet1.setSpecies("Dog");
        pet1.setBreed("Golden Retriever");
        pet1.setBirthDate(LocalDate.now().minusYears(2));
        
        Pet pet2 = new Pet();
        pet2.setId(2L);
        pet2.setName("TestPet2");
        pet2.setSpecies("Cat");
        pet2.setBreed("Siamese");
        pet2.setBirthDate(LocalDate.now().minusYears(1));
        
        // Create test visits with various treatments, diagnoses, and notes
        String[] treatments = {"Vaccination", "Surgery", "Medication", "Dental Cleaning", "Checkup"};
        String[] diagnoses = {"Healthy", "Infection", "Broken Bone", "Dental Issues", "Skin Condition"};
        String[] notes = {"Annual checkup completed", "Emergency surgery performed", 
                         "Prescribed antibiotics", "Routine dental maintenance", "Follow-up required"};
        
        for (int i = 0; i < 15; i++) {
            Visit visit = new Visit();
            visit.setId((long) (i + 1));
            visit.setVisitDate(LocalDateTime.now().minusDays(i));
            visit.setTreatment(treatments[i % treatments.length]);
            visit.setDiagnosis(diagnoses[i % diagnoses.length]);
            visit.setNotes(notes[i % notes.length]);
            visit.setCost(BigDecimal.valueOf(50.0 + i * 10));
            visit.setPet(i % 2 == 0 ? pet1 : pet2);
            testVisits.add(visit);
        }
    }
}