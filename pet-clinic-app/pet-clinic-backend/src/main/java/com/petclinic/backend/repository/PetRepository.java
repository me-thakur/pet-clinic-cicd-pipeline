package com.petclinic.backend.repository;

import com.petclinic.backend.model.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Pet entity
 * Provides CRUD operations and custom queries for pet management
 * Validates: Requirements 1.1, 1.4
 */
@Repository
public interface PetRepository extends BaseRepository<Pet, Long> {
    
    /**
     * Find pets by name (case-insensitive)
     * Supports search functionality - Requirement 1.3
     */
    List<Pet> findByNameContainingIgnoreCase(String name);
    
    /**
     * Find pets by species (case-insensitive)
     * Supports filtering functionality - Requirement 1.3
     */
    List<Pet> findBySpeciesIgnoreCase(String species);
    
    /**
     * Find pets by breed (case-insensitive)
     * Supports filtering functionality - Requirement 1.3
     */
    List<Pet> findByBreedContainingIgnoreCase(String breed);
    
    /**
     * Find pets by owner ID
     * Supports owner-pet relationship queries - Requirement 1.4
     */
    List<Pet> findByOwnerId(Long ownerId);
    
    /**
     * Find pets by owner's full name (case-insensitive)
     * Supports search functionality - Requirement 1.3
     */
    @Query("SELECT p FROM Pet p WHERE " +
           "LOWER(CONCAT(p.owner.firstName, ' ', p.owner.lastName)) LIKE LOWER(CONCAT('%', :ownerName, '%'))")
    List<Pet> findByOwnerNameContaining(@Param("ownerName") String ownerName);
    
    /**
     * Find pets born between two dates
     * Supports age-based filtering - Requirement 1.3
     */
    List<Pet> findByBirthDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find pets by species and breed
     * Supports combined filtering - Requirement 1.3
     */
    List<Pet> findBySpeciesIgnoreCaseAndBreedContainingIgnoreCase(String species, String breed);
    
    /**
     * Find pets with medical history containing specific text
     * Supports medical record search - Requirement 1.3
     */
    List<Pet> findByMedicalHistoryContainingIgnoreCase(String medicalHistory);
    
    /**
     * Find pets created after a specific date
     * Supports date-based filtering
     */
    List<Pet> findByCreatedAtAfter(LocalDate date);
    
    /**
     * Count pets by species
     * Supports statistics and reporting
     */
    @Query("SELECT p.species, COUNT(p) FROM Pet p GROUP BY p.species")
    List<Object[]> countPetsBySpecies();
    
    /**
     * Find pets with visits in a date range
     * Supports visit-based queries - Requirement 8.1
     */
    @Query("SELECT DISTINCT p FROM Pet p JOIN p.visits v WHERE v.visitDate BETWEEN :startDate AND :endDate")
    List<Pet> findPetsWithVisitsBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * Find pets without any visits
     * Supports identifying pets needing attention
     */
    @Query("SELECT p FROM Pet p WHERE p.visits IS EMPTY")
    List<Pet> findPetsWithoutVisits();
    
    /**
     * Find young pets (less than 2 years old)
     * Supports age-based filtering
     */
    @Query("SELECT p FROM Pet p WHERE p.birthDate > :cutoffDate")
    List<Pet> findYoungPets(@Param("cutoffDate") LocalDate cutoffDate);
    
    /**
     * Find senior pets (more than 7 years old)
     * Supports age-based filtering
     */
    @Query("SELECT p FROM Pet p WHERE p.birthDate < :cutoffDate")
    List<Pet> findSeniorPets(@Param("cutoffDate") LocalDate cutoffDate);
    
    /**
     * Search pets by multiple criteria
     * Comprehensive search functionality - Requirement 1.3
     */
    @Query("SELECT p FROM Pet p WHERE " +
           "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:species IS NULL OR LOWER(p.species) = LOWER(:species)) AND " +
           "(:breed IS NULL OR LOWER(p.breed) LIKE LOWER(CONCAT('%', :breed, '%'))) AND " +
           "(:ownerId IS NULL OR p.owner.id = :ownerId)")
    Page<Pet> searchPets(@Param("name") String name,
                        @Param("species") String species,
                        @Param("breed") String breed,
                        @Param("ownerId") Long ownerId,
                        Pageable pageable);
    
    /**
     * Find pet by name and owner ID (for uniqueness validation)
     * Supports business rule validation
     */
    Optional<Pet> findByNameIgnoreCaseAndOwnerId(String name, Long ownerId);
    
    /**
     * Check if pet exists by name and owner ID
     * Supports duplicate prevention
     */
    boolean existsByNameIgnoreCaseAndOwnerId(String name, Long ownerId);
    
    /**
     * Find pets by owner email
     * Supports owner-based queries
     */
    @Query("SELECT p FROM Pet p WHERE p.owner.email = :email")
    List<Pet> findByOwnerEmail(@Param("email") String email);
    
    /**
     * Get pet statistics
     * Supports dashboard and reporting
     */
    @Query("SELECT " +
           "COUNT(p) as totalPets, " +
           "COUNT(DISTINCT p.species) as totalSpecies, " +
           "COUNT(DISTINCT p.owner) as totalOwners " +
           "FROM Pet p")
    Object[] getPetStatistics();
}