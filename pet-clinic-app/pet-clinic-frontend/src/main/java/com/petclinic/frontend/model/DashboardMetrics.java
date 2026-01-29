package com.petclinic.frontend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Frontend model for dashboard metrics
 */
public class DashboardMetrics {
    
    private LocalDateTime generatedAt;
    
    // Daily metrics
    private long todayAppointments;
    private long todayCompletedVisits;
    private long todayPendingVisits;
    private BigDecimal todayRevenue;
    
    // Overall metrics
    private long totalActivePets;
    private long totalActiveOwners;
    private long totalVeterinarians;
    private long totalVisitsThisMonth;
    private BigDecimal monthlyRevenue;
    
    // Utilization metrics
    private double veterinarianUtilization;
    private double appointmentCompletionRate;
    private double averageVisitDuration;
    
    // Recent activity
    private List<RecentActivity> recentActivities;
    
    // Upcoming appointments
    private List<UpcomingAppointment> upcomingAppointments;
    
    // Performance indicators
    private Map<String, Object> performanceIndicators;
    
    // Alerts and notifications
    private List<Alert> alerts;
    
    // Constructors
    public DashboardMetrics() {
        this.generatedAt = LocalDateTime.now();
        this.todayRevenue = BigDecimal.ZERO;
        this.monthlyRevenue = BigDecimal.ZERO;
    }
    
    // Getters and Setters
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
    
    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
    
    public long getTodayAppointments() {
        return todayAppointments;
    }
    
    public void setTodayAppointments(long todayAppointments) {
        this.todayAppointments = todayAppointments;
    }
    
    public long getTodayCompletedVisits() {
        return todayCompletedVisits;
    }
    
    public void setTodayCompletedVisits(long todayCompletedVisits) {
        this.todayCompletedVisits = todayCompletedVisits;
    }
    
    public long getTodayPendingVisits() {
        return todayPendingVisits;
    }
    
    public void setTodayPendingVisits(long todayPendingVisits) {
        this.todayPendingVisits = todayPendingVisits;
    }
    
    public BigDecimal getTodayRevenue() {
        return todayRevenue;
    }
    
    public void setTodayRevenue(BigDecimal todayRevenue) {
        this.todayRevenue = todayRevenue != null ? todayRevenue : BigDecimal.ZERO;
    }
    
    public long getTotalActivePets() {
        return totalActivePets;
    }
    
    public void setTotalActivePets(long totalActivePets) {
        this.totalActivePets = totalActivePets;
    }
    
    public long getTotalActiveOwners() {
        return totalActiveOwners;
    }
    
    public void setTotalActiveOwners(long totalActiveOwners) {
        this.totalActiveOwners = totalActiveOwners;
    }
    
    public long getTotalVeterinarians() {
        return totalVeterinarians;
    }
    
    public void setTotalVeterinarians(long totalVeterinarians) {
        this.totalVeterinarians = totalVeterinarians;
    }
    
    public long getTotalVisitsThisMonth() {
        return totalVisitsThisMonth;
    }
    
    public void setTotalVisitsThisMonth(long totalVisitsThisMonth) {
        this.totalVisitsThisMonth = totalVisitsThisMonth;
    }
    
    public BigDecimal getMonthlyRevenue() {
        return monthlyRevenue;
    }
    
    public void setMonthlyRevenue(BigDecimal monthlyRevenue) {
        this.monthlyRevenue = monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO;
    }
    
    public double getVeterinarianUtilization() {
        return veterinarianUtilization;
    }
    
    public void setVeterinarianUtilization(double veterinarianUtilization) {
        this.veterinarianUtilization = veterinarianUtilization;
    }
    
    public double getAppointmentCompletionRate() {
        return appointmentCompletionRate;
    }
    
    public void setAppointmentCompletionRate(double appointmentCompletionRate) {
        this.appointmentCompletionRate = appointmentCompletionRate;
    }
    
    public double getAverageVisitDuration() {
        return averageVisitDuration;
    }
    
    public void setAverageVisitDuration(double averageVisitDuration) {
        this.averageVisitDuration = averageVisitDuration;
    }
    
    public List<RecentActivity> getRecentActivities() {
        return recentActivities;
    }
    
    public void setRecentActivities(List<RecentActivity> recentActivities) {
        this.recentActivities = recentActivities;
    }
    
    public List<UpcomingAppointment> getUpcomingAppointments() {
        return upcomingAppointments;
    }
    
    public void setUpcomingAppointments(List<UpcomingAppointment> upcomingAppointments) {
        this.upcomingAppointments = upcomingAppointments;
    }
    
    public Map<String, Object> getPerformanceIndicators() {
        return performanceIndicators;
    }
    
    public void setPerformanceIndicators(Map<String, Object> performanceIndicators) {
        this.performanceIndicators = performanceIndicators;
    }
    
    public List<Alert> getAlerts() {
        return alerts;
    }
    
    public void setAlerts(List<Alert> alerts) {
        this.alerts = alerts;
    }
    
    // Business Methods
    public double getTodayCompletionRate() {
        if (todayAppointments > 0) {
            return (double) todayCompletedVisits / todayAppointments * 100.0;
        }
        return 0.0;
    }
    
    public boolean hasAlerts() {
        return alerts != null && !alerts.isEmpty();
    }
    
    public int getAlertCount() {
        return alerts != null ? alerts.size() : 0;
    }
    
    public boolean isHealthy() {
        return appointmentCompletionRate >= 80.0 && veterinarianUtilization >= 60.0 && veterinarianUtilization <= 90.0;
    }
    
    // Inner classes for nested data
    public static class RecentActivity {
        private String type;
        private String description;
        private LocalDateTime timestamp;
        private String entityId;
        private String entityType;
        
        public RecentActivity() {}
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public String getEntityId() { return entityId; }
        public void setEntityId(String entityId) { this.entityId = entityId; }
        
        public String getEntityType() { return entityType; }
        public void setEntityType(String entityType) { this.entityType = entityType; }
    }
    
    public static class UpcomingAppointment {
        private Long visitId;
        private String petName;
        private String ownerName;
        private String veterinarianName;
        private LocalDateTime appointmentTime;
        private String visitType;
        private int duration;
        
        public UpcomingAppointment() {}
        
        // Getters and Setters
        public Long getVisitId() { return visitId; }
        public void setVisitId(Long visitId) { this.visitId = visitId; }
        
        public String getPetName() { return petName; }
        public void setPetName(String petName) { this.petName = petName; }
        
        public String getOwnerName() { return ownerName; }
        public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
        
        public String getVeterinarianName() { return veterinarianName; }
        public void setVeterinarianName(String veterinarianName) { this.veterinarianName = veterinarianName; }
        
        public LocalDateTime getAppointmentTime() { return appointmentTime; }
        public void setAppointmentTime(LocalDateTime appointmentTime) { this.appointmentTime = appointmentTime; }
        
        public String getVisitType() { return visitType; }
        public void setVisitType(String visitType) { this.visitType = visitType; }
        
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
    }
    
    public static class Alert {
        private String type;
        private String title;
        private String message;
        private LocalDateTime createdAt;
        private String severity;
        private boolean acknowledged;
        
        public Alert() {
            this.createdAt = LocalDateTime.now();
            this.acknowledged = false;
        }
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        
        public boolean isAcknowledged() { return acknowledged; }
        public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
    }
}