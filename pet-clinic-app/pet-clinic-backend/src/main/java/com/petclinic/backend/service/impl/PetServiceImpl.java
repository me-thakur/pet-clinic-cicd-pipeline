package com.petclinic.backend.service.impl;

import com.petclinic.backend.config.CacheConfig;
import com.petclinic.backend.exception.BusinessRuleException;
import com.petclinic.backend.exception.EntityNotFoundException;
import com.petclinic.backend.exception.ValidationException;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.repository.OwnerRepository;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.service.AuditService;
import com.petclinic.backend.service.PetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;

/**
 * Implementation of PetService providing comprehensive pet management functionality
 * Validates: Requirements 1.2, 1.3, 1.5
 */
@Service
@Transactional
public class PetServiceImpl implements PetService {
    
    private static final Logger logger = LoggerFactory.getLogger(PetServiceImpl.class);
    
    @Autowired
    private PetRepository petRepository;
    
    @Autowired
    private OwnerRepository ownerRepository;
    
    @Autowired
    private AuditService auditService;
    
    // CRUD Operations
    
    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.PETS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.SEARCH_RESULTS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.STATISTICS_CACHE, allEntries = true)
    })
    public Pet create(Pet pet) {
        logger.debug("Creating new pet: {}", pet);
        
        validatePetForCreation(pet);
        
        // Check for duplicate pet names for the same owner
        if (pet.getOwner() != null && StringUtils.hasText(pet.getName())) {
            boolean exists = petRepository.existsByNameIgnoreCaseAndOwnerId(
                pet.getName(), pet.getOwner().getId());
            if (exists) {
                throw new ValidationException("Pet name '" + pet.getName() + 
                    "' already exists for this owner");
            }
        }
        
        Pet savedPet = petRepository.save(pet);
        logger.info("Created pet with ID: {}", savedPet.getId());
        
        // Log pet creation
        auditService.logDataAccess("system", "CREATE", "Pet", savedPet.getId(), 
                                  "New pet created: " + savedPet.getName() + " for owner ID: " + 
                                  (savedPet.getOwner() != null ? savedPet.getOwner().getId() : "unknown"));
        
        return savedPet;
    }
    
    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.PETS_CACHE, key = "#id"),
        @CacheEvict(value = CacheConfig.SEARCH_RESULTS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.STATISTICS_CACHE, allEntries = true)
    })
    public Pet update(Long id, Pet pet) {
        logger.debug("Updating pet with ID: {}", id);
        
        Pet existingPet = petRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Pet", id));
        
        validatePetForUpdate(pet, existingPet);
        
        // Check for duplicate names if name is being changed
        if (!existingPet.getName().equalsIgnoreCase(pet.getName()) && 
            pet.getOwner() != null) {
            boolean exists = petRepository.existsByNameIgnoreCaseAndOwnerId(
                pet.getName(), pet.getOwner().getId());
            if (exists) {
                throw new ValidationException("Pet name '" + pet.getName() + 
                    "' already exists for this owner");
            }
        }
        
        // Update fields
        updatePetFields(existingPet, pet);
        
        Pet updatedPet = petRepository.save(existingPet);
        logger.info("Updated pet with ID: {}", updatedPet.getId());
        
        // Log pet update
        auditService.logDataAccess("system", "UPDATE", "Pet", updatedPet.getId(), 
                                  "Pet updated: " + updatedPet.getName());
        
        return updatedPet;
    }
    
    @Override
    @Cacheable(value = CacheConfig.PETS_CACHE, key = "#id")
    public Optional<Pet> findById(Long id) {
        logger.debug("Finding pet by ID: {}", id);
        Optional<Pet> pet = petRepository.findById(id);
        
        if (pet.isPresent()) {
            // Log sensitive pet data access
            auditService.logDataAccess("system", "READ", "Pet", id, 
                                      "Pet data accessed: " + pet.get().getName());
        }
        
        return pet;
    }
    
    @Override
    @Cacheable(value = CacheConfig.PETS_CACHE, key = "'all'")
    public List<Pet> findAll() {
        logger.debug("Finding all pets");
        return petRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }
    
    @Override
    public Page<Pet> findAllWithPagination(Pageable pageable) {
        logger.debug("Finding all pets with server-side pagination and sorting: {}", pageable);
        // Use repository's findAll method with Pageable for server-side sorting and pagination
        return petRepository.findAll(pageable);
    }
    
    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.PETS_CACHE, key = "#id"),
        @CacheEvict(value = CacheConfig.PETS_CACHE, key = "'all'"),
        @CacheEvict(value = CacheConfig.SEARCH_RESULTS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.STATISTICS_CACHE, allEntries = true)
    })
    public void deleteById(Long id) {
        logger.debug("Attempting to delete pet with ID: {}", id);
        
        Pet pet = petRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Pet", id));
        
        // Business rule: Cannot delete pet with existing visits
        if (!canDeletePet(id)) {
            throw new BusinessRuleException("Cannot delete pet with existing visits. " +
                "Pet '" + pet.getName() + "' has " + pet.getVisits().size() + " visit(s) on record.");
        }
        
        // Log pet deletion before actual deletion
        auditService.logDataAccess("system", "DELETE", "Pet", id, 
                                  "Pet deleted: " + pet.getName());
        
        petRepository.deleteById(id);
        logger.info("Deleted pet with ID: {}", id);
    }
    
    @Override
    public boolean existsById(Long id) {
        return petRepository.existsById(id);
    }
    
    @Override
    public long count() {
        return petRepository.count();
    }
    
    // Pet-specific methods
    
    @Override
    @Cacheable(value = CacheConfig.PETS_CACHE, key = "'owner-' + #ownerId")
    public List<Pet> findByOwner(Long ownerId) {
        logger.debug("Finding pets by owner ID: {}", ownerId);
        
        if (ownerId == null) {
            throw new ValidationException("Owner ID cannot be null");
        }
        
        // Verify owner exists
        if (!ownerRepository.existsById(ownerId)) {
            throw new EntityNotFoundException("Owner", ownerId);
        }
        
        return petRepository.findByOwnerId(ownerId);
    }
    
    @Override
    @Cacheable(value = CacheConfig.SEARCH_RESULTS_CACHE, key = "'search-' + #searchTerm")
    public List<Pet> searchPets(String searchTerm) {
        logger.debug("Searching pets with term: {}", searchTerm);
        
        if (!StringUtils.hasText(searchTerm)) {
            return findAll();
        }
        
        String trimmedTerm = searchTerm.trim();
        
        // Search across multiple fields using repository method
        Page<Pet> results = petRepository.searchPets(
            trimmedTerm,  // name
            null,         // species (search all)
            trimmedTerm,  // breed
            null,         // ownerId (search all)
            PageRequest.of(0, 1000, Sort.by("name"))
        );
        
        List<Pet> pets = results.getContent();
        
        // Also search by owner name
        List<Pet> petsByOwner = petRepository.findByOwnerNameContaining(trimmedTerm);
        
        // Combine results and remove duplicates
        List<Pet> allPets = new ArrayList<>(pets);
        petsByOwner.stream()
            .filter(pet -> !allPets.contains(pet))
            .forEach(allPets::add);
        
        logger.debug("Found {} pets matching search term: {}", allPets.size(), searchTerm);
        return allPets;
    }
    
    @Override
    @Cacheable(value = CacheConfig.PETS_CACHE, key = "'species-' + #species")
    public List<Pet> findBySpecies(String species) {
        logger.debug("Finding pets by species: {}", species);
        
        if (!StringUtils.hasText(species)) {
            throw new ValidationException("Species cannot be null or empty");
        }
        
        return petRepository.findBySpeciesIgnoreCase(species.trim());
    }
    
    @Override
    public List<Pet> findByBreed(String breed) {
        logger.debug("Finding pets by breed: {}", breed);
        
        if (!StringUtils.hasText(breed)) {
            throw new ValidationException("Breed cannot be null or empty");
        }
        
        return petRepository.findByBreedContainingIgnoreCase(breed.trim());
    }
    
    @Override
    public boolean canDeletePet(Long id) {
        logger.debug("Checking if pet can be deleted: {}", id);
        
        if (id == null) {
            return false;
        }
        
        Optional<Pet> petOpt = petRepository.findById(id);
        if (petOpt.isEmpty()) {
            return false;
        }
        
        Pet pet = petOpt.get();
        boolean canDelete = pet.getVisits().isEmpty();
        
        logger.debug("Pet {} can be deleted: {}", id, canDelete);
        return canDelete;
    }
    
    @Override
    public List<Pet> findPetsWithUpcomingVisits() {
        logger.debug("Finding pets with upcoming visits");
        
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusYears(1); // Look ahead 1 year
        
        return petRepository.findPetsWithVisitsBetween(today, futureDate);
    }
    
    @Override
    public List<Pet> findByAgeRange(int minAge, int maxAge) {
        logger.debug("Finding pets by age range: {} to {}", minAge, maxAge);
        
        if (minAge < 0 || maxAge < 0 || minAge > maxAge) {
            throw new ValidationException("Invalid age range: min=" + minAge + ", max=" + maxAge);
        }
        
        LocalDate today = LocalDate.now();
        LocalDate maxBirthDate = today.minusYears(minAge);
        LocalDate minBirthDate = today.minusYears(maxAge + 1);
        
        return petRepository.findByBirthDateBetween(minBirthDate, maxBirthDate);
    }
    
    // Additional search and filtering methods
    
    /**
     * Advanced search with multiple criteria
     * @param name Pet name (partial match)
     * @param species Pet species (exact match)
     * @param breed Pet breed (partial match)
     * @param ownerId Owner ID (exact match)
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of matching pets
     */
    @Override
    public Page<Pet> searchPetsAdvanced(String name, String species, String breed, 
                                       Long ownerId, int page, int size) {
        logger.debug("Advanced search - name: {}, species: {}, breed: {}, ownerId: {}", 
                    name, species, breed, ownerId);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        
        return petRepository.searchPets(
            StringUtils.hasText(name) ? name.trim() : null,
            StringUtils.hasText(species) ? species.trim() : null,
            StringUtils.hasText(breed) ? breed.trim() : null,
            ownerId,
            pageable
        );
    }
    
    /**
     * Find pets by medical history keywords
     * @param keywords Keywords to search in medical history
     * @return List of matching pets
     */
    @Override
    public List<Pet> findByMedicalHistory(String keywords) {
        logger.debug("Finding pets by medical history keywords: {}", keywords);
        
        if (!StringUtils.hasText(keywords)) {
            throw new ValidationException("Keywords cannot be null or empty");
        }
        
        return petRepository.findByMedicalHistoryContainingIgnoreCase(keywords.trim());
    }
    
    /**
     * Find young pets (less than specified age)
     * @param age Maximum age to be considered young
     * @return List of young pets
     */
    @Override
    public List<Pet> findYoungPets(int age) {
        logger.debug("Finding young pets under {} years old", age);
        
        if (age < 0) {
            throw new ValidationException("Age cannot be negative");
        }
        
        LocalDate cutoffDate = LocalDate.now().minusYears(age);
        return petRepository.findYoungPets(cutoffDate);
    }
    
    /**
     * Find senior pets (more than specified age)
     * @param age Minimum age to be considered senior
     * @return List of senior pets
     */
    @Override
    public List<Pet> findSeniorPets(int age) {
        logger.debug("Finding senior pets over {} years old", age);
        
        if (age < 0) {
            throw new ValidationException("Age cannot be negative");
        }
        
        LocalDate cutoffDate = LocalDate.now().minusYears(age);
        return petRepository.findSeniorPets(cutoffDate);
    }
    
    /**
     * Find pets without any visits
     * @return List of pets without visits
     */
    @Override
    public List<Pet> findPetsWithoutVisits() {
        logger.debug("Finding pets without visits");
        return petRepository.findPetsWithoutVisits();
    }
    
    /**
     * Get pet statistics
     * @return Map containing various pet statistics
     */
    @Override
    @Cacheable(value = CacheConfig.STATISTICS_CACHE, key = "'pet-statistics'")
    public Map<String, Object> getPetStatistics() {
        logger.debug("Getting pet statistics");
        Object[] stats = petRepository.getPetStatistics();
        
        Map<String, Object> statisticsMap = Map.of(
            "totalPets", stats[0],
            "totalSpecies", stats[1],
            "totalOwners", stats[2]
        );
        
        return statisticsMap;
    }
    
    /**
     * Get pet count by species
     * @return Map of species to count
     */
    @Override
    @Cacheable(value = CacheConfig.STATISTICS_CACHE, key = "'pet-count-by-species'")
    public Map<String, Long> getPetCountBySpecies() {
        logger.debug("Getting pet count by species");
        List<Object[]> speciesCount = petRepository.countPetsBySpecies();
        
        Map<String, Long> result = new HashMap<>();
        for (Object[] row : speciesCount) {
            result.put((String) row[0], (Long) row[1]);
        }
        
        return result;
    }
    
    /**
     * Count pets by species (legacy method)
     * @return List of [species, count] arrays
     */
    public List<Object[]> countPetsBySpecies() {
        logger.debug("Counting pets by species");
        return petRepository.countPetsBySpecies();
    }
    
    // Private helper methods
    
    private void validatePetForCreation(Pet pet) {
        if (pet == null) {
            throw new ValidationException("Pet cannot be null");
        }
        
        if (!StringUtils.hasText(pet.getName())) {
            throw new ValidationException("Pet name is required");
        }
        
        if (!StringUtils.hasText(pet.getSpecies())) {
            throw new ValidationException("Pet species is required");
        }
        
        if (pet.getOwner() == null || pet.getOwner().getId() == null) {
            throw new ValidationException("Pet must have a valid owner");
        }
        
        // Verify owner exists
        if (!ownerRepository.existsById(pet.getOwner().getId())) {
            throw new EntityNotFoundException("Owner", pet.getOwner().getId());
        }
        
        // Validate birth date
        if (pet.getBirthDate() != null) {
            LocalDate today = LocalDate.now();
            LocalDate maxAge = today.minusYears(30); // Reasonable max age for pets
            
            if (pet.getBirthDate().isAfter(today)) {
                throw new ValidationException("Birth date cannot be in the future");
            }
            
            if (pet.getBirthDate().isBefore(maxAge)) {
                throw new ValidationException("Birth date cannot be more than 30 years ago");
            }
        }
        
        // Validate name length and format
        if (pet.getName().length() > 50) {
            throw new ValidationException("Pet name cannot exceed 50 characters");
        }
        
        // Validate species
        if (pet.getSpecies().length() > 30) {
            throw new ValidationException("Species cannot exceed 30 characters");
        }
        
        // Validate breed if provided
        if (pet.getBreed() != null && pet.getBreed().length() > 50) {
            throw new ValidationException("Breed cannot exceed 50 characters");
        }
        
        // Validate medical history if provided
        if (pet.getMedicalHistory() != null && pet.getMedicalHistory().length() > 1000) {
            throw new ValidationException("Medical history cannot exceed 1000 characters");
        }
    }
    
    private void validatePetForUpdate(Pet pet, Pet existingPet) {
        if (pet == null) {
            throw new ValidationException("Pet cannot be null");
        }
        
        // Use existing owner if not provided
        if (pet.getOwner() == null) {
            pet.setOwner(existingPet.getOwner());
        }
        
        // Validate the updated pet
        validatePetForCreation(pet);
    }
    
    private void updatePetFields(Pet existingPet, Pet updatedPet) {
        if (StringUtils.hasText(updatedPet.getName())) {
            existingPet.setName(updatedPet.getName());
        }
        
        if (StringUtils.hasText(updatedPet.getSpecies())) {
            existingPet.setSpecies(updatedPet.getSpecies());
        }
        
        if (updatedPet.getBreed() != null) {
            existingPet.setBreed(updatedPet.getBreed());
        }
        
        if (updatedPet.getBirthDate() != null) {
            existingPet.setBirthDate(updatedPet.getBirthDate());
        }
        
        if (updatedPet.getMedicalHistory() != null) {
            existingPet.setMedicalHistory(updatedPet.getMedicalHistory());
        }
        
        if (updatedPet.getOwner() != null && 
            !updatedPet.getOwner().getId().equals(existingPet.getOwner().getId())) {
            // Verify new owner exists
            Owner newOwner = ownerRepository.findById(updatedPet.getOwner().getId())
                .orElseThrow(() -> new EntityNotFoundException("Owner", updatedPet.getOwner().getId()));
            existingPet.setOwner(newOwner);
        }
    }
    
    // ========================================
    // Owner-related methods for Pet Form
    // ========================================
    
    @Override
    @Cacheable(value = CacheConfig.SEARCH_RESULTS_CACHE, key = "'owners_' + #searchTerm + '_' + #page + '_' + #size")
    public Page<Owner> searchOwners(String searchTerm, int page, int size) {
        logger.debug("Searching owners with term: '{}', page: {}, size: {}", searchTerm, page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastName", "firstName"));
        
        if (!StringUtils.hasText(searchTerm)) {
            // Return all owners if no search term
            return ownerRepository.findAll(pageable);
        }
        
        // Search by name, email, or phone
        String searchPattern = "%" + searchTerm.toLowerCase() + "%";
        Page<Owner> owners = ownerRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            searchTerm, searchTerm, searchTerm, pageable);
        
        logger.debug("Found {} owners matching search term '{}'", owners.getTotalElements(), searchTerm);
        return owners;
    }
    
    @Override
    public boolean ownerExists(Long ownerId) {
        if (ownerId == null) {
            return false;
        }
        
        boolean exists = ownerRepository.existsById(ownerId);
        logger.debug("Owner with ID {} exists: {}", ownerId, exists);
        return exists;
    }
    
    @Override
    @Cacheable(value = CacheConfig.PETS_CACHE, key = "'owner_' + #ownerId")
    public Owner getOwnerById(Long ownerId) {
        logger.debug("Getting owner by ID: {}", ownerId);
        
        if (ownerId == null) {
            throw new ValidationException("Owner ID cannot be null");
        }
        
        Owner owner = ownerRepository.findById(ownerId)
            .orElseThrow(() -> new EntityNotFoundException("Owner", ownerId));
        
        logger.debug("Found owner: {}", owner.getFullName());
        return owner;
    }
}