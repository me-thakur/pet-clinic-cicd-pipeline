package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.*;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.model.VisitType;
import com.petclinic.backend.repository.VisitRepository;
import com.petclinic.backend.service.EnhancedVisitService;
import com.petclinic.backend.service.VisitService;
import com.petclinic.backend.util.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced visit service implementation providing server-side sorting, filtering, and bulk operations
 * Extends base visit functionality with enhanced table capabilities
 * Validates: Requirements 3.2, 3.4
 */
@Service
@Transactional
public class EnhancedVisitServiceImpl implements EnhancedVisitService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedVisitServiceImpl.class);
    
    @Autowired
    private VisitRepository visitRepository;
    
    @Autowired
    private VisitService visitService;
    
    // Delegate base VisitService methods to the injected service
    
    @Override
    public Visit create(Visit visit) {
        return visitService.create(visit);
    }
    
    @Override
    public Visit update(Long id, Visit visit) {
        return visitService.update(id, visit);
    }
    
    @Override
    public Optional<Visit> findById(Long id) {
        return visitService.findById(id);
    }
    
    @Override
    public List<Visit> findAll() {
        return visitService.findAll();
    }
    
    @Override
    public void deleteById(Long id) {
        visitService.deleteById(id);
    }
    
    @Override
    public boolean existsById(Long id) {
        return visitService.existsById(id);
    }
    
    @Override
    public long count() {
        return visitService.count();
    }
    
    @Override
    public Visit scheduleVisit(Visit visit) {
        return visitService.scheduleVisit(visit);
    }
    
    @Override
    public Visit completeVisit(Long id, String diagnosis, String treatment, String notes) {
        return visitService.completeVisit(id, diagnosis, treatment, notes);
    }
    
    @Override
    public List<Visit> findByPet(Long petId) {
        return visitService.findByPet(petId);
    }
    
    @Override
    public List<Visit> findByVeterinarian(Long vetId) {
        return visitService.findByVeterinarian(vetId);
    }
    
    @Override
    public List<Visit> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return visitService.findByDateRange(startDate, endDate);
    }
    
    @Override
    public List<Visit> findByDate(LocalDate date) {
        return visitService.findByDate(date);
    }
    
    @Override
    public boolean hasSchedulingConflict(Long vetId, LocalDateTime dateTime, int duration) {
        return visitService.hasSchedulingConflict(vetId, dateTime, duration);
    }
    
    @Override
    public List<Visit> getUpcomingVisits(Long vetId, int days) {
        return visitService.getUpcomingVisits(vetId, days);
    }
    
    @Override
    public List<Visit> findCompletedVisits() {
        return visitService.findCompletedVisits();
    }
    
    @Override
    public List<Visit> findByCostRange(BigDecimal minCost, BigDecimal maxCost) {
        return visitService.findByCostRange(minCost, maxCost);
    }
    
    @Override
    public VisitStatistics getVisitStatistics(LocalDate startDate, LocalDate endDate) {
        return visitService.getVisitStatistics(startDate, endDate);
    }
    
    @Override
    public Page<Visit> findAllWithPagination(Pageable pageable) {
        logger.debug("Finding all visits with pagination: {}", pageable);
        return visitRepository.findAll(pageable);
    }
    
    // Enhanced functionality implementation
    
    @Override
    public PagedResponse<Visit> findVisitsWithSortAndFilter(
            Pageable pageable,
            SortMetadata sortMetadata,
            List<FilterCriteria> filters) {
        
        logger.debug("Finding visits with sort and filter: pageable={}, sort={}, filters={}", 
                    pageable, sortMetadata, filters);
        
        try {
            // Start with all visits
            List<Visit> allVisits = visitRepository.findAll();
            
            // Apply filters
            List<Visit> filteredVisits = applyFilters(allVisits, filters);
            
            // Apply sorting
            List<Visit> sortedVisits = applySorting(filteredVisits, sortMetadata);
            
            // Apply pagination
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), sortedVisits.size());
            List<Visit> pageContent = sortedVisits.subList(start, end);
            
            // Create page
            Page<Visit> page = new PageImpl<>(pageContent, pageable, sortedVisits.size());
            
            // Create response
            PagedResponse<Visit> response = new PagedResponse<>(page);
            
            logger.debug("Found {} visits after filtering and sorting, returning page with {} items", 
                        sortedVisits.size(), pageContent.size());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error finding visits with sort and filter: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing visit query", e);
        }
    }
    
    @Override
    public List<String> getAvailableVisitStatusValues() {
        logger.debug("Getting available visit status values");
        
        // Visit status is based on completion: completed or pending
        return Arrays.asList("completed", "pending");
    }
    
    @Override
    public List<String> getAvailableVisitTypeValues() {
        logger.debug("Getting available visit type values");
        
        return Arrays.stream(VisitType.values())
            .map(VisitType::name)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Visit> findVisitsByCompletionStatus(boolean completed) {
        logger.debug("Finding visits by completion status: {}", completed);
        
        List<Visit> allVisits = visitRepository.findAll();
        return allVisits.stream()
            .filter(visit -> {
                try {
                    return visit.isCompleted() == completed;
                } catch (Exception e) {
                    logger.warn("Error calculating completion status for visit {}: {}. Defaulting to pending.", 
                               visit.getId(), e.getMessage());
                    return !completed; // Default to pending for error cases
                }
            })
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Visit> findVisitsByType(String visitType) {
        logger.debug("Finding visits by type: {}", visitType);
        
        if (visitType == null || visitType.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            VisitType type = VisitType.valueOf(visitType.toUpperCase());
            List<Visit> allVisits = visitRepository.findAll();
            return allVisits.stream()
                .filter(visit -> type.equals(visit.getVisitType()))
                .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid visit type: {}", visitType);
            return new ArrayList<>();
        }
    }
    
    @Override
    public BulkOperationResult bulkDeleteVisits(BulkDeleteRequest request) {
        logger.debug("Bulk deleting visits: {}", request);
        
        try {
            List<String> errors = new ArrayList<>();
            int deletedCount = 0;
            int totalRequested = 0;
            
            if (request.getSelectedIds() != null && !request.getSelectedIds().isEmpty()) {
                totalRequested = request.getSelectedIds().size();
                
                for (Long id : request.getSelectedIds()) {
                    try {
                        if (visitRepository.existsById(id)) {
                            visitRepository.deleteById(id);
                            deletedCount++;
                            logger.debug("Deleted visit with ID: {}", id);
                        } else {
                            errors.add("Visit with ID " + id + " not found");
                        }
                    } catch (Exception e) {
                        logger.error("Error deleting visit with ID {}: {}", id, e.getMessage());
                        errors.add("Error deleting visit " + id + ": " + e.getMessage());
                    }
                }
            } else if (request.isSelectAll() && request.getCurrentFilters() != null) {
                // Handle select all with filters
                List<Visit> allVisits = visitRepository.findAll();
                List<Visit> filteredVisits = applyFilters(allVisits, request.getCurrentFilters());
                totalRequested = filteredVisits.size();
                
                for (Visit visit : filteredVisits) {
                    try {
                        visitRepository.deleteById(visit.getId());
                        deletedCount++;
                        logger.debug("Deleted visit with ID: {}", visit.getId());
                    } catch (Exception e) {
                        logger.error("Error deleting visit with ID {}: {}", visit.getId(), e.getMessage());
                        errors.add("Error deleting visit " + visit.getId() + ": " + e.getMessage());
                    }
                }
            }
            
            BulkOperationResult result = new BulkOperationResult();
            result.setDeletedCount(deletedCount);
            result.setTotalRequested(totalRequested);
            result.setErrors(errors);
            result.setSuccess(errors.isEmpty());
            
            logger.info("Bulk delete completed: deleted {}/{} visits", deletedCount, totalRequested);
            return result;
            
        } catch (Exception e) {
            logger.error("Error during bulk delete: {}", e.getMessage(), e);
            BulkOperationResult result = new BulkOperationResult();
            result.setDeletedCount(0);
            result.setTotalRequested(0);
            result.setErrors(Arrays.asList("Bulk delete failed: " + e.getMessage()));
            result.setSuccess(false);
            return result;
        }
    }
    
    @Override
    public List<String> getSortableVisitColumns() {
        return Arrays.asList(
            "id", "visitDate", "visitType", "diagnosis", "treatment", 
            "cost", "duration", "notes", "pet.name", "veterinarian.lastName"
        );
    }
    
    @Override
    public List<String> getFilterableVisitColumns() {
        return Arrays.asList(
            "status", "visitType", "pet.name", "veterinarian.lastName", 
            "diagnosis", "treatment", "cost"
        );
    }
    
    @Override
    public boolean validateVisitSortCriteria(SortMetadata sortMetadata) {
        if (sortMetadata == null || sortMetadata.getColumn() == null) {
            return false;
        }
        
        List<String> sortableColumns = getSortableVisitColumns();
        String column = sortMetadata.getColumn().toLowerCase();
        
        return sortableColumns.stream()
            .anyMatch(col -> col.toLowerCase().equals(column));
    }
    
    @Override
    public boolean validateVisitFilterCriteria(FilterCriteria filterCriteria) {
        if (filterCriteria == null || filterCriteria.getField() == null) {
            return false;
        }
        
        List<String> filterableColumns = getFilterableVisitColumns();
        String field = filterCriteria.getField().toLowerCase();
        
        return filterableColumns.stream()
            .anyMatch(col -> col.toLowerCase().equals(field));
    }
    
    // Private helper methods
    
    private List<Visit> applyFilters(List<Visit> visits, List<FilterCriteria> filters) {
        if (filters == null || filters.isEmpty()) {
            return visits;
        }
        
        return visits.stream()
            .filter(visit -> matchesAllFilters(visit, filters))
            .collect(Collectors.toList());
    }
    
    private boolean matchesAllFilters(Visit visit, List<FilterCriteria> filters) {
        for (FilterCriteria filter : filters) {
            if (!matchesFilter(visit, filter)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean matchesFilter(Visit visit, FilterCriteria filter) {
        String field = filter.getField().toLowerCase();
        String operator = filter.getOperator().toLowerCase();
        Object value = filter.getValue();
        
        try {
            switch (field) {
                case "status":
                    boolean isCompleted = visit.isCompleted();
                    String status = isCompleted ? "completed" : "pending";
                    return matchesStringValue(status, operator, value);
                    
                case "visittype":
                    String visitType = visit.getVisitType() != null ? visit.getVisitType().name() : "";
                    return matchesStringValue(visitType, operator, value);
                    
                case "pet.name":
                    String petName = visit.getPet() != null ? visit.getPet().getName() : "";
                    return matchesStringValue(petName, operator, value);
                    
                case "veterinarian.lastname":
                    String vetLastName = visit.getVeterinarian() != null ? visit.getVeterinarian().getLastName() : "";
                    return matchesStringValue(vetLastName, operator, value);
                    
                case "diagnosis":
                    String diagnosis = visit.getDiagnosis() != null ? visit.getDiagnosis() : "";
                    return matchesStringValue(diagnosis, operator, value);
                    
                case "treatment":
                    String treatment = visit.getTreatment() != null ? visit.getTreatment() : "";
                    return matchesStringValue(treatment, operator, value);
                    
                case "cost":
                    BigDecimal cost = visit.getCost();
                    return matchesNumericValue(cost, operator, value);
                    
                default:
                    logger.warn("Unknown filter field: {}", field);
                    return true; // Don't filter on unknown fields
            }
        } catch (Exception e) {
            logger.warn("Error applying filter {} to visit {}: {}", filter, visit.getId(), e.getMessage());
            return true; // Don't filter on error
        }
    }
    
    private boolean matchesStringValue(String fieldValue, String operator, Object filterValue) {
        if (fieldValue == null) fieldValue = "";
        String filterStr = filterValue != null ? filterValue.toString().toLowerCase() : "";
        String fieldStr = fieldValue.toLowerCase();
        
        switch (operator) {
            case "equals":
            case "eq":
                return fieldStr.equals(filterStr);
            case "contains":
                return fieldStr.contains(filterStr);
            case "startswith":
                return fieldStr.startsWith(filterStr);
            case "endswith":
                return fieldStr.endsWith(filterStr);
            default:
                return fieldStr.equals(filterStr);
        }
    }
    
    private boolean matchesNumericValue(BigDecimal fieldValue, String operator, Object filterValue) {
        if (fieldValue == null) return false;
        
        try {
            BigDecimal filterNum = new BigDecimal(filterValue.toString());
            
            switch (operator) {
                case "equals":
                case "eq":
                    return fieldValue.compareTo(filterNum) == 0;
                case "gt":
                    return fieldValue.compareTo(filterNum) > 0;
                case "lt":
                    return fieldValue.compareTo(filterNum) < 0;
                case "gte":
                    return fieldValue.compareTo(filterNum) >= 0;
                case "lte":
                    return fieldValue.compareTo(filterNum) <= 0;
                default:
                    return fieldValue.compareTo(filterNum) == 0;
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid numeric filter value: {}", filterValue);
            return false;
        }
    }
    
    private List<Visit> applySorting(List<Visit> visits, SortMetadata sortMetadata) {
        if (sortMetadata == null || sortMetadata.getColumn() == null) {
            return visits;
        }
        
        String column = sortMetadata.getColumn().toLowerCase();
        boolean ascending = "asc".equalsIgnoreCase(sortMetadata.getDirection());
        
        Comparator<Visit> comparator = getComparatorForColumn(column);
        if (comparator == null) {
            logger.warn("No comparator found for column: {}", column);
            return visits;
        }
        
        if (!ascending) {
            comparator = comparator.reversed();
        }
        
        return visits.stream()
            .sorted(comparator)
            .collect(Collectors.toList());
    }
    
    private Comparator<Visit> getComparatorForColumn(String column) {
        switch (column) {
            case "id":
                return Comparator.comparing(Visit::getId, Comparator.nullsLast(Comparator.naturalOrder()));
            case "visitdate":
                return Comparator.comparing(Visit::getVisitDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "visittype":
                return Comparator.comparing(v -> v.getVisitType() != null ? v.getVisitType().name() : "", 
                                          Comparator.nullsLast(Comparator.naturalOrder()));
            case "diagnosis":
                return Comparator.comparing(v -> v.getDiagnosis() != null ? v.getDiagnosis() : "", 
                                          Comparator.nullsLast(Comparator.naturalOrder()));
            case "treatment":
                return Comparator.comparing(v -> v.getTreatment() != null ? v.getTreatment() : "", 
                                          Comparator.nullsLast(Comparator.naturalOrder()));
            case "cost":
                return Comparator.comparing(Visit::getCost, Comparator.nullsLast(Comparator.naturalOrder()));
            case "duration":
                return Comparator.comparing(Visit::getDuration, Comparator.nullsLast(Comparator.naturalOrder()));
            case "pet.name":
                return Comparator.comparing(v -> v.getPet() != null ? v.getPet().getName() : "", 
                                          Comparator.nullsLast(Comparator.naturalOrder()));
            case "veterinarian.lastname":
                return Comparator.comparing(v -> v.getVeterinarian() != null ? v.getVeterinarian().getLastName() : "", 
                                          Comparator.nullsLast(Comparator.naturalOrder()));
            default:
                return null;
        }
    }
}