package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.DashboardMetrics;
import com.petclinic.backend.dto.ReportFilter;
import com.petclinic.backend.exception.ValidationException;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.model.VisitType;
import com.petclinic.backend.repository.OwnerRepository;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.repository.VeterinarianRepository;
import com.petclinic.backend.repository.VisitRepository;
import com.petclinic.backend.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of DashboardService providing real-time dashboard metrics and analytics
 * Validates: Requirements 5.4, 5.5
 */
@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardServiceImpl.class);
    
    @Autowired
    private VisitRepository visitRepository;
    
    @Autowired
    private VeterinarianRepository veterinarianRepository;
    
    @Autowired
    private PetRepository petRepository;
    
    @Autowired
    private OwnerRepository ownerRepository;
    
    @Override
    public DashboardMetrics getRealTimeDashboardMetrics() {
        logger.debug("Generating real-time dashboard metrics");
        return getDashboardMetrics(LocalDate.now());
    }
    
    @Override
    public DashboardMetrics getDashboardMetrics(LocalDate date) {
        logger.debug("Generating dashboard metrics for date: {}", date);
        
        DashboardMetrics metrics = new DashboardMetrics();
        
        // Set generation timestamp
        metrics.setGeneratedAt(LocalDateTime.now());
        
        // Daily metrics
        populateDailyMetrics(metrics, date);
        
        // Overall system metrics
        populateOverallMetrics(metrics);
        
        // Monthly metrics
        populateMonthlyMetrics(metrics, date);
        
        // Utilization and performance metrics
        populateUtilizationMetrics(metrics, date);
        
        // Recent activities and upcoming appointments
        populateActivityMetrics(metrics, date);
        
        // System alerts
        populateSystemAlerts(metrics, date);
        
        return metrics;
    }
    
    @Override
    public DashboardMetrics getDashboardMetrics(LocalDate startDate, LocalDate endDate) {
        logger.debug("Generating dashboard metrics for date range: {} to {}", startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        DashboardMetrics metrics = new DashboardMetrics();
        metrics.setGeneratedAt(LocalDateTime.now());
        
        // Use end date for daily metrics
        populateDailyMetrics(metrics, endDate);
        populateOverallMetrics(metrics);
        populateMonthlyMetrics(metrics, endDate);
        
        // Use date range for utilization metrics
        populateUtilizationMetricsForRange(metrics, startDate, endDate);
        populateActivityMetrics(metrics, endDate);
        populateSystemAlerts(metrics, endDate);
        
        return metrics;
    }
    
    @Override
    public DashboardMetrics getDashboardMetrics(ReportFilter filter) {
        logger.debug("Generating dashboard metrics with filter: {}", filter);
        
        if (filter == null || !filter.isValid()) {
            throw new ValidationException("Invalid report filter provided");
        }
        
        LocalDate startDate = filter.getStartDate() != null ? filter.getStartDate() : LocalDate.now().minusMonths(1);
        LocalDate endDate = filter.getEndDate() != null ? filter.getEndDate() : LocalDate.now();
        
        DashboardMetrics metrics = getDashboardMetrics(startDate, endDate);
        
        // Apply additional filtering
        applyFilterToMetrics(metrics, filter);
        
        return metrics;
    }
    
    @Override
    public Map<String, Long> getDailyAppointmentMetrics(LocalDate date) {
        logger.debug("Getting daily appointment metrics for date: {}", date);
        
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        List<Visit> dayVisits = visitRepository.findByVisitDateBetween(startOfDay, endOfDay);
        
        Map<String, Long> metrics = new HashMap<>();
        metrics.put("scheduled", (long) dayVisits.size());
        metrics.put("completed", dayVisits.stream().mapToLong(v -> v.isCompleted() ? 1 : 0).sum());
        metrics.put("pending", dayVisits.stream().mapToLong(v -> !v.isCompleted() && v.getVisitDate().isAfter(LocalDateTime.now()) ? 1 : 0).sum());
        metrics.put("cancelled", dayVisits.stream().mapToLong(v -> v.getNotes() != null && v.getNotes().toLowerCase().contains("cancelled") ? 1 : 0).sum());
        
        return metrics;
    }
    
    @Override
    public long getActivePetsCount() {
        return petRepository.count();
    }
    
    @Override
    public Map<String, Long> getActivePetsBySpecies() {
        logger.debug("Getting active pets count by species");
        
        List<Object[]> results = petRepository.countPetsBySpecies();
        
        return results.stream()
                .collect(Collectors.toMap(
                        result -> (String) result[0],
                        result -> (Long) result[1],
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }
    
    @Override
    public long getActiveOwnersCount() {
        return ownerRepository.count();
    }
    
    @Override
    public Map<String, Object> getVeterinarianUtilization(LocalDate startDate, LocalDate endDate) {
        logger.debug("Getting veterinarian utilization for date range: {} to {}", startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        List<Veterinarian> vets = veterinarianRepository.findAll();
        
        Map<String, Object> utilization = new HashMap<>();
        Map<String, Double> vetUtilization = new HashMap<>();
        
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        
        for (Veterinarian vet : vets) {
            List<Visit> vetVisits = visits.stream()
                    .filter(v -> v.getVeterinarian() != null && v.getVeterinarian().getId().equals(vet.getId()))
                    .collect(Collectors.toList());
            
            if (!vetVisits.isEmpty()) {
                int totalMinutes = vetVisits.stream()
                        .mapToInt(v -> v.getDuration() != null ? v.getDuration() : 30)
                        .sum();
                
                // Assume 8 hours per day available (480 minutes)
                double availableMinutes = daysBetween * 480.0;
                double utilizationPercent = Math.min(100.0, (totalMinutes / availableMinutes) * 100.0);
                
                vetUtilization.put(vet.getFullName(), utilizationPercent);
            } else {
                vetUtilization.put(vet.getFullName(), 0.0);
            }
        }
        
        utilization.put("byVeterinarian", vetUtilization);
        utilization.put("average", vetUtilization.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        utilization.put("maximum", vetUtilization.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
        utilization.put("minimum", vetUtilization.values().stream().mapToDouble(Double::doubleValue).min().orElse(0.0));
        
        return utilization;
    }
    
    @Override
    public double getVeterinarianUtilization(Long veterinarianId, LocalDate startDate, LocalDate endDate) {
        logger.debug("Getting utilization for veterinarian {} for date range: {} to {}", veterinarianId, startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> vetVisits = visitRepository.findByVeterinarianIdAndVisitDateBetween(veterinarianId, startDateTime, endDateTime);
        
        if (vetVisits.isEmpty()) {
            return 0.0;
        }
        
        int totalMinutes = vetVisits.stream()
                .mapToInt(v -> v.getDuration() != null ? v.getDuration() : 30)
                .sum();
        
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        double availableMinutes = daysBetween * 480.0; // 8 hours per day
        
        return Math.min(100.0, (totalMinutes / availableMinutes) * 100.0);
    }
    
    @Override
    public double getAppointmentCompletionRate(LocalDate startDate, LocalDate endDate) {
        logger.debug("Getting appointment completion rate for date range: {} to {}", startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        
        if (visits.isEmpty()) {
            return 0.0;
        }
        
        long completedVisits = visits.stream().mapToLong(v -> v.isCompleted() ? 1 : 0).sum();
        
        return (double) completedVisits / visits.size() * 100.0;
    }
    
    @Override
    public Map<String, Double> getAppointmentCompletionRatesByVeterinarian(LocalDate startDate, LocalDate endDate) {
        logger.debug("Getting appointment completion rates by veterinarian for date range: {} to {}", startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        
        Map<String, List<Visit>> visitsByVet = visits.stream()
                .filter(v -> v.getVeterinarian() != null)
                .collect(Collectors.groupingBy(v -> v.getVeterinarian().getFullName()));
        
        return visitsByVet.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            List<Visit> vetVisits = entry.getValue();
                            if (vetVisits.isEmpty()) return 0.0;
                            
                            long completed = vetVisits.stream().mapToLong(v -> v.isCompleted() ? 1 : 0).sum();
                            return (double) completed / vetVisits.size() * 100.0;
                        }
                ));
    }
    
    @Override
    public List<DashboardMetrics.RecentActivity> getRecentActivities(int limit) {
        logger.debug("Getting recent activities with limit: {}", limit);
        
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<Visit> recentVisits = visitRepository.findByVisitDateBetween(weekAgo, LocalDateTime.now());
        
        return recentVisits.stream()
                .sorted((v1, v2) -> v2.getVisitDate().compareTo(v1.getVisitDate()))
                .limit(limit)
                .map(this::createRecentActivity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<DashboardMetrics.UpcomingAppointment> getUpcomingAppointments(int days, int limit) {
        logger.debug("Getting upcoming appointments for {} days with limit: {}", days, limit);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusDays(days);
        
        List<Visit> upcomingVisits = visitRepository.findByVisitDateBetween(now, futureDate);
        
        return upcomingVisits.stream()
                .filter(v -> !v.isCompleted())
                .sorted(Comparator.comparing(Visit::getVisitDate))
                .limit(limit)
                .map(this::createUpcomingAppointment)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<DashboardMetrics.Alert> getSystemAlerts() {
        logger.debug("Getting system alerts");
        
        List<DashboardMetrics.Alert> alerts = new ArrayList<>();
        
        // Check for overdue visits
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        
        List<Visit> overdueVisits = visitRepository.findByVisitDateBetween(yesterday, now)
                .stream()
                .filter(v -> !v.isCompleted() && v.getVisitDate().isBefore(now))
                .collect(Collectors.toList());
        
        if (!overdueVisits.isEmpty()) {
            alerts.add(new DashboardMetrics.Alert(
                    "WARNING",
                    "Overdue Visits",
                    String.format("%d visits are overdue and need attention", overdueVisits.size()),
                    "MEDIUM"
            ));
        }
        
        // Check for high utilization
        Map<String, Object> utilization = getVeterinarianUtilization(LocalDate.now().minusDays(7), LocalDate.now());
        Double avgUtilization = (Double) utilization.get("average");
        
        if (avgUtilization != null && avgUtilization > 90.0) {
            alerts.add(new DashboardMetrics.Alert(
                    "WARNING",
                    "High Veterinarian Utilization",
                    String.format("Average veterinarian utilization is %.1f%%, consider scheduling adjustments", avgUtilization),
                    "HIGH"
            ));
        }
        
        // Check for low completion rate
        double completionRate = getAppointmentCompletionRate(LocalDate.now().minusDays(7), LocalDate.now());
        
        if (completionRate < 70.0) {
            alerts.add(new DashboardMetrics.Alert(
                    "ERROR",
                    "Low Completion Rate",
                    String.format("Appointment completion rate is %.1f%%, below target of 80%%", completionRate),
                    "HIGH"
            ));
        }
        
        return alerts;
    }
    
    @Override
    public Map<String, Object> getPerformanceIndicators(LocalDate startDate, LocalDate endDate) {
        logger.debug("Getting performance indicators for date range: {} to {}", startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        Map<String, Object> indicators = new HashMap<>();
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        
        // Basic metrics
        indicators.put("totalVisits", visits.size());
        indicators.put("completedVisits", visits.stream().mapToLong(v -> v.isCompleted() ? 1 : 0).sum());
        indicators.put("completionRate", getAppointmentCompletionRate(startDate, endDate));
        
        // Revenue metrics
        BigDecimal totalRevenue = visits.stream()
                .filter(v -> v.getCost() != null)
                .map(Visit::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        indicators.put("totalRevenue", totalRevenue);
        
        if (!visits.isEmpty()) {
            indicators.put("averageRevenuePerVisit", totalRevenue.divide(BigDecimal.valueOf(visits.size()), 2, RoundingMode.HALF_UP));
        } else {
            indicators.put("averageRevenuePerVisit", BigDecimal.ZERO);
        }
        
        // Utilization metrics
        Map<String, Object> utilization = getVeterinarianUtilization(startDate, endDate);
        indicators.put("averageUtilization", utilization.get("average"));
        indicators.put("maxUtilization", utilization.get("maximum"));
        
        // Time-based metrics
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        indicators.put("averageVisitsPerDay", visits.isEmpty() ? 0.0 : (double) visits.size() / daysBetween);
        
        return indicators;
    }
    
    @Override
    public Map<String, Object> getRevenueMetrics(LocalDate startDate, LocalDate endDate) {
        logger.debug("Getting revenue metrics for date range: {} to {}", startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        
        Map<String, Object> metrics = new HashMap<>();
        
        BigDecimal totalRevenue = visits.stream()
                .filter(v -> v.getCost() != null)
                .map(Visit::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        metrics.put("totalRevenue", totalRevenue);
        
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        metrics.put("dailyAverage", totalRevenue.divide(BigDecimal.valueOf(daysBetween), 2, RoundingMode.HALF_UP));
        
        // Monthly total (if date range spans current month)
        LocalDate now = LocalDate.now();
        if (!startDate.isAfter(now) && !endDate.isBefore(now.withDayOfMonth(1))) {
            LocalDate monthStart = now.withDayOfMonth(1);
            LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());
            
            List<Visit> monthlyVisits = visitRepository.findByVisitDateBetween(
                    monthStart.atStartOfDay(), 
                    monthEnd.atTime(LocalTime.MAX)
            );
            
            BigDecimal monthlyRevenue = monthlyVisits.stream()
                    .filter(v -> v.getCost() != null)
                    .map(Visit::getCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            metrics.put("monthlyTotal", monthlyRevenue);
        }
        
        return metrics;
    }
    
    @Override
    public Map<String, Long> getTopVeterinariansByVisits(LocalDate startDate, LocalDate endDate, int limit) {
        logger.debug("Getting top {} veterinarians by visits for date range: {} to {}", limit, startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        
        return visits.stream()
                .filter(v -> v.getVeterinarian() != null)
                .collect(Collectors.groupingBy(
                        v -> v.getVeterinarian().getFullName(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }
    
    @Override
    public Map<String, Object> getTopVeterinariansByRevenue(LocalDate startDate, LocalDate endDate, int limit) {
        logger.debug("Getting top {} veterinarians by revenue for date range: {} to {}", limit, startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        
        Map<String, BigDecimal> revenueByVet = visits.stream()
                .filter(v -> v.getVeterinarian() != null && v.getCost() != null)
                .collect(Collectors.groupingBy(
                        v -> v.getVeterinarian().getFullName(),
                        Collectors.reducing(BigDecimal.ZERO, Visit::getCost, BigDecimal::add)
                ));
        
        return revenueByVet.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue(),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }
    
    @Override
    public Map<String, Long> getMostCommonVisitTypes(LocalDate startDate, LocalDate endDate, int limit) {
        logger.debug("Getting most common visit types for date range: {} to {}", startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        
        return visits.stream()
                .filter(v -> v.getVisitType() != null)
                .collect(Collectors.groupingBy(
                        v -> v.getVisitType().getDisplayName(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }
    
    @Override
    public Map<String, Long> getVisitStatisticsBySpecies(LocalDate startDate, LocalDate endDate) {
        logger.debug("Getting visit statistics by species for date range: {} to {}", startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        
        return visits.stream()
                .filter(v -> v.getPet() != null && v.getPet().getSpecies() != null)
                .collect(Collectors.groupingBy(
                        v -> v.getPet().getSpecies(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }
    
    @Override
    public DashboardMetrics getDashboardMetricsBySpecies(String species, LocalDate startDate, LocalDate endDate) {
        logger.debug("Getting dashboard metrics filtered by species: {} for date range: {} to {}", species, startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        ReportFilter filter = new ReportFilter();
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);
        filter.setSpecies(Arrays.asList(species));
        
        return getDashboardMetrics(filter);
    }
    
    @Override
    public DashboardMetrics getDashboardMetricsByVeterinarian(Long veterinarianId, LocalDate startDate, LocalDate endDate) {
        logger.debug("Getting dashboard metrics filtered by veterinarian: {} for date range: {} to {}", veterinarianId, startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        ReportFilter filter = new ReportFilter();
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);
        filter.setVeterinarianId(veterinarianId);
        
        return getDashboardMetrics(filter);
    }
    
    @Override
    public Map<String, Object> getMonthlyTrends(int months) {
        logger.debug("Getting monthly trends for {} months", months);
        
        Map<String, Object> trends = new HashMap<>();
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months);
        
        // Get monthly visit counts
        Map<String, Long> monthlyVisits = new LinkedHashMap<>();
        Map<String, BigDecimal> monthlyRevenue = new LinkedHashMap<>();
        
        for (int i = 0; i < months; i++) {
            LocalDate monthStart = endDate.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            
            List<Visit> visits = visitRepository.findByVisitDateBetween(
                    monthStart.atStartOfDay(),
                    monthEnd.atTime(LocalTime.MAX)
            );
            
            String monthKey = monthStart.getYear() + "-" + String.format("%02d", monthStart.getMonthValue());
            monthlyVisits.put(monthKey, (long) visits.size());
            
            BigDecimal revenue = visits.stream()
                    .filter(v -> v.getCost() != null)
                    .map(Visit::getCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            monthlyRevenue.put(monthKey, revenue);
        }
        
        trends.put("monthlyVisits", monthlyVisits);
        trends.put("monthlyRevenue", monthlyRevenue);
        
        return trends;
    }
    
    @Override
    public Map<String, Object> getWorkloadDistribution(LocalDate startDate, LocalDate endDate) {
        logger.debug("Getting workload distribution for date range: {} to {}", startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        Map<String, Object> distribution = new HashMap<>();
        
        Map<String, Long> visitsByVet = getTopVeterinariansByVisits(startDate, endDate, Integer.MAX_VALUE);
        Map<String, Object> revenueByVet = getTopVeterinariansByRevenue(startDate, endDate, Integer.MAX_VALUE);
        
        distribution.put("visitDistribution", visitsByVet);
        distribution.put("revenueDistribution", revenueByVet);
        
        // Calculate distribution statistics
        if (!visitsByVet.isEmpty()) {
            double avgVisits = visitsByVet.values().stream().mapToLong(Long::longValue).average().orElse(0.0);
            long maxVisits = visitsByVet.values().stream().mapToLong(Long::longValue).max().orElse(0L);
            long minVisits = visitsByVet.values().stream().mapToLong(Long::longValue).min().orElse(0L);
            
            distribution.put("averageVisitsPerVet", avgVisits);
            distribution.put("maxVisitsPerVet", maxVisits);
            distribution.put("minVisitsPerVet", minVisits);
            distribution.put("visitDistributionBalance", maxVisits > 0 ? (double) minVisits / maxVisits * 100.0 : 100.0);
        }
        
        return distribution;
    }
    
    @Override
    public Map<String, Object> getCapacityUtilization(LocalDate startDate, LocalDate endDate) {
        logger.debug("Getting capacity utilization for date range: {} to {}", startDate, endDate);
        
        Map<String, Object> capacity = new HashMap<>();
        
        // Get utilization data
        Map<String, Object> utilization = getVeterinarianUtilization(startDate, endDate);
        capacity.putAll(utilization);
        
        // Calculate capacity metrics
        long totalVets = veterinarianRepository.count();
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        
        // Theoretical maximum capacity (8 hours per vet per day, 30 min per visit)
        long theoreticalMaxVisits = totalVets * daysBetween * 16; // 16 visits per day per vet
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        long actualVisits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime).size();
        
        double capacityUtilization = theoreticalMaxVisits > 0 ? (double) actualVisits / theoreticalMaxVisits * 100.0 : 0.0;
        
        capacity.put("theoreticalMaxVisits", theoreticalMaxVisits);
        capacity.put("actualVisits", actualVisits);
        capacity.put("capacityUtilization", capacityUtilization);
        capacity.put("availableCapacity", theoreticalMaxVisits - actualVisits);
        
        return capacity;
    }
    
    @Override
    public boolean isDashboardHealthy() {
        DashboardMetrics metrics = getRealTimeDashboardMetrics();
        
        // Check various health indicators
        boolean completionRateHealthy = metrics.getAppointmentCompletionRate() >= 80.0;
        boolean utilizationHealthy = metrics.getVeterinarianUtilization() >= 60.0 && metrics.getVeterinarianUtilization() <= 90.0;
        boolean noHighSeverityAlerts = metrics.getAlerts() == null || 
                metrics.getAlerts().stream().noneMatch(alert -> "HIGH".equals(alert.getSeverity()) || "CRITICAL".equals(alert.getSeverity()));
        
        return completionRateHealthy && utilizationHealthy && noHighSeverityAlerts;
    }
    
    @Override
    public double getDashboardHealthScore() {
        DashboardMetrics metrics = getRealTimeDashboardMetrics();
        
        double score = 100.0;
        
        // Deduct points for low completion rate
        if (metrics.getAppointmentCompletionRate() < 80.0) {
            score -= (80.0 - metrics.getAppointmentCompletionRate());
        }
        
        // Deduct points for poor utilization
        double utilization = metrics.getVeterinarianUtilization();
        if (utilization < 60.0) {
            score -= (60.0 - utilization) * 0.5;
        } else if (utilization > 90.0) {
            score -= (utilization - 90.0) * 0.5;
        }
        
        // Deduct points for alerts
        if (metrics.getAlerts() != null) {
            for (DashboardMetrics.Alert alert : metrics.getAlerts()) {
                switch (alert.getSeverity()) {
                    case "LOW":
                        score -= 2;
                        break;
                    case "MEDIUM":
                        score -= 5;
                        break;
                    case "HIGH":
                        score -= 10;
                        break;
                    case "CRITICAL":
                        score -= 20;
                        break;
                }
            }
        }
        
        return Math.max(0.0, Math.min(100.0, score));
    }
    
    @Override
    public void refreshDashboardCache() {
        logger.info("Dashboard cache refresh requested - no caching implemented yet");
        // TODO: Implement caching mechanism if needed
    }
    
    @Override
    public Map<String, Object> getDashboardSummary() {
        logger.debug("Getting dashboard summary");
        
        Map<String, Object> summary = new HashMap<>();
        
        // Basic counts
        summary.put("totalPets", getActivePetsCount());
        summary.put("totalOwners", getActiveOwnersCount());
        summary.put("totalVeterinarians", veterinarianRepository.count());
        
        // Today's metrics
        Map<String, Long> todayMetrics = getDailyAppointmentMetrics(LocalDate.now());
        summary.put("todayAppointments", todayMetrics.get("scheduled"));
        summary.put("todayCompleted", todayMetrics.get("completed"));
        
        // Health indicators
        summary.put("healthScore", getDashboardHealthScore());
        summary.put("isHealthy", isDashboardHealthy());
        
        // Alert count
        List<DashboardMetrics.Alert> alerts = getSystemAlerts();
        summary.put("alertCount", alerts.size());
        summary.put("hasHighPriorityAlerts", alerts.stream().anyMatch(a -> "HIGH".equals(a.getSeverity()) || "CRITICAL".equals(a.getSeverity())));
        
        return summary;
    }
    
    // Private helper methods
    
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date are required");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new ValidationException("Start date must be before or equal to end date");
        }
    }
    
    private void populateDailyMetrics(DashboardMetrics metrics, LocalDate date) {
        Map<String, Long> dailyMetrics = getDailyAppointmentMetrics(date);
        
        metrics.setTodayAppointments(dailyMetrics.get("scheduled"));
        metrics.setTodayCompletedVisits(dailyMetrics.get("completed"));
        metrics.setTodayPendingVisits(dailyMetrics.get("pending"));
        
        // Calculate today's revenue
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        List<Visit> todayVisits = visitRepository.findByVisitDateBetween(startOfDay, endOfDay);
        BigDecimal todayRevenue = todayVisits.stream()
                .filter(v -> v.getCost() != null)
                .map(Visit::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        metrics.setTodayRevenue(todayRevenue);
    }
    
    private void populateOverallMetrics(DashboardMetrics metrics) {
        metrics.setTotalActivePets(getActivePetsCount());
        metrics.setTotalActiveOwners(getActiveOwnersCount());
        metrics.setTotalVeterinarians(veterinarianRepository.count());
    }
    
    private void populateMonthlyMetrics(DashboardMetrics metrics, LocalDate date) {
        LocalDate startOfMonth = date.withDayOfMonth(1);
        LocalDate endOfMonth = date.withDayOfMonth(date.lengthOfMonth());
        
        LocalDateTime monthStart = startOfMonth.atStartOfDay();
        LocalDateTime monthEnd = endOfMonth.atTime(LocalTime.MAX);
        
        List<Visit> monthlyVisits = visitRepository.findByVisitDateBetween(monthStart, monthEnd);
        metrics.setTotalVisitsThisMonth(monthlyVisits.size());
        
        BigDecimal monthlyRevenue = monthlyVisits.stream()
                .filter(v -> v.getCost() != null)
                .map(Visit::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        metrics.setMonthlyRevenue(monthlyRevenue);
    }
    
    private void populateUtilizationMetrics(DashboardMetrics metrics, LocalDate date) {
        populateUtilizationMetricsForRange(metrics, date, date);
    }
    
    private void populateUtilizationMetricsForRange(DashboardMetrics metrics, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> utilization = getVeterinarianUtilization(startDate, endDate);
        Double avgUtilization = (Double) utilization.get("average");
        
        if (avgUtilization != null) {
            metrics.setVeterinarianUtilization(avgUtilization);
        }
        
        double completionRate = getAppointmentCompletionRate(startDate, endDate);
        metrics.setAppointmentCompletionRate(completionRate);
        
        // Calculate average visit duration
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        OptionalDouble avgDuration = visits.stream()
                .filter(v -> v.getDuration() != null)
                .mapToInt(Visit::getDuration)
                .average();
        
        if (avgDuration.isPresent()) {
            metrics.setAverageVisitDuration(avgDuration.getAsDouble());
        }
    }
    
    private void populateActivityMetrics(DashboardMetrics metrics, LocalDate date) {
        metrics.setRecentActivities(getRecentActivities(10));
        metrics.setUpcomingAppointments(getUpcomingAppointments(7, 10));
    }
    
    private void populateSystemAlerts(DashboardMetrics metrics, LocalDate date) {
        metrics.setAlerts(getSystemAlerts());
    }
    
    private void applyFilterToMetrics(DashboardMetrics metrics, ReportFilter filter) {
        // Apply additional filtering logic based on the filter criteria
        // This could involve recalculating metrics with filtered data
        
        LocalDateTime startDateTime = filter.getStartDate() != null ? 
                filter.getStartDate().atStartOfDay() : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime endDateTime = filter.getEndDate() != null ? 
                filter.getEndDate().atTime(LocalTime.MAX) : LocalDateTime.now().plusYears(1);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        
        // Apply veterinarian filter
        if (filter.hasVeterinarianFilter()) {
            visits = visits.stream()
                    .filter(v -> v.getVeterinarian() != null && 
                               (filter.getVeterinarianId() == null || 
                                v.getVeterinarian().getId().equals(filter.getVeterinarianId())))
                    .collect(Collectors.toList());
        }
        
        // Apply species filter
        if (filter.hasSpeciesFilter()) {
            visits = visits.stream()
                    .filter(v -> v.getPet() != null && 
                               filter.getSpecies().contains(v.getPet().getSpecies()))
                    .collect(Collectors.toList());
        }
        
        // Apply visit type filter
        if (filter.hasVisitTypeFilter()) {
            visits = visits.stream()
                    .filter(v -> filter.getVisitTypes().contains(v.getVisitType()))
                    .collect(Collectors.toList());
        }
        
        // Recalculate metrics with filtered data
        long totalVisits = visits.size();
        long completedVisits = visits.stream().mapToLong(v -> v.isCompleted() ? 1 : 0).sum();
        
        // Update metrics with filtered data
        Map<String, Object> performanceIndicators = new HashMap<>();
        performanceIndicators.put("filteredTotalVisits", totalVisits);
        performanceIndicators.put("filteredCompletedVisits", completedVisits);
        performanceIndicators.put("filteredCompletionRate", totalVisits > 0 ? (double) completedVisits / totalVisits * 100.0 : 0.0);
        
        BigDecimal filteredRevenue = visits.stream()
                .filter(v -> v.getCost() != null)
                .map(Visit::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        performanceIndicators.put("filteredRevenue", filteredRevenue);
        
        metrics.setPerformanceIndicators(performanceIndicators);
    }
    
    private DashboardMetrics.RecentActivity createRecentActivity(Visit visit) {
        String type = visit.isCompleted() ? "VISIT_COMPLETED" : "VISIT_SCHEDULED";
        String description = String.format("%s visit for %s", 
                visit.getVisitType() != null ? visit.getVisitType().getDisplayName() : "General",
                visit.getPet() != null ? visit.getPet().getName() : "Unknown pet");
        
        DashboardMetrics.RecentActivity activity = new DashboardMetrics.RecentActivity();
        activity.setType(type);
        activity.setDescription(description);
        activity.setTimestamp(visit.getVisitDate());
        activity.setEntityId(visit.getId().toString());
        activity.setEntityType("VISIT");
        
        return activity;
    }
    
    private DashboardMetrics.UpcomingAppointment createUpcomingAppointment(Visit visit) {
        DashboardMetrics.UpcomingAppointment appointment = new DashboardMetrics.UpcomingAppointment();
        appointment.setVisitId(visit.getId());
        appointment.setPetName(visit.getPet() != null ? visit.getPet().getName() : "Unknown");
        appointment.setOwnerName(visit.getPet() != null && visit.getPet().getOwner() != null ? 
                visit.getPet().getOwner().getFirstName() + " " + visit.getPet().getOwner().getLastName() : "Unknown");
        appointment.setVeterinarianName(visit.getVeterinarian() != null ? 
                visit.getVeterinarian().getFullName() : "TBD");
        appointment.setAppointmentTime(visit.getVisitDate());
        appointment.setVisitType(visit.getVisitType() != null ? 
                visit.getVisitType().getDisplayName() : "General");
        appointment.setDuration(visit.getDuration() != null ? visit.getDuration() : 30);
        
        return appointment;
    }
}