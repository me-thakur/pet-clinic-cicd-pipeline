package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.FilterCriteria;
import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.model.VisitType;
import com.petclinic.backend.service.VisitFilterService;
import com.petclinic.backend.service.VisitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of visit filtering service
 * Provides comprehensive filtering capabilities for visits
 * 
 * Validates: Requirements 13.1, 13.2, 13.3, 13.4, 13.5
 */
@Service
public class VisitFilterServiceImpl implements VisitFilterService {

    private static final Logger logger = LoggerFactory.getLogger(VisitFilterServiceImpl.class);

    @Autowired
    private VisitService visitService;

    // Valid filter fields
    private static final Set<String> VALID_FILTER_FIELDS = Set.of(
        "status", "visitType", "petId", "veterinarianId", "dateRange", 
        "startDate", "endDate", "emergencyOnly", "completedOnly"
    );

    // Valid status values
    private static final Set<String> VALID_STATUS_VALUES = Set.of(
        "completed", "pending", "cancelled", "emergency", "all"
    );

    @Override
    public PagedResponse<Visit> applyFilters(List<FilterCriteria> filters, Pageable pageable) {
        logger.debug("Applying {} filters to visits with pagination: {}", filters.size(), pageable);

        try {
            // Start with all visits
            List<Visit> allVisits = visitService.findAll();
            List<Visit> filteredVisits = new ArrayList<>(allVisits);

            // Apply each filter sequentially
            for (FilterCriteria filter : filters) {
                if (!validateFilterCriteria(filter)) {
                    logger.warn("Invalid filter criteria: {}", filter);
                    continue;
                }

                filteredVisits = applyIndividualFilter(filteredVisits, filter);
            }

            // Apply pagination
            Page<Visit> pagedVisits = createPagedResult(filteredVisits, pageable);

            // Create response with metadata
            PagedResponse<Visit> response = new PagedResponse<>(pagedVisits);
            response.setFilterStatistics(getFilterStatistics(filters));
            response.setActiveFilterSummary(getActiveFilterSummary(filters));

            logger.debug("Applied filters: {} total visits, {} filtered visits, {} page visits", 
                        allVisits.size(), filteredVisits.size(), pagedVisits.getContent().size());

            return response;

        } catch (Exception e) {
            logger.error("Error applying visit filters: {}", e.getMessage(), e);
            // Return empty result on error
            Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            return new PagedResponse<>(emptyPage);
        }
    }

    @Override
    public PagedResponse<Visit> filterByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        logger.debug("Filtering visits by date range: {} to {}", startDate, endDate);

