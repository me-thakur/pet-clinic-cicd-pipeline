package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.DashboardMetrics;
import com.petclinic.backend.dto.ReportFilter;
import com.petclinic.backend.dto.RevenueReport;
import com.petclinic.backend.dto.VisitStatisticsReport;
import com.petclinic.backend.exception.ValidationException;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.model.VisitType;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.repository.VeterinarianRepository;
import com.petclinic.backend.repository.VisitRepository;
import com.petclinic.backend.service.ReportService;
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
 * Implementation of ReportService providing comprehensive reporting and analytics functionality
 * Validates: Requirements 5.1, 5.2, 5.4, 5.5
 */
@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportServiceImpl.class);
    
    @Autowired
    private VisitRepository visitRepository;
    
    @Autowired
    private VeterinarianRepository veterinarianRepository;
    
    @Autowired
    private PetRepository petRepository;
    
    @Override
    public VisitStatisticsReport generateVisitStatistics(LocalDate startDate, LocalDate endDate) {
        logger.debug("Generating visit statistics for date range: {} to {}", startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        
        return buildVisitStatisticsReport(visits, startDate, endDate);
    }
    
    @Override
    public VisitStatisticsReport generateVisitStatistics(ReportFilter filter) {
        logger.debug("Generating visit statistics with filter: {}", filter);
        
        if (!validateReportFilter(filter)) {
            throw new ValidationException("Invalid report filter: " + filter.getValidationError());
        }
        
        List<Visit> visits = applyFilterToVisits(filter);
        
        return buildVisitStatisticsReport(visits, filter.getStartDate(), filter.getEndDate());
    }
    
    @Override
    public RevenueReport generateRevenueReport(LocalDate startDate, LocalDate endDate) {
        return generateRevenueReport(startDate, endDate, false);
    }
    
    @Override
    public RevenueReport generateRevenueReport(ReportFilter filter) {
        logger.debug("Generating revenue report with filter: {}", filter);
        
        if (!validateReportFilter(filter)) {
            throw new ValidationException("Invalid report filter: " + filter.getValidationError());
        }
        
        List<Visit> visits = applyFilterToVisits(filter);
        
        return buildRevenueReport(visits, filter.getStartDate(), filter.getEndDate(), false);
    }
    
    @Override
    public RevenueReport generateRevenueReport(LocalDate startDate, LocalDate endDate, boolean includeTrends) {
        logger.debug("Generating revenue report for date range: {} to {}, includeTrends: {}", 
                    startDate, endDate, includeTrends);
        
        validateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        
        return buildRevenueReport(visits, startDate, endDate, includeTrends);
    }
    
    @Override
    public DashboardMetrics getDashboardMetrics() {
        return getDashboardMetrics(LocalDate.now());
    }
    
    @Override
    public DashboardMetrics getDashboardMetrics(LocalDate date) {
        logger.debug("Generating dashboard metrics for date: {}", date);
        
        DashboardMetrics metrics = new DashboardMetrics();
        
        // Today's metrics
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        List<Visit> todayVisits = visitRepository.findByVisitDateBetween(startOfDay, endOfDay);
        
        metrics.setTodayAppointments(todayVisits.size());
        metrics.setTodayCompletedVisits(todayVisits.stream()
                .mapToLong(v -> v.isCompleted() ? 1 : 0).sum());
        metrics.setTodayPendingVisits(metrics.getTodayAppointments() - metrics.getTodayCompletedVisits());
        
        BigDecimal todayRevenue = todayVisits.stream()
                .filter(v -> v.getCost() != null)
                .map(Visit::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        metrics.setTodayRevenue(todayRevenue);
        
        // Overall metrics
        metrics.setTotalActivePets(petRepository.count());
        metrics.setTotalVeterinarians(veterinarianRepository.count());
        
        // Monthly metrics
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
        
        // Calculate utilization and completion rates
        calculateUtilizationMetrics(metrics, date);
        
        // Add recent activities and upcoming appointments
        addRecentActivities(metrics, date);
        addUpcomingAppointments(metrics, date);
        
        // Add alerts
        addAlerts(metrics, date);
        
        return metrics;
    }
    
    @Override
    public List<VisitStatisticsReport> generateVisitStatisticsByVeterinarian(LocalDate startDate, LocalDate endDate) {
        logger.debug("Generating visit statistics by veterinarian for date range: {} to {}", startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        
        Map<Veterinarian, List<Visit>> visitsByVet = visits.stream()
                .filter(v -> v.getVeterinarian() != null)
                .collect(Collectors.groupingBy(Visit::getVeterinarian));
        
        return visitsByVet.entrySet().stream()
                .map(entry -> {
                    VisitStatisticsReport report = buildVisitStatisticsReport(entry.getValue(), startDate, endDate);
                    // Add veterinarian-specific data
                    Map<String, Long> vetMap = new HashMap<>();
                    vetMap.put(entry.getKey().getFullName(), (long) entry.getValue().size());
                    report.setVisitsByVeterinarian(vetMap);
                    return report;
                })
                .sorted((r1, r2) -> Long.compare(r2.getTotalVisits(), r1.getTotalVisits()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<VisitStatisticsReport> generateVisitStatisticsByType(LocalDate startDate, LocalDate endDate) {
        logger.debug("Generating visit statistics by type for date range: {} to {}", startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        
        Map<VisitType, List<Visit>> visitsByType = visits.stream()
                .filter(v -> v.getVisitType() != null)
                .collect(Collectors.groupingBy(Visit::getVisitType));
        
        return visitsByType.entrySet().stream()
                .map(entry -> {
                    VisitStatisticsReport report = buildVisitStatisticsReport(entry.getValue(), startDate, endDate);
                    // Add visit type-specific data
                    Map<VisitType, Long> typeMap = new HashMap<>();
                    typeMap.put(entry.getKey(), (long) entry.getValue().size());
                    report.setVisitsByType(typeMap);
                    return report;
                })
                .sorted((r1, r2) -> Long.compare(r2.getTotalVisits(), r1.getTotalVisits()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<VisitStatisticsReport> generateVisitStatisticsBySpecies(LocalDate startDate, LocalDate endDate) {
        logger.debug("Generating visit statistics by species for date range: {} to {}", startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        
        Map<String, List<Visit>> visitsBySpecies = visits.stream()
                .filter(v -> v.getPet() != null && v.getPet().getSpecies() != null)
                .collect(Collectors.groupingBy(v -> v.getPet().getSpecies()));
        
        return visitsBySpecies.entrySet().stream()
                .map(entry -> {
                    VisitStatisticsReport report = buildVisitStatisticsReport(entry.getValue(), startDate, endDate);
                    // Add species-specific data
                    Map<String, Long> speciesMap = new HashMap<>();
                    speciesMap.put(entry.getKey(), (long) entry.getValue().size());
                    report.setVisitsBySpecies(speciesMap);
                    return report;
                })
                .sorted((r1, r2) -> Long.compare(r2.getTotalVisits(), r1.getTotalVisits()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<RevenueReport> generateRevenueReportByVeterinarian(LocalDate startDate, LocalDate endDate) {
        logger.debug("Generating revenue report by veterinarian for date range: {} to {}", startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        
        Map<Veterinarian, List<Visit>> visitsByVet = visits.stream()
                .filter(v -> v.getVeterinarian() != null)
                .collect(Collectors.groupingBy(Visit::getVeterinarian));
        
        return visitsByVet.entrySet().stream()
                .map(entry -> {
                    RevenueReport report = buildRevenueReport(entry.getValue(), startDate, endDate, false);
                    // Add veterinarian-specific data
                    Map<String, BigDecimal> vetMap = new HashMap<>();
                    vetMap.put(entry.getKey().getFullName(), report.getTotalRevenue());
                    report.setRevenueByVeterinarian(vetMap);
                    return report;
                })
                .sorted((r1, r2) -> r2.getTotalRevenue().compareTo(r1.getTotalRevenue()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<RevenueReport> generateRevenueReportBySpecies(LocalDate startDate, LocalDate endDate) {
        logger.debug("Generating revenue report by species for date range: {} to {}", startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        
        Map<String, List<Visit>> visitsBySpecies = visits.stream()
                .filter(v -> v.getPet() != null && v.getPet().getSpecies() != null)
                .collect(Collectors.groupingBy(v -> v.getPet().getSpecies()));
        
        return visitsBySpecies.entrySet().stream()
                .map(entry -> {
                    RevenueReport report = buildRevenueReport(entry.getValue(), startDate, endDate, false);
                    // Add species-specific data
                    Map<String, BigDecimal> speciesMap = new HashMap<>();
                    speciesMap.put(entry.getKey(), report.getTotalRevenue());
                    report.setRevenueBySpecies(speciesMap);
                    return report;
                })
                .sorted((r1, r2) -> r2.getTotalRevenue().compareTo(r1.getTotalRevenue()))
                .collect(Collectors.toList());
    }
    
    @Override
    public RevenueReport getMonthlyRevenueTrends(int months) {
        logger.debug("Getting monthly revenue trends for {} months", months);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months);
        
        return generateRevenueReport(startDate, endDate, true);
    }
    
    @Override
    public DashboardMetrics getVeterinarianUtilization(LocalDate startDate, LocalDate endDate) {
        logger.debug("Getting veterinarian utilization for date range: {} to {}", startDate, endDate);
        
        DashboardMetrics metrics = new DashboardMetrics();
        calculateUtilizationMetrics(metrics, startDate, endDate);
        
        return metrics;
    }
    
    @Override
    public DashboardMetrics getAppointmentCompletionRates(LocalDate startDate, LocalDate endDate) {
        logger.debug("Getting appointment completion rates for date range: {} to {}", startDate, endDate);
        
        validateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        
        DashboardMetrics metrics = new DashboardMetrics();
        
        long totalVisits = visits.size();
        long completedVisits = visits.stream().mapToLong(v -> v.isCompleted() ? 1 : 0).sum();
        
        if (totalVisits > 0) {
            metrics.setAppointmentCompletionRate((double) completedVisits / totalVisits * 100.0);
        }
        
        return metrics;
    }
    
    @Override
    public DashboardMetrics generatePerformanceReport(LocalDate startDate, LocalDate endDate) {
        logger.debug("Generating performance report for date range: {} to {}", startDate, endDate);
        
        DashboardMetrics metrics = getDashboardMetrics(endDate);
        
        // Add period-specific metrics
        calculateUtilizationMetrics(metrics, startDate, endDate);
        
        // Add performance indicators
        Map<String, Object> indicators = new HashMap<>();
        
        VisitStatisticsReport visitStats = generateVisitStatistics(startDate, endDate);
        indicators.put("totalVisits", visitStats.getTotalVisits());
        indicators.put("completionRate", visitStats.getCompletionRate());
        indicators.put("averageVisitsPerDay", visitStats.getAverageVisitsPerDay());
        
        RevenueReport revenueReport = generateRevenueReport(startDate, endDate, true);
        indicators.put("totalRevenue", revenueReport.getTotalRevenue());
        indicators.put("averageRevenuePerVisit", revenueReport.getAverageRevenuePerVisit());
        indicators.put("paymentRate", revenueReport.getPaymentRate());
        
        metrics.setPerformanceIndicators(indicators);
        
        return metrics;
    }
    
    @Override
    public List<VisitStatisticsReport> getTopVeterinariansByVisits(LocalDate startDate, LocalDate endDate, int limit) {
        return generateVisitStatisticsByVeterinarian(startDate, endDate).stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<RevenueReport> getTopVeterinariansByRevenue(LocalDate startDate, LocalDate endDate, int limit) {
        return generateRevenueReportByVeterinarian(startDate, endDate).stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<VisitStatisticsReport> getMostCommonVisitTypes(LocalDate startDate, LocalDate endDate, int limit) {
        return generateVisitStatisticsByType(startDate, endDate).stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<RevenueReport> getMostProfitableVisitTypes(LocalDate startDate, LocalDate endDate, int limit) {
        logger.debug("Getting most profitable visit types for date range: {} to {}, limit: {}", 
                    startDate, endDate, limit);
        
        validateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        
        Map<VisitType, List<Visit>> visitsByType = visits.stream()
                .filter(v -> v.getVisitType() != null)
                .collect(Collectors.groupingBy(Visit::getVisitType));
        
        return visitsByType.entrySet().stream()
                .map(entry -> {
                    RevenueReport report = buildRevenueReport(entry.getValue(), startDate, endDate, false);
                    // Add visit type-specific data
                    Map<String, BigDecimal> typeMap = new HashMap<>();
                    typeMap.put(entry.getKey().getDisplayName(), report.getTotalRevenue());
                    report.setRevenueByVisitType(typeMap);
                    return report;
                })
                .sorted((r1, r2) -> r2.getTotalRevenue().compareTo(r1.getTotalRevenue()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean validateReportFilter(ReportFilter filter) {
        if (filter == null) {
            return false;
        }
        
        return filter.isValid();
    }
    
    @Override
    public ReportFilter getAvailableFilterOptions() {
        ReportFilter filter = new ReportFilter();
        
        // Set available visit types
        filter.setVisitTypes(Arrays.asList(VisitType.values()));
        
        // Set available species (get from existing pets)
        List<String> species = petRepository.findAll().stream()
                .map(Pet::getSpecies)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        filter.setSpecies(species);
        
        // Set available veterinarians
        List<Veterinarian> veterinarians = veterinarianRepository.findAll();
        filter.setAvailableVeterinarians(veterinarians);
        
        return filter;
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
    
    private List<Visit> applyFilterToVisits(ReportFilter filter) {
        LocalDateTime startDateTime = filter.getStartDate() != null ? 
                filter.getStartDate().atStartOfDay() : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime endDateTime = filter.getEndDate() != null ? 
                filter.getEndDate().atTime(LocalTime.MAX) : LocalDateTime.now().plusYears(1);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        
        // Apply additional filters
        if (filter.hasVeterinarianFilter()) {
            visits = visits.stream()
                    .filter(v -> v.getVeterinarian() != null && 
                               (filter.getVeterinarianId() == null || 
                                v.getVeterinarian().getId().equals(filter.getVeterinarianId())))
                    .collect(Collectors.toList());
        }
        
        if (filter.hasSpeciesFilter()) {
            visits = visits.stream()
                    .filter(v -> v.getPet() != null && 
                               filter.getSpecies().contains(v.getPet().getSpecies()))
                    .collect(Collectors.toList());
        }
        
        if (filter.hasVisitTypeFilter()) {
            visits = visits.stream()
                    .filter(v -> filter.getVisitTypes().contains(v.getVisitType()))
                    .collect(Collectors.toList());
        }
        
        if (filter.hasCompletionFilter()) {
            visits = visits.stream()
                    .filter(v -> filter.getCompletedOnly() == null || 
                               v.isCompleted() == filter.getCompletedOnly())
                    .collect(Collectors.toList());
        }
        
        if (filter.hasPetFilter()) {
            visits = visits.stream()
                    .filter(v -> v.getPet() != null && 
                               filter.getPetIds().contains(v.getPet().getId()))
                    .collect(Collectors.toList());
        }
        
        return visits;
    }
    
    private VisitStatisticsReport buildVisitStatisticsReport(List<Visit> visits, LocalDate startDate, LocalDate endDate) {
        VisitStatisticsReport report = new VisitStatisticsReport(startDate, endDate);
        
        long totalVisits = visits.size();
        long completedVisits = visits.stream().mapToLong(v -> v.isCompleted() ? 1 : 0).sum();
        long cancelledVisits = visits.stream()
                .mapToLong(v -> v.getNotes() != null && v.getNotes().contains("CANCELLED") ? 1 : 0).sum();
        long scheduledVisits = totalVisits - completedVisits - cancelledVisits;
        
        report.setTotalVisits(totalVisits);
        report.setCompletedVisits(completedVisits);
        report.setScheduledVisits(scheduledVisits);
        report.setCancelledVisits(cancelledVisits);
        
        // Group by veterinarian
        Map<String, Long> visitsByVet = visits.stream()
                .filter(v -> v.getVeterinarian() != null)
                .collect(Collectors.groupingBy(
                        v -> v.getVeterinarian().getFullName(),
                        Collectors.counting()));
        report.setVisitsByVeterinarian(visitsByVet);
        
        // Group by visit type
        Map<VisitType, Long> visitsByType = visits.stream()
                .filter(v -> v.getVisitType() != null)
                .collect(Collectors.groupingBy(Visit::getVisitType, Collectors.counting()));
        report.setVisitsByType(visitsByType);
        
        // Group by species
        Map<String, Long> visitsBySpecies = visits.stream()
                .filter(v -> v.getPet() != null && v.getPet().getSpecies() != null)
                .collect(Collectors.groupingBy(
                        v -> v.getPet().getSpecies(),
                        Collectors.counting()));
        report.setVisitsBySpecies(visitsBySpecies);
        
        // Group by date
        Map<LocalDate, Long> visitsByDate = visits.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getVisitDate().toLocalDate(),
                        Collectors.counting()));
        report.setVisitsByDate(visitsByDate);
        
        return report;
    }
    
    private RevenueReport buildRevenueReport(List<Visit> visits, LocalDate startDate, LocalDate endDate, boolean includeTrends) {
        RevenueReport report = new RevenueReport(startDate, endDate);
        
        List<Visit> paidVisits = visits.stream()
                .filter(v -> v.getCost() != null && v.getCost().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        
        List<Visit> unpaidVisits = visits.stream()
                .filter(v -> v.getCost() == null || v.getCost().compareTo(BigDecimal.ZERO) == 0)
                .collect(Collectors.toList());
        
        BigDecimal totalRevenue = paidVisits.stream()
                .map(Visit::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        report.setTotalRevenue(totalRevenue);
        report.setTotalPaidVisits(paidVisits.size());
        report.setTotalUnpaidVisits(unpaidVisits.size());
        
        // Group revenue by date
        Map<LocalDate, BigDecimal> dailyRevenue = paidVisits.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getVisitDate().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, Visit::getCost, BigDecimal::add)));
        report.setDailyRevenue(dailyRevenue);
        
        // Group revenue by veterinarian
        Map<String, BigDecimal> revenueByVet = paidVisits.stream()
                .filter(v -> v.getVeterinarian() != null)
                .collect(Collectors.groupingBy(
                        v -> v.getVeterinarian().getFullName(),
                        Collectors.reducing(BigDecimal.ZERO, Visit::getCost, BigDecimal::add)));
        report.setRevenueByVeterinarian(revenueByVet);
        
        // Group revenue by species
        Map<String, BigDecimal> revenueBySpecies = paidVisits.stream()
                .filter(v -> v.getPet() != null && v.getPet().getSpecies() != null)
                .collect(Collectors.groupingBy(
                        v -> v.getPet().getSpecies(),
                        Collectors.reducing(BigDecimal.ZERO, Visit::getCost, BigDecimal::add)));
        report.setRevenueBySpecies(revenueBySpecies);
        
        // Group revenue by visit type
        Map<String, BigDecimal> revenueByType = paidVisits.stream()
                .filter(v -> v.getVisitType() != null)
                .collect(Collectors.groupingBy(
                        v -> v.getVisitType().getDisplayName(),
                        Collectors.reducing(BigDecimal.ZERO, Visit::getCost, BigDecimal::add)));
        report.setRevenueByVisitType(revenueByType);
        
        // Add trend data if requested
        if (includeTrends && startDate != null && endDate != null) {
            addTrendData(report, startDate, endDate);
        }
        
        return report;
    }
    
    private void addTrendData(RevenueReport report, LocalDate startDate, LocalDate endDate) {
        // Calculate previous period revenue for trend analysis
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        LocalDate previousStart = startDate.minusDays(daysBetween + 1);
        LocalDate previousEnd = startDate.minusDays(1);
        
        LocalDateTime previousStartDateTime = previousStart.atStartOfDay();
        LocalDateTime previousEndDateTime = previousEnd.atTime(LocalTime.MAX);
        
        List<Visit> previousVisits = visitRepository.findByVisitDateBetween(previousStartDateTime, previousEndDateTime);
        
        BigDecimal previousRevenue = previousVisits.stream()
                .filter(v -> v.getCost() != null)
                .map(Visit::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        RevenueReport.TrendData trendData = new RevenueReport.TrendData(previousRevenue, report.getTotalRevenue());
        report.setTrendData(trendData);
    }
    
    private void calculateUtilizationMetrics(DashboardMetrics metrics, LocalDate date) {
        calculateUtilizationMetrics(metrics, date, date);
    }
    
    private void calculateUtilizationMetrics(DashboardMetrics metrics, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        List<Veterinarian> vets = veterinarianRepository.findAll();
        
        if (!vets.isEmpty() && !visits.isEmpty()) {
            // Calculate average utilization
            double totalUtilization = 0.0;
            int vetCount = 0;
            
            for (Veterinarian vet : vets) {
                List<Visit> vetVisits = visits.stream()
                        .filter(v -> v.getVeterinarian() != null && v.getVeterinarian().getId().equals(vet.getId()))
                        .collect(Collectors.toList());
                
                if (!vetVisits.isEmpty()) {
                    // Simple utilization calculation: total visit hours / available hours
                    int totalMinutes = vetVisits.stream()
                            .mapToInt(v -> v.getDuration() != null ? v.getDuration() : 30)
                            .sum();
                    
                    // Assume 8 hours per day available
                    long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
                    int availableMinutes = (int) (daysBetween * 8 * 60);
                    
                    double utilization = Math.min(100.0, (double) totalMinutes / availableMinutes * 100.0);
                    totalUtilization += utilization;
                    vetCount++;
                }
            }
            
            if (vetCount > 0) {
                metrics.setVeterinarianUtilization(totalUtilization / vetCount);
            }
        }
        
        // Calculate completion rate
        long totalVisits = visits.size();
        long completedVisits = visits.stream().mapToLong(v -> v.isCompleted() ? 1 : 0).sum();
        
        if (totalVisits > 0) {
            metrics.setAppointmentCompletionRate((double) completedVisits / totalVisits * 100.0);
        }
        
        // Calculate average visit duration
        OptionalDouble avgDuration = visits.stream()
                .filter(v -> v.getDuration() != null)
                .mapToInt(Visit::getDuration)
                .average();
        
        if (avgDuration.isPresent()) {
            metrics.setAverageVisitDuration(avgDuration.getAsDouble());
        }
    }
    
    private void addRecentActivities(DashboardMetrics metrics, LocalDate date) {
        // Get recent visits (last 7 days)
        LocalDateTime weekAgo = date.minusDays(7).atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        List<Visit> recentVisits = visitRepository.findByVisitDateBetween(weekAgo, endOfDay);
        
        List<DashboardMetrics.RecentActivity> activities = recentVisits.stream()
                .sorted((v1, v2) -> v2.getVisitDate().compareTo(v1.getVisitDate()))
                .limit(10)
                .map(visit -> {
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
                })
                .collect(Collectors.toList());
        
        metrics.setRecentActivities(activities);
    }
    
    private void addUpcomingAppointments(DashboardMetrics metrics, LocalDate date) {
        // Get upcoming appointments (next 7 days)
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime weekLater = date.plusDays(7).atTime(LocalTime.MAX);
        
        List<Visit> upcomingVisits = visitRepository.findByVisitDateBetween(startOfDay, weekLater);
        
        List<DashboardMetrics.UpcomingAppointment> appointments = upcomingVisits.stream()
                .filter(v -> !v.isCompleted())
                .sorted(Comparator.comparing(Visit::getVisitDate))
                .limit(10)
                .map(visit -> {
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
                })
                .collect(Collectors.toList());
        
        metrics.setUpcomingAppointments(appointments);
    }
    
    private void addAlerts(DashboardMetrics metrics, LocalDate date) {
        List<DashboardMetrics.Alert> alerts = new ArrayList<>();
        
        // Check for overdue visits
        LocalDateTime now = LocalDateTime.now();
        List<Visit> overdueVisits = visitRepository.findByVisitDateBetween(
                date.minusDays(1).atStartOfDay(), now)
                .stream()
                .filter(v -> !v.isCompleted() && v.getVisitDate().isBefore(now))
                .collect(Collectors.toList());
        
        if (!overdueVisits.isEmpty()) {
            DashboardMetrics.Alert alert = new DashboardMetrics.Alert();
            alert.setType("WARNING");
            alert.setTitle("Overdue Visits");
            alert.setMessage(String.format("%d visits are overdue and need attention", overdueVisits.size()));
            alert.setSeverity("MEDIUM");
            alerts.add(alert);
        }
        
        // Check for high utilization
        if (metrics.getVeterinarianUtilization() > 90.0) {
            DashboardMetrics.Alert alert = new DashboardMetrics.Alert();
            alert.setType("WARNING");
            alert.setTitle("High Veterinarian Utilization");
            alert.setMessage(String.format("Veterinarian utilization is %.1f%%, consider scheduling adjustments", 
                    metrics.getVeterinarianUtilization()));
            alert.setSeverity("HIGH");
            alerts.add(alert);
        }
        
        // Check for low completion rate
        if (metrics.getAppointmentCompletionRate() < 70.0) {
            DashboardMetrics.Alert alert = new DashboardMetrics.Alert();
            alert.setType("ERROR");
            alert.setTitle("Low Completion Rate");
            alert.setMessage(String.format("Appointment completion rate is %.1f%%, below target of 80%%", 
                    metrics.getAppointmentCompletionRate()));
            alert.setSeverity("HIGH");
            alerts.add(alert);
        }
        
        metrics.setAlerts(alerts);
    }
}