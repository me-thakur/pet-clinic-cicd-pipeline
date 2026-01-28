package com.petclinic.backend.repository;

import com.petclinic.backend.model.Visit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Visit entity
 * Provides CRUD operations and custom queries for visit management
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4
 */
@Repository
public interface VisitRepository extends JpaRepository<Visit, Long> {
    
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
     * Find visits by description containing text (case-insensitive)
     * Supports search functionality - Requirement 8.4
     */
    List<Visit> findByDescriptionContainingIgnoreCase(String description);
    
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
     * Find emergency visits
     * Supports priority visit identification - Requirement 8.1
     */
    @Query("SELECT v FROM Visit v WHERE LOWER(v.description) LIKE '%emergency%' OR LOWER(v.description) LIKE '%urgent%'")
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
     * Comprehensive search functionality - Requirement 8.4
     */
    @Query("SELECT v FROM Visit v WHERE " +
           "(:petId IS NULL OR v.pet.id = :petId) AND " +
           "(:veterinarianId IS NULL OR v.veterinarian.id = :veterinarianId) AND " +
           "(:startDate IS NULL OR v.visitDate >= :startDate) AND " +
           "(:endDate IS NULL OR v.visitDate <= :endDate) AND " +
           "(:description IS NULL OR LOWER(v.description) LIKE LOWER(CONCAT('%', :description, '%'))) AND " +
           "(:diagnosis IS NULL OR LOWER(v.diagnosis) LIKE LOWER(CONCAT('%', :diagnosis, '%')))")
    Page<Visit> searchVisits(@Param("petId") Long petId,
                            @Param("veterinarianId") Long veterinarianId,
                            @Param("startDate") LocalDateTime startDate,
                            @Param("endDate") LocalDateTime endDate,
                            @Param("description") String description,
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
}