        try {
            List<Visit> allVisits = visitService.findAll();
            List<Visit> filteredVisits = allVisits.stream()
                .filter(visit -> {
                    LocalDate visitDate = visit.getVisitDate().toLocalDate();
                    boolean afterStart = startDate == null || !visitDate.isBefore(startDate);
                    boolean beforeEnd = endDate == null || !visitDate.isAfter(endDate);
                    return afterStart && beforeEnd;
                })
                .collect(Collectors.toList());

            Page<Visit> pagedVisits = createPagedResult(filteredVisits, pageable);
            PagedResponse<Visit> response = new PagedResponse<>(pagedVisits);

            // Add date range metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("startDate", startDate);
            metadata.put("endDate", endDate);
            metadata.put("totalInRange", filteredVisits.size());
            response.setFilterStatistics(metadata);

            logger.debug("Date range filter: {} visits found between {} and {}", 
                        filteredVisits.size(), startDate, endDate);

            return response;

        } catch (Exception e) {
            logger.error("Error filtering visits by date range: {}", e.getMessage(), e);
            Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            return new PagedResponse<>(emptyPage);
        }
    }

    @Override
    public PagedResponse<Visit> filterByVisitType(String visitType, Pageable pageable) {
        logger.debug("Filtering visits by visit type: {}", visitType);

        try {
            List<Visit> allVisits = visitService.findAll();
            List<Visit> filteredVisits = allVisits.stream()
                .filter(visit -> {
                    if (visit.getVisitType() == null) {
                        return false;
                    }
                    return visit.getVisitType().name().equalsIgnoreCase(visitType) ||
                           visit.getVisitType().getDisplayName().equalsIgnoreCase(visitType);
                })
                .collect(Collectors.toList());

            Page<Visit> pagedVisits = createPagedResult(filteredVisits, pageable);
            PagedResponse<Visit> response = new PagedResponse<>(pagedVisits);

            // Add visit type metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("visitType", visitType);
            metadata.put("totalOfType", filteredVisits.size());
            response.setFilterStatistics(metadata);

            logger.debug("Visit type filter: {} visits found with type {}", filteredVisits.size(), visitType);

            return response;

        } catch (Exception e) {
            logger.error("Error filtering visits by visit type: {}", e.getMessage(), e);
            Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            return new PagedResponse<>(emptyPage);
        }
    }

    @Override
    public PagedResponse<Visit> filterByStatus(String status, Pageable pageable) {
        logger.debug("Filtering visits by status: {}", status);

        try {
            List<Visit> allVisits = visitService.findAll();
            List<Visit> filteredVisits = new ArrayList<>();

            String normalizedStatus = status.toLowerCase().trim();
            
            switch (normalizedStatus) {
                case "completed":
                    filteredVisits = allVisits.stream()
                        .filter(visit -> {
                            try {
                                return visit.isCompleted();
                            } catch (Exception e) {
                                logger.warn("Error checking completion status for visit {}: {}", 
                                           visit.getId(), e.getMessage());
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
                    break;

                case "pending":
                    filteredVisits = allVisits.stream()
                        .filter(visit -> {
                            try {
                                return !visit.isCompleted();
                            } catch (Exception e) {
                                logger.warn("Error checking completion status for visit {}: {}", 
                                           visit.getId(), e.getMessage());
                                return true; // Default to pending for error cases
                            }
                        })
                        .collect(Collectors.toList());
                    break;

                case "emergency":
                    filteredVisits = allVisits.stream()
                        .filter(visit -> visit.getVisitType() != null && visit.getVisitType().isEmergency())
                        .collect(Collectors.toList());
                    break;

                case "cancelled":
                    // For now, we don't have a cancelled status in the model
                    // This could be extended in the future with a proper status field
                    filteredVisits = new ArrayList<>();
                    break;

                case "all":
                default:
                    filteredVisits = new ArrayList<>(allVisits);
                    break;
            }

            Page<Visit> pagedVisits = createPagedResult(filteredVisits, pageable);
            PagedResponse<Visit> response = new PagedResponse<>(pagedVisits);

            // Add status metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("status", normalizedStatus);
            metadata.put("totalWithStatus", filteredVisits.size());
            
            // Add status breakdown
            Map<String, Long> statusBreakdown = calculateStatusBreakdown(filteredVisits);
            metadata.put("statusBreakdown", statusBreakdown);
            
            response.setFilterStatistics(metadata);

            logger.debug("Status filter: {} visits found with status {}", filteredVisits.size(), status);

            return response;

        } catch (Exception e) {
            logger.error("Error filtering visits by status: {}", e.getMessage(), e);
            Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            return new PagedResponse<>(emptyPage);
        }
    }

    @Override
    public PagedResponse<Visit> filterByPet(Long petId, Pageable pageable) {
        logger.debug("Filtering visits by pet ID: {}", petId);

        try {
            List<Visit> petVisits = visitService.findByPet(petId);
            Page<Visit> pagedVisits = createPagedResult(petVisits, pageable);
            PagedResponse<Visit> response = new PagedResponse<>(pagedVisits);

            // Add pet metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("petId", petId);
            metadata.put("totalForPet", petVisits.size());
            response.setFilterStatistics(metadata);

            logger.debug("Pet filter: {} visits found for pet ID {}", petVisits.size(), petId);

            return response;

        } catch (Exception e) {
            logger.error("Error filtering visits by pet: {}", e.getMessage(), e);
            Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            return new PagedResponse<>(emptyPage);
        }
    }

    @Override
    public PagedResponse<Visit> filterByVeterinarian(Long veterinarianId, Pageable pageable) {
        logger.debug("Filtering visits by veterinarian ID: {}", veterinarianId);

        try {
            List<Visit> vetVisits = visitService.findByVeterinarian(veterinarianId);
            Page<Visit> pagedVisits = createPagedResult(vetVisits, pageable);
            PagedResponse<Visit> response = new PagedResponse<>(pagedVisits);

            // Add veterinarian metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("veterinarianId", veterinarianId);
            metadata.put("totalForVeterinarian", vetVisits.size());
            response.setFilterStatistics(metadata);

            logger.debug("Veterinarian filter: {} visits found for veterinarian ID {}", 
                        vetVisits.size(), veterinarianId);

            return response;

        } catch (Exception e) {
            logger.error("Error filtering visits by veterinarian: {}", e.getMessage(), e);
            Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            return new PagedResponse<>(emptyPage);
        }
    }

    @Override
    public List<String> getAvailableFilterValues(String filterType) {
        logger.debug("Getting available filter values for type: {}", filterType);

        try {
            switch (filterType.toLowerCase()) {
                case "status":
                    return Arrays.asList("all", "completed", "pending", "emergency", "cancelled");

                case "visittype":
                    return Arrays.stream(VisitType.values())
                        .map(VisitType::getDisplayName)
                        .collect(Collectors.toList());

                case "pets":
                    return visitService.findAll().stream()
                        .filter(visit -> visit.getPet() != null)
                        .map(visit -> visit.getPet().getName())
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());

                case "veterinarians":
                    return visitService.findAll().stream()
                        .filter(visit -> visit.getVeterinarian() != null)
                        .map(visit -> "Dr. " + visit.getVeterinarian().getLastName())
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());

                default:
                    logger.warn("Unknown filter type: {}", filterType);
                    return Collections.emptyList();
            }

        } catch (Exception e) {
            logger.error("Error getting filter values for type {}: {}", filterType, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Object> getFilterStatistics(List<FilterCriteria> filters) {
        Map<String, Object> statistics = new HashMap<>();

        try {
            List<Visit> allVisits = visitService.findAll();
            statistics.put("totalVisits", allVisits.size());

            // Calculate status breakdown
            Map<String, Long> statusBreakdown = calculateStatusBreakdown(allVisits);
            statistics.put("statusBreakdown", statusBreakdown);

            // Calculate visit type breakdown
            Map<String, Long> visitTypeBreakdown = allVisits.stream()
                .filter(visit -> visit.getVisitType() != null)
                .collect(Collectors.groupingBy(
                    visit -> visit.getVisitType().getDisplayName(),
                    Collectors.counting()
                ));
            statistics.put("visitTypeBreakdown", visitTypeBreakdown);

            // Add filter-specific statistics
            for (FilterCriteria filter : filters) {
                String key = filter.getField() + "FilterCount";
                // This would be calculated based on the specific filter
                statistics.put(key, 0); // Placeholder
            }

        } catch (Exception e) {
            logger.error("Error calculating filter statistics: {}", e.getMessage(), e);
        }

        return statistics;
    }

    @Override
    public boolean validateFilterCriteria(FilterCriteria criteria) {
        if (criteria == null || criteria.getField() == null || criteria.getValue() == null) {
            return false;
        }

        String field = criteria.getField().toLowerCase();
        String value = criteria.getValue() != null ? criteria.getValue().toString().toLowerCase() : "";

        // Validate field
        if (!VALID_FILTER_FIELDS.contains(field)) {
            logger.warn("Invalid filter field: {}", field);
            return false;
        }

        // Validate value based on field
        switch (field) {
            case "status":
                return VALID_STATUS_VALUES.contains(value);
            case "visittype":
                return Arrays.stream(VisitType.values())
                    .anyMatch(type -> type.name().equalsIgnoreCase(value) || 
                                    type.getDisplayName().equalsIgnoreCase(value));
            case "petid":
            case "veterinarianid":
                try {
                    Long.parseLong(criteria.getValue().toString());
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            default:
                return true; // Allow other fields for now
        }
    }

    @Override
    public PagedResponse<Visit> clearAllFilters(Pageable pageable) {
        logger.debug("Clearing all filters and returning all visits");

        try {
            Page<Visit> allVisits = visitService.findAllWithPagination(pageable);
            PagedResponse<Visit> response = new PagedResponse<>(allVisits);

            // Add metadata indicating no filters
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("filtersCleared", true);
            metadata.put("totalVisits", allVisits.getTotalElements());
            response.setFilterStatistics(metadata);

            logger.debug("Cleared all filters: {} total visits", allVisits.getTotalElements());

            return response;

        } catch (Exception e) {
            logger.error("Error clearing all filters: {}", e.getMessage(), e);
            Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            return new PagedResponse<>(emptyPage);
        }
    }

    @Override
    public Map<String, Object> getActiveFilterSummary(List<FilterCriteria> filters) {
        Map<String, Object> summary = new HashMap<>();

        try {
            summary.put("totalActiveFilters", filters.size());
            summary.put("hasActiveFilters", !filters.isEmpty());

            // Group filters by type
            Map<String, List<FilterCriteria>> filtersByType = filters.stream()
                .collect(Collectors.groupingBy(FilterCriteria::getField));

            summary.put("filtersByType", filtersByType);

            // Create display-friendly filter list
            List<Map<String, String>> displayFilters = filters.stream()
                .map(filter -> {
                    Map<String, String> displayFilter = new HashMap<>();
                    displayFilter.put("field", filter.getField());
                    displayFilter.put("value", filter.getValue() != null ? filter.getValue().toString() : "");
                    displayFilter.put("operator", filter.getOperator());
                    displayFilter.put("displayName", createFilterDisplayName(filter));
                    return displayFilter;
                })
                .collect(Collectors.toList());

            summary.put("displayFilters", displayFilters);

        } catch (Exception e) {
            logger.error("Error creating active filter summary: {}", e.getMessage(), e);
        }

        return summary;
    }

    // Helper Methods

    private List<Visit> applyIndividualFilter(List<Visit> visits, FilterCriteria filter) {
        String field = filter.getField().toLowerCase();
        String value = filter.getValue() != null ? filter.getValue().toString() : "";
        String operator = filter.getOperator();

        switch (field) {
            case "status":
                return filterVisitsByStatus(visits, value);
            case "visittype":
                return filterVisitsByType(visits, value);
            case "petid":
                return filterVisitsByPetId(visits, Long.parseLong(value));
            case "veterinarianid":
                return filterVisitsByVeterinarianId(visits, Long.parseLong(value));
            case "startdate":
                return filterVisitsByStartDate(visits, value);
            case "enddate":
                return filterVisitsByEndDate(visits, value);
            default:
                logger.warn("Unknown filter field: {}", field);
                return visits;
        }
    }

    private List<Visit> filterVisitsByStatus(List<Visit> visits, String status) {
        String normalizedStatus = status.toLowerCase().trim();
        
        return visits.stream()
            .filter(visit -> {
                switch (normalizedStatus) {
                    case "completed":
                        try {
                            return visit.isCompleted();
                        } catch (Exception e) {
                            return false;
                        }
                    case "pending":
                        try {
                            return !visit.isCompleted();
                        } catch (Exception e) {
                            return true;
                        }
                    case "emergency":
                        return visit.getVisitType() != null && visit.getVisitType().isEmergency();
                    case "cancelled":
                        return false; // Not implemented yet
                    case "all":
                    default:
                        return true;
                }
            })
            .collect(Collectors.toList());
    }

    private List<Visit> filterVisitsByType(List<Visit> visits, String visitType) {
        return visits.stream()
            .filter(visit -> visit.getVisitType() != null && 
                           (visit.getVisitType().name().equalsIgnoreCase(visitType) ||
                            visit.getVisitType().getDisplayName().equalsIgnoreCase(visitType)))
            .collect(Collectors.toList());
    }

    private List<Visit> filterVisitsByPetId(List<Visit> visits, Long petId) {
        return visits.stream()
            .filter(visit -> visit.getPet() != null && visit.getPet().getId().equals(petId))
            .collect(Collectors.toList());
    }

    private List<Visit> filterVisitsByVeterinarianId(List<Visit> visits, Long veterinarianId) {
        return visits.stream()
            .filter(visit -> visit.getVeterinarian() != null && 
                           visit.getVeterinarian().getId().equals(veterinarianId))
            .collect(Collectors.toList());
    }

    private List<Visit> filterVisitsByStartDate(List<Visit> visits, String startDateStr) {
        try {
            LocalDate startDate = LocalDate.parse(startDateStr);
            return visits.stream()
                .filter(visit -> !visit.getVisitDate().toLocalDate().isBefore(startDate))
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Invalid start date format: {}", startDateStr);
            return visits;
        }
    }

    private List<Visit> filterVisitsByEndDate(List<Visit> visits, String endDateStr) {
        try {
            LocalDate endDate = LocalDate.parse(endDateStr);
            return visits.stream()
                .filter(visit -> !visit.getVisitDate().toLocalDate().isAfter(endDate))
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Invalid end date format: {}", endDateStr);
            return visits;
        }
    }

    private Page<Visit> createPagedResult(List<Visit> visits, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), visits.size());
        
        if (start > visits.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, visits.size());
        }
        
        List<Visit> pageContent = visits.subList(start, end);
        return new PageImpl<>(pageContent, pageable, visits.size());
    }

    private Map<String, Long> calculateStatusBreakdown(List<Visit> visits) {
        Map<String, Long> breakdown = new HashMap<>();
        
        long completedCount = visits.stream()
            .mapToLong(visit -> {
                try {
                    return visit.isCompleted() ? 1 : 0;
                } catch (Exception e) {
                    return 0;
                }
            })
            .sum();
        
        long pendingCount = visits.size() - completedCount;
        long emergencyCount = visits.stream()
            .mapToLong(visit -> (visit.getVisitType() != null && visit.getVisitType().isEmergency()) ? 1 : 0)
            .sum();
        
        breakdown.put("completed", completedCount);
        breakdown.put("pending", pendingCount);
        breakdown.put("emergency", emergencyCount);
        breakdown.put("cancelled", 0L); // Not implemented yet
        breakdown.put("total", (long) visits.size());
        
        return breakdown;
    }

    private String createFilterDisplayName(FilterCriteria filter) {
        String field = filter.getField();
        String value = filter.getValue() != null ? filter.getValue().toString() : "";
        
        switch (field.toLowerCase()) {
            case "status":
                return "Status: " + (value.length() > 0 ? value.substring(0, 1).toUpperCase() + value.substring(1) : "");
            case "visittype":
                return "Type: " + value;
            case "petid":
                return "Pet ID: " + value;
            case "veterinarianid":
                return "Veterinarian ID: " + value;
            default:
                return field + ": " + value;
        }
    }
}