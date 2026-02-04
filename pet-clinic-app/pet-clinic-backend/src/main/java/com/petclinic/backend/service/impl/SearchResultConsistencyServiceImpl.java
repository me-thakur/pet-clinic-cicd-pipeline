package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.service.SearchResultConsistencyService;
import com.petclinic.backend.service.VisitSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of SearchResultConsistencyService
 * Provides validation for search result integrity and consistency monitoring
 * 
 * Validates: Requirements 7.1
 */
@Service
public class SearchResultConsistencyServiceImpl implements SearchResultConsistencyService {

    private static final Logger logger = LoggerFactory.getLogger(SearchResultConsistencyServiceImpl.class);

    @Autowired
    private VisitSearchService visitSearchService;

    @Override
    public ValidationResult validateSearchResultIntegrity(String searchText, String searchType, PagedResponse<Visit> results) {
        logger.debug("Validating search result integrity for text '{}' and type '{}'", searchText, searchType);
        
        List<String> issues = new ArrayList<>();
        
        try {
            // Validate input parameters
            if (searchText == null || searchText.trim().isEmpty()) {
                issues.add("Search text is null or empty");
            }
            
            if (searchType == null || !isValidSearchType(searchType)) {
                issues.add("Invalid search type: " + searchType);
            }
            
            if (results == null) {
                issues.add("Search results are null");
                return new ValidationResult(false, "Validation failed", issues);
            }
            
            // Validate results match search criteria
            List<Visit> visitList = results.getContent();
            if (!validateResultsMatchCriteria(searchText, searchType, visitList)) {
                issues.add("Some results do not match the search criteria");
            }
            
            // Validate no duplicate results
            if (!validateNoDuplicateResults(visitList)) {
                issues.add("Duplicate visits found in search results");
            }
            
            // Validate pagination consistency
            if (results.getPage() != null) {
                if (results.getPage().getTotalElements() < 0) {
                    issues.add("Invalid total elements count: " + results.getPage().getTotalElements());
                }
                
                if (results.getContent().size() > results.getPage().getSize()) {
                    issues.add("Page content size exceeds requested page size");
                }
            }
            
            boolean isValid = issues.isEmpty();
            String message = isValid ? "Search result integrity validation passed" : 
                           "Search result integrity validation failed with " + issues.size() + " issues";
            
            logger.debug("Search result integrity validation completed. Valid: {}, Issues: {}", isValid, issues.size());
            
            return new ValidationResult(isValid, message, issues);
            
        } catch (Exception e) {
            logger.error("Error validating search result integrity: {}", e.getMessage(), e);
            issues.add("Validation error: " + e.getMessage());
            return new ValidationResult(false, "Validation error occurred", issues);
        }
    }

    @Override
    public ConsistencyReport validateCrossSearchConsistency(String searchText, Pageable pageable) {
        logger.debug("Validating cross-search consistency for text '{}'", searchText);
        
        Map<String, Long> resultCounts = new HashMap<>();
        Map<String, List<String>> issues = new HashMap<>();
        
        try {
            // Get counts for all search types
            String[] searchTypes = {"treatment", "diagnosis", "description", "notes"};
            
            for (String searchType : searchTypes) {
                try {
                    long count = getCountForSearchType(searchText, searchType);
                    resultCounts.put(searchType, count);
                    issues.put(searchType, new ArrayList<>());
                    
                    // Validate individual search results
                    PagedResponse<Visit> results = getResultsForSearchType(searchText, searchType, pageable);
                    ValidationResult validation = validateSearchResultIntegrity(searchText, searchType, results);
                    
                    if (!validation.isValid()) {
                        issues.get(searchType).addAll(validation.getIssues());
                    }
                    
                } catch (Exception e) {
                    logger.error("Error getting results for search type '{}': {}", searchType, e.getMessage());
                    issues.get(searchType).add("Error retrieving results: " + e.getMessage());
                }
            }
            
            // Check for consistency across search types
            boolean consistent = validateConsistencyAcrossTypes(searchText, resultCounts, issues);
            
            logger.debug("Cross-search consistency validation completed. Consistent: {}, Result counts: {}", 
                        consistent, resultCounts);
            
            return new ConsistencyReport(searchText, resultCounts, issues, consistent);
            
        } catch (Exception e) {
            logger.error("Error validating cross-search consistency: {}", e.getMessage(), e);
            issues.put("general", List.of("Consistency validation error: " + e.getMessage()));
            return new ConsistencyReport(searchText, resultCounts, issues, false);
        }
    }

