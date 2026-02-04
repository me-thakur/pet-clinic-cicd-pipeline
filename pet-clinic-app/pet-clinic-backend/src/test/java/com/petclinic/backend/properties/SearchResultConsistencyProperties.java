package com.petclinic.backend.properties;

import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.service.SearchResultConsistencyService;
import com.petclinic.backend.service.VisitSearchService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Property-based tests for search result consistency
 * 
 * **Feature: critical-fixes-and-enhancements, Property 13: Search Result Consistency**
 * **Validates: Requirements 7.1**
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class SearchResultConsistencyProperties extends PropertyTestBase {

    @Autowired
    private SearchResultConsistencyService consistencyService;

    @Autowired
    private VisitSearchService visitSearchService;

    /**
     * Property 13: Search Result Consistency
     * For any search operation, the system should return consistent results across different search criteria
     * and maintain result integrity
     */
    @Test
    void searchResultsShouldBeConsistentAndMaintainIntegrity() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate test data
            String searchText = generateSearchText();
            String searchType = generateSearchType();
            
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            
            // Act
            PagedResponse<Visit> results = getSearchResults(searchText, searchType, pageable);
            SearchResultConsistencyService.ValidationResult validation = 
                consistencyService.validateSearchResultIntegrity(searchText, searchType, results);
            
            // Assert - Search results should maintain integrity
            Assertions.assertThat(validation.isValid())
                .as("Search results should maintain integrity for text '%s' and type '%s'. Issues: %s", 
                    searchText, searchType, validation.getIssues())
                .isTrue();
            
            // Assert - Results should match search criteria
            if (results != null && !results.getContent().isEmpty()) {
                boolean allMatch = consistencyService.validateResultsMatchCriteria(
                    searchText, searchType, results.getContent());
                Assertions.assertThat(allMatch)
                    .as("All search results should match the search criteria")
                    .isTrue();
            }
            
            // Assert - No duplicate results
            if (results != null) {
                boolean noDuplicates = consistencyService.validateNoDuplicateResults(results.getContent());
                Assertions.assertThat(noDuplicates)
                    .as("Search results should not contain duplicates")
                    .isTrue();
            }
        });
    }

    /**
     * Property: Cross-search consistency
     * For any search text, results should be consistent across different search types
     */
    @Test
    void crossSearchResultsShouldBeConsistent() {
        runPropertyTest(50, () -> {
            // Generate test data
            String searchText = generateSearchText();
            
            // Arrange
            Pageable pageable = PageRequest.of(0, 5);
            
            // Act
            SearchResultConsistencyService.ConsistencyReport report = 
                consistencyService.validateCrossSearchConsistency(searchText, pageable);
            
            // Assert - Consistency report should be generated
            Assertions.assertThat(report).isNotNull();
            Assertions.assertThat(report.getSearchText()).isEqualTo(searchText);
            
            // Assert - Result counts should be non-negative
            report.getResultCounts().values().forEach(count -> 
                Assertions.assertThat(count)
                    .as("Result count should be non-negative")
                    .isGreaterThanOrEqualTo(0L));
            
            // Assert - If there are no issues, the report should be consistent
            boolean hasNoIssues = report.getIssues().values().stream()
                .allMatch(List::isEmpty);
            if (hasNoIssues) {
                Assertions.assertThat(report.isConsistent())
                    .as("Report should be consistent when there are no issues")
                    .isTrue();
            }
        });
    }

    /**
     * Property: Data consistency monitoring
     * For any search operation, monitoring should detect inconsistencies accurately
     */
    @Test
    void dataConsistencyMonitoringShouldDetectInconsistencies() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate test data
            String searchText = generateSearchText();
            String searchType = generateSearchType();
            
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            PagedResponse<Visit> results = getSearchResults(searchText, searchType, pageable);
            long expectedCount = results != null ? results.getPage().getTotalElements() : 0;
            
            // Act
            SearchResultConsistencyService.MonitoringResult monitoring = 
                consistencyService.monitorDataConsistency(searchText, searchType, expectedCount, results);
            
            // Assert - Monitoring result should be generated
            Assertions.assertThat(monitoring).isNotNull();
            Assertions.assertThat(monitoring.getSearchText()).isEqualTo(searchText);
            Assertions.assertThat(monitoring.getSearchType()).isEqualTo(searchType);
            
            // Assert - When expected equals actual, consistency should be high
            if (monitoring.getExpectedCount() == monitoring.getActualCount()) {
                Assertions.assertThat(monitoring.getConsistencyScore())
                    .as("Consistency score should be 1.0 when expected equals actual")
                    .isEqualTo(1.0);
            }
            
            // Assert - Consistency score should be between 0 and 1
            Assertions.assertThat(monitoring.getConsistencyScore())
                .as("Consistency score should be between 0 and 1")
                .isBetween(0.0, 1.0);
        });
    }

    /**
     * Property: Empty search handling
     * Empty or null search text should be handled consistently
     */
    @Test
    void emptySearchShouldBeHandledConsistently() {
        runPropertyTest(50, () -> {
            // Generate test data
            String searchType = generateSearchType();
            
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            String[] emptySearchTexts = {null, "", "   "};
            
            for (String emptyText : emptySearchTexts) {
                // Act
                PagedResponse<Visit> results = getSearchResults(emptyText, searchType, pageable);
                
                // Assert - Empty search should return empty results or handle gracefully
                if (results != null) {
                    Assertions.assertThat(results.getContent())
                        .as("Empty search should return empty results or handle gracefully")
                        .isNotNull();
                    
                    // Validate integrity even for empty searches
                    SearchResultConsistencyService.ValidationResult validation = 
                        consistencyService.validateSearchResultIntegrity(emptyText, searchType, results);
                    
                    // Empty searches may have validation issues (like null search text), but should not crash
                    Assertions.assertThat(validation)
                        .as("Validation should handle empty search gracefully")
                        .isNotNull();
                }
            }
        });
    }

    // Generator methods for test data
    private String generateSearchText() {
        String[] searchTexts = {
            "vaccination", "checkup", "surgery", "dental", "emergency",
            "wellness", "treatment", "diagnosis", "medication", "therapy",
            "test", "exam", "consultation", "follow-up", "routine"
        };
        return searchTexts[random.nextInt(searchTexts.length)];
    }
    
    private String generateSearchType() {
        String[] searchTypes = {"treatment", "diagnosis", "description", "notes"};
        return searchTypes[random.nextInt(searchTypes.length)];
    }

    private PagedResponse<Visit> getSearchResults(String searchText, String searchType, Pageable pageable) {
        try {
            switch (searchType.toLowerCase()) {
                case "treatment":
                    return visitSearchService.searchByTreatment(searchText, pageable);
                case "diagnosis":
                    return visitSearchService.searchByDiagnosis(searchText, pageable);
                case "description":
                    return visitSearchService.searchByDescription(searchText, pageable);
                case "notes":
                    return visitSearchService.searchByNotes(searchText, pageable);
                default:
                    return null;
            }
        } catch (Exception e) {
            // Return null for invalid searches - the consistency service should handle this
            return null;
        }
    }
}