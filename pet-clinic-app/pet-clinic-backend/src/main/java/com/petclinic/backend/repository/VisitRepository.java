package com.petclinic.backend.repository;

import com.petclinic.backend.model.Visit;
import com.petclinic.backend.model.VisitType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Visit entity
 * Provides CRUD operations and custom queries for visit management
 * Validates: Requirements 2.1, 2.5
 */
@Repository
public interface VisitRepository extends BaseRepository<Visit, Long> {
    
    // Task-specific required methods
    
    /**
     * Find visits by pet ID
     * @param petId Pet ID to search for
     * @return List of visits for the pet
     */
    List<Visit> findByPetId(Long petId);
    
    /**
     * Find visits by veterinarian ID
     * @param veterinarianId Veterinarian ID to search for
     * @return List of visits for the veterinarian
     */
    List<Visit> findByVeterinarianId(Long veterinarianId);
    
    /**
     * Find visits between two dates
     * @param start Start date (inclusive)
     * @param end End date (inclusive)
     * @return List of visits in the date range
     */
    List<Visit> findByVisitDateBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Find visits by pet ID and date range
     * @param petId Pet ID to search for
     * @param start Start date (inclusive)
     * @param end End date (inclusive)
     * @return List of visits for the pet in the date range
     */
    List<Visit> findByPetIdAndVisitDateBetween(Long petId, LocalDateTime start, LocalDateTime end);
    
    /**
     * Find visits by veterinarian ID and date range
     * @param veterinarianId Veterinarian ID to search for
     * @param start Start date (inclusive)
     * @param end End date (inclusive)
     * @return List of visits for the veterinarian in the date range
     */
    List<Visit> findByVeterinarianIdAndVisitDateBetween(Long veterinarianId, LocalDateTime start, LocalDateTime end);
    
