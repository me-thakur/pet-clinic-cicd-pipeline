package com.petclinic.backend.dto;

import com.petclinic.backend.model.VisitType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

/**
 * Data model for visit statistics reports
 * Validates: Requirements 5.1
 */
public class VisitStatisticsReport {
    
    private LocalDate startDate;
    private LocalDate endDate;
    private long totalVisits;
    private long completedVisits;
    private long scheduledVisits;
    private long cancelledVisits;
    private Map<String, Long> visitsByVeterinarian;
    private Map<VisitType, Long> visitsByType;
    private Map<String, Long> visitsBySpecies;
    private Map<LocalDate, Long> visitsByDate;
    private double averageVisitsPerDay;
    private double completionRate;
    
    // Constructors
    public VisitStatisticsReport() {}
    
    public VisitStatisticsReport(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    public VisitStatisticsReport(LocalDate startDate, LocalDate endDate, long totalVisits, 
                               long completedVisits, long scheduledVisits, long cancelledVisits) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalVisits = totalVisits;
        this.completedVisits = completedVisits;
        this.scheduledVisits = scheduledVisits;
        this.cancelledVisits = cancelledVisits;
        calculateDerivedMetrics();
    }
    
    // Getters and Setters
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public long getTotalVisits() {
        return totalVisits;
    }
    
    public void setTotalVisits(long totalVisits) {
        this.totalVisits = totalVisits;
        calculateDerivedMetrics();
    }
    
    public long getCompletedVisits() {
        return completedVisits;
    }
    
    public void setCompletedVisits(long completedVisits) {
        this.completedVisits = completedVisits;
        calculateDerivedMetrics();
    }
    
    public long getScheduledVisits() {
        return scheduledVisits;
    }
    
    public void setScheduledVisits(long scheduledVisits) {
        this.scheduledVisits = scheduledVisits;
    }
    
    public long getCancelledVisits() {
        return cancelledVisits;
    }
    
    public void setCancelledVisits(long cancelledVisits) {
        this.cancelledVisits = cancelledVisits;
    }
    
    public Map<String, Long> getVisitsByVeterinarian() {
        return visitsByVeterinarian;
    }
    
    public void setVisitsByVeterinarian(Map<String, Long> visitsByVeterinarian) {
        this.visitsByVeterinarian = visitsByVeterinarian;
    }
    
    public Map<VisitType, Long> getVisitsByType() {
        return visitsByType;
    }
    
    public void setVisitsByType(Map<VisitType, Long> visitsByType) {
        this.visitsByType = visitsByType;
    }
    
    public Map<String, Long> getVisitsBySpecies() {
        return visitsBySpecies;
    }
    
    public void setVisitsBySpecies(Map<String, Long> visitsBySpecies) {
        this.visitsBySpecies = visitsBySpecies;
    }
    
    public Map<LocalDate, Long> getVisitsByDate() {
        return visitsByDate;
    }
    
    public void setVisitsByDate(Map<LocalDate, Long> visitsByDate) {
        this.visitsByDate = visitsByDate;
    }
    
    public double getAverageVisitsPerDay() {
        return averageVisitsPerDay;
    }
    
    public void setAverageVisitsPerDay(double averageVisitsPerDay) {
        this.averageVisitsPerDay = averageVisitsPerDay;
    }
    
    public double getCompletionRate() {
        return completionRate;
    }
    
    public void setCompletionRate(double completionRate) {
        this.completionRate = completionRate;
    }
    
    // Business Methods
    private void calculateDerivedMetrics() {
        // Calculate completion rate
        if (totalVisits > 0) {
            this.completionRate = (double) completedVisits / totalVisits * 100.0;
        } else {
            this.completionRate = 0.0;
        }
        
        // Calculate average visits per day
        if (startDate != null && endDate != null) {
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
            if (daysBetween > 0) {
                this.averageVisitsPerDay = (double) totalVisits / daysBetween;
            }
        }
    }
    
    public long getPendingVisits() {
        return totalVisits - completedVisits - cancelledVisits;
    }
    
    public boolean hasData() {
        return totalVisits > 0;
    }
    
    public String getDateRangeDescription() {
        if (startDate != null && endDate != null) {
            if (startDate.equals(endDate)) {
                return startDate.toString();
            } else {
                return startDate + " to " + endDate;
            }
        }
        return "All time";
    }
    
    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VisitStatisticsReport that = (VisitStatisticsReport) o;
        return totalVisits == that.totalVisits &&
               completedVisits == that.completedVisits &&
               scheduledVisits == that.scheduledVisits &&
               cancelledVisits == that.cancelledVisits &&
               Double.compare(that.averageVisitsPerDay, averageVisitsPerDay) == 0 &&
               Double.compare(that.completionRate, completionRate) == 0 &&
               Objects.equals(startDate, that.startDate) &&
               Objects.equals(endDate, that.endDate);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(startDate, endDate, totalVisits, completedVisits, 
                          scheduledVisits, cancelledVisits, averageVisitsPerDay, completionRate);
    }
    
    @Override
    public String toString() {
        return "VisitStatisticsReport{" +
                "dateRange='" + getDateRangeDescription() + '\'' +
                ", totalVisits=" + totalVisits +
                ", completedVisits=" + completedVisits +
                ", scheduledVisits=" + scheduledVisits +
                ", completionRate=" + String.format("%.1f%%", completionRate) +
                ", averageVisitsPerDay=" + String.format("%.1f", averageVisitsPerDay) +
                '}';
    }
}