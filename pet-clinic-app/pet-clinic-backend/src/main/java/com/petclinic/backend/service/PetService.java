package com.petclinic.backend.service;

import com.petclinic.backend.model.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;

/**
 * Service interface for Pet management operations
 * Extends BaseService with Pet-specific functionality
 */
public interface PetService extends BaseService<Pet, Long> {
    
    /**
     * Find all pets with server-side pagination and sorting
     * @param pageable Pagination and sorting parameters
     * @return Page of pets with server-side sorting applied
     */
    Page<Pet> findAllWithPagination(Pageable pageable);
    
    /**
     * Find pets by owner ID
     * @param ownerId Owner ID
     * @return List of pets belonging to the owner
     */
    List<Pet> findByOwner(Long ownerId);
    
    /**
     * Search pets by various criteria
     * @param searchTerm Search term to match against name, species, or breed
     * @return List of matching pets
     */
    List<Pet> searchPets(String searchTerm);
    
    /**
     * Advanced search pets with pagination
     * @param name Pet name
     * @param species Pet species
     * @param breed Pet breed
     * @param ownerId Owner ID
     * @param page Page number
     * @param size Page size
     * @return Page of pets matching criteria
     */
    Page<Pet> searchPetsAdvanced(String name, String species, String breed, Long ownerId, int page, int size);
    
    /**
     * Find pets by species
     * @param species Pet species
     * @return List of pets of the specified species
     */
    List<Pet> findBySpecies(String species);
    
    /**
     * Find pets by breed
     * @param breed Pet breed
     * @return List of pets of the specified breed
     */
    List<Pet> findByBreed(String breed);
    
    /**
     * Check if a pet can be deleted (no associated visits)
     * @param id Pet ID
     * @return true if pet can be deleted, false otherwise
     */
    boolean canDeletePet(Long id);
    
    /**
     * Get pets with upcoming visits
     * @return List of pets with scheduled visits
     */
    List<Pet> findPetsWithUpcomingVisits();
    
    /**
     * Get pets by age range
     * @param minAge Minimum age in years
     * @param maxAge Maximum age in years
     * @return List of pets within the age range
     */
    List<Pet> findByAgeRange(int minAge, int maxAge);
    
    /**
     * Find pets without visits
     * @return List of pets that have no visits
     */
    List<Pet> findPetsWithoutVisits();
    
    /**
     * Find senior pets (older than specified age)
     * @param age Minimum age to be considered senior
     * @return List of senior pets
     */
    List<Pet> findSeniorPets(int age);
    
    /**
     * Find young pets (younger than specified age)
     * @param age Maximum age to be considered young
     * @return List of young pets
     */
    List<Pet> findYoungPets(int age);
    
    /**
     * Find pets by medical history keywords
     * @param keywords Medical history keywords to search for
     * @return List of pets with matching medical history
     */
    List<Pet> findByMedicalHistory(String keywords);
    
    /**
     * Get pet statistics
     * @return Map containing various pet statistics
     */
    Map<String, Object> getPetStatistics();
    
    /**
     * Get pet count by species
     * @return Map of species to count
     */
    Map<String, Long> getPetCountBySpecies();
    
    /**
     * Search owners for pet form dropdown
     * @param searchTerm Search term to match against owner name, email, or phone
     * @param page Page number
     * @param size Page size
     * @return Page of owners matching search criteria
     */
    Page<com.petclinic.backend.model.Owner> searchOwners(String searchTerm, int page, int size);
    
    /**
     * Check if owner exists
     * @param ownerId Owner ID
     * @return true if owner exists, false otherwise
     */
    boolean ownerExists(Long ownerId);
    
    /**
     * Get owner by ID
     * @param ownerId Owner ID
     * @return Owner entity
     */
    com.petclinic.backend.model.Owner getOwnerById(Long ownerId);
}