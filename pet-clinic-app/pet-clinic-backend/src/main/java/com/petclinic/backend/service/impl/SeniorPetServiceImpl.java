package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.SeniorPetInfo;
import com.petclinic.backend.dto.SeniorPetSearchCriteria;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.service.SeniorPetService;
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

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of SeniorPetService for senior pet management and search operations
 * Provides specialized functionality for senior pet care and monitoring
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5
 */
@Service
@Transactional(readOnly = true)
public class SeniorPetServiceImpl implements SeniorPetService {
    
    private static final Logger logger = LoggerFactory.getLogger(SeniorPetServiceImpl.class);
    
    @Autowired
    private PetRepository petRepository;
    
    // Configurable age thresholds per species - Requirement 2.2
    private final Map<String, Integer> speciesAgeThresholds = new HashMap<String, Integer>() {{
        put("Dog", 7);
        put("Cat", 7);
        put("Bird", 5);
        put("Rabbit", 5);
        put("Hamster", 2);
        put("Guinea Pig", 4);
        put("Ferret", 6);
        put("Horse", 15);
        put("Reptile", 10);
        put("Fish", 3);
    }};
    
    @Override
    public Page<SeniorPetInfo> searchSeniorPets(SeniorPetSearchCriteria criteria, Pageable pageable) {
        logger.debug("Searching senior pets with criteria: {}", criteria);
        
        try {
            Specification<Pet> spec = buildSeniorPetSpecification(criteria);
            
            // Apply sorting
            Sort sort = buildSort(criteria);
            Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(), 
                pageable.getPageSize(), 
                sort
            );
            
            Page<Pet> pets = petRepository.findAll(spec, sortedPageable);
            
            List<SeniorPetInfo> seniorPetInfos = pets.getContent().stream()
                .map(pet -> toSeniorPetInfo(pet, criteria))
                .collect(Collectors.toList());
            
            logger.debug("Found {} senior pets matching criteria", pets.getTotalElements());
            return new PageImpl<>(seniorPetInfos, sortedPageable, pets.getTotalElements());
            
        } catch (Exception e) {
            logger.error("Error searching senior pets with criteria: {}", criteria, e);
            // Fallback to basic senior pets list - Requirement 2.5
            return getFallbackSeniorPets(pageable);
        }
    }
    
    @Override
    public Page<SeniorPetInfo> getAllSeniorPets(Pageable pageable) {
        logger.debug("Getting all senior pets with pagination: {}", pageable);
        
        try {
            // Use default age threshold of 7 years
            LocalDate cutoffDate = LocalDate.now().minusYears(7);
            
            Specification<Pet> spec = (root, query, criteriaBuilder) -> 
                criteriaBuilder.lessThan(root.get("birthDate"), cutoffDate);
            
            Sort sort = Sort.by(Sort.Direction.DESC, "birthDate"); // Oldest first
            Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(), 
                pageable.getPageSize(), 
                sort
            );
            
            Page<Pet> pets = petRepository.findAll(spec, sortedPageable);
            
            List<SeniorPetInfo> seniorPetInfos = pets.getContent().stream()
                .map(pet -> toSeniorPetInfo(pet, null))
                .collect(Collectors.toList());
            
            logger.debug("Found {} senior pets total", pets.getTotalElements());
            return new PageImpl<>(seniorPetInfos, sortedPageable, pets.getTotalElements());
            
        } catch (Exception e) {
            logger.error("Error getting all senior pets", e);
            return getFallbackSeniorPets(pageable);
        }
    }
    
    @Override
    public Page<SeniorPetInfo> getSeniorPetsBySpecies(String species, Pageable pageable) {
        logger.debug("Getting senior pets by species: {}", species);
        
        try {
            Integer threshold = getAgeThresholdForSpecies(species);
            LocalDate cutoffDate = LocalDate.now().minusYears(threshold);
            
            Specification<Pet> spec = (root, query, criteriaBuilder) -> 
                criteriaBuilder.and(
                    criteriaBuilder.equal(criteriaBuilder.lower(root.get("species")), species.toLowerCase()),
                    criteriaBuilder.lessThan(root.get("birthDate"), cutoffDate)
                );
            
            Sort sort = Sort.by(Sort.Direction.DESC, "birthDate");
            Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(), 
                pageable.getPageSize(), 
                sort
            );
            
            Page<Pet> pets = petRepository.findAll(spec, sortedPageable);
            
            List<SeniorPetInfo> seniorPetInfos = pets.getContent().stream()
                .map(pet -> toSeniorPetInfo(pet, null))
                .collect(Collectors.toList());
            
            logger.debug("Found {} senior {} pets", pets.getTotalElements(), species);
            return new PageImpl<>(seniorPetInfos, sortedPageable, pets.getTotalElements());
            
        } catch (Exception e) {
            logger.error("Error getting senior pets by species: {}", species, e);
            return getFallbackSeniorPets(pageable);
        }
    }
    
    @Override
    public Page<SeniorPetInfo> getSeniorPetsWithHealthCondition(String healthCondition, Pageable pageable) {
        logger.debug("Getting senior pets with health condition: {}", healthCondition);
        
        try {
            LocalDate cutoffDate = LocalDate.now().minusYears(7);
            
            Specification<Pet> spec = (root, query, criteriaBuilder) -> 
                criteriaBuilder.and(
                    criteriaBuilder.lessThan(root.get("birthDate"), cutoffDate),
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("medicalHistory")), 
                        "%" + healthCondition.toLowerCase() + "%"
                    )
                );
            
            Sort sort = Sort.by(Sort.Direction.DESC, "birthDate");
            Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(), 
                pageable.getPageSize(), 
                sort
            );
            
            Page<Pet> pets = petRepository.findAll(spec, sortedPageable);
            
            List<SeniorPetInfo> seniorPetInfos = pets.getContent().stream()
                .map(pet -> {
                    SeniorPetInfo info = toSeniorPetInfo(pet, null);
                    info.setHealthHighlighted(true); // Highlight health condition match
                    return info;
                })
                .collect(Collectors.toList());
            
            logger.debug("Found {} senior pets with health condition: {}", pets.getTotalElements(), healthCondition);
            return new PageImpl<>(seniorPetInfos, sortedPageable, pets.getTotalElements());
            
        } catch (Exception e) {
            logger.error("Error getting senior pets with health condition: {}", healthCondition, e);
            return getFallbackSeniorPets(pageable);
        }
    }
    
    @Override
    public Page<SeniorPetInfo> getSeniorPetsNeedingSpecialCare(Pageable pageable) {
        logger.debug("Getting senior pets needing special care");
        
        try {
            LocalDate cutoffDate = LocalDate.now().minusYears(7);
            LocalDate recentVisitCutoff = LocalDate.now().minusMonths(6);
            
            Specification<Pet> spec = (root, query, criteriaBuilder) -> {
                // Senior pets that either have medical history or haven't had recent visits
                Predicate isSenior = criteriaBuilder.lessThan(root.get("birthDate"), cutoffDate);
                
                Predicate hasMedicalHistory = criteriaBuilder.and(
                    criteriaBuilder.isNotNull(root.get("medicalHistory")),
                    criteriaBuilder.notEqual(root.get("medicalHistory"), "")
                );
                
                // This is a simplified check - in a real system you'd join with visits table
                Predicate needsCare = criteriaBuilder.or(hasMedicalHistory);
                
                return criteriaBuilder.and(isSenior, needsCare);
            };
            
            Sort sort = Sort.by(Sort.Direction.DESC, "birthDate");
            Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(), 
                pageable.getPageSize(), 
                sort
            );
            
            Page<Pet> pets = petRepository.findAll(spec, sortedPageable);
            
            List<SeniorPetInfo> seniorPetInfos = pets.getContent().stream()
                .map(pet -> {
                    SeniorPetInfo info = toSeniorPetInfo(pet, null);
                    info.setSpecialCareNeeded(true);
                    return info;
                })
                .collect(Collectors.toList());
            
            logger.debug("Found {} senior pets needing special care", pets.getTotalElements());
            return new PageImpl<>(seniorPetInfos, sortedPageable, pets.getTotalElements());
            
        } catch (Exception e) {
            logger.error("Error getting senior pets needing special care", e);
            return getFallbackSeniorPets(pageable);
        }
    }
    
    @Override
    public Page<SeniorPetInfo> getFallbackSeniorPets(Pageable pageable) {
        logger.debug("Using fallback senior pets list with age sorting");
        
        try {
            // Simple fallback - get all pets and filter by age
            LocalDate cutoffDate = LocalDate.now().minusYears(7);
            List<Pet> allPets = petRepository.findSeniorPets(cutoffDate);
            
            // Sort by age (oldest first)
            allPets.sort((p1, p2) -> {
                if (p1.getBirthDate() == null && p2.getBirthDate() == null) return 0;
                if (p1.getBirthDate() == null) return 1;
                if (p2.getBirthDate() == null) return -1;
                return p1.getBirthDate().compareTo(p2.getBirthDate());
            });
            
            // Apply pagination manually
            int start = pageable.getPageNumber() * pageable.getPageSize();
            int end = Math.min(start + pageable.getPageSize(), allPets.size());
            
            List<Pet> pageContent = allPets.subList(start, end);
            List<SeniorPetInfo> seniorPetInfos = pageContent.stream()
                .map(pet -> toSeniorPetInfo(pet, null))
                .collect(Collectors.toList());
            
            logger.debug("Fallback returned {} senior pets out of {} total", pageContent.size(), allPets.size());
            return new PageImpl<>(seniorPetInfos, pageable, allPets.size());
            
        } catch (Exception e) {
            logger.error("Error in fallback senior pets list", e);
            // Ultimate fallback - empty page
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
    }
    
    @Override
    public Map<String, Integer> getSpeciesAgeThresholds() {
        return new HashMap<>(speciesAgeThresholds);
    }
    
    @Override
    @Transactional
    public void updateSpeciesAgeThreshold(String species, Integer threshold) {
        logger.debug("Updating age threshold for species {} to {}", species, threshold);
        speciesAgeThresholds.put(species, threshold);
    }
    
    @Override
    public Map<String, Object> getSeniorPetStatistics() {
        logger.debug("Getting senior pet statistics");
        
        try {
            LocalDate cutoffDate = LocalDate.now().minusYears(7);
            List<Pet> seniorPets = petRepository.findSeniorPets(cutoffDate);
            
            Map<String, Long> speciesCount = seniorPets.stream()
                .collect(Collectors.groupingBy(Pet::getSpecies, Collectors.counting()));
            
            long totalSeniorPets = seniorPets.size();
            long totalPets = petRepository.count();
            double seniorPercentage = totalPets > 0 ? (double) totalSeniorPets / totalPets * 100 : 0;
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalSeniorPets", totalSeniorPets);
            statistics.put("totalPets", totalPets);
            statistics.put("seniorPercentage", Math.round(seniorPercentage * 100.0) / 100.0);
            statistics.put("speciesBreakdown", speciesCount);
            statistics.put("ageThresholds", getSpeciesAgeThresholds());
            
            logger.debug("Senior pet statistics: {}", statistics);
            return statistics;
            
        } catch (Exception e) {
            logger.error("Error getting senior pet statistics", e);
            return Collections.emptyMap();
        }
    }
    
    @Override
    public List<String> getAvailableHealthConditions() {
        logger.debug("Getting available health conditions");
        
        try {
            LocalDate cutoffDate = LocalDate.now().minusYears(7);
            List<Pet> seniorPets = petRepository.findSeniorPets(cutoffDate);
            
            Set<String> conditions = new HashSet<>();
            for (Pet pet : seniorPets) {
                if (pet.getMedicalHistory() != null && !pet.getMedicalHistory().trim().isEmpty()) {
                    // Extract common health conditions from medical history
                    String history = pet.getMedicalHistory().toLowerCase();
                    if (history.contains("arthritis")) conditions.add("Arthritis");
                    if (history.contains("diabetes")) conditions.add("Diabetes");
                    if (history.contains("heart")) conditions.add("Heart Disease");
                    if (history.contains("kidney")) conditions.add("Kidney Disease");
                    if (history.contains("cancer")) conditions.add("Cancer");
                    if (history.contains("allergy") || history.contains("allergies")) conditions.add("Allergies");
                    if (history.contains("dental")) conditions.add("Dental Issues");
                    if (history.contains("vision") || history.contains("eye")) conditions.add("Vision Problems");
                    if (history.contains("mobility") || history.contains("joint")) conditions.add("Mobility Issues");
                }
            }
            
            List<String> sortedConditions = new ArrayList<>(conditions);
            Collections.sort(sortedConditions);
            
            logger.debug("Found {} health conditions", sortedConditions.size());
            return sortedConditions;
            
        } catch (Exception e) {
            logger.error("Error getting available health conditions", e);
            return Arrays.asList("Arthritis", "Diabetes", "Heart Disease", "Kidney Disease", "Cancer", "Allergies");
        }
    }
    
    @Override
    public List<String> getAvailableSpecies() {
        logger.debug("Getting available species with senior pets");
        
        try {
            LocalDate cutoffDate = LocalDate.now().minusYears(7);
            List<Pet> seniorPets = petRepository.findSeniorPets(cutoffDate);
            
            List<String> species = seniorPets.stream()
                .map(Pet::getSpecies)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            
            logger.debug("Found {} species with senior pets", species.size());
            return species;
            
        } catch (Exception e) {
            logger.error("Error getting available species", e);
            return Arrays.asList("Dog", "Cat", "Bird", "Rabbit");
        }
    }
    
    @Override
    public boolean isPetSenior(Long petId) {
        try {
            Optional<Pet> petOpt = petRepository.findById(petId);
            if (petOpt.isPresent()) {
                Pet pet = petOpt.get();
                Integer threshold = getAgeThresholdForSpecies(pet.getSpecies());
                return pet.getAge() >= threshold;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error checking if pet {} is senior", petId, e);
            return false;
        }
    }
    
    @Override
    public Integer getAgeThresholdForSpecies(String species) {
        return speciesAgeThresholds.getOrDefault(species, 7); // Default to 7 years
    }
    
    // Helper Methods
    
    private Specification<Pet> buildSeniorPetSpecification(SeniorPetSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Age-based filtering with species-specific thresholds
            if (criteria.getSpecies() != null && !criteria.getSpecies().trim().isEmpty()) {
                Integer threshold = getAgeThresholdForSpecies(criteria.getSpecies());
                LocalDate cutoffDate = LocalDate.now().minusYears(threshold);
                predicates.add(criteriaBuilder.lessThan(root.get("birthDate"), cutoffDate));
                predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("species")), 
                    criteria.getSpecies().toLowerCase()
                ));
            } else {
                // Use default threshold if no species specified
                LocalDate cutoffDate = LocalDate.now().minusYears(7);
                predicates.add(criteriaBuilder.lessThan(root.get("birthDate"), cutoffDate));
            }
            
            // Min age filter
            if (criteria.getMinAge() != null) {
                LocalDate maxBirthDate = LocalDate.now().minusYears(criteria.getMinAge());
                predicates.add(criteriaBuilder.lessThan(root.get("birthDate"), maxBirthDate));
            }
            
            // Max age filter
            if (criteria.getMaxAge() != null) {
                LocalDate minBirthDate = LocalDate.now().minusYears(criteria.getMaxAge());
                predicates.add(criteriaBuilder.greaterThan(root.get("birthDate"), minBirthDate));
            }
            
            // Breed filter
            if (criteria.getBreed() != null && !criteria.getBreed().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("breed")), 
                    "%" + criteria.getBreed().toLowerCase() + "%"
                ));
            }
            
            // Health condition filter
            if (criteria.getHealthCondition() != null && !criteria.getHealthCondition().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("medicalHistory")), 
                    "%" + criteria.getHealthCondition().toLowerCase() + "%"
                ));
            }
            
            // Owner ID filter
            if (criteria.getOwnerId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("owner").get("id"), criteria.getOwnerId()));
            }
            
            // Owner name filter
            if (criteria.getOwnerName() != null && !criteria.getOwnerName().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(
                        criteriaBuilder.concat(
                            criteriaBuilder.concat(root.get("owner").get("firstName"), " "),
                            root.get("owner").get("lastName")
                        )
                    ),
                    "%" + criteria.getOwnerName().toLowerCase() + "%"
                ));
            }
            
            // General search term
            if (criteria.getSearchTerm() != null && !criteria.getSearchTerm().trim().isEmpty()) {
                String searchTerm = "%" + criteria.getSearchTerm().toLowerCase() + "%";
                Predicate nameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchTerm);
                Predicate speciesMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("species")), searchTerm);
                Predicate breedMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("breed")), searchTerm);
                Predicate ownerMatch = criteriaBuilder.like(
                    criteriaBuilder.lower(
                        criteriaBuilder.concat(
                            criteriaBuilder.concat(root.get("owner").get("firstName"), " "),
                            root.get("owner").get("lastName")
                        )
                    ),
                    searchTerm
                );
                
                predicates.add(criteriaBuilder.or(nameMatch, speciesMatch, breedMatch, ownerMatch));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    private Sort buildSort(SeniorPetSearchCriteria criteria) {
        String sortBy = criteria.getSortBy() != null ? criteria.getSortBy() : "age";
        String sortDirection = criteria.getSortDirection() != null ? criteria.getSortDirection() : "desc";
        
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? 
            Sort.Direction.ASC : Sort.Direction.DESC;
        
        // Map sort fields
        switch (sortBy.toLowerCase()) {
            case "age":
                return Sort.by(direction, "birthDate").reverse(); // Reverse because older birth date = higher age
            case "name":
                return Sort.by(direction, "name");
            case "species":
                return Sort.by(direction, "species");
            case "breed":
                return Sort.by(direction, "breed");
            case "owner":
                return Sort.by(direction, "owner.lastName", "owner.firstName");
            default:
                return Sort.by(Sort.Direction.DESC, "birthDate"); // Default: oldest first
        }
    }
    
    private SeniorPetInfo toSeniorPetInfo(Pet pet, SeniorPetSearchCriteria criteria) {
        SeniorPetInfo info = new SeniorPetInfo();
        
        // Basic pet information
        info.setPetId(pet.getId());
        info.setPetName(pet.getName());
        info.setSpecies(pet.getSpecies());
        info.setBreed(pet.getBreed());
        info.setBirthDate(pet.getBirthDate());
        info.setAge(pet.getAge());
        info.setMedicalHistory(pet.getMedicalHistory());
        info.setCreatedAt(pet.getCreatedAt());
        info.setUpdatedAt(pet.getUpdatedAt());
        
        // Senior status with species-specific threshold
        Integer threshold = getAgeThresholdForSpecies(pet.getSpecies());
        info.setSeniorThreshold(threshold);
        info.setIsSenior(pet.getAge() >= threshold);
        
        // Owner information
        if (pet.getOwner() != null) {
            info.setOwnerId(pet.getOwner().getId());
            info.setOwnerName(pet.getOwner().getFirstName() + " " + pet.getOwner().getLastName());
            info.setOwnerEmail(pet.getOwner().getEmail());
            info.setOwnerPhone(pet.getOwner().getMobileNumber() != null ? 
                pet.getOwner().getMobileNumber() : pet.getOwner().getTelephone());
        }
        
        // Visit information
        if (pet.getVisits() != null && !pet.getVisits().isEmpty()) {
            info.setVisitCount(pet.getVisits().size());
            
            // Find last visit date
            Optional<LocalDate> lastVisit = pet.getVisits().stream()
                .map(Visit::getVisitDate)
                .filter(Objects::nonNull)
                .map(dateTime -> dateTime.toLocalDate())
                .max(LocalDate::compareTo);
            lastVisit.ifPresent(info::setLastVisitDate);
            
            // Find upcoming visit date
            Optional<LocalDate> upcomingVisit = pet.getVisits().stream()
                .map(Visit::getVisitDate)
                .filter(Objects::nonNull)
                .map(dateTime -> dateTime.toLocalDate())
                .filter(date -> date.isAfter(LocalDate.now()))
                .min(LocalDate::compareTo);
            upcomingVisit.ifPresent(info::setUpcomingVisitDate);
        } else {
            info.setVisitCount(0);
        }
        
        // Health conditions extraction
        if (pet.getMedicalHistory() != null && !pet.getMedicalHistory().trim().isEmpty()) {
            List<String> conditions = extractHealthConditions(pet.getMedicalHistory());
            info.setHealthConditions(conditions);
            info.setSpecialCareNeeded(!conditions.isEmpty());
        }
        
        // Highlighting based on search criteria - Requirement 2.3
        if (criteria != null) {
            // Age highlighting
            if (criteria.getMinAge() != null || criteria.getMaxAge() != null) {
                info.setAgeHighlighted(true);
            }
            
            // Health highlighting
            if (criteria.getHealthCondition() != null && pet.getMedicalHistory() != null) {
                boolean hasCondition = pet.getMedicalHistory().toLowerCase()
                    .contains(criteria.getHealthCondition().toLowerCase());
                info.setHealthHighlighted(hasCondition);
            }
        }
        
        return info;
    }
    
    private List<String> extractHealthConditions(String medicalHistory) {
        List<String> conditions = new ArrayList<>();
        String history = medicalHistory.toLowerCase();
        
        if (history.contains("arthritis")) conditions.add("Arthritis");
        if (history.contains("diabetes")) conditions.add("Diabetes");
        if (history.contains("heart")) conditions.add("Heart Disease");
        if (history.contains("kidney")) conditions.add("Kidney Disease");
        if (history.contains("cancer")) conditions.add("Cancer");
        if (history.contains("allergy") || history.contains("allergies")) conditions.add("Allergies");
        if (history.contains("dental")) conditions.add("Dental Issues");
        if (history.contains("vision") || history.contains("eye")) conditions.add("Vision Problems");
        if (history.contains("mobility") || history.contains("joint")) conditions.add("Mobility Issues");
        
        return conditions;
    }
}