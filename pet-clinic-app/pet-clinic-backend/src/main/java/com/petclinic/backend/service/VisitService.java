package com.petclinic.backend.service;

import com.petclinic.backend.model.Visit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for Visit management and scheduling operations
 * Extends BaseService with Visit-specific functionality
 */
public interface VisitService extends BaseService<Visit, Long> {
    
    /**
     * Schedule a new visit
     * @param visit Visit to schedule
     * @return Scheduled visit
     */
    Visit scheduleVisit(Visit visit);
    
    /**
     * Complete a visit with diagnosis and treatment
     * @param id Visit ID
     * @param diagnosis Diagnosis information
     * @param treatment Treatment provided
     * @param notes Additional notes
     * @return Completed visit
     */
    Visit completeVisit(Long id, String diagnosis, String treatment, String notes);
    
    /**
     * Find visits by pet ID
     * @param petId Pet ID
     * @return List of visits for the specified pet
     */
    List<Visit> findByPet(Long petId);
    
    /**
     * Find visits by veterinarian ID
     * @param vetId Veterinarian ID
     * @return List of visits for the specified veterinarian
     */
    List<Visit> findByVeterinarian(Long vetId);
    
    /**
     * Find visits within a date range
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of visits within the date range
     */
    List<Visit> findByDateRange(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find visits scheduled for a specific date
     * @param date Date to search for
     * @return List of visits on the specified date
     */
    List<Visit> findByDate(LocalDate date);
    
    /**
     * Check for scheduling conflicts
     * @param vetId Veterinarian ID
     * @param dateTime Proposed visit date and time
     * @param duration Expected duration in minutes
     * @return true if there's a conflict, false otherwise
     */
    boolean hasSchedulingConflict(Long vetId, LocalDateTime dateTime, int duration);
    
    /**
     * Get upcoming visits for a veterinarian
     * @param vetId Veterinarian ID
     * @param days Number of days to look ahead
     * @return List of upcoming visits
     */
    List<Visit> getUpcomingVisits(Long vetId, int days);
    
    /**
     * Find completed visits
     * @return List of completed visits
     */
    List<Visit> findCompletedVisits();
    
    /**
     * Find visits by cost range
     * @param minCost Minimum cost
     * @param maxCost Maximum cost
     * @return List of visits within the cost range
     */
    List<Visit> findByCostRange(BigDecimal minCost, BigDecimal maxCost);
    
    /**
     * Get visit statistics for a date range
     * @param startDate Start date
     * @param endDate End date
     * @return Visit statistics
     */
    VisitStatistics getVisitStatistics(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find visits with pagination and sorting support
     * @param pageable Pagination and sorting parameters
     * @return Paginated visits
     */
    Page<Visit> findAllWithPagination(Pageable pageable);
    
    /**
     * Inner class for visit statistics
     */
    class VisitStatistics {
        private long totalVisits;
        private long completedVisits;
        private long scheduledVisits;
        private BigDecimal totalRevenue;
        private double averageCost;
        
        // Constructors, getters, and setters
        public VisitStatistics() {}
        
        public VisitStatistics(long totalVisits, long completedVisits, long scheduledVisits, 
                             BigDecimal totalRevenue, double averageCost) {
            this.totalVisits = totalVisits;
            this.completedVisits = completedVisits;
            this.scheduledVisits = scheduledVisits;
            this.totalRevenue = totalRevenue;
            this.averageCost = averageCost;
        }
        
        // Getters and setters
        public long getTotalVisits() { return totalVisits; }
        public void setTotalVisits(long totalVisits) { this.totalVisits = totalVisits; }
        
        public long getCompletedVisits() { return completedVisits; }
        public void setCompletedVisits(long completedVisits) { this.completedVisits = completedVisits; }
        
        public long getScheduledVisits() { return scheduledVisits; }
        public void setScheduledVisits(long scheduledVisits) { this.scheduledVisits = scheduledVisits; }
        
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
        
        public double getAverageCost() { return averageCost; }
        public void setAverageCost(double averageCost) { this.averageCost = averageCost; }
    }
}