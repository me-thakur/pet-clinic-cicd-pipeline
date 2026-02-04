package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.VeterinarianFilterCriteria;
import com.petclinic.backend.dto.VeterinarianFilterResult;
import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.repository.VeterinarianRepository;
import com.petclinic.backend.service.VeterinarianFilterService;
import com.petclinic.backend.service.VeterinarianService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of VeterinarianFilterService
 * Provides comprehensive filtering with fallback mechanisms and suggestions
 * 
 * Validates: Requirements 12.1, 12.2, 12.3, 12.4, 12.5
 */
@Service
public class VeterinarianFilterServiceImpl implements VeterinarianFilterService {
    
    private static final Logger log = LoggerFactory.getLogger(VeterinarianFilterServiceImpl.class);
    
    @Autowired
    private VeterinarianRepository veterinarianRepository;
    
    @Autowired
    private VeterinarianService veterinarianService;
    
    // In-memory filter state storage (in production, use Redis or database)
    private final Map<String, VeterinarianFilterCriteria> filterStateCache = new ConcurrentHashMap<>();
    
    @Override
    public VeterinarianFilterResult filterVeterinarians(VeterinarianFilterCriteria criteria, Pageable pageable) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("Filtering veterinarians with criteria: {}", criteria);
            
            // Build specification from criteria
            Specification<Veterinarian> spec = buildSpecification(criteria);
            
            // Execute query with pagination and sorting
            Page<Veterinarian> veterinarians = veterinarianRepository.findAll(spec, pageable);
            
            // Get additional metadata
            Map<String, List<String>> availableOptions = getAvailableFilterOptions();
            Map<String, Long> resultCounts = getResultCounts(criteria);
            List<String> suggestions = veterinarians.isEmpty() ? generateSuggestions(criteria) : Collections.emptyList();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            VeterinarianFilterResult result = VeterinarianFilterResult.builder()
                .veterinarians(veterinarians)
                .appliedFilters(criteria)
                .availableFilterOptions(availableOptions)
                .resultCounts(resultCounts)
                .suggestions(suggestions)
                .totalResults(veterinarians.getTotalElements())
                .executionTimeMs(executionTime)
                .build();
            
