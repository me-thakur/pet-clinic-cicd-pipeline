package com.petclinic.backend.service;

import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.model.Visit;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * Service interface for search result consistency validation
 * Ensures search results maintain integrity and consistency across different criteria
 * 
 * Validates: Requirements 7.1
 */
public interface SearchResultConsistencyService {

    /**
     * Validate search result integrity for a given search operation
     * 
     * @param searchText The search text used
     * @param searchType The type of search (treatment, diagnosis, description, notes)
     * @param results The search results to validate
     * @return ValidationResult indicating if results are consistent
     */
    ValidationResult validateSearchResultIntegrity(String searchText, String searchType, PagedResponse<Visit> results);

    /**
     * Ensure consistent results across different search criteria for the same text
     * 
     * @param searchText The search text to validate across criteria
     * @param pageable Pagination parameters
     * @return ConsistencyReport showing results across all search types
     */
    ConsistencyReport validateCrossSearchConsistency(String searchText, Pageable pageable);

    /**
     * Monitor data consistency for search operations
     * 
     * @param searchText The search text
     * @param searchType The search type
     * @param expectedCount Expected number of results
     * @param actualResults Actual search results
     * @return MonitoringResult with consistency metrics
     */
    MonitoringResult monitorDataConsistency(String searchText, String searchType, long expectedCount, PagedResponse<Visit> actualResults);

    /**
     * Validate that search results contain only visits that actually match the criteria
     * 
     * @param searchText The search text
     * @param searchType The search type
     * @param results The results to validate
     * @return true if all results match the search criteria
     */
    boolean validateResultsMatchCriteria(String searchText, String searchType, List<Visit> results);

    /**
     * Check for duplicate visits in search results
     * 
     * @param results The search results to check
     * @return true if no duplicates are found
     */
    boolean validateNoDuplicateResults(List<Visit> results);

    /**
     * Validation result for search operations
     */
    class ValidationResult {
        private final boolean valid;
        private final String message;
        private final List<String> issues;
        private final long timestamp;

        public ValidationResult(boolean valid, String message, List<String> issues) {
            this.valid = valid;
            this.message = message;
            this.issues = issues;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public List<String> getIssues() { return issues; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Consistency report across different search types
     */
    class ConsistencyReport {
        private final String searchText;
        private final Map<String, Long> resultCounts;
        private final Map<String, List<String>> issues;
        private final boolean consistent;
        private final long timestamp;

        public ConsistencyReport(String searchText, Map<String, Long> resultCounts, Map<String, List<String>> issues, boolean consistent) {
            this.searchText = searchText;
            this.resultCounts = resultCounts;
            this.issues = issues;
            this.consistent = consistent;
            this.timestamp = System.currentTimeMillis();
        }

        public String getSearchText() { return searchText; }
        public Map<String, Long> getResultCounts() { return resultCounts; }
        public Map<String, List<String>> getIssues() { return issues; }
        public boolean isConsistent() { return consistent; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Monitoring result for data consistency
     */
    class MonitoringResult {
        private final String searchText;
        private final String searchType;
        private final long expectedCount;
        private final long actualCount;
        private final boolean consistent;
        private final double consistencyScore;
        private final List<String> anomalies;
        private final long timestamp;

        public MonitoringResult(String searchText, String searchType, long expectedCount, long actualCount, 
                              boolean consistent, double consistencyScore, List<String> anomalies) {
            this.searchText = searchText;
            this.searchType = searchType;
            this.expectedCount = expectedCount;
            this.actualCount = actualCount;
            this.consistent = consistent;
            this.consistencyScore = consistencyScore;
            this.anomalies = anomalies;
            this.timestamp = System.currentTimeMillis();
        }

        public String getSearchText() { return searchText; }
        public String getSearchType() { return searchType; }
        public long getExpectedCount() { return expectedCount; }
        public long getActualCount() { return actualCount; }
        public boolean isConsistent() { return consistent; }
        public double getConsistencyScore() { return consistencyScore; }
        public List<String> getAnomalies() { return anomalies; }
        public long getTimestamp() { return timestamp; }
    }
}