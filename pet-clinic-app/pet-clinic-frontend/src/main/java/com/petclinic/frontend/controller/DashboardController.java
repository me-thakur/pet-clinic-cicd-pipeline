package com.petclinic.frontend.controller;

import com.petclinic.frontend.model.DashboardMetrics;
import com.petclinic.frontend.model.ReportFilter;
import com.petclinic.frontend.service.DashboardService;
import com.petclinic.frontend.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Map;

/**
 * Dashboard Controller
 * 
 * Handles web requests for dashboard and reporting operations.
 * Provides endpoints for viewing dashboard metrics, generating reports, and exporting data.
 * 
 * Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5
 */
@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final ReportService reportService;

    @Autowired
    public DashboardController(DashboardService dashboardService, ReportService reportService) {
        this.dashboardService = dashboardService;
        this.reportService = reportService;
    }

    /**
     * Show main dashboard
     */
    @GetMapping
    public String showDashboard(Model model) {
        try {
            DashboardMetrics metrics = dashboardService.getRealTimeDashboardMetrics().block();
            model.addAttribute("metrics", metrics);
            
            // Get additional data for dashboard
            LocalDate today = LocalDate.now();
            LocalDate weekAgo = today.minusDays(7);
            
            Map<String, Object> weeklyTrends = dashboardService.getPerformanceIndicators(weekAgo, today).block();
            model.addAttribute("weeklyTrends", weeklyTrends);
            
            Map<String, Long> petsBySpecies = dashboardService.getActivePetsBySpecies().block();
            model.addAttribute("petsBySpecies", petsBySpecies);
            
            return "dashboard/index";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading dashboard: " + e.getMessage());
            return "dashboard/index";
        }
    }

    /**
     * Show dashboard for specific date
     */
    @GetMapping("/date/{date}")
    public String showDashboardForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        
        try {
            DashboardMetrics metrics = dashboardService.getDashboardMetricsForDate(date).block();
            model.addAttribute("metrics", metrics);
            model.addAttribute("selectedDate", date);
            
            return "dashboard/index";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading dashboard for date " + date + ": " + e.getMessage());
            return "dashboard/index";
        }
    }

    /**
     * Show reports page
     */
    @GetMapping("/reports")
    public String showReports(Model model) {
        try {
            // Set default date range (last 30 days)
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            
            model.addAttribute("defaultStartDate", startDate);
            model.addAttribute("defaultEndDate", endDate);
            
            // Get available filter options
            ReportFilter filterOptions = reportService.getAvailableFilterOptions().block();
            model.addAttribute("filterOptions", filterOptions);
            
            return "dashboard/reports";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading reports page: " + e.getMessage());
            return "dashboard/reports";
        }
    }

    /**
     * Generate visit statistics report
     */
    @GetMapping("/reports/visits")
    public String generateVisitStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long veterinarianId,
            @RequestParam(required = false) String species,
            Model model) {
        
        try {
            Map<String, Object> report;
            
            if (veterinarianId != null || species != null) {
                // Use filtered report
                ReportFilter filter = new ReportFilter(startDate, endDate);
                filter.setVeterinarianId(veterinarianId);
                if (species != null && !species.trim().isEmpty()) {
                    filter.setSpecies(java.util.List.of(species));
                }
                report = reportService.getVisitStatisticsWithFilter(filter).block();
            } else {
                // Use simple date range report
                report = reportService.getVisitStatistics(startDate, endDate).block();
            }
            
            model.addAttribute("visitReport", report);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("veterinarianId", veterinarianId);
            model.addAttribute("species", species);
            
            return "dashboard/visit-report";
        } catch (Exception e) {
            model.addAttribute("error", "Error generating visit statistics: " + e.getMessage());
            return "dashboard/reports";
        }
    }

    /**
     * Generate revenue report
     */
    @GetMapping("/reports/revenue")
    public String generateRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "true") boolean includeTrends,
            @RequestParam(required = false) Long veterinarianId,
            Model model) {
        
        try {
            Map<String, Object> report;
            
            if (veterinarianId != null) {
                // Use filtered report
                ReportFilter filter = new ReportFilter(startDate, endDate);
                filter.setVeterinarianId(veterinarianId);
                report = reportService.getRevenueReportWithFilter(filter).block();
            } else {
                // Use simple date range report
                report = reportService.getRevenueReport(startDate, endDate, includeTrends).block();
            }
            
            model.addAttribute("revenueReport", report);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("includeTrends", includeTrends);
            model.addAttribute("veterinarianId", veterinarianId);
            
            return "dashboard/revenue-report";
        } catch (Exception e) {
            model.addAttribute("error", "Error generating revenue report: " + e.getMessage());
            return "dashboard/reports";
        }
    }

    /**
     * Show analytics page
     */
    @GetMapping("/analytics")
    public String showAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
        
        try {
            // Set default date range if not provided
            if (startDate == null) {
                startDate = LocalDate.now().minusDays(30);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }
            
            // Get various analytics data
            Map<String, Object> visitsByVet = reportService.getVisitStatisticsByVeterinarian(startDate, endDate).block();
            Map<String, Object> visitsBySpecies = reportService.getVisitStatisticsBySpecies(startDate, endDate).block();
            Map<String, Object> revenueByVet = reportService.getRevenueReportByVeterinarian(startDate, endDate).block();
            Map<String, Object> monthlyTrends = reportService.getMonthlyRevenueTrends(12).block();
            Map<String, Object> utilization = dashboardService.getVeterinarianUtilization(startDate, endDate).block();
            
            model.addAttribute("visitsByVet", visitsByVet);
            model.addAttribute("visitsBySpecies", visitsBySpecies);
            model.addAttribute("revenueByVet", revenueByVet);
            model.addAttribute("monthlyTrends", monthlyTrends);
            model.addAttribute("utilization", utilization);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            
            return "dashboard/analytics";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading analytics: " + e.getMessage());
            return "dashboard/analytics";
        }
    }

    /**
     * Export report
     */
    @GetMapping("/export/{reportType}")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable String reportType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(defaultValue = "false") boolean includeTrends) {
        
        try {
            ResponseEntity<byte[]> response;
            
            switch (reportType.toLowerCase()) {
                case "visits":
                    response = reportService.exportVisitStatistics(startDate, endDate, format).block();
                    break;
                case "revenue":
                    response = reportService.exportRevenueReport(startDate, endDate, format, includeTrends).block();
                    break;
                case "dashboard":
                    response = reportService.exportDashboardMetrics(format, startDate).block();
                    break;
                default:
                    return ResponseEntity.badRequest().build();
            }
            
            return response;
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Refresh dashboard data
     */
    @PostMapping("/refresh")
    public String refreshDashboard(RedirectAttributes redirectAttributes) {
        try {
            dashboardService.refreshDashboardCache().block();
            redirectAttributes.addFlashAttribute("success", "Dashboard data refreshed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error refreshing dashboard: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }

    /**
     * AJAX endpoint for real-time metrics
     */
    @GetMapping("/api/metrics")
    @ResponseBody
    public DashboardMetrics getMetrics() {
        return dashboardService.getRealTimeDashboardMetrics().block();
    }

    /**
     * AJAX endpoint for daily appointment metrics
     */
    @GetMapping("/api/appointments/daily")
    @ResponseBody
    public Map<String, Long> getDailyAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return dashboardService.getDailyAppointmentMetrics(targetDate).block();
    }

    /**
     * AJAX endpoint for recent activities
     */
    @GetMapping("/api/activities")
    @ResponseBody
    public java.util.List<DashboardMetrics.RecentActivity> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit) {
        
        return dashboardService.getRecentActivities(limit).block();
    }

    /**
     * AJAX endpoint for upcoming appointments
     */
    @GetMapping("/api/appointments/upcoming")
    @ResponseBody
    public java.util.List<DashboardMetrics.UpcomingAppointment> getUpcomingAppointments(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int limit) {
        
        return dashboardService.getUpcomingAppointments(days, limit).block();
    }
}