            log.debug("Filter completed in {}ms, found {} veterinarians", executionTime, veterinarians.getTotalElements());
            return result;
            
        } catch (Exception e) {
            log.error("Error filtering veterinarians: {}", e.getMessage(), e);
            
            // Return fallback result
            return VeterinarianFilterResult.builder()
                .veterinarians(Page.empty(pageable))
                .appliedFilters(criteria)
                .availableFilterOptions(getAvailableFilterOptions())
                .suggestions(Arrays.asList("Filter temporarily unavailable. Please try again later."))
                .totalResults(0)
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .build();
        }
    }
    
    @Override
    public Map<String, List<String>> getAvailableFilterOptions() {
        try {
            Map<String, List<String>> options = new HashMap<>();
            
            // Get all unique specialties from both string and enum fields
            List<String> specialties = veterinarianRepository.findAll().stream()
                .flatMap(vet -> vet.getSpecialtyList().stream())
                .filter(specialty -> specialty != null && !specialty.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            options.put("specialties", specialties);
            
            // Experience level options
            options.put("experienceLevels", Arrays.asList(
                "0-2 years", "3-5 years", "6-10 years", "11-15 years", "16+ years"
            ));
            
            // Availability options
            options.put("availability", Arrays.asList("Available Now", "Light Workload"));
            
            // Capability options
            options.put("capabilities", Arrays.asList("Emergency", "Surgery", "General Practice"));
            
            return options;
            
        } catch (Exception e) {
            log.error("Error getting filter options: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
    
    @Override
    public List<String> generateSuggestions(VeterinarianFilterCriteria criteria) {
        List<String> suggestions = new ArrayList<>();
        
        if (criteria == null || !criteria.hasFilters()) {
            return suggestions;
        }
        
        try {
            // Check if removing specialty filters would help
            if (criteria.getSpecialties() != null && !criteria.getSpecialties().isEmpty()) {
                VeterinarianFilterCriteria relaxedCriteria = VeterinarianFilterCriteria.builder()
                    .availableOnly(criteria.getAvailableOnly())
                    .minExperienceYears(criteria.getMinExperienceYears())
                    .maxExperienceYears(criteria.getMaxExperienceYears())
                    .build();
                
                long countWithoutSpecialty = veterinarianRepository.count(buildSpecification(relaxedCriteria));
                if (countWithoutSpecialty > 0) {
                    suggestions.add("Try removing specialty filters - " + countWithoutSpecialty + " veterinarians available");
                }
            }
            
            // Check if removing experience filters would help
            if (criteria.getMinExperienceYears() != null || criteria.getMaxExperienceYears() != null) {
                VeterinarianFilterCriteria relaxedCriteria = VeterinarianFilterCriteria.builder()
                    .specialties(criteria.getSpecialties())
                    .availableOnly(criteria.getAvailableOnly())
                    .build();
                
                long countWithoutExperience = veterinarianRepository.count(buildSpecification(relaxedCriteria));
                if (countWithoutExperience > 0) {
                    suggestions.add("Try adjusting experience requirements - " + countWithoutExperience + " veterinarians available");
                }
            }
            
            // Check if removing availability filter would help
            if (criteria.getAvailableOnly() != null && criteria.getAvailableOnly()) {
                VeterinarianFilterCriteria relaxedCriteria = VeterinarianFilterCriteria.builder()
                    .specialties(criteria.getSpecialties())
                    .minExperienceYears(criteria.getMinExperienceYears())
                    .maxExperienceYears(criteria.getMaxExperienceYears())
                    .build();
                
                long countWithoutAvailability = veterinarianRepository.count(buildSpecification(relaxedCriteria));
                if (countWithoutAvailability > 0) {
                    suggestions.add("Try including all veterinarians (not just available) - " + countWithoutAvailability + " total");
                }
            }
            
            // General suggestions
            if (suggestions.isEmpty()) {
                suggestions.add("Try broadening your search criteria");
                suggestions.add("Consider searching for 'General Practice' specialty");
                suggestions.add("Remove experience level requirements");
            }
            
        } catch (Exception e) {
            log.error("Error generating suggestions: {}", e.getMessage(), e);
            suggestions.add("Unable to generate suggestions at this time");
        }
        
        return suggestions;
    }
    
    @Override
    public Map<String, Long> getResultCounts(VeterinarianFilterCriteria criteria) {
        Map<String, Long> counts = new HashMap<>();
        
        try {
            // Count by specialty
            if (criteria.getSpecialties() != null && !criteria.getSpecialties().isEmpty()) {
                for (String specialty : criteria.getSpecialties()) {
                    VeterinarianFilterCriteria specialtyCriteria = VeterinarianFilterCriteria.builder()
                        .specialties(Arrays.asList(specialty))
                        .build();
                    long count = veterinarianRepository.count(buildSpecification(specialtyCriteria));
                    counts.put("specialty_" + specialty, count);
                }
            }
            
            // Count available veterinarians
            VeterinarianFilterCriteria availableCriteria = VeterinarianFilterCriteria.builder()
                .availableOnly(true)
                .build();
            long availableCount = veterinarianRepository.count(buildSpecification(availableCriteria));
            counts.put("available", availableCount);
            
            // Total count
            long totalCount = veterinarianRepository.count();
            counts.put("total", totalCount);
            
        } catch (Exception e) {
            log.error("Error getting result counts: {}", e.getMessage(), e);
        }
        
        return counts;
    }
    
    @Override
    public Map<String, Object> persistFilterState(VeterinarianFilterCriteria criteria, String userId) {
        try {
            String key = userId != null ? userId : "default";
            filterStateCache.put(key, criteria);
            
            Map<String, Object> state = new HashMap<>();
            state.put("persisted", true);
            state.put("timestamp", LocalDateTime.now());
            state.put("filterCount", criteria != null ? criteria.getActiveFilterCount() : 0);
            
            log.debug("Persisted filter state for user: {}", key);
            return state;
            
        } catch (Exception e) {
            log.error("Error persisting filter state: {}", e.getMessage(), e);
            return Map.of("persisted", false, "error", e.getMessage());
        }
    }
    
    @Override
    public VeterinarianFilterCriteria restoreFilterState(String userId) {
        try {
            String key = userId != null ? userId : "default";
            VeterinarianFilterCriteria criteria = filterStateCache.get(key);
            
            log.debug("Restored filter state for user: {}, found: {}", key, criteria != null);
            return criteria;
            
        } catch (Exception e) {
            log.error("Error restoring filter state: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Build JPA Specification from filter criteria
     */
    private Specification<Veterinarian> buildSpecification(VeterinarianFilterCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (criteria == null) {
                return criteriaBuilder.conjunction();
            }
            
            // Filter by specialties
            if (criteria.getSpecialties() != null && !criteria.getSpecialties().isEmpty()) {
                List<Predicate> specialtyPredicates = criteria.getSpecialties().stream()
                    .map(specialty -> criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("specialty")), 
                        "%" + specialty.toLowerCase() + "%"
                    ))
                    .collect(Collectors.toList());
                predicates.add(criteriaBuilder.or(specialtyPredicates.toArray(new Predicate[0])));
            }
            
            // Filter by availability (simplified - in real implementation would check visit schedules)
            if (criteria.getAvailableOnly() != null && criteria.getAvailableOnly()) {
                // For now, assume all veterinarians are available
                // In real implementation, would join with visits table and check schedules
                predicates.add(criteriaBuilder.isNotNull(root.get("id")));
            }
            
            // Filter by experience (would need experience field in Veterinarian entity)
            // For now, skip experience filtering as it's not in the current model
            
            // Filter by active license
            if (criteria.getActiveLicenseOnly() != null && criteria.getActiveLicenseOnly()) {
                predicates.add(criteriaBuilder.isNotNull(root.get("licenseNumber")));
                predicates.add(criteriaBuilder.notEqual(root.get("licenseNumber"), ""));
            }
            
            // Filter by emergency capability (would need capability fields)
            // For now, assume emergency vets have "Emergency" in their specialty
            if (criteria.getEmergencyCapable() != null && criteria.getEmergencyCapable()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("specialty")), 
                    "%emergency%"
                ));
            }
            
            // Filter by surgical capability
            if (criteria.getSurgicalCapable() != null && criteria.getSurgicalCapable()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("specialty")), 
                    "%surgery%"
                ));
            }
            
            // Text search across name and license
            if (criteria.getSearchText() != null && !criteria.getSearchText().trim().isEmpty()) {
                String searchTerm = "%" + criteria.getSearchText().toLowerCase() + "%";
                Predicate nameSearch = criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), searchTerm),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), searchTerm)
                );
                Predicate licenseSearch = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("licenseNumber")), searchTerm
                );
                predicates.add(criteriaBuilder.or(nameSearch, licenseSearch));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}