package com.petclinic.backend.service;

import com.petclinic.backend.dto.PetWithOwnerInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Enhanced Pet Service interface for operations requiring owner information
 * Extends PetService with methods that include joined owner data
 * Validates: Requirements 5.1, 5.2, 5.3, 5.5
 */
public interface EnhancedPetService extends PetService {
    
    /**
     * Find all pets with their owner information using pagination
     * @param pageable Pagination information
     * @return Page of pets with owner information
     */
    Page<PetWithOwnerInfo> findPetsWithOwnerInfo(Pageable pageable);
    
    /**
     * Find all pets with their owner information
     * @return List of pets with owner information
     */
    List<PetWithOwnerInfo> findAllPetsWithOwnerInfo();
    
    /**
     * Find pets with owner information by owner ID
     * @param ownerId Owner ID
     * @return List of pets with owner information for the specified owner
     */
    List<PetWithOwnerInfo> findPetsWithOwnerInfoByOwner(Long ownerId);
    
    /**
     * Find a specific pet with owner information
     * @param petId Pet ID
     * @return Optional containing pet with owner information if found
     */
    Optional<PetWithOwnerInfo> findPetWithOwnerInfo(Long petId);
    
    /**
     * Search pets with owner information by various criteria
     * @param searchTerm Search term to match against pet name, species, breed, or owner name
     * @param pageable Pagination information
     * @return Page of matching pets with owner information
     */
    Page<PetWithOwnerInfo> searchPetsWithOwnerInfo(String searchTerm, Pageable pageable);
    
    /**
     * Find pets with owner information by species
     * @param species Pet species
     * @param pageable Pagination information
     * @return Page of pets with owner information for the specified species
     */
    Page<PetWithOwnerInfo> findPetsWithOwnerInfoBySpecies(String species, Pageable pageable);
    
    /**
     * Find pets with owner information by breed
     * @param breed Pet breed
     * @param pageable Pagination information
     * @return Page of pets with owner information for the specified breed
     */
    Page<PetWithOwnerInfo> findPetsWithOwnerInfoByBreed(String breed, Pageable pageable);
    
    /**
     * Find pets without owners (orphaned pets)
     * @param pageable Pagination information
     * @return Page of pets without owner information
     */
    Page<PetWithOwnerInfo> findOrphanedPets(Pageable pageable);
    
    /**
     * Find pets with owner information by age range
     * @param minAge Minimum age in years
     * @param maxAge Maximum age in years
     * @param pageable Pagination information
     * @return Page of pets with owner information within the age range
     */
    Page<PetWithOwnerInfo> findPetsWithOwnerInfoByAgeRange(int minAge, int maxAge, Pageable pageable);
    
    /**
     * Advanced search for pets with owner information
     * @param petName Pet name (partial match)
     * @param petSpecies Pet species
     * @param petBreed Pet breed
     * @param ownerFirstName Owner first name (partial match)
     * @param ownerLastName Owner last name (partial match)
     * @param ownerEmail Owner email (partial match)
     * @param pageable Pagination information
     * @return Page of pets with owner information matching the criteria
     */
    Page<PetWithOwnerInfo> advancedSearchPetsWithOwnerInfo(
        String petName, String petSpecies, String petBreed,
        String ownerFirstName, String ownerLastName, String ownerEmail,
        Pageable pageable
    );
    
    /**
     * Refresh pet-owner relationship data for a specific pet
     * Used when owner information is updated and needs to be reflected immediately
     * @param petId Pet ID
     * @return Updated pet with owner information
     */
    Optional<PetWithOwnerInfo> refreshPetOwnerInfo(Long petId);
    
    /**
     * Get count of pets with owners vs without owners
     * @return Array with [petsWithOwners, petsWithoutOwners]
     */
    long[] getPetOwnershipStatistics();
}