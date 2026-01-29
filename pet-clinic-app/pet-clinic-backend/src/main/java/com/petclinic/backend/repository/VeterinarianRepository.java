package com.petclinic.backend.repository;

import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.model.Specialty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Veterinarian entity
 * Provides CRUD operations and custom queries for veterinarian management
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4
 */
@Repository
public interface VeterinarianRepository extends BaseRepository<Veterinarian, Long> {
    
    /**
     * Find veterinarian by license number (unique identifier)
     * Supports professional validation - Requirement 8.3
     */
    Optional<Veterinarian> findByLicenseNumber(String licenseNumber);
    
    /**
     * Find veterinarians by first name (case-insensitive)
     * Supports search functionality - Requirement 8.4
     */
    List<Veterinarian> findByFirstNameContainingIgnoreCase(String firstName);
    
    /**
     * Find veterinarians by last name (case-insensitive)
     * Supports search functionality - Requirement 8.4
     */
    List<Veterinarian> findByLastNameContainingIgnoreCase(String lastName);
    
    /**
     * Find veterinarians by full name (case-insensitive)
     * Supports comprehensive name search - Requirement 8.4
     */
    @Query("SELECT v FROM Veterinarian v WHERE " +
           "LOWER(CONCAT(v.firstName, ' ', v.lastName)) LIKE LOWER(CONCAT('%', :fullName, '%'))")
    List<Veterinarian> findByFullNameContaining(@Param("fullName") String fullName);
    
    /**
     * Find veterinarians by specialty (case-insensitive)
     * Supports specialty-based search - Requirement 8.4
     */
    List<Veterinarian> findBySpecialtiesContainingIgnoreCase(String specialty);
    
    /**
     * Find veterinarians with specific specialty
     * Supports specialized care assignment - Requirement 8.4
     */
    @Query("SELECT v FROM Veterinarian v WHERE LOWER(v.specialties) LIKE LOWER(CONCAT('%', :specialty, '%'))")
    List<Veterinarian> findBySpecialty(@Param("specialty") String specialty);
    
    /**
     * Find veterinarians without specialties (general practitioners)
     * Supports staff categorization
     */
    @Query("SELECT v FROM Veterinarian v WHERE v.specialties IS NULL OR v.specialties = ''")
    List<Veterinarian> findGeneralPractitioners();
    
    /**
     * Find veterinarians with specialties (specialists)
     * Supports specialized care management
     */
    @Query("SELECT v FROM Veterinarian v WHERE v.specialties IS NOT NULL AND v.specialties != ''")
    List<Veterinarian> findSpecialists();
    
    /**
     * Find veterinarians with visits in date range
     * Supports workload analysis - Requirement 8.1
     */
    @Query("SELECT DISTINCT v FROM Veterinarian v JOIN v.visits visit WHERE visit.visitDate BETWEEN :startDate AND :endDate")
    List<Veterinarian> findVeterinariansWithVisitsBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * Find veterinarians without visits
     * Supports workload balancing
     */
    @Query("SELECT v FROM Veterinarian v WHERE v.visits IS EMPTY")
    List<Veterinarian> findVeterinariansWithoutVisits();
    
    /**
     * Find veterinarians created after a specific date
     * Supports new staff tracking
     */
    List<Veterinarian> findByCreatedAtAfter(LocalDate date);
    
    /**
     * Count veterinarians by specialty
     * Supports staff analytics
     */
    @Query("SELECT v.specialties, COUNT(v) FROM Veterinarian v WHERE v.specialties IS NOT NULL AND v.specialties != '' GROUP BY v.specialties")
    List<Object[]> countVeterinariansBySpecialty();
    
    /**
     * Find veterinarians with most visits
     * Supports performance analytics
     */
    @Query("SELECT v FROM Veterinarian v ORDER BY SIZE(v.visits) DESC")
    List<Veterinarian> findVeterinariansByVisitCount(Pageable pageable);
    
    /**
     * Search veterinarians by multiple criteria
     * Comprehensive search functionality - Requirement 8.4
     */
    @Query("SELECT v FROM Veterinarian v WHERE " +
           "(:firstName IS NULL OR LOWER(v.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))) AND " +
           "(:lastName IS NULL OR LOWER(v.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) AND " +
           "(:specialty IS NULL OR LOWER(v.specialties) LIKE LOWER(CONCAT('%', :specialty, '%'))) AND " +
           "(:licenseNumber IS NULL OR v.licenseNumber = :licenseNumber)")
    Page<Veterinarian> searchVeterinarians(@Param("firstName") String firstName,
                                          @Param("lastName") String lastName,
                                          @Param("specialty") String specialty,
                                          @Param("licenseNumber") String licenseNumber,
                                          Pageable pageable);
    
    /**
     * Check if license number exists (for uniqueness validation)
     * Supports duplicate prevention - Requirement 8.3
     */
    boolean existsByLicenseNumber(String licenseNumber);
    
    /**
     * Find veterinarians available for specific specialty
     * Supports appointment scheduling - Requirement 8.1
     */
    @Query("SELECT v FROM Veterinarian v WHERE " +
           "v.specialties IS NULL OR v.specialties = '' OR " +
           "LOWER(v.specialties) LIKE LOWER(CONCAT('%', :specialty, '%'))")
    List<Veterinarian> findAvailableForSpecialty(@Param("specialty") String specialty);
    
    /**
     * Find veterinarians with recent visits
     * Supports active staff identification
     */
    @Query("SELECT DISTINCT v FROM Veterinarian v JOIN v.visits visit WHERE visit.visitDate >= :sinceDate")
    List<Veterinarian> findVeterinariansWithRecentVisits(@Param("sinceDate") LocalDate sinceDate);
    
