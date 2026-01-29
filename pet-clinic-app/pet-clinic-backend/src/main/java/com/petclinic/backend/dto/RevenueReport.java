package com.petclinic.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

/**
 * Data model for revenue reports with totals and trends
 * Validates: Requirements 5.2
 */
public class RevenueReport {
    
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalRevenue;
    private BigDecimal averageRevenuePerVisit;
    private BigDecimal averageRevenuePerDay;
    private long totalPaidVisits;
    private long totalUnpaidVisits;
    private Map<LocalDate, BigDecimal> dailyRevenue;
    private Map<String, BigDecimal> revenueByVeterinarian;
    private Map<String, BigDecimal> revenueBySpecies;
    private Map<String, BigDecimal> revenueByVisitType;
    private TrendData trendData;
    
    // Constructors
    public RevenueReport() {
        this.totalRevenue = BigDecimal.ZERO;
        this.averageRevenuePerVisit = BigDecimal.ZERO;
        this.averageRevenuePerDay = BigDecimal.ZERO;
    }
    
    public RevenueReport(LocalDate startDate, LocalDate endDate) {
        this();
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    public RevenueReport(LocalDate startDate, LocalDate endDate, BigDecimal totalRevenue, 
                        long totalPaidVisits, long totalUnpaidVisits) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
        this.totalPaidVisits = totalPaidVisits;
        this.totalUnpaidVisits = totalUnpaidVisits;
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
    
    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }
    
    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
        calculateDerivedMetrics();
    }
    
    public BigDecimal getAverageRevenuePerVisit() {
        return averageRevenuePerVisit;
    }
    
    public void setAverageRevenuePerVisit(BigDecimal averageRevenuePerVisit) {
        this.averageRevenuePerVisit = averageRevenuePerVisit != null ? averageRevenuePerVisit : BigDecimal.ZERO;
    }
    
    public BigDecimal getAverageRevenuePerDay() {
        return averageRevenuePerDay;
    }
    
    public void setAverageRevenuePerDay(BigDecimal averageRevenuePerDay) {
        this.averageRevenuePerDay = averageRevenuePerDay != null ? averageRevenuePerDay : BigDecimal.ZERO;
    }
    
    public long getTotalPaidVisits() {
        return totalPaidVisits;
    }
    
    public void setTotalPaidVisits(long totalPaidVisits) {
        this.totalPaidVisits = totalPaidVisits;
        calculateDerivedMetrics();
    }
    
    public long getTotalUnpaidVisits() {
        return totalUnpaidVisits;
    }
    
    public void setTotalUnpaidVisits(long totalUnpaidVisits) {
        this.totalUnpaidVisits = totalUnpaidVisits;
    }
    
    public Map<LocalDate, BigDecimal> getDailyRevenue() {
        return dailyRevenue;
    }
    
    public void setDailyRevenue(Map<LocalDate, BigDecimal> dailyRevenue) {
        this.dailyRevenue = dailyRevenue;
    }
    
    public Map<String, BigDecimal> getRevenueByVeterinarian() {
        return revenueByVeterinarian;
    }
    
    public void setRevenueByVeterinarian(Map<String, BigDecimal> revenueByVeterinarian) {
        this.revenueByVeterinarian = revenueByVeterinarian;
    }
    
    public Map<String, BigDecimal> getRevenueBySpecies() {
        return revenueBySpecies;
    }
    
    public void setRevenueBySpecies(Map<String, BigDecimal> revenueBySpecies) {
        this.revenueBySpecies = revenueBySpecies;
    }
    
    public Map<String, BigDecimal> getRevenueByVisitType() {
        return revenueByVisitType;
    }
    
    public void setRevenueByVisitType(Map<String, BigDecimal> revenueByVisitType) {
        this.revenueByVisitType = revenueByVisitType;
    }
    
    public TrendData getTrendData() {
        return trendData;
    }
    
    public void setTrendData(TrendData trendData) {
        this.trendData = trendData;
    }
    
    // Business Methods
    private void calculateDerivedMetrics() {
        // Calculate average revenue per visit
        if (totalPaidVisits > 0 && totalRevenue != null) {
            this.averageRevenuePerVisit = totalRevenue.divide(
                BigDecimal.valueOf(totalPaidVisits), 2, BigDecimal.ROUND_HALF_UP);
        } else {
            this.averageRevenuePerVisit = BigDecimal.ZERO;
        }
        
        // Calculate average revenue per day
        if (startDate != null && endDate != null && totalRevenue != null) {
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
            if (daysBetween > 0) {
                this.averageRevenuePerDay = totalRevenue.divide(
                    BigDecimal.valueOf(daysBetween), 2, BigDecimal.ROUND_HALF_UP);
            }
        }
    }
    
    public long getTotalVisits() {
        return totalPaidVisits + totalUnpaidVisits;
    }
    
    public double getPaymentRate() {
        long totalVisits = getTotalVisits();
        if (totalVisits > 0) {
            return (double) totalPaidVisits / totalVisits * 100.0;
        }
        return 0.0;
    }
    
    public boolean hasRevenue() {
        return totalRevenue != null && totalRevenue.compareTo(BigDecimal.ZERO) > 0;
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
    
    // Inner class for trend data
    public static class TrendData {
        private BigDecimal previousPeriodRevenue;
        private double growthRate;
        private String trendDirection; // "UP", "DOWN", "STABLE"
        private String trendDescription;
        
        public TrendData() {}
        
        public TrendData(BigDecimal previousPeriodRevenue, BigDecimal currentRevenue) {
            this.previousPeriodRevenue = previousPeriodRevenue != null ? previousPeriodRevenue : BigDecimal.ZERO;
            calculateTrend(currentRevenue);
        }
        
        private void calculateTrend(BigDecimal currentRevenue) {
            if (previousPeriodRevenue.compareTo(BigDecimal.ZERO) > 0 && currentRevenue != null) {
                BigDecimal difference = currentRevenue.subtract(previousPeriodRevenue);
                this.growthRate = difference.divide(previousPeriodRevenue, 4, BigDecimal.ROUND_HALF_UP)
                                           .multiply(BigDecimal.valueOf(100)).doubleValue();
                
                if (Math.abs(growthRate) < 1.0) {
                    this.trendDirection = "STABLE";
                    this.trendDescription = "Revenue remained stable";
                } else if (growthRate > 0) {
                    this.trendDirection = "UP";
                    this.trendDescription = String.format("Revenue increased by %.1f%%", growthRate);
                } else {
                    this.trendDirection = "DOWN";
                    this.trendDescription = String.format("Revenue decreased by %.1f%%", Math.abs(growthRate));
                }
            } else {
                this.growthRate = 0.0;
                this.trendDirection = "STABLE";
                this.trendDescription = "No previous period data available";
            }
        }
        
        // Getters and Setters
        public BigDecimal getPreviousPeriodRevenue() {
            return previousPeriodRevenue;
        }
        
        public void setPreviousPeriodRevenue(BigDecimal previousPeriodRevenue) {
            this.previousPeriodRevenue = previousPeriodRevenue;
        }
        
        public double getGrowthRate() {
            return growthRate;
        }
        
        public void setGrowthRate(double growthRate) {
            this.growthRate = growthRate;
        }
        
        public String getTrendDirection() {
            return trendDirection;
        }
        
        public void setTrendDirection(String trendDirection) {
            this.trendDirection = trendDirection;
        }
        
        public String getTrendDescription() {
            return trendDescription;
        }
        
        public void setTrendDescription(String trendDescription) {
            this.trendDescription = trendDescription;
        }
    }
    
    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RevenueReport that = (RevenueReport) o;
        return totalPaidVisits == that.totalPaidVisits &&
               totalUnpaidVisits == that.totalUnpaidVisits &&
               Objects.equals(startDate, that.startDate) &&
               Objects.equals(endDate, that.endDate) &&
               Objects.equals(totalRevenue, that.totalRevenue);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(startDate, endDate, totalRevenue, totalPaidVisits, totalUnpaidVisits);
    }
    
    @Override
    public String toString() {
        return "RevenueReport{" +
                "dateRange='" + getDateRangeDescription() + '\'' +
                ", totalRevenue=" + totalRevenue +
                ", totalPaidVisits=" + totalPaidVisits +
                ", totalUnpaidVisits=" + totalUnpaidVisits +
                ", averageRevenuePerVisit=" + averageRevenuePerVisit +
                ", paymentRate=" + String.format("%.1f%%", getPaymentRate()) +
                '}';
    }
}