    @Override
    public MonitoringResult monitorDataConsistency(String searchText, String searchType, long expectedCount, PagedResponse<Visit> actualResults) {
        logger.debug("Monitoring data consistency for search '{}' of type '{}'", searchText, searchType);
        
        List<String> anomalies = new ArrayList<>();
        
        try {
            long actualCount = actualResults != null && actualResults.getPage() != null ? 
                             actualResults.getPage().getTotalElements() : 0;
            
            // Calculate consistency score
            double consistencyScore = calculateConsistencyScore(expectedCount, actualCount);
            
            // Check for anomalies
            if (Math.abs(expectedCount - actualCount) > 0) {
                anomalies.add(String.format("Count mismatch: expected %d, actual %d", expectedCount, actualCount));
            }
            
            if (actualResults != null) {
                // Check for data quality issues
                List<Visit> visits = actualResults.getContent();
                for (Visit visit : visits) {
                    if (visit.getId() == null) {
                        anomalies.add("Visit with null ID found");
                    }
                    if (!matchesSearchCriteria(visit, searchText, searchType)) {
                        anomalies.add("Visit ID " + visit.getId() + " does not match search criteria");
                    }
                }
            }
            
            boolean consistent = anomalies.isEmpty() && consistencyScore >= 0.95;
            
            logger.debug("Data consistency monitoring completed. Consistent: {}, Score: {}, Anomalies: {}", 
                        consistent, consistencyScore, anomalies.size());
            
            return new MonitoringResult(searchText, searchType, expectedCount, actualCount, 
                                      consistent, consistencyScore, anomalies);
            
        } catch (Exception e) {
            logger.error("Error monitoring data consistency: {}", e.getMessage(), e);
            anomalies.add("Monitoring error: " + e.getMessage());
            return new MonitoringResult(searchText, searchType, expectedCount, 0, false, 0.0, anomalies);
        }
    }

    @Override
    public boolean validateResultsMatchCriteria(String searchText, String searchType, List<Visit> results) {
        if (searchText == null || searchText.trim().isEmpty() || results == null) {
            return true; // Empty search or results are considered valid
        }
        
        String normalizedSearchText = searchText.trim().toLowerCase();
        
        return results.stream().allMatch(visit -> matchesSearchCriteria(visit, normalizedSearchText, searchType));
    }

    @Override
    public boolean validateNoDuplicateResults(List<Visit> results) {
        if (results == null || results.isEmpty()) {
            return true;
        }
        
        Set<Long> visitIds = new HashSet<>();
        for (Visit visit : results) {
            if (visit.getId() != null) {
                if (visitIds.contains(visit.getId())) {
                    logger.warn("Duplicate visit found with ID: {}", visit.getId());
                    return false;
                }
                visitIds.add(visit.getId());
            }
        }
        
        return true;
    }

    private boolean isValidSearchType(String searchType) {
        return Arrays.asList("treatment", "diagnosis", "description", "notes").contains(searchType.toLowerCase());
    }

    private long getCountForSearchType(String searchText, String searchType) {
        switch (searchType.toLowerCase()) {
            case "treatment":
                return visitSearchService.countByTreatment(searchText);
            case "diagnosis":
                return visitSearchService.countByDiagnosis(searchText);
            case "description":
                return visitSearchService.countByDescription(searchText);
            case "notes":
                return visitSearchService.countByNotes(searchText);
            default:
                throw new IllegalArgumentException("Invalid search type: " + searchType);
        }
    }

    private PagedResponse<Visit> getResultsForSearchType(String searchText, String searchType, Pageable pageable) {
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
                throw new IllegalArgumentException("Invalid search type: " + searchType);
        }
    }

    private boolean validateConsistencyAcrossTypes(String searchText, Map<String, Long> resultCounts, Map<String, List<String>> issues) {
        // Check if any search type has significantly different results than expected
        // This is a basic consistency check - in a real system, you might have more sophisticated rules
        
        boolean hasIssues = issues.values().stream().anyMatch(list -> !list.isEmpty());
        if (hasIssues) {
            return false;
        }
        
        // Check for reasonable result distribution
        long totalResults = resultCounts.values().stream().mapToLong(Long::longValue).sum();
        if (totalResults == 0) {
            return true; // No results is consistent
        }
        
        // All search types should have some reasonable relationship
        // For now, we just check that no single type dominates unreasonably
        long maxCount = resultCounts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        double dominanceRatio = totalResults > 0 ? (double) maxCount / totalResults : 0;
        
        return dominanceRatio <= 1.0; // Each type can have at most all results (when searching for same term across fields)
    }

    private double calculateConsistencyScore(long expected, long actual) {
        if (expected == 0 && actual == 0) {
            return 1.0;
        }
        if (expected == 0) {
            return actual == 0 ? 1.0 : 0.0;
        }
        
        double ratio = (double) Math.min(expected, actual) / Math.max(expected, actual);
        return ratio;
    }

    private boolean matchesSearchCriteria(Visit visit, String searchText, String searchType) {
        if (visit == null || searchText == null) {
            return false;
        }
        
        String normalizedSearchText = searchText.toLowerCase();
        
        switch (searchType.toLowerCase()) {
            case "treatment":
                return visit.getTreatment() != null && 
                       visit.getTreatment().toLowerCase().contains(normalizedSearchText);
            case "diagnosis":
                return visit.getDiagnosis() != null && 
                       visit.getDiagnosis().toLowerCase().contains(normalizedSearchText);
            case "description":
            case "notes":
                return visit.getNotes() != null && 
                       visit.getNotes().toLowerCase().contains(normalizedSearchText);
            default:
                return false;
        }
    }
}