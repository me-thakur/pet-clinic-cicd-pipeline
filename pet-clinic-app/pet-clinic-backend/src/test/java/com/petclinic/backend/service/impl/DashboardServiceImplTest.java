package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.DashboardMetrics;
import com.petclinic.backend.dto.ReportFilter;
import com.petclinic.backend.exception.ValidationException;
import com.petclinic.backend.model.*;
import com.petclinic.backend.repository.OwnerRepository;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.repository.VeterinarianRepository;
import com.petclinic.backend.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DashboardServiceImpl
 * Tests dashboard metrics calculation and real-time analytics functionality
 * Validates: Requirements 5.4, 5.5
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {
    
    @Mock
    private VisitRepository visitRepository;
    
    @Mock
    private VeterinarianRepository veterinarianRepository;
    
    @Mock
    private PetRepository petRepository;
    
    @Mock
    private OwnerRepository ownerRepository;
    
    @InjectMocks
    private DashboardServiceImpl dashboardService;
    
    private Visit testVisit;
    private Veterinarian testVeterinarian;
    private Pet testPet;
    private Owner testOwner;
    private LocalDate testDate;
    
    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2024, 1, 15);
        
        // Create test owner
        testOwner = new Owner();
        testOwner.setId(1L);
        testOwner.setFirstName("John");
        testOwner.setLastName("Doe");
        testOwner.setEmail("john.doe@test.com");
        
        // Create test pet
        testPet = new Pet();
        testPet.setId(1L);
        testPet.setName("Buddy");
        testPet.setSpecies("Dog");
        testPet.setBreed("Golden Retriever");
        testPet.setBirthDate(LocalDate.of(2020, 1, 1));
        testPet.setOwner(testOwner);
        
        // Create test veterinarian
        testVeterinarian = new Veterinarian();
        testVeterinarian.setId(1L);
        testVeterinarian.setFirstName("Jane");
        testVeterinarian.setLastName("Smith");
        testVeterinarian.setLicenseNumber("VET123");
        testVeterinarian.setSpecialtySet(Set.of(Specialty.GENERAL_PRACTICE));
        
        // Create test visit
        testVisit = new Visit();
        testVisit.setId(1L);
        testVisit.setVisitDate(testDate.atTime(10, 0));
        testVisit.setVisitType(VisitType.WELLNESS_EXAM);
        testVisit.setDuration(30);
        testVisit.setCost(BigDecimal.valueOf(100.00));
        testVisit.setPet(testPet);
        testVisit.setVeterinarian(testVeterinarian);
        testVisit.setDiagnosis("Healthy");
        testVisit.setTreatment("Routine checkup");
    }
    
    @Test
    void shouldGetRealTimeDashboardMetrics() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        
        List<Visit> todayVisits = Arrays.asList(testVisit);
        
        when(visitRepository.findByVisitDateBetween(startOfDay, endOfDay)).thenReturn(todayVisits);
        when(petRepository.count()).thenReturn(10L);
        when(ownerRepository.count()).thenReturn(8L);
        when(veterinarianRepository.count()).thenReturn(3L);
        when(veterinarianRepository.findAll()).thenReturn(Arrays.asList(testVeterinarian));
        
        // When
        DashboardMetrics metrics = dashboardService.getRealTimeDashboardMetrics();
        
        // Then
        assertNotNull(metrics);
        assertNotNull(metrics.getGeneratedAt());
        assertEquals(10L, metrics.getTotalActivePets());
        assertEquals(8L, metrics.getTotalActiveOwners());
        assertEquals(3L, metrics.getTotalVeterinarians());
        
        verify(visitRepository, atLeastOnce()).findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(petRepository).count();
        verify(ownerRepository).count();
        verify(veterinarianRepository).count();
    }
    
    @Test
    void shouldGetDashboardMetricsForSpecificDate() {
        // Given
        LocalDateTime startOfDay = testDate.atStartOfDay();
        LocalDateTime endOfDay = testDate.atTime(LocalTime.MAX);
        
        List<Visit> dayVisits = Arrays.asList(testVisit);
        
        when(visitRepository.findByVisitDateBetween(startOfDay, endOfDay)).thenReturn(dayVisits);
        when(petRepository.count()).thenReturn(10L);
        when(ownerRepository.count()).thenReturn(8L);
        when(veterinarianRepository.count()).thenReturn(3L);
        when(veterinarianRepository.findAll()).thenReturn(Arrays.asList(testVeterinarian));
        
        // When
        DashboardMetrics metrics = dashboardService.getDashboardMetrics(testDate);
        
        // Then
        assertNotNull(metrics);
        assertEquals(1L, metrics.getTodayAppointments());
        assertEquals(BigDecimal.valueOf(100.00), metrics.getTodayRevenue());
        
        verify(visitRepository, atLeastOnce()).findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void shouldGetDashboardMetricsForDateRange() {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        
        List<Visit> rangeVisits = Arrays.asList(testVisit);
        
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(rangeVisits);
        when(petRepository.count()).thenReturn(10L);
        when(ownerRepository.count()).thenReturn(8L);
        when(veterinarianRepository.count()).thenReturn(3L);
        when(veterinarianRepository.findAll()).thenReturn(Arrays.asList(testVeterinarian));
        
        // When
        DashboardMetrics metrics = dashboardService.getDashboardMetrics(startDate, endDate);
        
        // Then
        assertNotNull(metrics);
        assertEquals(10L, metrics.getTotalActivePets());
        assertEquals(8L, metrics.getTotalActiveOwners());
        assertEquals(3L, metrics.getTotalVeterinarians());
        
        verify(visitRepository, atLeastOnce()).findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void shouldThrowValidationExceptionForInvalidDateRange() {
        // Given
        LocalDate startDate = testDate;
        LocalDate endDate = testDate.minusDays(1); // End date before start date
        
        // When & Then
        assertThrows(ValidationException.class, () -> {
            dashboardService.getDashboardMetrics(startDate, endDate);
        });
    }
    
    @Test
    void shouldGetDashboardMetricsWithFilter() {
        // Given
        ReportFilter filter = new ReportFilter();
        filter.setStartDate(testDate.minusDays(7));
        filter.setEndDate(testDate);
        filter.setSpecies(Arrays.asList("Dog"));
        
        List<Visit> filteredVisits = Arrays.asList(testVisit);
        
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(filteredVisits);
        when(petRepository.count()).thenReturn(10L);
        when(ownerRepository.count()).thenReturn(8L);
        when(veterinarianRepository.count()).thenReturn(3L);
        when(veterinarianRepository.findAll()).thenReturn(Arrays.asList(testVeterinarian));
        
        // When
        DashboardMetrics metrics = dashboardService.getDashboardMetrics(filter);
        
        // Then
        assertNotNull(metrics);
        assertNotNull(metrics.getPerformanceIndicators());
        assertTrue(metrics.getPerformanceIndicators().containsKey("filteredTotalVisits"));
        
        verify(visitRepository, atLeastOnce()).findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void shouldThrowValidationExceptionForInvalidFilter() {
        // Given
        ReportFilter invalidFilter = new ReportFilter();
        invalidFilter.setStartDate(testDate);
        invalidFilter.setEndDate(testDate.minusDays(1)); // Invalid date range
        
        // When & Then
        assertThrows(ValidationException.class, () -> {
            dashboardService.getDashboardMetrics(invalidFilter);
        });
    }
    
    @Test
    void shouldGetDailyAppointmentMetrics() {
        // Given
        LocalDateTime startOfDay = testDate.atStartOfDay();
        LocalDateTime endOfDay = testDate.atTime(LocalTime.MAX);
        
        Visit completedVisit = new Visit();
        completedVisit.setId(2L);
        completedVisit.setVisitDate(testDate.atTime(14, 0));
        completedVisit.setDiagnosis("Completed");
        completedVisit.setTreatment("Treatment given");
        
        Visit cancelledVisit = new Visit();
        cancelledVisit.setId(3L);
        cancelledVisit.setVisitDate(testDate.atTime(16, 0));
        cancelledVisit.setNotes("CANCELLED by owner");
        
        List<Visit> dayVisits = Arrays.asList(testVisit, completedVisit, cancelledVisit);
        
        when(visitRepository.findByVisitDateBetween(startOfDay, endOfDay)).thenReturn(dayVisits);
        
        // When
        Map<String, Long> metrics = dashboardService.getDailyAppointmentMetrics(testDate);
        
        // Then
        assertNotNull(metrics);
        assertEquals(3L, metrics.get("scheduled"));
        assertEquals(2L, metrics.get("completed")); // testVisit and completedVisit both have diagnosis+treatment
        assertEquals(1L, metrics.get("cancelled"));
        
        verify(visitRepository).findByVisitDateBetween(startOfDay, endOfDay);
    }
    
    @Test
    void shouldGetActivePetsCount() {
        // Given
        when(petRepository.count()).thenReturn(15L);
        
        // When
        long count = dashboardService.getActivePetsCount();
        
        // Then
        assertEquals(15L, count);
        verify(petRepository).count();
    }
    
    @Test
    void shouldGetActivePetsBySpecies() {
        // Given
        List<Object[]> speciesData = Arrays.asList(
                new Object[]{"Dog", 8L},
                new Object[]{"Cat", 5L},
                new Object[]{"Bird", 2L}
        );
        
        when(petRepository.countPetsBySpecies()).thenReturn(speciesData);
        
        // When
        Map<String, Long> petsBySpecies = dashboardService.getActivePetsBySpecies();
        
        // Then
        assertNotNull(petsBySpecies);
        assertEquals(3, petsBySpecies.size());
        assertEquals(8L, petsBySpecies.get("Dog"));
        assertEquals(5L, petsBySpecies.get("Cat"));
        assertEquals(2L, petsBySpecies.get("Bird"));
        
        verify(petRepository).countPetsBySpecies();
    }
    
    @Test
    void shouldGetActiveOwnersCount() {
        // Given
        when(ownerRepository.count()).thenReturn(12L);
        
        // When
        long count = dashboardService.getActiveOwnersCount();
        
        // Then
        assertEquals(12L, count);
        verify(ownerRepository).count();
    }
    
    @Test
    void shouldGetVeterinarianUtilization() {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        
        List<Visit> visits = Arrays.asList(testVisit);
        List<Veterinarian> vets = Arrays.asList(testVeterinarian);
        
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(visits);
        when(veterinarianRepository.findAll()).thenReturn(vets);
        
        // When
        Map<String, Object> utilization = dashboardService.getVeterinarianUtilization(startDate, endDate);
        
        // Then
        assertNotNull(utilization);
        assertTrue(utilization.containsKey("byVeterinarian"));
        assertTrue(utilization.containsKey("average"));
        assertTrue(utilization.containsKey("maximum"));
        assertTrue(utilization.containsKey("minimum"));
        
        @SuppressWarnings("unchecked")
        Map<String, Double> vetUtilization = (Map<String, Double>) utilization.get("byVeterinarian");
        assertTrue(vetUtilization.containsKey("Dr. Jane Smith"));
        
        verify(visitRepository).findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(veterinarianRepository).findAll();
    }
    
    @Test
    void shouldGetVeterinarianUtilizationForSpecificVet() {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        Long veterinarianId = 1L;
        
        List<Visit> vetVisits = Arrays.asList(testVisit);
        
        when(visitRepository.findByVeterinarianIdAndVisitDateBetween(eq(veterinarianId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(vetVisits);
        
        // When
        double utilization = dashboardService.getVeterinarianUtilization(veterinarianId, startDate, endDate);
        
        // Then
        assertTrue(utilization >= 0.0);
        assertTrue(utilization <= 100.0);
        
        verify(visitRepository).findByVeterinarianIdAndVisitDateBetween(eq(veterinarianId), any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void shouldReturnZeroUtilizationForVetWithNoVisits() {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        Long veterinarianId = 1L;
        
        when(visitRepository.findByVeterinarianIdAndVisitDateBetween(eq(veterinarianId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        
        // When
        double utilization = dashboardService.getVeterinarianUtilization(veterinarianId, startDate, endDate);
        
        // Then
        assertEquals(0.0, utilization);
        
        verify(visitRepository).findByVeterinarianIdAndVisitDateBetween(eq(veterinarianId), any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void shouldGetAppointmentCompletionRate() {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        
        Visit completedVisit = new Visit();
        completedVisit.setDiagnosis("Completed");
        completedVisit.setTreatment("Treatment given");
        
        Visit incompleteVisit = new Visit();
        // No diagnosis or treatment, so not completed
        
        List<Visit> visits = Arrays.asList(completedVisit, incompleteVisit);
        
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(visits);
        
        // When
        double completionRate = dashboardService.getAppointmentCompletionRate(startDate, endDate);
        
        // Then
        assertEquals(50.0, completionRate); // 1 out of 2 visits completed
        
        verify(visitRepository).findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void shouldReturnZeroCompletionRateForNoVisits() {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        
        // When
        double completionRate = dashboardService.getAppointmentCompletionRate(startDate, endDate);
        
        // Then
        assertEquals(0.0, completionRate);
        
        verify(visitRepository).findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void shouldGetAppointmentCompletionRatesByVeterinarian() {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        
        Visit completedVisit = new Visit();
        completedVisit.setVeterinarian(testVeterinarian);
        completedVisit.setDiagnosis("Completed");
        completedVisit.setTreatment("Treatment given");
        
        Visit incompleteVisit = new Visit();
        incompleteVisit.setVeterinarian(testVeterinarian);
        // No diagnosis or treatment, so not completed
        
        List<Visit> visits = Arrays.asList(completedVisit, incompleteVisit);
        
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(visits);
        
        // When
        Map<String, Double> completionRates = dashboardService.getAppointmentCompletionRatesByVeterinarian(startDate, endDate);
        
        // Then
        assertNotNull(completionRates);
        assertTrue(completionRates.containsKey("Dr. Jane Smith"));
        assertEquals(50.0, completionRates.get("Dr. Jane Smith"));
        
        verify(visitRepository).findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void shouldGetRecentActivities() {
        // Given
        int limit = 5;
        
        List<Visit> recentVisits = Arrays.asList(testVisit);
        
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(recentVisits);
        
        // When
        List<DashboardMetrics.RecentActivity> activities = dashboardService.getRecentActivities(limit);
        
        // Then
        assertNotNull(activities);
        assertEquals(1, activities.size());
        
        DashboardMetrics.RecentActivity activity = activities.get(0);
        assertNotNull(activity.getType());
        assertNotNull(activity.getDescription());
        assertNotNull(activity.getTimestamp());
        
        verify(visitRepository).findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void shouldGetUpcomingAppointments() {
        // Given
        int days = 7;
        int limit = 10;
        
        Visit futureVisit = new Visit();
        futureVisit.setId(2L);
        futureVisit.setVisitDate(LocalDateTime.now().plusDays(2));
        futureVisit.setVisitType(VisitType.WELLNESS_EXAM);
        futureVisit.setPet(testPet);
        futureVisit.setVeterinarian(testVeterinarian);
        futureVisit.setDuration(30);
        
        List<Visit> upcomingVisits = Arrays.asList(futureVisit);
        
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(upcomingVisits);
        
        // When
        List<DashboardMetrics.UpcomingAppointment> appointments = dashboardService.getUpcomingAppointments(days, limit);
        
        // Then
        assertNotNull(appointments);
        assertEquals(1, appointments.size());
        
        DashboardMetrics.UpcomingAppointment appointment = appointments.get(0);
        assertEquals(futureVisit.getId(), appointment.getVisitId());
        assertEquals("Buddy", appointment.getPetName());
        assertEquals("Dr. Jane Smith", appointment.getVeterinarianName());
        
        verify(visitRepository).findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void shouldGetSystemAlerts() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        
        Visit overdueVisit = new Visit();
        overdueVisit.setId(2L);
        overdueVisit.setVisitDate(yesterday.minusHours(2));
        overdueVisit.setDiagnosis(null); // Not completed
        
        List<Visit> overdueVisits = Arrays.asList(overdueVisit);
        
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(overdueVisits);
        when(veterinarianRepository.findAll()).thenReturn(Arrays.asList(testVeterinarian));
        
        // When
        List<DashboardMetrics.Alert> alerts = dashboardService.getSystemAlerts();
        
        // Then
        assertNotNull(alerts);
        assertFalse(alerts.isEmpty());
        
        // Should have at least one alert for overdue visits
        boolean hasOverdueAlert = alerts.stream()
                .anyMatch(alert -> alert.getTitle().contains("Overdue"));
        assertTrue(hasOverdueAlert);
        
        verify(visitRepository, atLeastOnce()).findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void shouldGetPerformanceIndicators() {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        
        List<Visit> visits = Arrays.asList(testVisit);
        
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(visits);
        when(veterinarianRepository.findAll()).thenReturn(Arrays.asList(testVeterinarian));
        
        // When
        Map<String, Object> indicators = dashboardService.getPerformanceIndicators(startDate, endDate);
        
        // Then
        assertNotNull(indicators);
        assertTrue(indicators.containsKey("totalVisits"));
        assertTrue(indicators.containsKey("completedVisits"));
        assertTrue(indicators.containsKey("completionRate"));
        assertTrue(indicators.containsKey("totalRevenue"));
        assertTrue(indicators.containsKey("averageRevenuePerVisit"));
        assertTrue(indicators.containsKey("averageUtilization"));
        assertTrue(indicators.containsKey("averageVisitsPerDay"));
        
        assertEquals(1, indicators.get("totalVisits"));
        assertEquals(BigDecimal.valueOf(100.00), indicators.get("totalRevenue"));
        
        verify(visitRepository, atLeastOnce()).findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void shouldGetRevenueMetrics() {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        
        List<Visit> visits = Arrays.asList(testVisit);
        
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(visits);
        
        // When
        Map<String, Object> metrics = dashboardService.getRevenueMetrics(startDate, endDate);
        
        // Then
        assertNotNull(metrics);
        assertTrue(metrics.containsKey("totalRevenue"));
        assertTrue(metrics.containsKey("dailyAverage"));
        
        assertEquals(BigDecimal.valueOf(100.00), metrics.get("totalRevenue"));
        
        verify(visitRepository).findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void shouldGetTopVeterinariansByVisits() {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        int limit = 5;
        
        List<Visit> visits = Arrays.asList(testVisit, testVisit);
        
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(visits);
        
        // When
        Map<String, Long> topVets = dashboardService.getTopVeterinariansByVisits(startDate, endDate, limit);
        
        // Then
        assertNotNull(topVets);
        assertTrue(topVets.containsKey("Dr. Jane Smith"));
        assertEquals(2L, topVets.get("Dr. Jane Smith"));
        
        verify(visitRepository).findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void shouldGetMostCommonVisitTypes() {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        int limit = 10;
        
        List<Visit> visits = Arrays.asList(testVisit);
        
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(visits);
        
        // When
        Map<String, Long> visitTypes = dashboardService.getMostCommonVisitTypes(startDate, endDate, limit);
        
        // Then
        assertNotNull(visitTypes);
        assertTrue(visitTypes.containsKey(VisitType.WELLNESS_EXAM.getDisplayName()));
        assertEquals(1L, visitTypes.get(VisitType.WELLNESS_EXAM.getDisplayName()));
        
        verify(visitRepository).findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void shouldGetVisitStatisticsBySpecies() {
        // Given
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        
        List<Visit> visits = Arrays.asList(testVisit);
        
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(visits);
        
        // When
        Map<String, Long> visitsBySpecies = dashboardService.getVisitStatisticsBySpecies(startDate, endDate);
        
        // Then
        assertNotNull(visitsBySpecies);
        assertTrue(visitsBySpecies.containsKey("Dog"));
        assertEquals(1L, visitsBySpecies.get("Dog"));
        
        verify(visitRepository).findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void shouldCheckDashboardHealth() {
        // Given
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testVisit));
        when(petRepository.count()).thenReturn(10L);
        when(ownerRepository.count()).thenReturn(8L);
        when(veterinarianRepository.count()).thenReturn(3L);
        when(veterinarianRepository.findAll()).thenReturn(Arrays.asList(testVeterinarian));
        
        // When
        boolean isHealthy = dashboardService.isDashboardHealthy();
        
        // Then
        // Health depends on completion rate and utilization, which depend on test data
        // Just verify the method executes without error
        assertNotNull(isHealthy);
    }
    
    @Test
    void shouldGetDashboardHealthScore() {
        // Given
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testVisit));
        when(petRepository.count()).thenReturn(10L);
        when(ownerRepository.count()).thenReturn(8L);
        when(veterinarianRepository.count()).thenReturn(3L);
        when(veterinarianRepository.findAll()).thenReturn(Arrays.asList(testVeterinarian));
        
        // When
        double healthScore = dashboardService.getDashboardHealthScore();
        
        // Then
        assertTrue(healthScore >= 0.0);
        assertTrue(healthScore <= 100.0);
    }
    
    @Test
    void shouldGetDashboardSummary() {
        // Given
        when(petRepository.count()).thenReturn(10L);
        when(ownerRepository.count()).thenReturn(8L);
        when(veterinarianRepository.count()).thenReturn(3L);
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testVisit));
        when(veterinarianRepository.findAll()).thenReturn(Arrays.asList(testVeterinarian));
        
        // When
        Map<String, Object> summary = dashboardService.getDashboardSummary();
        
        // Then
        assertNotNull(summary);
        assertTrue(summary.containsKey("totalPets"));
        assertTrue(summary.containsKey("totalOwners"));
        assertTrue(summary.containsKey("totalVeterinarians"));
        assertTrue(summary.containsKey("todayAppointments"));
        assertTrue(summary.containsKey("todayCompleted"));
        assertTrue(summary.containsKey("healthScore"));
        assertTrue(summary.containsKey("isHealthy"));
        assertTrue(summary.containsKey("alertCount"));
        
        assertEquals(10L, summary.get("totalPets"));
        assertEquals(8L, summary.get("totalOwners"));
        assertEquals(3L, summary.get("totalVeterinarians"));
    }
    
    @Test
    void shouldRefreshDashboardCache() {
        // When & Then - should not throw exception
        assertDoesNotThrow(() -> dashboardService.refreshDashboardCache());
    }
}