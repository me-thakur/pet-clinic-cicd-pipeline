package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.FilterCriteria;
import com.petclinic.backend.dto.FilterResult;
import com.petclinic.backend.dto.SearchResult;
import com.petclinic.backend.exception.ValidationException;
import com.petclinic.backend.model.*;
import com.petclinic.backend.repository.*;
import com.petclinic.backend.service.FilterService;
import com.petclinic.backend.service.GlobalSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of FilterService providing advanced filtering capabilities
 * Validates: Requirements 4.2, 4.5
 */
@Service
@Transactional
public class FilterServiceImpl implements FilterService {
    
    private static final Logger logger = LoggerFactory.getLogger(FilterServiceImpl.class);
    
    @Autowired
    private PetRepository petRepository;
    
    @Autowired
    private VisitRepository visitRepository;
    
    @Autowired
    private VeterinarianRepository veterinarianRepository;
    
    @Autowired
    private OwnerRepository ownerRepository;
    
    @Autowired
    private GlobalSearchService globalSearchService;
    
    // In-memory storage for filter history and saved combinations
    // In production, these would be stored in database tables
    private final Map<Long, List<List<FilterCriteria>>> filterHistory = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, String>> savedFilterCombinations = new ConcurrentHashMap<>();
    private final Map<Long, List<FilterCriteria>> savedFilters = new ConcurrentHashMap<>();
    private final Map<List<FilterCriteria>, Integer> popularFilters = new ConcurrentHashMap<>();
    private Long nextFilterId = 1L;
    
    @Override
    public FilterResult applyFilters(List<FilterCriteria> filters) {
        return applyFilters(filters, 0, 20);
    }
    
    @Override
    public FilterResult applyFilters(List<FilterCriteria> filters, int page, int size) {
        return applyFilters(filters, page, size, null, null);
    }
    
    @Override
    public FilterResult applyFilters(List<FilterCriteria> filters, int page, int size, String sortBy, String sortDirection) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate and sanitize filters
            List<String> validationErrors = validateFilters(filters);
            if (!validationErrors.isEmpty()) {
                throw new ValidationException("Invalid filter criteria: " + String.join(", ", validationErrors));
            }
            
            List<FilterCriteria> sanitizedFilters = sanitizeFilters(filters);
            
            // Apply filters to each entity type
            List<SearchResult> allResults = new ArrayList<>();
            Map<String, Integer> resultsByType = new HashMap<>();
            
            // Group filters by entity type
            Map<String, List<FilterCriteria>> filtersByType = sanitizedFilters.stream()
                    .collect(Collectors.groupingBy(FilterCriteria::getEntityType));
            
            // Apply filters to each entity type
            for (Map.Entry<String, List<FilterCriteria>> entry : filtersByType.entrySet()) {
                String entityType = entry.getKey();
                List<FilterCriteria> entityFilters = entry.getValue();
                
                List<SearchResult> entityResults;
                switch (entityType.toLowerCase()) {
                    case "pet":
                        entityResults = filterPets(entityFilters);
                        break;
                    case "visit":
                        entityResults = filterVisits(entityFilters);
                        break;
                    case "veterinarian":
                        entityResults = filterVeterinarians(entityFilters);
                        break;
                    case "owner":
                        entityResults = filterOwners(entityFilters);
                        break;
                    default:
                        entityResults = new ArrayList<>();
                        break;
                }
                
                allResults.addAll(entityResults);
                resultsByType.put(entityType, entityResults.size());
            }
            
            // Sort results if specified
            if (sortBy != null && !sortBy.trim().isEmpty()) {
                allResults = sortResults(allResults, sortBy, sortDirection);
            }
            
