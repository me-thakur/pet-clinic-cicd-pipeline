package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.DashboardMetrics;
import com.petclinic.backend.dto.ReportFilter;
import com.petclinic.backend.dto.RevenueReport;
import com.petclinic.backend.dto.VisitStatisticsReport;
import com.petclinic.backend.exception.ValidationException;
import com.petclinic.backend.model.*;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.repository.VeterinarianRepository;
import com.petclinic.backend.repository.VisitRepository;
import com.petclinic.backend.service.ReportService;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReportServiceImpl
 * Tests visit statistics generation, revenue calculation, and dashboard metrics
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {
    
    @Mock
    private VisitRepository visitRepository;
    
    @Mock
    private VeterinarianRepository veterinarianRepository;
    
    @Mock
    private PetRepository petRepository;
    
    @InjectMocks
    private ReportServiceImpl reportService;
    
    private Owner testOwner;
    private Pet testPet;
    private Veterinarian testVeterinarian;
    private Visit testVisit;
    private LocalDate startDate;
    private LocalDate endDate;
    
    @BeforeEach
    void setUp() {
        // Create test data
        testOwner = new Owner();
        testOwner.setId(1L);
        testOwner.setFirstName("John");
        testOwner.setLastName("Doe");
        
        testPet = new Pet();
        testPet.setId(1L);
        testPet.setName("Buddy");
        testPet.setSpecies("Dog");
        testPet.setOwner(testOwner);
        
        testVeterinarian = new Veterinarian();
        testVeterinarian.setId(1L);
        testVeterinarian.setFirstName("Dr. Jane");
        testVeterinarian.setLastName("Smith");
        testVeterinarian.setLicenseNumber("VET123456");
        
        testVisit = new Visit();
        testVisit.setId(1L);
        testVisit.setVisitDate(LocalDateTime.now());
        testVisit.setVisitType(VisitType.WELLNESS_EXAM);
        testVisit.setPet(testPet);
        testVisit.setVeterinarian(testVeterinarian);
        testVisit.setCost(BigDecimal.valueOf(100.00));
        testVisit.setDiagnosis("Healthy");
        testVisit.setTreatment("Routine checkup");
        
        startDate = LocalDate.now().minusDays(30);
        endDate = LocalDate.now();
    }
    
    @Test
    void testGenerateVisitStatistics_ValidDateRange_ReturnsReport() {
        // Arrange
        List<Visit> visits = Arrays.asList(testVisit);
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(visits);
        
        // Act
        VisitStatisticsReport report = reportService.generateVisitStatistics(startDate, endDate);
        
        // Assert
        assertNotNull(report);
        assertEquals(startDate, report.getStartDate());
        assertEquals(endDate, report.getEndDate());
        assertEquals(1, report.getTotalVisits());
        assertEquals(1, report.getCompletedVisits());
        assertEquals(0, report.getScheduledVisits());
        assertTrue(report.hasData());
        
        verify(visitRepository).findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void testGenerateVisitStatistics_InvalidDateRange_ThrowsException() {
        // Arrange
        LocalDate invalidStartDate = LocalDate.now();
        LocalDate invalidEndDate = LocalDate.now().minusDays(1);
        
        // Act & Assert
        assertThrows(ValidationException.class, () -> 
                reportService.generateVisitStatistics(invalidStartDate, invalidEndDate));
    }
    
    @Test
    void testGenerateVisitStatistics_NullDates_ThrowsException() {
        // Act & Assert
        assertThrows(ValidationException.class, () -> 
                reportService.generateVisitStatistics(null, endDate));
        assertThrows(ValidationException.class, () -> 
                reportService.generateVisitStatistics(startDate, null));
    }
    
    @Test
    void testGenerateVisitStatisticsWithFilter_ValidFilter_ReturnsReport() {
        // Arrange
        ReportFilter filter = new ReportFilter(startDate, endDate);
        filter.setVeterinarianId(1L);
        
        List<Visit> visits = Arrays.asList(testVisit);
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(visits);
        
        // Act
        VisitStatisticsReport report = reportService.generateVisitStatistics(filter);
        
        // Assert
        assertNotNull(report);
        assertEquals(1, report.getTotalVisits());
        assertTrue(report.hasData());
    }
    
    @Test
    void testGenerateRevenueReport_ValidDateRange_ReturnsReport() {
        // Arrange
        List<Visit> visits = Arrays.asList(testVisit);
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(visits);
        
        // Act
        RevenueReport report = reportService.generateRevenueReport(startDate, endDate);
        
        // Assert
        assertNotNull(report);
        assertEquals(startDate, report.getStartDate());
        assertEquals(endDate, report.getEndDate());
        assertEquals(BigDecimal.valueOf(100.00), report.getTotalRevenue());
        assertEquals(1, report.getTotalPaidVisits());
        assertEquals(0, report.getTotalUnpaidVisits());
        assertTrue(report.hasRevenue());
        assertEquals(100.0, report.getPaymentRate());
        
        verify(visitRepository).findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void testGenerateRevenueReport_WithTrends_IncludesTrendData() {
        // Arrange
        List<Visit> currentVisits = Arrays.asList(testVisit);
        List<Visit> previousVisits = Arrays.asList(createVisitWithCost(BigDecimal.valueOf(80.00)));
        
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(currentVisits)
                .thenReturn(previousVisits);
        
        // Act
        RevenueReport report = reportService.generateRevenueReport(startDate, endDate, true);
        
        // Assert
        assertNotNull(report);
        assertNotNull(report.getTrendData());
        assertEquals(BigDecimal.valueOf(80.00), report.getTrendData().getPreviousPeriodRevenue());
        assertTrue(report.getTrendData().getGrowthRate() > 0);
        assertEquals("UP", report.getTrendData().getTrendDirection());
    }
    
    @Test
    void testGetDashboardMetrics_ReturnsMetrics() {
        // Arrange
        List<Visit> todayVisits = Arrays.asList(testVisit);
        List<Visit> monthlyVisits = Arrays.asList(testVisit);
        List<Veterinarian> vets = Arrays.asList(testVeterinarian);
        
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(todayVisits)
                .thenReturn(monthlyVisits);
        when(petRepository.count()).thenReturn(10L);
        when(veterinarianRepository.count()).thenReturn(3L);
        when(veterinarianRepository.findAll()).thenReturn(vets);
        
        // Act
        DashboardMetrics metrics = reportService.getDashboardMetrics();
        
        // Assert
        assertNotNull(metrics);
        assertEquals(1, metrics.getTodayAppointments());
        assertEquals(1, metrics.getTodayCompletedVisits());
        assertEquals(0, metrics.getTodayPendingVisits());
        assertEquals(BigDecimal.valueOf(100.00), metrics.getTodayRevenue());
        assertEquals(10L, metrics.getTotalActivePets());
        assertEquals(3L, metrics.getTotalVeterinarians());
        
        // Check that metrics are calculated (may not be healthy due to low utilization)
        assertTrue(metrics.getAppointmentCompletionRate() >= 0);
        assertTrue(metrics.getVeterinarianUtilization() >= 0);
    }
    
    @Test
    void testGenerateVisitStatisticsByVeterinarian_ReturnsGroupedReports() {
        // Arrange
        Veterinarian vet2 = new Veterinarian();
        vet2.setId(2L);
        vet2.setFirstName("Dr. Bob");
        vet2.setLastName("Johnson");
        
        Visit visit2 = createVisitWithVeterinarian(vet2);
        
        List<Visit> visits = Arrays.asList(testVisit, visit2);
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(visits);
        
        // Act
        List<VisitStatisticsReport> reports = reportService.generateVisitStatisticsByVeterinarian(startDate, endDate);
        
        // Assert
        assertNotNull(reports);
        assertEquals(2, reports.size());
        
        // Reports should be sorted by visit count (descending)
        assertTrue(reports.get(0).getTotalVisits() >= reports.get(1).getTotalVisits());
        
        // Each report should have veterinarian-specific data
        for (VisitStatisticsReport report : reports) {
            assertNotNull(report.getVisitsByVeterinarian());
            assertFalse(report.getVisitsByVeterinarian().isEmpty());
        }
    }
    
    @Test
    void testGenerateVisitStatisticsByType_ReturnsGroupedReports() {
        // Arrange
        Visit emergencyVisit = createVisitWithType(VisitType.EMERGENCY);
        
        List<Visit> visits = Arrays.asList(testVisit, emergencyVisit);
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(visits);
        
        // Act
        List<VisitStatisticsReport> reports = reportService.generateVisitStatisticsByType(startDate, endDate);
        
        // Assert
        assertNotNull(reports);
        assertEquals(2, reports.size());
        
        // Each report should have visit type-specific data
        for (VisitStatisticsReport report : reports) {
            assertNotNull(report.getVisitsByType());
            assertFalse(report.getVisitsByType().isEmpty());
        }
    }
    
    @Test
    void testGenerateVisitStatisticsBySpecies_ReturnsGroupedReports() {
        // Arrange
        Pet catPet = new Pet();
        catPet.setId(2L);
        catPet.setName("Whiskers");
        catPet.setSpecies("Cat");
        catPet.setOwner(testOwner);
        
        Visit catVisit = createVisitWithPet(catPet);
        
        List<Visit> visits = Arrays.asList(testVisit, catVisit);
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(visits);
        
        // Act
        List<VisitStatisticsReport> reports = reportService.generateVisitStatisticsBySpecies(startDate, endDate);
        
        // Assert
        assertNotNull(reports);
        assertEquals(2, reports.size());
        
        // Each report should have species-specific data
        for (VisitStatisticsReport report : reports) {
            assertNotNull(report.getVisitsBySpecies());
            assertFalse(report.getVisitsBySpecies().isEmpty());
        }
    }
    
    @Test
    void testValidateReportFilter_ValidFilter_ReturnsTrue() {
        // Arrange
        ReportFilter filter = new ReportFilter(startDate, endDate);
        filter.setVeterinarianId(1L);
        filter.setCompletedOnly(true);
        
        // Act
        boolean isValid = reportService.validateReportFilter(filter);
        
        // Assert
        assertTrue(isValid);
    }
    
    @Test
    void testValidateReportFilter_InvalidFilter_ReturnsFalse() {
        // Arrange
        ReportFilter filter = new ReportFilter();
        filter.setStartDate(LocalDate.now());
        filter.setEndDate(LocalDate.now().minusDays(1)); // Invalid: start after end
        
        // Act
        boolean isValid = reportService.validateReportFilter(filter);
        
        // Assert
        assertFalse(isValid);
    }
    
    @Test
    void testValidateReportFilter_NullFilter_ReturnsFalse() {
        // Act
        boolean isValid = reportService.validateReportFilter(null);
        
        // Assert
        assertFalse(isValid);
    }
    
    @Test
    void testGetAvailableFilterOptions_ReturnsFilterWithOptions() {
        // Arrange
        List<Pet> pets = Arrays.asList(testPet);
        when(petRepository.findAll()).thenReturn(pets);
        
        // Act
        ReportFilter filter = reportService.getAvailableFilterOptions();
        
        // Assert
        assertNotNull(filter);
        assertNotNull(filter.getVisitTypes());
        assertFalse(filter.getVisitTypes().isEmpty());
        assertNotNull(filter.getSpecies());
        assertTrue(filter.getSpecies().contains("Dog"));
    }
    
    @Test
    void testGetMonthlyRevenueTrends_ReturnsReportWithTrends() {
        // Arrange
        List<Visit> visits = Arrays.asList(testVisit);
        when(visitRepository.findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(visits)
                .thenReturn(Collections.emptyList()); // Previous period
        
        // Act
        RevenueReport report = reportService.getMonthlyRevenueTrends(6);
        
        // Assert
        assertNotNull(report);
        assertTrue(report.hasRevenue());
        // Should have called repository twice (current and previous period)
        verify(visitRepository, atLeast(2)).findByVisitDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    // Helper methods for creating test data
    
    private Visit createVisitWithCost(BigDecimal cost) {
        Visit visit = new Visit();
        visit.setId(2L);
        visit.setVisitDate(LocalDateTime.now().minusDays(1));
        visit.setVisitType(VisitType.WELLNESS_EXAM);
        visit.setPet(testPet);
        visit.setVeterinarian(testVeterinarian);
        visit.setCost(cost);
        visit.setDiagnosis("Test diagnosis");
        visit.setTreatment("Test treatment");
        return visit;
    }
    
    private Visit createVisitWithVeterinarian(Veterinarian veterinarian) {
        Visit visit = new Visit();
        visit.setId(3L);
        visit.setVisitDate(LocalDateTime.now());
        visit.setVisitType(VisitType.WELLNESS_EXAM);
        visit.setPet(testPet);
        visit.setVeterinarian(veterinarian);
        visit.setCost(BigDecimal.valueOf(75.00));
        visit.setDiagnosis("Test diagnosis");
        visit.setTreatment("Test treatment");
        return visit;
    }
    
    private Visit createVisitWithType(VisitType visitType) {
        Visit visit = new Visit();
        visit.setId(4L);
        visit.setVisitDate(LocalDateTime.now());
        visit.setVisitType(visitType);
        visit.setPet(testPet);
        visit.setVeterinarian(testVeterinarian);
        visit.setCost(BigDecimal.valueOf(200.00));
        visit.setDiagnosis("Emergency diagnosis");
        visit.setTreatment("Emergency treatment");
        return visit;
    }
    
    private Visit createVisitWithPet(Pet pet) {
        Visit visit = new Visit();
        visit.setId(5L);
        visit.setVisitDate(LocalDateTime.now());
        visit.setVisitType(VisitType.WELLNESS_EXAM);
        visit.setPet(pet);
        visit.setVeterinarian(testVeterinarian);
        visit.setCost(BigDecimal.valueOf(90.00));
        visit.setDiagnosis("Cat diagnosis");
        visit.setTreatment("Cat treatment");
        return visit;
    }
}