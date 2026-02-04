package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.*;
import com.petclinic.backend.model.*;
import com.petclinic.backend.service.*;
import com.petclinic.backend.util.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of enhanced table service providing generic sort, filter, and bulk operations
 * Delegates to specific entity services for actual data operations
 * Validates: Requirements 2.2, 2.3
 */
@Service
@Transactional
public class EnhancedTableServiceImpl implements EnhancedTableService<Object> {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedTableServiceImpl.class);
    
    @Autowired
    private VisitService visitService;
    
    @Autowired
    private PetService petService;
    
    @Autowired
    private UserService userService; // For owners
    
    @Autowired
    private VeterinarianService veterinarianService;
    
    @Autowired
    private EnhancedTableCacheService cacheService;
    
    // Repository dependencies for bulk operations
    @Autowired
    private com.petclinic.backend.repository.OwnerRepository ownerRepository;
    
    @Autowired
    private com.petclinic.backend.repository.PetRepository petRepository;
    
    @Autowired
    private com.petclinic.backend.repository.VeterinarianRepository veterinarianRepository;
    
    @Autowired
    private com.petclinic.backend.repository.VisitRepository visitRepository;
    
    // Column definitions for each entity type
    private static final Map<String, List<String>> SORTABLE_COLUMNS = Map.of(
        "visits", Arrays.asList("id", "visitDate", "visitType", "diagnosis", "treatment", "cost", "pet.name", "veterinarian.lastName"),
        "owners", Arrays.asList("id", "firstName", "lastName", "address", "city", "telephone"),
        "pets", Arrays.asList("id", "name", "birthDate", "petType", "owner.lastName"),
        "veterinarians", Arrays.asList("id", "firstName", "lastName", "specialties")
    );
    
    private static final Map<String, List<String>> FILTERABLE_COLUMNS = Map.of(
        "visits", Arrays.asList("visitType", "diagnosis", "treatment", "pet.name", "veterinarian.lastName"),
        "owners", Arrays.asList("firstName", "lastName", "city"),
        "pets", Arrays.asList("name", "petType", "owner.lastName"),
        "veterinarians", Arrays.asList("firstName", "lastName", "specialties")
    );

    @Override
    public PagedResponse<Object> findWithSortAndFilter(
            String entityType, 
            Pageable pageable, 
            SortMetadata sortMetadata, 
            List<FilterCriteria> filters) {
        
        logger.debug("Finding {} with sort and filter: sort={}, filters={}", entityType, sortMetadata, filters);
        
        // Try to get from cache first
        PagedResponse<Object> cachedResult = cacheService.getCachedResults(entityType, pageable, sortMetadata, filters);
        if (cachedResult != null) {
            logger.debug("Returning cached results for {}: {} items", entityType, cachedResult.getContent().size());
            return cachedResult;
        }
        
        try {
            PagedResponse<Object> result;
            
            switch (entityType.toLowerCase()) {
                case "visits":
                    result = findVisitsWithSortAndFilter(pageable, sortMetadata, filters);
                    break;
                case "owners":
                    result = findOwnersWithSortAndFilter(pageable, sortMetadata, filters);
                    break;
                case "pets":
                    result = findPetsWithSortAndFilter(pageable, sortMetadata, filters);
                    break;
                case "veterinarians":
                    result = findVeterinariansWithSortAndFilter(pageable, sortMetadata, filters);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported entity type: " + entityType);
            }
            
            // Cache the result
            cacheService.cacheResults(entityType, pageable, sortMetadata, filters, result);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error finding {} with sort and filter: {}", entityType, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public PagedResponse<Object> findWithMultiColumnSortAndFilter(
            String entityType, 
            Pageable pageable, 
            MultiColumnSortMetadata multiColumnSortMetadata, 
            List<FilterCriteria> filters) {
        
        logger.debug("Finding {} with multi-column sort and filter: sort={}, filters={}", 
                    entityType, multiColumnSortMetadata, filters);
        
        // Validate multi-column sort criteria
        if (multiColumnSortMetadata != null && !validateMultiColumnSortCriteria(entityType, multiColumnSortMetadata)) {
            throw new IllegalArgumentException("Invalid multi-column sort criteria for entity type: " + entityType);
        }
        
        try {
            PagedResponse<Object> result;
            
            switch (entityType.toLowerCase()) {
                case "visits":
                    result = findVisitsWithMultiColumnSortAndFilter(pageable, multiColumnSortMetadata, filters);
                    break;
                case "owners":
                    result = findOwnersWithMultiColumnSortAndFilter(pageable, multiColumnSortMetadata, filters);
                    break;
                case "pets":
                    result = findPetsWithMultiColumnSortAndFilter(pageable, multiColumnSortMetadata, filters);
                    break;
                case "veterinarians":
                    result = findVeterinariansWithMultiColumnSortAndFilter(pageable, multiColumnSortMetadata, filters);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported entity type: " + entityType);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error finding {} with multi-column sort and filter: {}", entityType, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BulkOperationResult bulkDelete(BulkDeleteRequest request) {
        logger.debug("Executing bulk delete: {}", request);
        
        long startTime = System.currentTimeMillis();
        BulkOperationResult result = new BulkOperationResult(false, 0, request.getSelectedCount(), request.getEntityType());
        
        try {
            if (!request.isValid()) {
                result.addError("Invalid bulk delete request");
                return result;
            }
            
            List<Long> idsToDelete = request.getSelectedIds();
            int totalItems = idsToDelete.size();
            
            // Add progress tracking for large operations
            if (totalItems > 100) {
                logger.info("Starting bulk delete of {} {} items with progress tracking", totalItems, request.getEntityType());
            }
            
            int deletedCount = 0;
            
            switch (request.getEntityType().toLowerCase()) {
                case "visits":
                    deletedCount = bulkDeleteVisitsWithProgress(idsToDelete, result, totalItems);
                    break;
                case "owners":
                    deletedCount = bulkDeleteOwnersWithProgress(idsToDelete, result, totalItems);
                    break;
                case "pets":
                    deletedCount = bulkDeletePetsWithProgress(idsToDelete, result, totalItems);
                    break;
                case "veterinarians":
                    deletedCount = bulkDeleteVeterinariansWithProgress(idsToDelete, result, totalItems);
                    break;
                default:
                    result.addError("Unsupported entity type: " + request.getEntityType());
                    return result;
            }
            
            result.setDeletedCount(deletedCount);
            result.setSuccess(deletedCount > 0 && !result.hasErrors());
            
            long duration = System.currentTimeMillis() - startTime;
            result.setDuration(duration);
            
            // Invalidate caches for the affected entity type
            if (deletedCount > 0) {
                logger.debug("Invalidating caches for {} after bulk delete of {} items", 
                           request.getEntityType(), deletedCount);
                cacheService.invalidateEntityCaches(request.getEntityType());
            }
            
            logger.info("Bulk delete completed: deleted {}/{} {} in {}ms", 
                       deletedCount, request.getSelectedCount(), request.getEntityType(), duration);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error during bulk delete: {}", e.getMessage(), e);
            result.addError("Bulk delete failed: " + e.getMessage());
            result.setDuration(System.currentTimeMillis() - startTime);
            throw e; // Re-throw to trigger transaction rollback
        }
    }

    @Override
    public List<String> getAvailableFilterValues(String entityType, String column) {
        logger.debug("Getting filter values for {}.{}", entityType, column);
        
        // Try to get from cache first
        List<String> cachedValues = cacheService.getCachedFilterValues(entityType, column);
        if (cachedValues != null) {
            logger.debug("Returning cached filter values for {}.{}: {} values", entityType, column, cachedValues.size());
            return cachedValues;
        }
        
        try {
            List<String> values;
            
            switch (entityType.toLowerCase()) {
                case "visits":
                    values = getVisitFilterValues(column);
                    break;
                case "owners":
                    values = getOwnerFilterValues(column);
                    break;
                case "pets":
                    values = getPetFilterValues(column);
                    break;
                case "veterinarians":
                    values = getVeterinarianFilterValues(column);
                    break;
                default:
                    values = Collections.emptyList();
            }
            
            // Cache the result
            cacheService.cacheFilterValues(entityType, column, values);
            
            return values;
            
        } catch (Exception e) {
            logger.error("Error getting filter values for {}.{}: {}", entityType, column, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean validateSortCriteria(String entityType, SortMetadata sortMetadata) {
        if (sortMetadata == null || sortMetadata.getColumn() == null) {
            return false;
        }
        
        List<String> sortableColumns = getSortableColumns(entityType);
        String columnName = sortMetadata.getColumn();
        
        // Case-insensitive comparison
        return sortableColumns.stream()
            .anyMatch(column -> column.equalsIgnoreCase(columnName)) &&
               sortMetadata.isValidDirection();
    }

    @Override
    public boolean validateFilterCriteria(String entityType, FilterCriteria filterCriteria) {
        if (filterCriteria == null || !filterCriteria.isValid()) {
            return false;
        }
        
        List<String> filterableColumns = getFilterableColumns(entityType);
        String fieldName = filterCriteria.getField();
        
        // Case-insensitive comparison
        return filterableColumns.stream()
            .anyMatch(column -> column.equalsIgnoreCase(fieldName));
    }

    @Override
    public boolean validateMultiColumnSortCriteria(String entityType, MultiColumnSortMetadata multiColumnSortMetadata) {
        if (multiColumnSortMetadata == null || !multiColumnSortMetadata.isValid()) {
            return false;
        }
        
        List<String> sortableColumns = getSortableColumns(entityType);
        
        // Validate each sort criterion
        for (MultiColumnSortMetadata.SortCriterion criterion : multiColumnSortMetadata.getSortCriteria()) {
            if (!criterion.isValid()) {
                return false;
            }
            
            // Case-insensitive comparison
            boolean columnExists = sortableColumns.stream()
                .anyMatch(column -> column.equalsIgnoreCase(criterion.getColumn()));
            
            if (!columnExists) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public List<String> getSortableColumns(String entityType) {
        return SORTABLE_COLUMNS.getOrDefault(entityType.toLowerCase(), Collections.emptyList());
    }

    @Override
    public List<String> getFilterableColumns(String entityType) {
        return FILTERABLE_COLUMNS.getOrDefault(entityType.toLowerCase(), Collections.emptyList());
    }

    // Private helper methods for each entity type

    private PagedResponse<Object> findVisitsWithSortAndFilter(
            Pageable pageable, SortMetadata sortMetadata, List<FilterCriteria> filters) {
        
        // Use proper database-level sorting and pagination instead of in-memory operations
        Page<Visit> page = visitService.findAllWithPagination(pageable);
        
        // Convert to PagedResponse<Object>
        List<Object> content = new ArrayList<>(page.getContent());
        PagedResponse.PageInfo pageInfo = new PagedResponse.PageInfo(
            page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
            page.isFirst(), page.isLast(), page.hasNext(), page.hasPrevious()
        );
        
        return new PagedResponse<>(content, pageInfo);
    }

    private PagedResponse<Object> findVisitsWithMultiColumnSortAndFilter(
            Pageable pageable, MultiColumnSortMetadata multiColumnSortMetadata, List<FilterCriteria> filters) {
        
        // Use the pageable that already has multi-column sort applied from controller
        Page<Visit> page = visitService.findAllWithPagination(pageable);
        
        // Convert to PagedResponse<Object>
        List<Object> content = new ArrayList<>(page.getContent());
        PagedResponse.PageInfo pageInfo = new PagedResponse.PageInfo(
            page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
            page.isFirst(), page.isLast(), page.hasNext(), page.hasPrevious()
        );
        
        return new PagedResponse<>(content, pageInfo);
    }

    private PagedResponse<Object> findOwnersWithSortAndFilter(
            Pageable pageable, SortMetadata sortMetadata, List<FilterCriteria> filters) {
        
        // Use server-side sorting and pagination for owners
        Page<Owner> page = ownerRepository.findAllWithPets(pageable);
        
        // Convert to PagedResponse<Object>
        List<Object> content = new ArrayList<>(page.getContent());
        PagedResponse.PageInfo pageInfo = new PagedResponse.PageInfo(
            page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
            page.isFirst(), page.isLast(), page.hasNext(), page.hasPrevious()
        );
        
        return new PagedResponse<>(content, pageInfo);
    }

    private PagedResponse<Object> findOwnersWithMultiColumnSortAndFilter(
            Pageable pageable, MultiColumnSortMetadata multiColumnSortMetadata, List<FilterCriteria> filters) {
        
        // Use the pageable that already has multi-column sort applied from controller
        Page<Owner> page = ownerRepository.findAllWithPets(pageable);
        
        // Convert to PagedResponse<Object>
        List<Object> content = new ArrayList<>(page.getContent());
        PagedResponse.PageInfo pageInfo = new PagedResponse.PageInfo(
            page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
            page.isFirst(), page.isLast(), page.hasNext(), page.hasPrevious()
        );
        
        return new PagedResponse<>(content, pageInfo);
    }

    private PagedResponse<Object> findPetsWithSortAndFilter(
            Pageable pageable, SortMetadata sortMetadata, List<FilterCriteria> filters) {
        
        // Use server-side sorting and pagination for pets
        Page<Pet> page = petRepository.findAll(pageable);
        
        // Convert to PagedResponse<Object>
        List<Object> content = new ArrayList<>(page.getContent());
        PagedResponse.PageInfo pageInfo = new PagedResponse.PageInfo(
            page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
            page.isFirst(), page.isLast(), page.hasNext(), page.hasPrevious()
        );
        
        return new PagedResponse<>(content, pageInfo);
    }

    private PagedResponse<Object> findPetsWithMultiColumnSortAndFilter(
            Pageable pageable, MultiColumnSortMetadata multiColumnSortMetadata, List<FilterCriteria> filters) {
        
        // Use the pageable that already has multi-column sort applied from controller
        Page<Pet> page = petRepository.findAll(pageable);
        
        // Convert to PagedResponse<Object>
        List<Object> content = new ArrayList<>(page.getContent());
        PagedResponse.PageInfo pageInfo = new PagedResponse.PageInfo(
            page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
            page.isFirst(), page.isLast(), page.hasNext(), page.hasPrevious()
        );
        
        return new PagedResponse<>(content, pageInfo);
    }

    private PagedResponse<Object> findVeterinariansWithSortAndFilter(
            Pageable pageable, SortMetadata sortMetadata, List<FilterCriteria> filters) {
        
        // Use server-side sorting and pagination for veterinarians
        Page<Veterinarian> page = veterinarianRepository.findAll(pageable);
        
        // Convert to PagedResponse<Object>
        List<Object> content = new ArrayList<>(page.getContent());
        PagedResponse.PageInfo pageInfo = new PagedResponse.PageInfo(
            page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
            page.isFirst(), page.isLast(), page.hasNext(), page.hasPrevious()
        );
        
        return new PagedResponse<>(content, pageInfo);
    }

    private PagedResponse<Object> findVeterinariansWithMultiColumnSortAndFilter(
            Pageable pageable, MultiColumnSortMetadata multiColumnSortMetadata, List<FilterCriteria> filters) {
        
        // Use the pageable that already has multi-column sort applied from controller
        Page<Veterinarian> page = veterinarianRepository.findAll(pageable);
        
        // Convert to PagedResponse<Object>
        List<Object> content = new ArrayList<>(page.getContent());
        PagedResponse.PageInfo pageInfo = new PagedResponse.PageInfo(
            page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
            page.isFirst(), page.isLast(), page.hasNext(), page.hasPrevious()
        );
        
        return new PagedResponse<>(content, pageInfo);
    }

    private List<Visit> applyVisitFilters(List<Visit> visits, List<FilterCriteria> filters) {
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
        
        Object fieldValue = getVisitFieldValue(visit, field);
        
        if (fieldValue == null) {
            return "isnull".equals(operator);
        }
        
        String fieldStr = fieldValue.toString().toLowerCase();
        String valueStr = value != null ? value.toString().toLowerCase() : "";
        
        switch (operator) {
            case "equals":
            case "eq":
                return fieldStr.equals(valueStr);
            case "contains":
                return fieldStr.contains(valueStr);
            case "startswith":
                return fieldStr.startsWith(valueStr);
            case "endswith":
                return fieldStr.endsWith(valueStr);
            default:
                return true; // Default to include if operator not recognized
        }
    }

    private Object getVisitFieldValue(Visit visit, String field) {
        switch (field) {
            case "visittype":
                return visit.getVisitType();
            case "diagnosis":
                return visit.getDiagnosis();
            case "treatment":
                return visit.getTreatment();
            case "pet.name":
                return visit.getPet() != null ? visit.getPet().getName() : null;
            case "veterinarian.lastname":
                return visit.getVeterinarian() != null ? visit.getVeterinarian().getLastName() : null;
            default:
                return null;
        }
    }

    private List<Visit> applyVisitSorting(List<Visit> visits, SortMetadata sortMetadata) {
        if (sortMetadata == null || sortMetadata.getColumn() == null) {
            return visits;
        }
        
        String column = sortMetadata.getColumn().toLowerCase();
        boolean ascending = sortMetadata.isAscending();
        
        Comparator<Visit> comparator = getVisitComparator(column);
        if (comparator == null) {
            return visits;
        }
        
        if (!ascending) {
            comparator = comparator.reversed();
        }
        
        return visits.stream()
            .sorted(comparator)
            .collect(Collectors.toList());
    }

    private Comparator<Visit> getVisitComparator(String column) {
        switch (column) {
            case "id":
                return Comparator.comparing(Visit::getId, Comparator.nullsLast(Comparator.naturalOrder()));
            case "visitdate":
                return Comparator.comparing(Visit::getVisitDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "visittype":
                return Comparator.comparing(v -> v.getVisitType() != null ? v.getVisitType().toString() : "", 
                                          Comparator.nullsLast(Comparator.naturalOrder()));
            case "cost":
                return Comparator.comparing(Visit::getCost, Comparator.nullsLast(Comparator.naturalOrder()));
            case "pet.name":
                return Comparator.comparing(v -> v.getPet() != null ? v.getPet().getName() : "", 
                                          Comparator.nullsLast(Comparator.naturalOrder()));
            default:
                return null;
        }
    }

    private int bulkDeleteVisitsWithProgress(List<Long> ids, BulkOperationResult result, int totalItems) {
        int deletedCount = 0;
        int progressInterval = Math.max(1, totalItems / 10); // Report progress every 10%
        
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            try {
                visitService.deleteById(id);
                deletedCount++;
                
                // Log progress for large operations
                if (totalItems > 100 && (i + 1) % progressInterval == 0) {
                    int progressPercent = (int) ((double) (i + 1) / totalItems * 100);
                    logger.info("Bulk delete progress: {}/{} visits deleted ({}%)", 
                               i + 1, totalItems, progressPercent);
                }
            } catch (Exception e) {
                logger.warn("Failed to delete visit {}: {}", id, e.getMessage());
                result.addError("Failed to delete visit " + id + ": " + e.getMessage());
                
                // For transaction safety, if we have too many errors, fail fast
                if (result.getErrors().size() > totalItems * 0.1) { // More than 10% failure rate
                    result.addError("Too many deletion failures, aborting bulk operation");
                    throw new RuntimeException("Bulk delete failed with too many errors");
                }
            }
        }
        
        return deletedCount;
    }

    private int bulkDeleteOwnersWithProgress(List<Long> ids, BulkOperationResult result, int totalItems) {
        logger.debug("Bulk deleting {} owners", ids.size());
        
        int deletedCount = 0;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            try {
                // Check if owner exists
                if (!ownerRepository.existsById(id)) {
                    result.addWarning("Owner with ID " + id + " not found");
                    continue;
                }
                
                // Check if owner has pets - prevent deletion if they do
                List<Pet> ownerPets = petRepository.findByOwnerId(id);
                if (!ownerPets.isEmpty()) {
                    result.addError("Cannot delete owner with ID " + id + " - has " + ownerPets.size() + " associated pets");
                    continue;
                }
                
                // Delete the owner
                ownerRepository.deleteById(id);
                deletedCount++;
                
                // Update progress
                result.updateProgress(i + 1, startTime);
                
                logger.debug("Successfully deleted owner with ID: {}", id);
                
            } catch (Exception e) {
                logger.error("Error deleting owner with ID {}: {}", id, e.getMessage(), e);
                result.addError("Failed to delete owner with ID " + id + ": " + e.getMessage());
            }
        }
        
        logger.info("Bulk delete owners completed: {}/{} deleted", deletedCount, ids.size());
        return deletedCount;
    }

    private int bulkDeletePetsWithProgress(List<Long> ids, BulkOperationResult result, int totalItems) {
        logger.debug("Bulk deleting {} pets", ids.size());
        
        int deletedCount = 0;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            try {
                // Check if pet exists
                if (!petRepository.existsById(id)) {
                    result.addWarning("Pet with ID " + id + " not found");
                    continue;
                }
                
                // Check if pet has visits - prevent deletion if they do
                List<Visit> petVisits = visitRepository.findByPetId(id);
                if (!petVisits.isEmpty()) {
                    result.addError("Cannot delete pet with ID " + id + " - has " + petVisits.size() + " associated visits");
                    continue;
                }
                
                // Delete the pet
                petRepository.deleteById(id);
                deletedCount++;
                
                // Update progress
                result.updateProgress(i + 1, startTime);
                
                logger.debug("Successfully deleted pet with ID: {}", id);
                
            } catch (Exception e) {
                logger.error("Error deleting pet with ID {}: {}", id, e.getMessage(), e);
                result.addError("Failed to delete pet with ID " + id + ": " + e.getMessage());
            }
        }
        
        logger.info("Bulk delete pets completed: {}/{} deleted", deletedCount, ids.size());
        return deletedCount;
    }

    private int bulkDeleteVeterinariansWithProgress(List<Long> ids, BulkOperationResult result, int totalItems) {
        logger.debug("Bulk deleting {} veterinarians", ids.size());
        
        int deletedCount = 0;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            try {
                // Check if veterinarian exists
                if (!veterinarianRepository.existsById(id)) {
                    result.addWarning("Veterinarian with ID " + id + " not found");
                    continue;
                }
                
                // Check if veterinarian has visits - prevent deletion if they do
                List<Visit> vetVisits = visitRepository.findByVeterinarianId(id);
                if (!vetVisits.isEmpty()) {
                    result.addError("Cannot delete veterinarian with ID " + id + " - has " + vetVisits.size() + " associated visits");
                    continue;
                }
                
                // Delete the veterinarian
                veterinarianRepository.deleteById(id);
                deletedCount++;
                
                // Update progress
                result.updateProgress(i + 1, startTime);
                
                logger.debug("Successfully deleted veterinarian with ID: {}", id);
                
            } catch (Exception e) {
                logger.error("Error deleting veterinarian with ID {}: {}", id, e.getMessage(), e);
                result.addError("Failed to delete veterinarian with ID " + id + ": " + e.getMessage());
            }
        }
        
        logger.info("Bulk delete veterinarians completed: {}/{} deleted", deletedCount, ids.size());
        return deletedCount;
    }

    private List<String> getVisitFilterValues(String column) {
        switch (column.toLowerCase()) {
            case "visittype":
                return Arrays.stream(VisitType.values())
                    .map(VisitType::toString)
                    .collect(Collectors.toList());
            case "veterinarian.lastname":
                return veterinarianService.findAll().stream()
                    .map(Veterinarian::getLastName)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            default:
                return Collections.emptyList();
        }
    }

    private List<String> getOwnerFilterValues(String column) {
        // Placeholder implementation
        return Collections.emptyList();
    }

    private List<String> getPetFilterValues(String column) {
        // Placeholder implementation
        return Collections.emptyList();
    }

    private List<String> getVeterinarianFilterValues(String column) {
        // Placeholder implementation
        return Collections.emptyList();
    }
}