            // Apply pagination
            int totalResults = allResults.size();
            int totalPages = (int) Math.ceil((double) totalResults / size);
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalResults);
            
            List<SearchResult> paginatedResults = startIndex < totalResults ? 
                    allResults.subList(startIndex, endIndex) : new ArrayList<>();
            
            // Create result
            FilterResult result = new FilterResult(sanitizedFilters, paginatedResults, resultsByType, page, size, totalPages);
            result.setSortBy(sortBy);
            result.setSortDirection(sortDirection);
            result.setTotalResults(totalResults);
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            
            // Update popular filters
            updatePopularFilters(sanitizedFilters);
            
            logger.info("Applied {} filters, found {} results in {}ms", 
                       sanitizedFilters.size(), totalResults, result.getExecutionTimeMs());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error applying filters: {}", e.getMessage(), e);
            throw new ValidationException("Error applying filters: " + e.getMessage());
        }
    }
    
    @Override
    public Page<SearchResult> applyFiltersToEntityType(List<FilterCriteria> filters, String entityType, int page, int size) {
        List<FilterCriteria> entityFilters = filters.stream()
                .filter(f -> entityType.equalsIgnoreCase(f.getEntityType()))
                .collect(Collectors.toList());
        
        List<SearchResult> results;
        switch (entityType.toLowerCase()) {
            case "pet":
                results = filterPets(entityFilters);
                break;
            case "visit":
                results = filterVisits(entityFilters);
                break;
            case "veterinarian":
                results = filterVeterinarians(entityFilters);
                break;
            case "owner":
                results = filterOwners(entityFilters);
                break;
            default:
                results = new ArrayList<>();
                break;
        }
        
        // Apply pagination
        int totalResults = results.size();
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalResults);
        
        List<SearchResult> paginatedResults = startIndex < totalResults ? 
                results.subList(startIndex, endIndex) : new ArrayList<>();
        
        Pageable pageable = PageRequest.of(page, size);
        return new PageImpl<>(paginatedResults, pageable, totalResults);
    }
    
    @Override
    public List<SearchResult> filterPets(List<FilterCriteria> filters) {
        try {
            Specification<Pet> spec = buildPetSpecification(filters);
            List<Pet> pets = petRepository.findAll(spec);
            
            return pets.stream()
                    .map(this::convertPetToSearchResult)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("Error filtering pets: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<SearchResult> filterVisits(List<FilterCriteria> filters) {
        try {
            Specification<Visit> spec = buildVisitSpecification(filters);
            List<Visit> visits = visitRepository.findAll(spec);
            
            return visits.stream()
                    .map(this::convertVisitToSearchResult)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("Error filtering visits: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<SearchResult> filterVeterinarians(List<FilterCriteria> filters) {
        try {
            Specification<Veterinarian> spec = buildVeterinarianSpecification(filters);
            List<Veterinarian> veterinarians = veterinarianRepository.findAll(spec);
            
            return veterinarians.stream()
                    .map(this::convertVeterinarianToSearchResult)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("Error filtering veterinarians: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<SearchResult> filterOwners(List<FilterCriteria> filters) {
        try {
            Specification<Owner> spec = buildOwnerSpecification(filters);
            List<Owner> owners = ownerRepository.findAll(spec);
            
            return owners.stream()
                    .map(this::convertOwnerToSearchResult)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("Error filtering owners: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<String> validateFilters(List<FilterCriteria> filters) {
        List<String> errors = new ArrayList<>();
        
        if (filters == null || filters.isEmpty()) {
            errors.add("At least one filter criteria is required");
            return errors;
        }
        
        for (int i = 0; i < filters.size(); i++) {
            FilterCriteria filter = filters.get(i);
            String prefix = "Filter " + (i + 1) + ": ";
            
            if (!filter.isValid()) {
                if (filter.getField() == null || filter.getField().trim().isEmpty()) {
                    errors.add(prefix + "Field is required");
                }
                if (!filter.hasValidOperator()) {
                    errors.add(prefix + "Invalid operator: " + filter.getOperator());
                }
                if (filter.getValue() == null) {
                    errors.add(prefix + "Value is required");
                }
                if (!filter.hasValidEntityType()) {
                    errors.add(prefix + "Invalid entity type: " + filter.getEntityType());
                }
            }
            
            // Validate field exists for entity type
            Map<String, String> availableFields = getAvailableFields(filter.getEntityType());
            if (!availableFields.containsKey(filter.getField())) {
                errors.add(prefix + "Field '" + filter.getField() + "' is not available for entity type '" + filter.getEntityType() + "'");
            }
            
            // Validate operator is compatible with field type
            String fieldType = availableFields.get(filter.getField());
            if (fieldType != null) {
                List<String> validOperators = getAvailableOperators(fieldType);
                if (!validOperators.contains(filter.getOperator().toLowerCase())) {
                    errors.add(prefix + "Operator '" + filter.getOperator() + "' is not valid for field type '" + fieldType + "'");
                }
            }
        }
        
        return errors;
    }
    
    @Override
    public List<FilterCriteria> sanitizeFilters(List<FilterCriteria> filters) {
        return filters.stream()
                .map(this::sanitizeFilter)
                .collect(Collectors.toList());
    }
    
    private FilterCriteria sanitizeFilter(FilterCriteria filter) {
        FilterCriteria sanitized = new FilterCriteria();
        
        // Sanitize field name
        sanitized.setField(sanitizeString(filter.getField()));
        
        // Sanitize operator
        sanitized.setOperator(sanitizeString(filter.getOperator()));
        
        // Sanitize value based on type
        sanitized.setValue(sanitizeValue(filter.getValue()));
        
        // Sanitize entity type
        sanitized.setEntityType(sanitizeString(filter.getEntityType()));
        
        // Sanitize logical operator
        sanitized.setLogicalOperator(sanitizeString(filter.getLogicalOperator()));
        
        return sanitized;
    }
    
    private String sanitizeString(String input) {
        if (input == null) return null;
        
        // Remove potentially dangerous characters
        return input.replaceAll("[<>\"'%;()&+]", "").trim();
    }
    
    private Object sanitizeValue(Object value) {
        if (value == null) return null;
        
        if (value instanceof String) {
            return sanitizeString((String) value);
        }
        
        return value;
    }
    
    @Override
    public Map<String, String> getAvailableFields(String entityType) {
        Map<String, String> fields = new HashMap<>();
        
        switch (entityType.toLowerCase()) {
            case "pet":
                fields.put("name", "text");
                fields.put("species", "text");
                fields.put("breed", "text");
                fields.put("birthDate", "date");
                fields.put("medicalHistory", "text");
                fields.put("owner.firstName", "text");
                fields.put("owner.lastName", "text");
                fields.put("owner.city", "text");
                fields.put("ownerId", "numeric");
                break;
                
            case "visit":
                fields.put("visitDate", "date");
                fields.put("visitType", "enum");
                fields.put("diagnosis", "text");
                fields.put("treatment", "text");
                fields.put("notes", "text");
                fields.put("cost", "numeric");
                fields.put("duration", "numeric");
                fields.put("pet.name", "text");
                fields.put("pet.species", "text");
                fields.put("veterinarian.firstName", "text");
                fields.put("veterinarian.lastName", "text");
                fields.put("petId", "numeric");
                fields.put("veterinarianId", "numeric");
                break;
                
            case "veterinarian":
                fields.put("firstName", "text");
                fields.put("lastName", "text");
                fields.put("licenseNumber", "text");
                fields.put("specialties", "text");
                break;
                
            case "owner":
                fields.put("firstName", "text");
                fields.put("lastName", "text");
                fields.put("address", "text");
                fields.put("city", "text");
                fields.put("telephone", "text");
                fields.put("email", "text");
                break;
        }
        
        return fields;
    }
    
    @Override
    public List<String> getAvailableOperators(String fieldType) {
        List<String> operators;
        switch (fieldType.toLowerCase()) {
            case "text":
                operators = Arrays.asList("eq", "ne", "contains", "startswith", "endswith");
                break;
            case "numeric":
                operators = Arrays.asList("eq", "ne", "gt", "lt", "gte", "lte");
                break;
            case "date":
                operators = Arrays.asList("eq", "ne", "before", "after", "between");
                break;
            case "boolean":
                operators = Arrays.asList("eq", "ne");
                break;
            case "enum":
                operators = Arrays.asList("eq", "ne", "in", "notin");
                break;
            default:
                operators = Arrays.asList("eq", "ne");
                break;
        }
        return operators;
    }
    
    // Specification builders for each entity type
    private Specification<Pet> buildPetSpecification(List<FilterCriteria> filters) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            for (FilterCriteria filter : filters) {
                Predicate predicate = buildPredicate(root, criteriaBuilder, filter);
                if (predicate != null) {
                    predicates.add(predicate);
                }
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    private Specification<Visit> buildVisitSpecification(List<FilterCriteria> filters) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            for (FilterCriteria filter : filters) {
                Predicate predicate = buildPredicate(root, criteriaBuilder, filter);
                if (predicate != null) {
                    predicates.add(predicate);
                }
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    private Specification<Veterinarian> buildVeterinarianSpecification(List<FilterCriteria> filters) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            for (FilterCriteria filter : filters) {
                Predicate predicate = buildPredicate(root, criteriaBuilder, filter);
                if (predicate != null) {
                    predicates.add(predicate);
                }
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    private Specification<Owner> buildOwnerSpecification(List<FilterCriteria> filters) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            for (FilterCriteria filter : filters) {
                Predicate predicate = buildPredicate(root, criteriaBuilder, filter);
                if (predicate != null) {
                    predicates.add(predicate);
                }
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate buildPredicate(Root<?> root, CriteriaBuilder criteriaBuilder, FilterCriteria filter) {
        try {
            Path<Object> path = getPath(root, filter.getField());
            Object value = convertValue(filter.getValue(), path.getJavaType());
            
            Predicate predicate;
            switch (filter.getOperator().toLowerCase()) {
                case "eq":
                    predicate = criteriaBuilder.equal(path, value);
                    break;
                case "ne":
                    predicate = criteriaBuilder.notEqual(path, value);
                    break;
                case "gt":
                    predicate = criteriaBuilder.greaterThan((Path) path, (Comparable) value);
                    break;
                case "lt":
                    predicate = criteriaBuilder.lessThan((Path) path, (Comparable) value);
                    break;
                case "gte":
                    predicate = criteriaBuilder.greaterThanOrEqualTo((Path) path, (Comparable) value);
                    break;
                case "lte":
                    predicate = criteriaBuilder.lessThanOrEqualTo((Path) path, (Comparable) value);
                    break;
                case "contains":
                    predicate = criteriaBuilder.like(criteriaBuilder.lower((Path) path), 
                                                  "%" + value.toString().toLowerCase() + "%");
                    break;
                case "startswith":
                    predicate = criteriaBuilder.like(criteriaBuilder.lower((Path) path), 
                                                     value.toString().toLowerCase() + "%");
                    break;
                case "endswith":
                    predicate = criteriaBuilder.like(criteriaBuilder.lower((Path) path), 
                                                   "%" + value.toString().toLowerCase());
                    break;
                case "before":
                    predicate = criteriaBuilder.lessThan((Path) path, (Comparable) value);
                    break;
                case "after":
                    predicate = criteriaBuilder.greaterThan((Path) path, (Comparable) value);
                    break;
                default:
                    predicate = null;
                    break;
            }
            
            return predicate;
            
        } catch (Exception e) {
            logger.warn("Error building predicate for filter {}: {}", filter, e.getMessage());
            return null;
        }
    }
    
    private Path<Object> getPath(Root<?> root, String fieldName) {
        String[] parts = fieldName.split("\\.");
        Path<Object> path = root.get(parts[0]);
        
        for (int i = 1; i < parts.length; i++) {
            path = path.get(parts[i]);
        }
        
        return path;
    }
    
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        
        String stringValue = value.toString();
        
        try {
            if (targetType == LocalDate.class) {
                return LocalDate.parse(stringValue);
            } else if (targetType == LocalDateTime.class) {
                return LocalDateTime.parse(stringValue);
            } else if (targetType == BigDecimal.class) {
                return new BigDecimal(stringValue);
            } else if (targetType == Integer.class || targetType == int.class) {
                return Integer.parseInt(stringValue);
            } else if (targetType == Long.class || targetType == long.class) {
                return Long.parseLong(stringValue);
            } else if (targetType == Double.class || targetType == double.class) {
                return Double.parseDouble(stringValue);
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                return Boolean.parseBoolean(stringValue);
            }
        } catch (Exception e) {
            logger.warn("Error converting value '{}' to type {}: {}", stringValue, targetType.getSimpleName(), e.getMessage());
        }
        
        return value;
    }
    
    // Convert entities to SearchResult objects
    private SearchResult convertPetToSearchResult(Pet pet) {
        SearchResult result = new SearchResult("Pet", pet.getId(), pet.getName(), 
                                             pet.getSpecies() + " - " + (pet.getBreed() != null ? pet.getBreed() : "Mixed"));
        result.setLastModified(pet.getUpdatedAt().atStartOfDay());
        result.setUrl("/pets/" + pet.getId());
        return result;
    }
    
    private SearchResult convertVisitToSearchResult(Visit visit) {
        String title = "Visit on " + visit.getVisitDate().toLocalDate();
        String description = (visit.getPet() != null ? visit.getPet().getName() : "Unknown Pet") + 
                           (visit.getVeterinarian() != null ? " with Dr. " + visit.getVeterinarian().getLastName() : "");
        
        SearchResult result = new SearchResult("Visit", visit.getId(), title, description);
        result.setLastModified(visit.getUpdatedAt());
        result.setUrl("/visits/" + visit.getId());
        return result;
    }
    
    private SearchResult convertVeterinarianToSearchResult(Veterinarian vet) {
        SearchResult result = new SearchResult("Veterinarian", vet.getId(), vet.getFullName(), 
                                             "License: " + vet.getLicenseNumber() + 
                                             (vet.getSpecialties() != null ? " - " + vet.getSpecialties() : ""));
        result.setLastModified(vet.getUpdatedAt().atStartOfDay());
        result.setUrl("/veterinarians/" + vet.getId());
        return result;
    }
    
    private SearchResult convertOwnerToSearchResult(Owner owner) {
        SearchResult result = new SearchResult("Owner", owner.getId(), owner.getFullName(), 
                                             (owner.getCity() != null ? owner.getCity() : "") + 
                                             " - " + owner.getPetCount() + " pet(s)");
        result.setLastModified(owner.getUpdatedAt().atStartOfDay());
        result.setUrl("/owners/" + owner.getId());
        return result;
    }
    
    private List<SearchResult> sortResults(List<SearchResult> results, String sortBy, String sortDirection) {
        boolean ascending = !"desc".equalsIgnoreCase(sortDirection);
        
        Comparator<SearchResult> comparator;
        switch (sortBy.toLowerCase()) {
            case "relevance":
                comparator = Comparator.comparing(SearchResult::getRelevanceScore);
                break;
            case "date":
                comparator = Comparator.comparing(SearchResult::getLastModified, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "name":
            case "title":
                comparator = Comparator.comparing(SearchResult::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
            case "type":
                comparator = Comparator.comparing(SearchResult::getEntityType);
                break;
            default:
                comparator = Comparator.comparing(SearchResult::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
        }
        
        if (!ascending) {
            comparator = comparator.reversed();
        }
        
        return results.stream().sorted(comparator).collect(Collectors.toList());
    }
    
    private void updatePopularFilters(List<FilterCriteria> filters) {
        popularFilters.merge(filters, 1, Integer::sum);
    }
    
    // Search history and saved combinations methods
    @Override
    public Long saveFilterCombination(Long userId, String filterName, List<FilterCriteria> filters) {
        Long filterId = nextFilterId++;
        savedFilters.put(filterId, new ArrayList<>(filters));
        
        savedFilterCombinations.computeIfAbsent(userId != null ? userId : 0L, k -> new HashMap<>())
                               .put(filterId, filterName);
        
        logger.info("Saved filter combination '{}' with ID {} for user {}", filterName, filterId, userId);
        return filterId;
    }
    
    @Override
    public Map<Long, String> getSavedFilterCombinations(Long userId) {
        return savedFilterCombinations.getOrDefault(userId != null ? userId : 0L, new HashMap<>());
    }
    
    @Override
    public List<FilterCriteria> loadFilterCombination(Long filterId) {
        return savedFilters.getOrDefault(filterId, new ArrayList<>());
    }
    
    @Override
    public boolean deleteFilterCombination(Long filterId, Long userId) {
        Long userKey = userId != null ? userId : 0L;
        Map<Long, String> userFilters = savedFilterCombinations.get(userKey);
        
        if (userFilters != null && userFilters.containsKey(filterId)) {
            userFilters.remove(filterId);
            savedFilters.remove(filterId);
            logger.info("Deleted filter combination {} for user {}", filterId, userId);
            return true;
        }
        
        return false;
    }
    
    @Override
    public List<List<FilterCriteria>> getRecentFilterCombinations(Long userId, int maxResults) {
        Long userKey = userId != null ? userId : 0L;
        List<List<FilterCriteria>> history = filterHistory.getOrDefault(userKey, new ArrayList<>());
        
        return history.stream()
                     .limit(maxResults)
                     .collect(Collectors.toList());
    }
    
    @Override
    public void saveFilterToHistory(Long userId, List<FilterCriteria> filters) {
        Long userKey = userId != null ? userId : 0L;
        List<List<FilterCriteria>> history = filterHistory.computeIfAbsent(userKey, k -> new ArrayList<>());
        
        // Add to beginning of list (most recent first)
        history.add(0, new ArrayList<>(filters));
        
        // Keep only last 50 entries
        if (history.size() > 50) {
            history.subList(50, history.size()).clear();
        }
    }
    
    @Override
    public List<Map<String, Object>> getPopularFilterCombinations(int maxResults) {
        return popularFilters.entrySet().stream()
                           .sorted(Map.Entry.<List<FilterCriteria>, Integer>comparingByValue().reversed())
                           .limit(maxResults)
                           .map(entry -> {
                               Map<String, Object> result = new HashMap<>();
                               result.put("filters", entry.getKey());
                               result.put("usageCount", entry.getValue());
                               return result;
                           })
                           .collect(Collectors.toList());
    }
    
    @Override
    public void clearFilterHistory(Long userId) {
        Long userKey = userId != null ? userId : 0L;
        filterHistory.remove(userKey);
        logger.info("Cleared filter history for user {}", userId);
    }
    
    @Override
    public Map<String, Object> getFilterAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        
        analytics.put("totalSavedCombinations", savedFilters.size());
        analytics.put("totalUsers", savedFilterCombinations.size());
        analytics.put("popularFiltersCount", popularFilters.size());
        
        // Most popular entity types
        Map<String, Long> entityTypeCounts = popularFilters.keySet().stream()
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(FilterCriteria::getEntityType, Collectors.counting()));
        analytics.put("popularEntityTypes", entityTypeCounts);
        
        // Most popular operators
        Map<String, Long> operatorCounts = popularFilters.keySet().stream()
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(FilterCriteria::getOperator, Collectors.counting()));
        analytics.put("popularOperators", operatorCounts);
        
        return analytics;
    }
    
    @Override
    public List<FilterCriteria> getFilterSuggestions(FilterCriteria partialFilter, int maxSuggestions) {
        // This is a simplified implementation
        // In a real system, this would use machine learning or more sophisticated algorithms
        return new ArrayList<>();
    }
    
    @Override
    public FilterResult combineSearchAndFilters(String searchQuery, List<FilterCriteria> filters, int page, int size) {
        // First apply filters
        FilterResult filterResult = applyFilters(filters, 0, Integer.MAX_VALUE);
        
        // Then apply search to filtered results
        // This is a simplified implementation - in practice, you'd want to optimize this
        List<SearchResult> combinedResults = filterResult.getResults().stream()
                .filter(result -> matchesSearchQuery(result, searchQuery))
                .collect(Collectors.toList());
        
        // Apply pagination
        int totalResults = combinedResults.size();
        int totalPages = (int) Math.ceil((double) totalResults / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalResults);
        
        List<SearchResult> paginatedResults = startIndex < totalResults ? 
                combinedResults.subList(startIndex, endIndex) : new ArrayList<>();
        
        FilterResult result = new FilterResult(filters, paginatedResults, filterResult.getResultsByType(), page, size, totalPages);
        result.setTotalResults(totalResults);
        
        return result;
    }
    
    private boolean matchesSearchQuery(SearchResult result, String query) {
        if (query == null || query.trim().isEmpty()) return true;
        
        String lowerQuery = query.toLowerCase();
        return (result.getTitle() != null && result.getTitle().toLowerCase().contains(lowerQuery)) ||
               (result.getDescription() != null && result.getDescription().toLowerCase().contains(lowerQuery));
    }
    
    @Override
    public Map<String, Object> getFilterAggregations(List<FilterCriteria> filters, List<String> aggregationFields) {
        // Simplified implementation - would be more sophisticated in production
        Map<String, Object> aggregations = new HashMap<>();
        
        FilterResult result = applyFilters(filters);
        aggregations.put("totalCount", result.getTotalResults());
        aggregations.put("resultsByType", result.getResultsByType());
        
        return aggregations;
    }
    
    @Override
    public byte[] exportFilteredResults(List<FilterCriteria> filters, String format) {
        // Simplified implementation - would use proper export libraries in production
        FilterResult result = applyFilters(filters);
        
        StringBuilder export = new StringBuilder();
        
        if ("csv".equalsIgnoreCase(format)) {
            export.append("Entity Type,ID,Title,Description\n");
            for (SearchResult searchResult : result.getResults()) {
                export.append(String.format("%s,%d,%s,%s\n", 
                    searchResult.getEntityType(),
                    searchResult.getEntityId(),
                    searchResult.getTitle().replace(",", ";"),
                    searchResult.getDescription().replace(",", ";")));
            }
        } else {
            export.append("Filter Results Export\n");
            export.append("===================\n\n");
            for (SearchResult searchResult : result.getResults()) {
                export.append(String.format("%s: %s - %s\n", 
                    searchResult.getEntityType(),
                    searchResult.getTitle(),
                    searchResult.getDescription()));
            }
        }
        
        return export.toString().getBytes();
    }
}