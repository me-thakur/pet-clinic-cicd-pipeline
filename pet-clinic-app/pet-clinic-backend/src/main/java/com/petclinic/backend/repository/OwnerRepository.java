package com.petclinic.backend.repository;

import com.petclinic.backend.model.Owner;
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
 * Repository interface for Owner entity
 * Provides CRUD operations and custom queries for owner management
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4
 */
@Repository
public interface OwnerRepository extends BaseRepository<Owner, Long> {
    
    /**
     * Find owner by email (unique identifier)
     * Supports authentication and user lookup - Requirement 8.3
     */
    Optional<Owner> findByEmail(String email);
    
    /**
     * Find owners by first name (case-insensitive)
     * Supports search functionality - Requirement 8.4
     */
    List<Owner> findByFirstNameContainingIgnoreCase(String firstName);
    
    /**
     * Find owners by last name (case-insensitive)
     * Supports search functionality - Requirement 8.4
     */
    List<Owner> findByLastNameContainingIgnoreCase(String lastName);
    
    /**
     * Find owners by full name (case-insensitive)
     * Supports comprehensive name search - Requirement 8.4
     */
    @Query("SELECT o FROM Owner o WHERE " +
           "LOWER(CONCAT(o.firstName, ' ', o.lastName)) LIKE LOWER(CONCAT('%', :fullName, '%'))")
    List<Owner> findByFullNameContaining(@Param("fullName") String fullName);
    
    /**
     * Find owners by city (case-insensitive)
     * Supports location-based filtering - Requirement 8.4
     */
    List<Owner> findByCityContainingIgnoreCase(String city);
    
    /**
     * Find owners by city (exact match, case-insensitive)
     * Supports location-based filtering - Requirement 8.4
     */
    List<Owner> findByCityIgnoreCase(String city);
    
    /**
     * Find owners by telephone number
     * Supports contact-based search - Requirement 8.4
     */
    List<Owner> findByTelephoneContaining(String telephone);
    
    /**
     * Find owners by address (case-insensitive)
     * Supports address-based search - Requirement 8.4
     */
    List<Owner> findByAddressContainingIgnoreCase(String address);
    
    /**
     * Find owners with pets of specific species
     * Supports pet-based owner queries - Requirement 8.4
     */
    @Query("SELECT DISTINCT o FROM Owner o JOIN o.pets p WHERE LOWER(p.species) = LOWER(:species)")
    List<Owner> findByPetSpecies(@Param("species") String species);
    
    /**
     * Find owners with multiple pets
     * Supports business analytics
     */
    @Query("SELECT o FROM Owner o WHERE SIZE(o.pets) > 1")
    List<Owner> findOwnersWithMultiplePets();
    
    /**
     * Find owners without pets
     * Supports data cleanup and validation
     */
    @Query("SELECT o FROM Owner o WHERE o.pets IS EMPTY")
    List<Owner> findOwnersWithoutPets();
    
    /**
     * Find owners created after a specific date
     * Supports date-based filtering
     */
    List<Owner> findByCreatedAtAfter(LocalDate date);
    
    /**
     * Count owners by city
     * Supports location-based statistics
     */
    @Query("SELECT o.city, COUNT(o) FROM Owner o WHERE o.city IS NOT NULL GROUP BY o.city")
    List<Object[]> countOwnersByCity();
    
    /**
     * Find owners with pets that have recent visits
     * Supports active customer identification
     */
    @Query("SELECT DISTINCT o FROM Owner o JOIN o.pets p JOIN p.visits v WHERE v.visitDate >= :sinceDate")
    List<Owner> findOwnersWithRecentVisits(@Param("sinceDate") LocalDate sinceDate);
    
    /**
     * Search owners by multiple criteria
     * Comprehensive search functionality - Requirement 8.4
     */
    @Query("SELECT o FROM Owner o WHERE " +
           "(:firstName IS NULL OR LOWER(o.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))) AND " +
           "(:lastName IS NULL OR LOWER(o.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) AND " +
           "(:city IS NULL OR LOWER(o.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
           "(:email IS NULL OR LOWER(o.email) LIKE LOWER(CONCAT('%', :email, '%')))")
    Page<Owner> searchOwners(@Param("firstName") String firstName,
                            @Param("lastName") String lastName,
                            @Param("city") String city,
                            @Param("email") String email,
                            Pageable pageable);
    
    /**
     * Check if email exists (for uniqueness validation)
     * Supports duplicate prevention - Requirement 8.3
     */
    boolean existsByEmail(String email);
    
    /**
     * Find owners by telephone (exact match)
     * Supports contact validation
     */
    Optional<Owner> findByTelephone(String telephone);
    
    /**
     * Check if telephone exists (for uniqueness validation)
     * Supports duplicate prevention
     */
    boolean existsByTelephone(String telephone);
    
    /**
     * Find owners with pets needing attention (no recent visits)
     * Supports customer care and follow-up
     */
    @Query("SELECT DISTINCT o FROM Owner o JOIN o.pets p WHERE " +
           "p.id NOT IN (SELECT v.pet.id FROM Visit v WHERE v.visitDate >= :cutoffDate)")
    List<Owner> findOwnersWithPetsNeedingAttention(@Param("cutoffDate") LocalDate cutoffDate);
    
    /**
     * Get owner statistics
     * Supports dashboard and reporting
     */
    @Query("SELECT " +
           "COUNT(o) as totalOwners, " +
           "COUNT(DISTINCT o.city) as totalCities, " +
           "AVG(SIZE(o.pets)) as avgPetsPerOwner " +
           "FROM Owner o")
    Object[] getOwnerStatistics();
    
    /**
     * Find top owners by pet count
     * Supports customer analytics
     */
    @Query("SELECT o FROM Owner o ORDER BY SIZE(o.pets) DESC")
    List<Owner> findTopOwnersByPetCount(Pageable pageable);
    
    /**
     * Find owners by pet name
     * Supports cross-entity search - Requirement 8.4
     */
    @Query("SELECT DISTINCT o FROM Owner o JOIN o.pets p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :petName, '%'))")
    List<Owner> findByPetName(@Param("petName") String petName);
    
    /**
     * Find all owners with pet count (for list views)
     * Optimized query that includes pet count without fetching full pet details
     */
    @Query("SELECT o FROM Owner o LEFT JOIN FETCH o.pets")
    Page<Owner> findAllWithPets(Pageable pageable);
    /**
     * Find owners by email containing text (case-insensitive)
     * Supports email-based search - Requirement 8.4
     */
    List<Owner> findByEmailContainingIgnoreCase(String email);
    
    /**
     * Find owners by multiple text criteria (name, email) with pagination
     * Supports comprehensive search for pet form owner selection
     */
    Page<Owner> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        String firstName, String lastName, String email, Pageable pageable);
}