    /**
     * Get veterinarian statistics
     * Supports dashboard and reporting
     */
    @Query("SELECT " +
           "COUNT(v) as totalVeterinarians, " +
           "COUNT(CASE WHEN v.specialties IS NOT NULL AND v.specialties != '' THEN 1 END) as specialists, " +
           "COUNT(CASE WHEN v.specialties IS NULL OR v.specialties = '' THEN 1 END) as generalPractitioners, " +
           "AVG(SIZE(v.visits)) as avgVisitsPerVet " +
           "FROM Veterinarian v")
    Object[] getVeterinarianStatistics();
    
    /**
     * Find veterinarians by license number pattern
     * Supports license validation and search
     */
    List<Veterinarian> findByLicenseNumberContaining(String licensePattern);
    
    /**
     * Find veterinarians by license number pattern (case-insensitive)
     * Supports license validation and search
     */
    List<Veterinarian> findByLicenseNumberContainingIgnoreCase(String licensePattern);
    
    /**
     * Find veterinarians treating specific pet species
     * Supports species-based staff assignment
     */
    @Query("SELECT DISTINCT v FROM Veterinarian v JOIN v.visits visit WHERE LOWER(visit.pet.species) = LOWER(:species)")
    List<Veterinarian> findVeterinariansTreatingSpecies(@Param("species") String species);
    
    /**
     * Find most experienced veterinarians (by visit count)
     * Supports expertise-based assignment
     */
    @Query("SELECT v, SIZE(v.visits) as visitCount FROM Veterinarian v ORDER BY SIZE(v.visits) DESC")
    List<Object[]> findMostExperiencedVeterinarians(Pageable pageable);
    
    /**
     * Find veterinarians with specific minimum visit count
     * Supports experience-based filtering
     */
    @Query("SELECT v FROM Veterinarian v WHERE SIZE(v.visits) >= :minVisits")
    List<Veterinarian> findVeterinariansWithMinimumVisits(@Param("minVisits") int minVisits);
    
    /**
     * Find veterinarians with multiple specialties
     * Supports multi-specialty staff identification
     */
    @Query("SELECT v FROM Veterinarian v WHERE v.specialties LIKE '%,%'")
    List<Veterinarian> findVeterinariansWithMultipleSpecialties();
    
    /**
     * Find available veterinarians (with fewer than specified visits)
     * Supports workload balancing
     */
    @Query("SELECT v FROM Veterinarian v WHERE SIZE(v.visits) < :maxVisits")
    List<Veterinarian> findAvailableVeterinarians(@Param("maxVisits") int maxVisits);
    
    /**
     * Find most common specialties
     * Supports specialty analytics
     */
    @Query("SELECT v.specialties, COUNT(v) FROM Veterinarian v WHERE v.specialties IS NOT NULL AND v.specialties != '' GROUP BY v.specialties ORDER BY COUNT(v) DESC")
    List<Object[]> findMostCommonSpecialties(Pageable pageable);
    
    /**
     * Find veterinarians by specialty enum
     * Supports enhanced specialty-based search
     */
    @Query("SELECT v FROM Veterinarian v JOIN v.specialtySet s WHERE s = :specialty")
    List<Veterinarian> findBySpecialtyEnum(@Param("specialty") Specialty specialty);
    
    /**
     * Find veterinarians with multiple specialties (enum-based)
     * Supports multi-specialty staff identification
     */
    @Query("SELECT v FROM Veterinarian v WHERE SIZE(v.specialtySet) > 1")
    List<Veterinarian> findVeterinariansWithMultipleSpecialtyEnums();
    
    /**
     * Find veterinarians available for emergency cases
     * Supports emergency care assignment
     */
    @Query("SELECT v FROM Veterinarian v JOIN v.specialtySet s WHERE s IN :emergencySpecialties")
    List<Veterinarian> findEmergencyVeterinarians(@Param("emergencySpecialties") List<Specialty> emergencySpecialties);
    
    /**
     * Find veterinarians who can perform surgery
     * Supports surgical care assignment
     */
    @Query("SELECT v FROM Veterinarian v JOIN v.specialtySet s WHERE s IN :surgicalSpecialties")
    List<Veterinarian> findSurgicalVeterinarians(@Param("surgicalSpecialties") List<Specialty> surgicalSpecialties);
    
    /**
     * Find veterinarians available at specific date and time
     * Supports appointment scheduling with conflict detection
     */
    @Query("SELECT v FROM Veterinarian v WHERE v.id NOT IN " +
           "(SELECT DISTINCT visit.veterinarian.id FROM Visit visit WHERE visit.veterinarian IS NOT NULL AND " +
           "visit.visitDate <= :endTime AND " +
           "visit.visitDate >= :startTime)")
    List<Veterinarian> findAvailableAtDateTime(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * Check if veterinarian is available at specific time
     * Supports scheduling conflict detection
     */
    @Query("SELECT COUNT(v) = 0 FROM Visit v WHERE v.veterinarian.id = :vetId AND " +
           "v.visitDate <= :endTime AND " +
           "v.visitDate >= :startTime")
    boolean isVeterinarianAvailable(@Param("vetId") Long vetId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * Count veterinarians by specialty enum
     * Supports enhanced specialty analytics
     */
    @Query("SELECT s, COUNT(v) FROM Veterinarian v JOIN v.specialtySet s GROUP BY s")
    List<Object[]> countVeterinariansBySpecialtyEnum();
}