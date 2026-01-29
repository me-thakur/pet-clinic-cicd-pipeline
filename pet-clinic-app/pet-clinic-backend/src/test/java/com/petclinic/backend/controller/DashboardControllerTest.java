package com.petclinic.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petclinic.backend.dto.DashboardMetrics;
import com.petclinic.backend.dto.ReportFilter;
import com.petclinic.backend.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for DashboardController
 * Tests REST endpoints for dashboard metrics and real-time analytics
 * Validates: Requirements 5.4, 5.5
 */
@WebMvcTest(DashboardController.class)
@WithMockUser
class DashboardControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private DashboardService dashboardService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private DashboardMetrics testMetrics;
    private LocalDate testDate;
    
    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2024, 1, 15);
        
        testMetrics = new DashboardMetrics();
        testMetrics.setGeneratedAt(LocalDateTime.now());
        testMetrics.setTodayAppointments(5L);
        testMetrics.setTodayCompletedVisits(3L);
        testMetrics.setTodayPendingVisits(2L);
        testMetrics.setTodayRevenue(BigDecimal.valueOf(500.00));
        testMetrics.setTotalActivePets(25L);
        testMetrics.setTotalActiveOwners(20L);
        testMetrics.setTotalVeterinarians(4L);
        testMetrics.setTotalVisitsThisMonth(45L);
        testMetrics.setMonthlyRevenue(BigDecimal.valueOf(4500.00));
        testMetrics.setVeterinarianUtilization(75.5);
        testMetrics.setAppointmentCompletionRate(85.2);
        testMetrics.setAverageVisitDuration(35.0);
        
        // Add some test activities and appointments
        List<DashboardMetrics.RecentActivity> activities = Arrays.asList(
                new DashboardMetrics.RecentActivity("VISIT_COMPLETED", "Checkup for Buddy", LocalDateTime.now().minusHours(2))
        );
        testMetrics.setRecentActivities(activities);
        
        List<DashboardMetrics.UpcomingAppointment> appointments = Arrays.asList(
                new DashboardMetrics.UpcomingAppointment(1L, "Max", "John Doe", "Dr. Smith", LocalDateTime.now().plusHours(2), "Wellness Examination", 30)
        );
        testMetrics.setUpcomingAppointments(appointments);
        
        List<DashboardMetrics.Alert> alerts = Arrays.asList(
                new DashboardMetrics.Alert("INFO", "System Update", "Scheduled maintenance tonight", "LOW")
        );
        testMetrics.setAlerts(alerts);
    }
    
    @Test
    void shouldGetRealTimeDashboardMetrics() throws Exception {
        // Given
        when(dashboardService.getRealTimeDashboardMetrics()).thenReturn(testMetrics);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.todayAppointments").value(5))
                .andExpect(jsonPath("$.todayCompletedVisits").value(3))
                .andExpect(jsonPath("$.todayRevenue").value(500.00))
                .andExpect(jsonPath("$.totalActivePets").value(25))
                .andExpect(jsonPath("$.totalActiveOwners").value(20))
                .andExpect(jsonPath("$.totalVeterinarians").value(4))
                .andExpect(jsonPath("$.veterinarianUtilization").value(75.5))
                .andExpect(jsonPath("$.appointmentCompletionRate").value(85.2));
    }
    
    @Test
    void shouldGetDashboardMetricsForSpecificDate() throws Exception {
        // Given
        when(dashboardService.getDashboardMetrics(testDate)).thenReturn(testMetrics);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/metrics/{date}", testDate))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.todayAppointments").value(5))
                .andExpect(jsonPath("$.totalActivePets").value(25));
    }
    
    @Test
    void shouldGetDashboardMetricsForDateRange() throws Exception {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        
        when(dashboardService.getDashboardMetrics(startDate, endDate)).thenReturn(testMetrics);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/metrics/range")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.todayAppointments").value(5))
                .andExpect(jsonPath("$.veterinarianUtilization").value(75.5));
    }
    
    @Test
    void shouldGetDashboardMetricsWithFilter() throws Exception {
        // Given
        ReportFilter filter = new ReportFilter();
        filter.setStartDate(testDate.minusDays(7));
        filter.setEndDate(testDate);
        filter.setSpecies(Arrays.asList("Dog"));
        
        when(dashboardService.getDashboardMetrics(any(ReportFilter.class))).thenReturn(testMetrics);
        
        // When & Then
        mockMvc.perform(post("/api/dashboard/metrics/filtered")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.todayAppointments").value(5));
    }
    
    @Test
    void shouldGetDailyAppointmentMetrics() throws Exception {
        // Given
        Map<String, Long> dailyMetrics = Map.of(
                "scheduled", 8L,
                "completed", 5L,
                "pending", 2L,
                "cancelled", 1L
        );
        
        when(dashboardService.getDailyAppointmentMetrics(testDate)).thenReturn(dailyMetrics);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/appointments/daily")
                        .param("date", testDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.scheduled").value(8))
                .andExpect(jsonPath("$.completed").value(5))
                .andExpect(jsonPath("$.pending").value(2))
                .andExpect(jsonPath("$.cancelled").value(1));
    }
    
    @Test
    void shouldGetDailyAppointmentMetricsForToday() throws Exception {
        // Given
        Map<String, Long> dailyMetrics = Map.of(
                "scheduled", 6L,
                "completed", 4L,
                "pending", 2L,
                "cancelled", 0L
        );
        
        when(dashboardService.getDailyAppointmentMetrics(any(LocalDate.class))).thenReturn(dailyMetrics);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/appointments/daily"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.scheduled").value(6))
                .andExpect(jsonPath("$.completed").value(4));
    }
    
    @Test
    void shouldGetActivePetsCount() throws Exception {
        // Given
        when(dashboardService.getActivePetsCount()).thenReturn(42L);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/pets/count"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("42"));
    }
    
    @Test
    void shouldGetActivePetsBySpecies() throws Exception {
        // Given
        Map<String, Long> petsBySpecies = Map.of(
                "Dog", 15L,
                "Cat", 12L,
                "Bird", 3L
        );
        
        when(dashboardService.getActivePetsBySpecies()).thenReturn(petsBySpecies);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/pets/by-species"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.Dog").value(15))
                .andExpect(jsonPath("$.Cat").value(12))
                .andExpect(jsonPath("$.Bird").value(3));
    }
    
    @Test
    void shouldGetActiveOwnersCount() throws Exception {
        // Given
        when(dashboardService.getActiveOwnersCount()).thenReturn(35L);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/owners/count"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("35"));
    }
    
    @Test
    void shouldGetVeterinarianUtilization() throws Exception {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        
        Map<String, Object> utilization = Map.of(
                "average", 78.5,
                "maximum", 95.0,
                "minimum", 62.0,
                "byVeterinarian", Map.of(
                        "Dr. Smith", 85.0,
                        "Dr. Johnson", 72.0
                )
        );
        
        when(dashboardService.getVeterinarianUtilization(startDate, endDate)).thenReturn(utilization);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/utilization")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.average").value(78.5))
                .andExpect(jsonPath("$.maximum").value(95.0))
                .andExpect(jsonPath("$.minimum").value(62.0));
    }
    
    @Test
    void shouldGetVeterinarianUtilizationForSpecificVet() throws Exception {
        // Given
        Long veterinarianId = 1L;
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        
        when(dashboardService.getVeterinarianUtilization(veterinarianId, startDate, endDate)).thenReturn(82.5);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/utilization/{veterinarianId}", veterinarianId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("82.5"));
    }
    
    @Test
    void shouldGetAppointmentCompletionRate() throws Exception {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        
        when(dashboardService.getAppointmentCompletionRate(startDate, endDate)).thenReturn(87.3);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/completion-rate")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("87.3"));
    }
    
    @Test
    void shouldGetAppointmentCompletionRatesByVeterinarian() throws Exception {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        
        Map<String, Double> completionRates = Map.of(
                "Dr. Smith", 92.5,
                "Dr. Johnson", 88.0,
                "Dr. Brown", 85.5
        );
        
        when(dashboardService.getAppointmentCompletionRatesByVeterinarian(startDate, endDate)).thenReturn(completionRates);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/completion-rate/by-veterinarian")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$['Dr. Smith']").value(92.5))
                .andExpect(jsonPath("$['Dr. Johnson']").value(88.0))
                .andExpect(jsonPath("$['Dr. Brown']").value(85.5));
    }
    
    @Test
    void shouldGetRecentActivities() throws Exception {
        // Given
        int limit = 5;
        
        List<DashboardMetrics.RecentActivity> activities = Arrays.asList(
                new DashboardMetrics.RecentActivity("VISIT_COMPLETED", "Checkup for Buddy", LocalDateTime.now().minusHours(1)),
                new DashboardMetrics.RecentActivity("VISIT_SCHEDULED", "Surgery for Max", LocalDateTime.now().minusHours(2))
        );
        
        when(dashboardService.getRecentActivities(limit)).thenReturn(activities);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/activities/recent")
                        .param("limit", String.valueOf(limit)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("VISIT_COMPLETED"))
                .andExpect(jsonPath("$[0].description").value("Checkup for Buddy"))
                .andExpect(jsonPath("$[1].type").value("VISIT_SCHEDULED"))
                .andExpect(jsonPath("$[1].description").value("Surgery for Max"));
    }
    
    @Test
    void shouldGetRecentActivitiesWithDefaultLimit() throws Exception {
        // Given
        List<DashboardMetrics.RecentActivity> activities = Arrays.asList(
                new DashboardMetrics.RecentActivity("VISIT_COMPLETED", "Checkup for Buddy", LocalDateTime.now())
        );
        
        when(dashboardService.getRecentActivities(10)).thenReturn(activities);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/activities/recent"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }
    
    @Test
    void shouldGetUpcomingAppointments() throws Exception {
        // Given
        int days = 7;
        int limit = 10;
        
        List<DashboardMetrics.UpcomingAppointment> appointments = Arrays.asList(
                new DashboardMetrics.UpcomingAppointment(1L, "Buddy", "John Doe", "Dr. Smith", LocalDateTime.now().plusHours(2), "Wellness Examination", 30),
                new DashboardMetrics.UpcomingAppointment(2L, "Max", "Jane Smith", "Dr. Johnson", LocalDateTime.now().plusDays(1), "Surgery", 60)
        );
        
        when(dashboardService.getUpcomingAppointments(days, limit)).thenReturn(appointments);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/appointments/upcoming")
                        .param("days", String.valueOf(days))
                        .param("limit", String.valueOf(limit)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].visitId").value(1))
                .andExpect(jsonPath("$[0].petName").value("Buddy"))
                .andExpect(jsonPath("$[0].ownerName").value("John Doe"))
                .andExpect(jsonPath("$[0].veterinarianName").value("Dr. Smith"))
                .andExpect(jsonPath("$[0].visitType").value("Wellness Examination"))
                .andExpect(jsonPath("$[0].duration").value(30));
    }
    
    @Test
    void shouldGetUpcomingAppointmentsWithDefaultParams() throws Exception {
        // Given
        List<DashboardMetrics.UpcomingAppointment> appointments = Arrays.asList(
                new DashboardMetrics.UpcomingAppointment(1L, "Buddy", "John Doe", "Dr. Smith", LocalDateTime.now().plusHours(2), "Wellness Examination", 30)
        );
        
        when(dashboardService.getUpcomingAppointments(7, 10)).thenReturn(appointments);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/appointments/upcoming"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }
    
    @Test
    void shouldGetSystemAlerts() throws Exception {
        // Given
        List<DashboardMetrics.Alert> alerts = Arrays.asList(
                new DashboardMetrics.Alert("WARNING", "High Utilization", "Veterinarian utilization is above 90%", "HIGH"),
                new DashboardMetrics.Alert("INFO", "System Update", "Scheduled maintenance tonight", "LOW")
        );
        
        when(dashboardService.getSystemAlerts()).thenReturn(alerts);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/alerts"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("WARNING"))
                .andExpect(jsonPath("$[0].title").value("High Utilization"))
                .andExpect(jsonPath("$[0].severity").value("HIGH"))
                .andExpect(jsonPath("$[1].type").value("INFO"))
                .andExpect(jsonPath("$[1].title").value("System Update"))
                .andExpect(jsonPath("$[1].severity").value("LOW"));
    }
    
    @Test
    void shouldGetPerformanceIndicators() throws Exception {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        
        Map<String, Object> indicators = Map.of(
                "totalVisits", 45,
                "completedVisits", 38,
                "completionRate", 84.4,
                "totalRevenue", BigDecimal.valueOf(4500.00),
                "averageRevenuePerVisit", BigDecimal.valueOf(100.00),
                "averageUtilization", 78.5,
                "averageVisitsPerDay", 6.4
        );
        
        when(dashboardService.getPerformanceIndicators(startDate, endDate)).thenReturn(indicators);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/performance")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalVisits").value(45))
                .andExpect(jsonPath("$.completedVisits").value(38))
                .andExpect(jsonPath("$.completionRate").value(84.4))
                .andExpect(jsonPath("$.totalRevenue").value(4500.00))
                .andExpect(jsonPath("$.averageUtilization").value(78.5));
    }
    
    @Test
    void shouldGetRevenueMetrics() throws Exception {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        
        Map<String, Object> metrics = Map.of(
                "totalRevenue", BigDecimal.valueOf(3500.00),
                "dailyAverage", BigDecimal.valueOf(500.00),
                "monthlyTotal", BigDecimal.valueOf(12000.00)
        );
        
        when(dashboardService.getRevenueMetrics(startDate, endDate)).thenReturn(metrics);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/revenue")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalRevenue").value(3500.00))
                .andExpect(jsonPath("$.dailyAverage").value(500.00))
                .andExpect(jsonPath("$.monthlyTotal").value(12000.00));
    }
    
    @Test
    void shouldGetTopVeterinariansByVisits() throws Exception {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        int limit = 5;
        
        Map<String, Long> topVets = Map.of(
                "Dr. Smith", 25L,
                "Dr. Johnson", 20L,
                "Dr. Brown", 18L
        );
        
        when(dashboardService.getTopVeterinariansByVisits(startDate, endDate, limit)).thenReturn(topVets);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/top-veterinarians/visits")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .param("limit", String.valueOf(limit)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$['Dr. Smith']").value(25))
                .andExpect(jsonPath("$['Dr. Johnson']").value(20))
                .andExpect(jsonPath("$['Dr. Brown']").value(18));
    }
    
    @Test
    void shouldGetTopVeterinariansByRevenue() throws Exception {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        int limit = 5;
        
        Map<String, Object> topVets = Map.of(
                "Dr. Smith", BigDecimal.valueOf(2500.00),
                "Dr. Johnson", BigDecimal.valueOf(2000.00),
                "Dr. Brown", BigDecimal.valueOf(1800.00)
        );
        
        when(dashboardService.getTopVeterinariansByRevenue(startDate, endDate, limit)).thenReturn(topVets);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/top-veterinarians/revenue")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .param("limit", String.valueOf(limit)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$['Dr. Smith']").value(2500.00))
                .andExpect(jsonPath("$['Dr. Johnson']").value(2000.00))
                .andExpect(jsonPath("$['Dr. Brown']").value(1800.00));
    }
    
    @Test
    void shouldGetMostCommonVisitTypes() throws Exception {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        int limit = 10;
        
        Map<String, Long> visitTypes = Map.of(
                "Wellness Examination", 15L,
                "Vaccination", 8L,
                "Surgery", 5L,
                "Emergency Visit", 3L
        );
        
        when(dashboardService.getMostCommonVisitTypes(startDate, endDate, limit)).thenReturn(visitTypes);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/visit-types/common")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .param("limit", String.valueOf(limit)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$['Wellness Examination']").value(15))
                .andExpect(jsonPath("$.Vaccination").value(8))
                .andExpect(jsonPath("$.Surgery").value(5))
                .andExpect(jsonPath("$['Emergency Visit']").value(3));
    }
    
    @Test
    void shouldGetVisitStatisticsBySpecies() throws Exception {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        
        Map<String, Long> visitsBySpecies = Map.of(
                "Dog", 20L,
                "Cat", 15L,
                "Bird", 3L,
                "Rabbit", 2L
        );
        
        when(dashboardService.getVisitStatisticsBySpecies(startDate, endDate)).thenReturn(visitsBySpecies);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/visits/by-species")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.Dog").value(20))
                .andExpect(jsonPath("$.Cat").value(15))
                .andExpect(jsonPath("$.Bird").value(3))
                .andExpect(jsonPath("$.Rabbit").value(2));
    }
    
    @Test
    void shouldGetDashboardMetricsBySpecies() throws Exception {
        // Given
        String species = "Dog";
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        
        when(dashboardService.getDashboardMetricsBySpecies(species, startDate, endDate)).thenReturn(testMetrics);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/metrics/by-species/{species}", species)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.todayAppointments").value(5))
                .andExpect(jsonPath("$.totalActivePets").value(25));
    }
    
    @Test
    void shouldGetDashboardMetricsByVeterinarian() throws Exception {
        // Given
        Long veterinarianId = 1L;
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        
        when(dashboardService.getDashboardMetricsByVeterinarian(veterinarianId, startDate, endDate)).thenReturn(testMetrics);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/metrics/by-veterinarian/{veterinarianId}", veterinarianId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.todayAppointments").value(5))
                .andExpect(jsonPath("$.veterinarianUtilization").value(75.5));
    }
    
    @Test
    void shouldGetMonthlyTrends() throws Exception {
        // Given
        int months = 12;
        
        Map<String, Object> trends = Map.of(
                "monthlyVisits", Map.of(
                        "2024-01", 45L,
                        "2023-12", 38L,
                        "2023-11", 42L
                ),
                "monthlyRevenue", Map.of(
                        "2024-01", BigDecimal.valueOf(4500.00),
                        "2023-12", BigDecimal.valueOf(3800.00),
                        "2023-11", BigDecimal.valueOf(4200.00)
                )
        );
        
        when(dashboardService.getMonthlyTrends(months)).thenReturn(trends);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/trends/monthly")
                        .param("months", String.valueOf(months)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.monthlyVisits").exists())
                .andExpect(jsonPath("$.monthlyRevenue").exists());
    }
    
    @Test
    void shouldGetWorkloadDistribution() throws Exception {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        
        Map<String, Object> distribution = Map.of(
                "visitDistribution", Map.of(
                        "Dr. Smith", 25L,
                        "Dr. Johnson", 20L
                ),
                "revenueDistribution", Map.of(
                        "Dr. Smith", BigDecimal.valueOf(2500.00),
                        "Dr. Johnson", BigDecimal.valueOf(2000.00)
                ),
                "averageVisitsPerVet", 22.5,
                "maxVisitsPerVet", 25L,
                "minVisitsPerVet", 20L,
                "visitDistributionBalance", 80.0
        );
        
        when(dashboardService.getWorkloadDistribution(startDate, endDate)).thenReturn(distribution);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/workload")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.visitDistribution").exists())
                .andExpect(jsonPath("$.revenueDistribution").exists())
                .andExpect(jsonPath("$.averageVisitsPerVet").value(22.5))
                .andExpect(jsonPath("$.visitDistributionBalance").value(80.0));
    }
    
    @Test
    void shouldGetCapacityUtilization() throws Exception {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        
        Map<String, Object> capacity = Map.of(
                "theoreticalMaxVisits", 448L,
                "actualVisits", 320L,
                "capacityUtilization", 71.4,
                "availableCapacity", 128L
        );
        
        when(dashboardService.getCapacityUtilization(startDate, endDate)).thenReturn(capacity);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/capacity")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.theoreticalMaxVisits").value(448))
                .andExpect(jsonPath("$.actualVisits").value(320))
                .andExpect(jsonPath("$.capacityUtilization").value(71.4))
                .andExpect(jsonPath("$.availableCapacity").value(128));
    }
    
    @Test
    void shouldGetDashboardHealth() throws Exception {
        // Given
        when(dashboardService.isDashboardHealthy()).thenReturn(true);
        when(dashboardService.getDashboardHealthScore()).thenReturn(87.5);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.isHealthy").value(true))
                .andExpect(jsonPath("$.healthScore").value(87.5));
    }
    
    @Test
    void shouldGetDashboardSummary() throws Exception {
        // Given
        Map<String, Object> summary = Map.of(
                "totalPets", 42L,
                "totalOwners", 35L,
                "totalVeterinarians", 4L,
                "todayAppointments", 8L,
                "todayCompleted", 6L,
                "healthScore", 87.5,
                "isHealthy", true,
                "alertCount", 2,
                "hasHighPriorityAlerts", false
        );
        
        when(dashboardService.getDashboardSummary()).thenReturn(summary);
        
        // When & Then
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalPets").value(42))
                .andExpect(jsonPath("$.totalOwners").value(35))
                .andExpect(jsonPath("$.totalVeterinarians").value(4))
                .andExpect(jsonPath("$.todayAppointments").value(8))
                .andExpect(jsonPath("$.todayCompleted").value(6))
                .andExpect(jsonPath("$.healthScore").value(87.5))
                .andExpect(jsonPath("$.isHealthy").value(true))
                .andExpect(jsonPath("$.alertCount").value(2))
                .andExpect(jsonPath("$.hasHighPriorityAlerts").value(false));
    }
    
    @Test
    void shouldRefreshDashboardCache() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/dashboard/refresh")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Dashboard cache refreshed successfully"));
    }
}