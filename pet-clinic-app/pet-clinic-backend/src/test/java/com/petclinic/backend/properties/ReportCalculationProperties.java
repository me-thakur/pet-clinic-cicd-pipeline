package com.petclinic.backend.properties;

import com.petclinic.backend.dto.RevenueReport;
import com.petclinic.backend.dto.VisitStatisticsReport;
import com.petclinic.backend.model.*;
import com.petclinic.backend.repository.OwnerRepository;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.repository.VeterinarianRepository;
import com.petclinic.backend.repository.VisitRepository;
import com.petclinic.backend.service.ReportService;
import net.java.quickcheck.Generator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for report calculation functionality
 * **Validates: Requirements 5.1, 5.2**
 * 
 * Tests universal properties that should hold for all report calculations
 * Uses H2 test database to verify report aggregation accuracy and revenue calculation correctness
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReportCalculationProperties extends PropertyTestBase {
    
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private VisitRepository visitRepository;
    
    @Autowired
    private PetRepository petRepository;
    
    @Autowired
    private VeterinarianRepository veterinarianRepository;
    
    @Autowired
    private OwnerRepository ownerRepository;
    
    /**
     * Property 13: Report Aggregation Accuracy
     * For any report request with grouping criteria (date range, veterinarian, visit type), 
     * aggregated data should accurately reflect the underlying data
     * **Validates: Requirements 5.1, 5.4**
     */
    @Test
    void testReportAggregationAccuracy() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Clear any existing data to ensure clean test
            visitRepository.deleteAll();
            petRepository.deleteAll();
            veterinarianRepository.deleteAll();
            ownerRepository.deleteAll();
            
            // Generate test data
            LocalDate startDate = validReportStartDates().next();
            LocalDate endDate = validReportEndDates(startDate).next();
            
            // Create test entities
            List<Owner> owners = createTestOwners(2 + random.nextInt(3)); // 2-4 owners
            List<Pet> pets = createTestPets(owners, 3 + random.nextInt(5)); // 3-7 pets
            List<Veterinarian> veterinarians = createTestVeterinarians(2 + random.nextInt(3)); // 2-4 vets
            
            // Create visits within the date range
            List<Visit> visits = createTestVisits(pets, veterinarians, startDate, endDate, 5 + random.nextInt(15)); // 5-19 visits
            
            // Generate visit statistics report
            VisitStatisticsReport report = reportService.generateVisitStatistics(startDate, endDate);
            
            // Verify total visit count accuracy
            long expectedTotalVisits = visits.size();
            assertEquals(expectedTotalVisits, report.getTotalVisits(), 
                        "Total visits in report should match actual visit count");
            
            // Verify completed visits count accuracy
            long expectedCompletedVisits = visits.stream()
                    .mapToLong(v -> v.isCompleted() ? 1 : 0)
                    .sum();
            assertEquals(expectedCompletedVisits, report.getCompletedVisits(), 
                        "Completed visits count should match actual completed visits");
            
            // Verify cancelled visits count accuracy
            long expectedCancelledVisits = visits.stream()
                    .mapToLong(v -> v.getNotes() != null && v.getNotes().contains("CANCELLED") ? 1 : 0)
                    .sum();
            assertEquals(expectedCancelledVisits, report.getCancelledVisits(), 
                        "Cancelled visits count should match actual cancelled visits");
            
            // Verify scheduled visits calculation
            long expectedScheduledVisits = expectedTotalVisits - expectedCompletedVisits - expectedCancelledVisits;
            assertEquals(expectedScheduledVisits, report.getScheduledVisits(), 
                        "Scheduled visits should be calculated correctly");
            
            // Verify visits by veterinarian aggregation
            Map<String, Long> expectedVisitsByVet = visits.stream()
                    .filter(v -> v.getVeterinarian() != null)
                    .collect(Collectors.groupingBy(
                            v -> v.getVeterinarian().getFullName(),
                            Collectors.counting()));
            
            Map<String, Long> actualVisitsByVet = report.getVisitsByVeterinarian();
            if (actualVisitsByVet != null && !expectedVisitsByVet.isEmpty()) {
                for (Map.Entry<String, Long> entry : expectedVisitsByVet.entrySet()) {
                    assertEquals(entry.getValue(), actualVisitsByVet.get(entry.getKey()), 
                                "Visits by veterinarian should match for: " + entry.getKey());
                }
            }
            
            // Verify visits by visit type aggregation
            Map<VisitType, Long> expectedVisitsByType = visits.stream()
                    .filter(v -> v.getVisitType() != null)
                    .collect(Collectors.groupingBy(Visit::getVisitType, Collectors.counting()));
            
            Map<VisitType, Long> actualVisitsByType = report.getVisitsByType();
            if (actualVisitsByType != null && !expectedVisitsByType.isEmpty()) {
                for (Map.Entry<VisitType, Long> entry : expectedVisitsByType.entrySet()) {
                    assertEquals(entry.getValue(), actualVisitsByType.get(entry.getKey()), 
                                "Visits by type should match for: " + entry.getKey());
                }
            }
            
            // Verify visits by species aggregation
            Map<String, Long> expectedVisitsBySpecies = visits.stream()
                    .filter(v -> v.getPet() != null && v.getPet().getSpecies() != null)
                    .collect(Collectors.groupingBy(
                            v -> v.getPet().getSpecies(),
                            Collectors.counting()));
            
            Map<String, Long> actualVisitsBySpecies = report.getVisitsBySpecies();
            if (actualVisitsBySpecies != null && !expectedVisitsBySpecies.isEmpty()) {
                for (Map.Entry<String, Long> entry : expectedVisitsBySpecies.entrySet()) {
                    assertEquals(entry.getValue(), actualVisitsBySpecies.get(entry.getKey()), 
                                "Visits by species should match for: " + entry.getKey());
                }
            }
            
            // Verify visits by date aggregation
            Map<LocalDate, Long> expectedVisitsByDate = visits.stream()
                    .collect(Collectors.groupingBy(
                            v -> v.getVisitDate().toLocalDate(),
                            Collectors.counting()));
            
            Map<LocalDate, Long> actualVisitsByDate = report.getVisitsByDate();
            if (actualVisitsByDate != null && !expectedVisitsByDate.isEmpty()) {
                for (Map.Entry<LocalDate, Long> entry : expectedVisitsByDate.entrySet()) {
                    assertEquals(entry.getValue(), actualVisitsByDate.get(entry.getKey()), 
                                "Visits by date should match for: " + entry.getKey());
                }
            }
            
            // Verify completion rate calculation
            if (expectedTotalVisits > 0) {
                double expectedCompletionRate = (double) expectedCompletedVisits / expectedTotalVisits * 100.0;
                assertEquals(expectedCompletionRate, report.getCompletionRate(), 0.01, 
                            "Completion rate should be calculated correctly");
            }
            
            // Verify average visits per day calculation
            if (startDate != null && endDate != null) {
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
                if (daysBetween > 0) {
                    double expectedAverageVisitsPerDay = (double) expectedTotalVisits / daysBetween;
                    assertEquals(expectedAverageVisitsPerDay, report.getAverageVisitsPerDay(), 0.01, 
                                "Average visits per day should be calculated correctly");
                }
            }
            
            // Test aggregation consistency across different grouping methods
            List<VisitStatisticsReport> reportsByVet = reportService.generateVisitStatisticsByVeterinarian(startDate, endDate);
            long totalVisitsFromVetReports = reportsByVet.stream()
                    .mapToLong(VisitStatisticsReport::getTotalVisits)
                    .sum();
            
            // Total visits from veterinarian reports should not exceed overall total
            // (some visits might not have veterinarians assigned)
            assertTrue(totalVisitsFromVetReports <= expectedTotalVisits, 
                      "Total visits from veterinarian reports should not exceed overall total");
            
            List<VisitStatisticsReport> reportsByType = reportService.generateVisitStatisticsByType(startDate, endDate);
            long totalVisitsFromTypeReports = reportsByType.stream()
                    .mapToLong(VisitStatisticsReport::getTotalVisits)
                    .sum();
            
            // Total visits from type reports should not exceed overall total
            // (some visits might not have types assigned)
            assertTrue(totalVisitsFromTypeReports <= expectedTotalVisits, 
                      "Total visits from type reports should not exceed overall total");
        });
    }
    
    /**
     * Property 14: Revenue Calculation Correctness
     * For any set of visits with associated fees, revenue calculations should accurately sum fees 
     * and display correct trends over time
     * **Validates: Requirements 5.2**
     */
    @Test
    void testRevenueCalculationCorrectness() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Clear any existing data to ensure clean test
            visitRepository.deleteAll();
            petRepository.deleteAll();
            veterinarianRepository.deleteAll();
            ownerRepository.deleteAll();
            
            // Generate test data
            LocalDate startDate = validReportStartDates().next();
            LocalDate endDate = validReportEndDates(startDate).next();
            
            // Create test entities
            List<Owner> owners = createTestOwners(2 + random.nextInt(3)); // 2-4 owners
            List<Pet> pets = createTestPets(owners, 3 + random.nextInt(5)); // 3-7 pets
            List<Veterinarian> veterinarians = createTestVeterinarians(2 + random.nextInt(3)); // 2-4 vets
            
            // Create visits with costs within the date range
            List<Visit> visits = createTestVisitsWithCosts(pets, veterinarians, startDate, endDate, 5 + random.nextInt(15)); // 5-19 visits
            
            // Generate revenue report
            RevenueReport report = reportService.generateRevenueReport(startDate, endDate);
            
            // Calculate expected values from actual data
            List<Visit> paidVisits = visits.stream()
                    .filter(v -> v.getCost() != null && v.getCost().compareTo(BigDecimal.ZERO) > 0)
                    .collect(Collectors.toList());
            
            List<Visit> unpaidVisits = visits.stream()
                    .filter(v -> v.getCost() == null || v.getCost().compareTo(BigDecimal.ZERO) == 0)
                    .collect(Collectors.toList());
            
            BigDecimal expectedTotalRevenue = paidVisits.stream()
                    .map(Visit::getCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Verify total revenue calculation
            assertEquals(expectedTotalRevenue, report.getTotalRevenue(), 
                        "Total revenue should match sum of all visit costs");
            
            // Verify paid/unpaid visit counts
            assertEquals(paidVisits.size(), report.getTotalPaidVisits(), 
                        "Paid visits count should match visits with positive costs");
            assertEquals(unpaidVisits.size(), report.getTotalUnpaidVisits(), 
                        "Unpaid visits count should match visits with zero or null costs");
            
            // Verify total visits calculation
            assertEquals(visits.size(), report.getTotalVisits(), 
                        "Total visits should equal paid + unpaid visits");
            
            // Verify payment rate calculation
            if (visits.size() > 0) {
                double expectedPaymentRate = (double) paidVisits.size() / visits.size() * 100.0;
                assertEquals(expectedPaymentRate, report.getPaymentRate(), 0.01, 
                            "Payment rate should be calculated correctly");
            }
            
            // Verify average revenue per visit calculation
            if (paidVisits.size() > 0 && expectedTotalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal expectedAveragePerVisit = expectedTotalRevenue.divide(
                    BigDecimal.valueOf(paidVisits.size()), 2, BigDecimal.ROUND_HALF_UP);
                assertEquals(expectedAveragePerVisit, report.getAverageRevenuePerVisit(), 
                            "Average revenue per visit should be calculated correctly");
            }
            
            // Verify average revenue per day calculation
            if (startDate != null && endDate != null && expectedTotalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
                if (daysBetween > 0) {
                    BigDecimal expectedAveragePerDay = expectedTotalRevenue.divide(
                        BigDecimal.valueOf(daysBetween), 2, BigDecimal.ROUND_HALF_UP);
                    assertEquals(expectedAveragePerDay, report.getAverageRevenuePerDay(), 
                                "Average revenue per day should be calculated correctly");
                }
            }
            
            // Verify daily revenue aggregation
            Map<LocalDate, BigDecimal> expectedDailyRevenue = paidVisits.stream()
                    .collect(Collectors.groupingBy(
                            v -> v.getVisitDate().toLocalDate(),
                            Collectors.reducing(BigDecimal.ZERO, Visit::getCost, BigDecimal::add)));
            
            Map<LocalDate, BigDecimal> actualDailyRevenue = report.getDailyRevenue();
            if (actualDailyRevenue != null && !expectedDailyRevenue.isEmpty()) {
                for (Map.Entry<LocalDate, BigDecimal> entry : expectedDailyRevenue.entrySet()) {
                    BigDecimal actualValue = actualDailyRevenue.get(entry.getKey());
                    if (actualValue != null) {
                        assertEquals(entry.getValue(), actualValue, 
                                    "Daily revenue should match for date: " + entry.getKey());
                    }
                }
                
                // Verify sum of daily revenues equals total revenue
                BigDecimal sumOfDailyRevenues = actualDailyRevenue.values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                assertEquals(expectedTotalRevenue, sumOfDailyRevenues, 
                            "Sum of daily revenues should equal total revenue");
            }
            
            // Verify revenue by veterinarian aggregation
            Map<String, BigDecimal> expectedRevenueByVet = paidVisits.stream()
                    .filter(v -> v.getVeterinarian() != null)
                    .collect(Collectors.groupingBy(
                            v -> v.getVeterinarian().getFullName(),
                            Collectors.reducing(BigDecimal.ZERO, Visit::getCost, BigDecimal::add)));
            
            Map<String, BigDecimal> actualRevenueByVet = report.getRevenueByVeterinarian();
            if (actualRevenueByVet != null && !expectedRevenueByVet.isEmpty()) {
                for (Map.Entry<String, BigDecimal> entry : expectedRevenueByVet.entrySet()) {
                    BigDecimal actualValue = actualRevenueByVet.get(entry.getKey());
                    if (actualValue != null) {
                        assertEquals(entry.getValue(), actualValue, 
                                    "Revenue by veterinarian should match for: " + entry.getKey());
                    }
                }
            }
            
            // Verify revenue by species aggregation
            Map<String, BigDecimal> expectedRevenueBySpecies = paidVisits.stream()
                    .filter(v -> v.getPet() != null && v.getPet().getSpecies() != null)
                    .collect(Collectors.groupingBy(
                            v -> v.getPet().getSpecies(),
                            Collectors.reducing(BigDecimal.ZERO, Visit::getCost, BigDecimal::add)));
            
            Map<String, BigDecimal> actualRevenueBySpecies = report.getRevenueBySpecies();
            if (actualRevenueBySpecies != null && !expectedRevenueBySpecies.isEmpty()) {
                for (Map.Entry<String, BigDecimal> entry : expectedRevenueBySpecies.entrySet()) {
                    BigDecimal actualValue = actualRevenueBySpecies.get(entry.getKey());
                    if (actualValue != null) {
                        assertEquals(entry.getValue(), actualValue, 
                                    "Revenue by species should match for: " + entry.getKey());
                    }
                }
            }
            
            // Verify revenue by visit type aggregation
            Map<String, BigDecimal> expectedRevenueByType = paidVisits.stream()
                    .filter(v -> v.getVisitType() != null)
                    .collect(Collectors.groupingBy(
                            v -> v.getVisitType().getDisplayName(),
                            Collectors.reducing(BigDecimal.ZERO, Visit::getCost, BigDecimal::add)));
            
            Map<String, BigDecimal> actualRevenueByType = report.getRevenueByVisitType();
            if (actualRevenueByType != null && !expectedRevenueByType.isEmpty()) {
                for (Map.Entry<String, BigDecimal> entry : expectedRevenueByType.entrySet()) {
                    BigDecimal actualValue = actualRevenueByType.get(entry.getKey());
                    if (actualValue != null) {
                        assertEquals(entry.getValue(), actualValue, 
                                    "Revenue by visit type should match for: " + entry.getKey());
                    }
                }
            }
            
            // Test revenue calculation consistency across different grouping methods
            List<RevenueReport> reportsByVet = reportService.generateRevenueReportByVeterinarian(startDate, endDate);
            BigDecimal totalRevenueFromVetReports = reportsByVet.stream()
                    .map(RevenueReport::getTotalRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Total revenue from veterinarian reports should not exceed overall total
            // (some visits might not have veterinarians assigned)
            assertTrue(totalRevenueFromVetReports.compareTo(expectedTotalRevenue) <= 0, 
                      "Total revenue from veterinarian reports should not exceed overall total");
            
            List<RevenueReport> reportsBySpecies = reportService.generateRevenueReportBySpecies(startDate, endDate);
            BigDecimal totalRevenueFromSpeciesReports = reportsBySpecies.stream()
                    .map(RevenueReport::getTotalRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Total revenue from species reports should equal overall total
            // (all visits should have pets with species)
            assertEquals(expectedTotalRevenue, totalRevenueFromSpeciesReports, 
                        "Total revenue from species reports should equal overall total");
            
            // Verify mathematical properties
            // Revenue should never be negative
            assertTrue(report.getTotalRevenue().compareTo(BigDecimal.ZERO) >= 0, 
                      "Total revenue should never be negative");
            
            // Average revenue per visit should never be negative
            assertTrue(report.getAverageRevenuePerVisit().compareTo(BigDecimal.ZERO) >= 0, 
                      "Average revenue per visit should never be negative");
            
            // Average revenue per day should never be negative
            assertTrue(report.getAverageRevenuePerDay().compareTo(BigDecimal.ZERO) >= 0, 
                      "Average revenue per day should never be negative");
            
            // Payment rate should be between 0 and 100
            assertTrue(report.getPaymentRate() >= 0.0 && report.getPaymentRate() <= 100.0, 
                      "Payment rate should be between 0 and 100 percent");
        });
    }
    
    /**
     * Property: Revenue Trend Calculation Accuracy
     * For any revenue report with trend data, trend calculations should be mathematically correct
     * **Validates: Requirements 5.2**
     */
    @Test
    void testRevenueTrendCalculationAccuracy() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Clear any existing data to ensure clean test
            visitRepository.deleteAll();
            petRepository.deleteAll();
            veterinarianRepository.deleteAll();
            ownerRepository.deleteAll();
            
            // Generate test data for current period
            LocalDate endDate = validReportStartDates().next();
            LocalDate startDate = endDate.minusDays(7 + random.nextInt(14)); // 7-20 days period
            
            // Generate test data for previous period (same length)
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
            LocalDate previousStart = startDate.minusDays(daysBetween + 1);
            LocalDate previousEnd = startDate.minusDays(1);
            
            // Create test entities
            List<Owner> owners = createTestOwners(2 + random.nextInt(3));
            List<Pet> pets = createTestPets(owners, 3 + random.nextInt(5));
            List<Veterinarian> veterinarians = createTestVeterinarians(2 + random.nextInt(3));
            
            // Create visits for both periods
            List<Visit> currentPeriodVisits = createTestVisitsWithCosts(pets, veterinarians, startDate, endDate, 3 + random.nextInt(10));
            List<Visit> previousPeriodVisits = createTestVisitsWithCosts(pets, veterinarians, previousStart, previousEnd, 3 + random.nextInt(10));
            
            // Generate revenue report with trends
            RevenueReport report = reportService.generateRevenueReport(startDate, endDate, true);
            
            // Calculate expected values
            BigDecimal currentRevenue = currentPeriodVisits.stream()
                    .filter(v -> v.getCost() != null && v.getCost().compareTo(BigDecimal.ZERO) > 0)
                    .map(Visit::getCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal previousRevenue = previousPeriodVisits.stream()
                    .filter(v -> v.getCost() != null && v.getCost().compareTo(BigDecimal.ZERO) > 0)
                    .map(Visit::getCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Verify current period revenue
            assertEquals(currentRevenue, report.getTotalRevenue(), 
                        "Current period revenue should be calculated correctly");
            
            // Verify trend data if available
            RevenueReport.TrendData trendData = report.getTrendData();
            if (trendData != null) {
                // Verify previous period revenue
                assertEquals(previousRevenue, trendData.getPreviousPeriodRevenue(), 
                            "Previous period revenue should be stored correctly in trend data");
                
                // Verify growth rate calculation
                if (previousRevenue.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal difference = currentRevenue.subtract(previousRevenue);
                    double expectedGrowthRate = difference.divide(previousRevenue, 4, BigDecimal.ROUND_HALF_UP)
                                                         .multiply(BigDecimal.valueOf(100)).doubleValue();
                    
                    assertEquals(expectedGrowthRate, trendData.getGrowthRate(), 0.01, 
                                "Growth rate should be calculated correctly");
                    
                    // Verify trend direction
                    if (Math.abs(expectedGrowthRate) < 1.0) {
                        assertEquals("STABLE", trendData.getTrendDirection(), 
                                    "Trend direction should be STABLE for small changes");
                    } else if (expectedGrowthRate > 0) {
                        assertEquals("UP", trendData.getTrendDirection(), 
                                    "Trend direction should be UP for positive growth");
                    } else {
                        assertEquals("DOWN", trendData.getTrendDirection(), 
                                    "Trend direction should be DOWN for negative growth");
                    }
                }
                
                // Verify trend description is not null and contains meaningful information
                assertNotNull(trendData.getTrendDescription(), 
                             "Trend description should not be null");
                assertFalse(trendData.getTrendDescription().trim().isEmpty(), 
                           "Trend description should not be empty");
            }
        });
    }
    
    // Generator methods for report-specific test data
    
    protected Generator<LocalDate> validReportStartDates() {
        return () -> {
            LocalDate now = LocalDate.now();
            LocalDate earliest = now.minusMonths(6);
            LocalDate latest = now.minusDays(7); // At least a week ago to allow for end dates
            long daysBetween = latest.toEpochDay() - earliest.toEpochDay();
            long randomDays = Math.abs(random.nextLong()) % (daysBetween + 1);
            return earliest.plusDays(randomDays);
        };
    }
    
    protected Generator<LocalDate> validReportEndDates(LocalDate startDate) {
        return () -> {
            LocalDate earliest = startDate.plusDays(1); // At least one day after start
            LocalDate latest = startDate.plusDays(30); // At most 30 days after start
            if (latest.isAfter(LocalDate.now())) {
                latest = LocalDate.now();
            }
            if (earliest.isAfter(latest)) {
                return startDate; // Same day if no valid range
            }
            long daysBetween = latest.toEpochDay() - earliest.toEpochDay();
            if (daysBetween <= 0) {
                return startDate;
            }
            long randomDays = Math.abs(random.nextLong()) % (daysBetween + 1);
            return earliest.plusDays(randomDays);
        };
    }
    
    protected Generator<BigDecimal> validVisitCosts() {
        return () -> {
            // Generate costs between $0 and $500, with some null values
            if (random.nextInt(10) == 0) {
                return null; // 10% chance of null cost
            }
            if (random.nextInt(5) == 0) {
                return BigDecimal.ZERO; // 20% chance of zero cost
            }
            double cost = random.nextDouble() * 500.0;
            return BigDecimal.valueOf(Math.round(cost * 100.0) / 100.0); // Round to 2 decimal places
        };
    }
    
    protected Generator<VisitType> validVisitTypes() {
        return () -> {
            VisitType[] types = VisitType.values();
            return types[random.nextInt(types.length)];
        };
    }
    
    protected Generator<String> validVisitNotes() {
        String[] notes = {
            "Regular checkup completed",
            "Patient responded well to treatment",
            "Follow-up recommended in 2 weeks",
            "CANCELLED - owner request",
            "CANCELLED - emergency",
            "Vaccination completed successfully",
            "Dental cleaning performed",
            "Surgery consultation completed",
            null, // Null notes
            "" // Empty notes
        };
        return () -> notes[random.nextInt(notes.length)];
    }
    
    // Helper methods for creating test data
    
    private List<Owner> createTestOwners(int count) {
        List<Owner> owners = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Owner owner = new Owner();
            owner.setFirstName(validOwnerNames().next());
            owner.setLastName(validOwnerNames().next());
            owner.setEmail(validEmails().next());
            owner.setAddress("123 Test Street " + i);
            owner.setCity("Test City");
            owner.setTelephone(validPhoneNumbers().next());
            owners.add(ownerRepository.save(owner));
        }
        return owners;
    }
    
    private List<Pet> createTestPets(List<Owner> owners, int count) {
        List<Pet> pets = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Pet pet = new Pet();
            pet.setName(validPetNames().next());
            pet.setSpecies(validSpecies().next());
            pet.setBirthDate(validBirthDates().next());
            pet.setOwner(owners.get(random.nextInt(owners.size())));
            pets.add(petRepository.save(pet));
        }
        return pets;
    }
    
    private List<Veterinarian> createTestVeterinarians(int count) {
        List<Veterinarian> veterinarians = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Veterinarian vet = new Veterinarian();
            vet.setFirstName(validVeterinarianFirstNames().next());
            vet.setLastName(validVeterinarianLastNames().next());
            vet.setLicenseNumber(validLicenseNumbers().next());
            vet.setSpecialties(validStringSpecialties().next());
            vet.setSpecialtySet(validEnumSpecialties().next());
            veterinarians.add(veterinarianRepository.save(vet));
        }
        return veterinarians;
    }
    
    private List<Visit> createTestVisits(List<Pet> pets, List<Veterinarian> veterinarians, 
                                       LocalDate startDate, LocalDate endDate, int count) {
        List<Visit> visits = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Visit visit = new Visit();
            
            // Generate random date within range
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
            long randomDays = daysBetween > 0 ? Math.abs(random.nextLong()) % (daysBetween + 1) : 0;
            LocalDate visitDate = startDate.plusDays(randomDays);
            LocalDateTime visitDateTime = visitDate.atTime(9 + random.nextInt(8), random.nextInt(60)); // 9 AM - 5 PM
            
            visit.setVisitDate(visitDateTime);
            visit.setVisitType(validVisitTypes().next());
            visit.setPet(pets.get(random.nextInt(pets.size())));
            visit.setVeterinarian(veterinarians.get(random.nextInt(veterinarians.size())));
            visit.setNotes(validVisitNotes().next());
            
            // Set diagnosis and treatment for some visits to make them completed
            if (random.nextBoolean()) {
                visit.setDiagnosis("Test diagnosis " + i);
                visit.setTreatment("Test treatment " + i);
            }
            
            visits.add(visitRepository.save(visit));
        }
        return visits;
    }
    
    private List<Visit> createTestVisitsWithCosts(List<Pet> pets, List<Veterinarian> veterinarians, 
                                                LocalDate startDate, LocalDate endDate, int count) {
        List<Visit> visits = createTestVisits(pets, veterinarians, startDate, endDate, count);
        
        // Add costs to visits
        for (Visit visit : visits) {
            visit.setCost(validVisitCosts().next());
            visitRepository.save(visit);
        }
        
        return visits;
    }
    
    // Additional generator methods from existing property tests
    
    protected Generator<String> validVeterinarianFirstNames() {
        String[] names = {"John", "Jane", "Michael", "Sarah", "David", "Emily", "Robert", "Lisa", "James", "Maria"};
        return () -> names[random.nextInt(names.length)];
    }
    
    protected Generator<String> validVeterinarianLastNames() {
        String[] names = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"};
        return () -> names[random.nextInt(names.length)];
    }
    
    protected Generator<String> validStringSpecialties() {
        String[] specialties = {
            "General Practice",
            "Surgery, Cardiology",
            "Dermatology",
            "Orthopedics, Surgery",
            "Emergency Medicine",
            "Internal Medicine, Cardiology",
            "Neurology",
            "Oncology, Surgery",
            "Ophthalmology",
            "Dentistry, General Practice",
            "" // Empty string for general practitioners
        };
        return () -> specialties[random.nextInt(specialties.length)];
    }
    
    protected Generator<Set<Specialty>> validEnumSpecialties() {
        return () -> {
            Set<Specialty> specialties = new HashSet<>();
            Specialty[] allSpecialties = Specialty.values();
            
            // Generate 0-3 random specialties
            int count = random.nextInt(4);
            for (int i = 0; i < count; i++) {
                specialties.add(allSpecialties[random.nextInt(allSpecialties.length)]);
            }
            
            return specialties;
        };
    }
}