    /**
     * Find upcoming visits from a specific date
     * @param fromDate Date to search from
     * @return List of upcoming visits
     */
    @Query("SELECT v FROM Visit v WHERE v.visitDate >= :fromDate ORDER BY v.visitDate")
    List<Visit> findUpcomingVisits(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Find visits by date range (alias for findByVisitDateBetween)
     * @param start Start date (inclusive)
     * @param end End date (inclusive)
     * @return List of visits in the date range
     */
    @Query("SELECT v FROM Visit v WHERE v.visitDate BETWEEN :start AND :end ORDER BY v.visitDate")
    List<Visit> findVisitsByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * Count visits by veterinarian and date
     * @param veterinarianId Veterinarian ID
     * @param date Date to count visits for
     * @return Number of visits for the veterinarian on the date
     */
    @Query("SELECT COUNT(v) FROM Visit v WHERE v.veterinarian.id = :veterinarianId AND FUNCTION('DATE', v.visitDate) = :date")
    long countVisitsByVeterinarianAndDate(@Param("veterinarianId") Long veterinarianId, @Param("date") LocalDate date);
    
    /**
     * Find conflicting visits for a veterinarian in a time range
     * Simple approach without complex date arithmetic
     * @param veterinarianId Veterinarian ID
     * @param start Start time
     * @param end End time
     * @return List of conflicting visits
     */
    @Query("SELECT v FROM Visit v WHERE v.veterinarian.id = :veterinarianId AND " +
           "v.visitDate BETWEEN :start AND :end")
    List<Visit> findConflictingVisits(@Param("veterinarianId") Long veterinarianId, 
                                     @Param("start") LocalDateTime start, 
                                     @Param("end") LocalDateTime end);
    
    // Existing methods below...
    
    /**
     * Find visits by pet ID
     * Supports pet visit history - Requirement 8.1
     */
    List<Visit> findByPetIdOrderByVisitDateDesc(Long petId);
    
    /**
     * Find visits by veterinarian ID
     * Supports veterinarian schedule and history - Requirement 8.1
     */
    List<Visit> findByVeterinarianIdOrderByVisitDateDesc(Long veterinarianId);
    
    /**
     * Find visits within date range
     * Supports date-based filtering - Requirement 8.4
     */
    List<Visit> findByVisitDateBetweenOrderByVisitDateDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find visits by notes containing text (case-insensitive)
     * Supports search functionality - Requirement 2.1
     */
    List<Visit> findByNotesContainingIgnoreCase(String notes);
    
    /**
     * Find visits by diagnosis containing text (case-insensitive)
     * Supports medical record search - Requirement 8.4
     */
    List<Visit> findByDiagnosisContainingIgnoreCase(String diagnosis);
    
    /**
     * Find visits by treatment containing text (case-insensitive)
     * Supports treatment search - Requirement 8.4
     */
    List<Visit> findByTreatmentContainingIgnoreCase(String treatment);
    
    /**
     * Find visits with cost greater than specified amount
     * Supports cost-based filtering - Requirement 8.4
     */
    List<Visit> findByCostGreaterThan(BigDecimal cost);
    
    /**
     * Find visits with cost between two amounts
     * Supports cost range filtering - Requirement 8.4
     */
    List<Visit> findByCostBetween(BigDecimal minCost, BigDecimal maxCost);
    
    /**
     * Find emergency visits using visit type
     * Supports priority visit identification - Requirement 2.1
     */
    @Query("SELECT v FROM Visit v WHERE v.visitType = :emergencyType")
    List<Visit> findEmergencyVisits();
    
    /**
     * Find completed visits (with diagnosis and treatment)
     * Supports visit status filtering - Requirement 8.1
     */
    @Query("SELECT v FROM Visit v WHERE v.diagnosis IS NOT NULL AND v.diagnosis != '' AND v.treatment IS NOT NULL AND v.treatment != ''")
    List<Visit> findCompletedVisits();
    
    /**
     * Find incomplete visits (missing diagnosis or treatment)
     * Supports workflow management - Requirement 8.1
     */
    @Query("SELECT v FROM Visit v WHERE v.diagnosis IS NULL OR v.diagnosis = '' OR v.treatment IS NULL OR v.treatment = ''")
    List<Visit> findIncompleteVisits();
    
    /**
     * Find visits without cost assigned
     * Supports billing workflow - Requirement 8.1
     */
    @Query("SELECT v FROM Visit v WHERE v.cost IS NULL OR v.cost = 0")
    List<Visit> findVisitsWithoutCost();
    
    /**
     * Find recent visits for a pet
     * Supports recent medical history - Requirement 8.1
     */
    @Query("SELECT v FROM Visit v WHERE v.pet.id = :petId AND v.visitDate >= :sinceDate ORDER BY v.visitDate DESC")
    List<Visit> findRecentVisitsForPet(@Param("petId") Long petId, @Param("sinceDate") LocalDateTime sinceDate);
    
    /**
     * Find visits by owner
     * Supports owner visit history - Requirement 8.1
     */
    @Query("SELECT v FROM Visit v WHERE v.pet.owner.id = :ownerId ORDER BY v.visitDate DESC")
    List<Visit> findByOwnerId(@Param("ownerId") Long ownerId);
    
    /**
     * Count visits by veterinarian
     * Supports workload analytics
     */
    @Query("SELECT v.veterinarian.id, v.veterinarian.firstName, v.veterinarian.lastName, COUNT(v) " +
           "FROM Visit v WHERE v.veterinarian IS NOT NULL GROUP BY v.veterinarian.id, v.veterinarian.firstName, v.veterinarian.lastName")
    List<Object[]> countVisitsByVeterinarian();
    
    /**
     * Count visits by month
     * Supports temporal analytics
     */
    @Query("SELECT FUNCTION('YEAR', v.visitDate), FUNCTION('MONTH', v.visitDate), COUNT(v) " +
           "FROM Visit v GROUP BY FUNCTION('YEAR', v.visitDate), FUNCTION('MONTH', v.visitDate) ORDER BY FUNCTION('YEAR', v.visitDate), FUNCTION('MONTH', v.visitDate)")
    List<Object[]> countVisitsByMonth();
    
    /**
     * Calculate total revenue by date range
     * Supports financial reporting
     */
    @Query("SELECT SUM(v.cost) FROM Visit v WHERE v.visitDate BETWEEN :startDate AND :endDate AND v.cost IS NOT NULL")
    BigDecimal calculateRevenueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find visits by pet species
     * Supports species-based analytics
     */
    @Query("SELECT v FROM Visit v WHERE LOWER(v.pet.species) = LOWER(:species) ORDER BY v.visitDate DESC")
    List<Visit> findByPetSpecies(@Param("species") String species);
    
    /**
     * Search visits by multiple criteria
     * Comprehensive search functionality - Requirement 2.1
     */
    @Query("SELECT v FROM Visit v WHERE " +
           "(:petId IS NULL OR v.pet.id = :petId) AND " +
           "(:veterinarianId IS NULL OR v.veterinarian.id = :veterinarianId) AND " +
           "(:startDate IS NULL OR v.visitDate >= :startDate) AND " +
           "(:endDate IS NULL OR v.visitDate <= :endDate) AND " +
           "(:notes IS NULL OR LOWER(v.notes) LIKE LOWER(CONCAT('%', :notes, '%'))) AND " +
           "(:diagnosis IS NULL OR LOWER(v.diagnosis) LIKE LOWER(CONCAT('%', :diagnosis, '%')))")
    Page<Visit> searchVisits(@Param("petId") Long petId,
                            @Param("veterinarianId") Long veterinarianId,
                            @Param("startDate") LocalDateTime startDate,
                            @Param("endDate") LocalDateTime endDate,
                            @Param("notes") String notes,
                            @Param("diagnosis") String diagnosis,
                            Pageable pageable);
    
    /**
     * Find visits scheduled for today
     * Supports daily schedule management
     */
    @Query("SELECT v FROM Visit v WHERE FUNCTION('DATE', v.visitDate) = FUNCTION('CURDATE') ORDER BY v.visitDate")
    List<Visit> findTodaysVisits();
    
    /**
     * Find visits scheduled for specific date
     * Supports schedule management - Requirement 8.1
     */
    @Query("SELECT v FROM Visit v WHERE FUNCTION('DATE', v.visitDate) = FUNCTION('DATE', :date) ORDER BY v.visitDate")
    List<Visit> findVisitsByDate(@Param("date") LocalDateTime date);
    
    /**
     * Find upcoming visits
     * Supports appointment management - Requirement 8.1
     */
    @Query("SELECT v FROM Visit v WHERE v.visitDate > CURRENT_TIMESTAMP ORDER BY v.visitDate")
    List<Visit> findUpcomingVisits();
    
    /**
     * Get visit statistics
     * Supports dashboard and reporting
     */
    @Query("SELECT " +
           "COUNT(v) as totalVisits, " +
           "COUNT(DISTINCT v.pet) as uniquePets, " +
           "COUNT(DISTINCT v.veterinarian) as uniqueVeterinarians, " +
           "AVG(v.cost) as averageCost, " +
           "SUM(v.cost) as totalRevenue " +
           "FROM Visit v WHERE v.cost IS NOT NULL")
    Object[] getVisitStatistics();
    
    /**
     * Find most common diagnoses
     * Supports medical analytics
     */
    @Query("SELECT v.diagnosis, COUNT(v) FROM Visit v WHERE v.diagnosis IS NOT NULL AND v.diagnosis != '' " +
           "GROUP BY v.diagnosis ORDER BY COUNT(v) DESC")
    List<Object[]> findMostCommonDiagnoses(Pageable pageable);
    
    /**
     * Find visits by visit type
     * Supports visit type filtering
     */
    List<Visit> findByVisitType(VisitType visitType);
    
    /**
     * Find visits by visit type and date range
     * Supports enhanced filtering
     */
    List<Visit> findByVisitTypeAndVisitDateBetween(VisitType visitType, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find emergency visits using visit type
     * Supports priority visit identification
     */
    @Query("SELECT v FROM Visit v WHERE v.visitType = :emergencyType")
    List<Visit> findEmergencyVisitsByType(@Param("emergencyType") VisitType emergencyType);
    
    /**
     * Find preventive care visits
     * Supports preventive care analytics
     */
    @Query("SELECT v FROM Visit v WHERE v.visitType IN :preventiveTypes")
    List<Visit> findPreventiveCareVisits(@Param("preventiveTypes") List<VisitType> preventiveTypes);
    
    /**
     * Find visits requiring specialists
     * Supports specialist assignment
     */
    @Query("SELECT v FROM Visit v WHERE v.visitType IN :specialistTypes")
    List<Visit> findVisitsRequiringSpecialists(@Param("specialistTypes") List<VisitType> specialistTypes);
    
    /**
     * Find visits with duration range
     * Supports scheduling analytics
     */
    List<Visit> findByDurationBetween(Integer minDuration, Integer maxDuration);
    
    /**
     * Count visits by visit type
     * Supports visit type analytics
     */
    @Query("SELECT v.visitType, COUNT(v) FROM Visit v WHERE v.visitType IS NOT NULL GROUP BY v.visitType")
    List<Object[]> countVisitsByType();
    
    /**
     * Find visits with scheduling conflicts
     * Supports conflict detection
     */
    @Query("SELECT v1 FROM Visit v1, Visit v2 WHERE v1.id != v2.id AND " +
           "v1.veterinarian = v2.veterinarian AND " +
           "v1.visitDate <= v2.visitDate AND " +
           "v1.visitDate >= v2.visitDate")
    List<Visit> findConflictingVisits();
    
    /**
     * Find visits requiring anesthesia
     * Supports special care planning
     */
    @Query("SELECT v FROM Visit v WHERE v.visitType IN :anesthesiaTypes")
    List<Visit> findVisitsRequiringAnesthesia(@Param("anesthesiaTypes") List<VisitType> anesthesiaTypes);
    
    /**
     * Calculate average duration by visit type
     * Supports scheduling optimization
     */
    @Query("SELECT v.visitType, AVG(v.duration) FROM Visit v WHERE v.visitType IS NOT NULL AND v.duration IS NOT NULL GROUP BY v.visitType")
    List<Object[]> calculateAverageDurationByType();
}