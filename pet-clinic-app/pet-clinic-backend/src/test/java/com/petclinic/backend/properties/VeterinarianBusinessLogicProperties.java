package com.petclinic.backend.properties;

import com.petclinic.backend.exception.BusinessRuleException;
import com.petclinic.backend.exception.EntityNotFoundException;
import com.petclinic.backend.exception.ValidationException;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.model.Specialty;
import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.model.VisitType;
import com.petclinic.backend.repository.OwnerRepository;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.repository.VeterinarianRepository;
import com.petclinic.backend.repository.VisitRepository;
import com.petclinic.backend.service.VeterinarianService;
import net.java.quickcheck.Generator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for Veterinarian business logic
 * **Validates: Requirements 3.3, 3.4, 3.5**
 * 
 * Tests universal properties that should hold for all valid Veterinarian business logic scenarios
 * Uses H2 test database with proper Spring Boot test configuration
 * Comprehensive coverage with randomized valid inputs and integration testing
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Veterinarian Business Logic Property-Based Tests")
class VeterinarianBusinessLogicProperties extends PropertyTestBase {
    
    @Autowired
    private VeterinarianService veterinarianService;
    
    @Autowired
    private VeterinarianRepository veterinarianRepository;
    
    @Autowired
    private OwnerRepository ownerRepository;
    
    @Autowired
    private PetRepository petRepository;
    
    @Autowired
    private VisitRepository visitRepository;
    
    private Owner testOwner;
    private Pet testPet;
    
    @BeforeEach
    void setUp() {
        super.setUp();
        
        // Create and save test owner with unique email for each test
        testOwner = new Owner();
        testOwner.setFirstName("Test");
        testOwner.setLastName("Owner");
        testOwner.setEmail("test.owner." + System.currentTimeMillis() + "." + random.nextInt(1000) + "@test.com");
        testOwner.setAddress("123 Test Street");
        testOwner.setCity("Test City");
        testOwner.setTelephone(validPhoneNumbers().next());
        testOwner = ownerRepository.save(testOwner);
        
        // Create and save test pet
        testPet = new Pet();
        testPet.setName("TestPet" + System.currentTimeMillis());
        testPet.setSpecies("Dog");
        testPet.setBreed("Labrador");
        testPet.setBirthDate(validBirthDates().next());
        testPet.setOwner(testOwner);
        testPet = petRepository.save(testPet);
    }
    
    /**
     * Property 9: Specialty-Based Filtering
     * For any visit type requiring specific specialties, only veterinarians with matching specialties 
     * should be available for selection
     * **Validates: Requirements 3.3**
     */
    @Test
    @DisplayName("Property 9: Specialty-Based Filtering - Only veterinarians with matching specialties should be available for specific visit types")
    void testSpecialtyBasedFiltering() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Given: Create veterinarians with different specialties
            String uniqueId = String.valueOf(System.currentTimeMillis() + random.nextInt(10000));
            
            // Create veterinarian with surgery specialty
            Veterinarian surgicalVet = createTestVeterinarian("Surgical" + uniqueId, "Vet", 
                Set.of(Specialty.SURGERY, Specialty.ORTHOPEDICS));
            Veterinarian savedSurgicalVet = veterinarianService.create(surgicalVet);
            
            // Create veterinarian with cardiology specialty
            Veterinarian cardiologyVet = createTestVeterinarian("Cardiology" + uniqueId, "Vet", 
                Set.of(Specialty.CARDIOLOGY));
            Veterinarian savedCardiologyVet = veterinarianService.create(cardiologyVet);
            
            // Create general practitioner
            Veterinarian generalVet = createTestVeterinarian("General" + uniqueId, "Vet", 
                Set.of(Specialty.GENERAL_PRACTICE));
            Veterinarian savedGeneralVet = veterinarianService.create(generalVet);
            
            // Create veterinarian with emergency specialty
            Veterinarian emergencyVet = createTestVeterinarian("Emergency" + uniqueId, "Vet", 
                Set.of(Specialty.EMERGENCY_MEDICINE, Specialty.SURGERY));
            Veterinarian savedEmergencyVet = veterinarianService.create(emergencyVet);
            
            // Test specialty-based filtering for different specialties
            String[] testSpecialties = {"Surgery", "Cardiology", "General Practice", "Emergency Medicine"};
            
            for (String specialty : testSpecialties) {
                // When: Searching for veterinarians by specialty
                List<Veterinarian> filteredVets = veterinarianService.findBySpecialty(specialty);
                
                // Then: All returned veterinarians should have the requested specialty
                for (Veterinarian vet : filteredVets) {
                    boolean hasSpecialty = vet.hasSpecialty(specialty) || 
                                         vet.getSpecialtySet().stream()
                                           .anyMatch(s -> s.getDisplayName().equalsIgnoreCase(specialty));
                    
                    assertTrue(hasSpecialty, 
                        String.format("Veterinarian '%s' (ID: %d) returned by specialty filter '%s' should have that specialty. " +
                                     "Specialties: %s, SpecialtySet: %s",
                            vet.getFullName(), vet.getId(), specialty, 
                            vet.getSpecialties(), vet.getSpecialtySet()));
                }
                
                // Verify specific expected matches based on our test data
                if (specialty.equalsIgnoreCase("Surgery")) {
                    boolean foundSurgicalVet = filteredVets.stream()
                        .anyMatch(v -> v.getId().equals(savedSurgicalVet.getId()));
                    boolean foundEmergencyVet = filteredVets.stream()
                        .anyMatch(v -> v.getId().equals(savedEmergencyVet.getId()));
                    
                    assertTrue(foundSurgicalVet || foundEmergencyVet,
                              "Surgery specialty filter should find veterinarians with surgery specialty");
                }
                
                if (specialty.equalsIgnoreCase("Cardiology")) {
                    boolean foundCardiologyVet = filteredVets.stream()
                        .anyMatch(v -> v.getId().equals(savedCardiologyVet.getId()));
                    
                    if (!filteredVets.isEmpty()) {
                        // If we found results, at least one should be our cardiology vet
                        assertTrue(foundCardiologyVet,
                                  "Cardiology specialty filter should find the cardiology veterinarian");
                    }
                }
                
                // Verify no false positives - vets without the specialty shouldn't be included
                for (Veterinarian vet : filteredVets) {
                    // Skip if this is one of our test vets that should be included
                    if (vet.getId().equals(savedSurgicalVet.getId()) && specialty.equalsIgnoreCase("Surgery")) continue;
                    if (vet.getId().equals(savedCardiologyVet.getId()) && specialty.equalsIgnoreCase("Cardiology")) continue;
                    if (vet.getId().equals(savedGeneralVet.getId()) && specialty.equalsIgnoreCase("General Practice")) continue;
                    if (vet.getId().equals(savedEmergencyVet.getId()) && 
                        (specialty.equalsIgnoreCase("Emergency Medicine") || specialty.equalsIgnoreCase("Surgery"))) continue;
                    
                    // For other vets, verify they actually have the specialty
                    assertTrue(vet.hasSpecialty(specialty) || 
                              vet.getSpecialtySet().stream().anyMatch(s -> s.getDisplayName().equalsIgnoreCase(specialty)),
                              "Veterinarian " + vet.getFullName() + " should not be returned for specialty " + specialty);
                }
            }
            
            // Test empty results for non-existent specialty
            String nonExistentSpecialty = "NonExistentSpecialty" + uniqueId;
            List<Veterinarian> emptyResults = veterinarianService.findBySpecialty(nonExistentSpecialty);
            assertTrue(emptyResults.isEmpty(), 
                      "Search for non-existent specialty should return empty results");
        });
    }
    
    /**
     * Property 10: Availability Tracking
     * For any veterinarian and time period, availability should be calculated correctly based on 
     * working hours and existing appointments
     * **Validates: Requirements 3.4**
     */
    @Test
    @DisplayName("Property 10: Availability Tracking - Availability should be calculated correctly based on working hours and appointments")
    void testAvailabilityTracking() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Given: Create veterinarian
            String uniqueId = String.valueOf(System.currentTimeMillis() + random.nextInt(10000));
            Veterinarian veterinarian = createTestVeterinarian("Available" + uniqueId, "Vet", 
                Set.of(Specialty.GENERAL_PRACTICE));
            Veterinarian savedVet = veterinarianService.create(veterinarian);
            
            // Generate test times during working hours (Monday-Friday, 8 AM - 6 PM)
            LocalDateTime workingHourTime = generateWorkingHourTime();
            LocalDateTime nonWorkingHourTime = generateNonWorkingHourTime();
            LocalDateTime futureWorkingTime = generateFutureWorkingHourTime();
            
            // Test availability during working hours without conflicts
            boolean availableDuringWorkingHours = veterinarianService.isVeterinarianAvailable(
                savedVet.getId(), workingHourTime);
            
            // Should be available during working hours if no conflicts
            assertTrue(availableDuringWorkingHours, 
                String.format("Veterinarian should be available during working hours (%s) when no conflicts exist", 
                    workingHourTime));
            
            // Test availability during non-working hours
            boolean availableDuringNonWorkingHours = veterinarianService.isVeterinarianAvailable(
                savedVet.getId(), nonWorkingHourTime);
            
            // Should not be available during non-working hours (unless emergency vet)
            if (!savedVet.canHandleEmergencies()) {
                assertFalse(availableDuringNonWorkingHours, 
                    String.format("Non-emergency veterinarian should not be available during non-working hours (%s)", 
                        nonWorkingHourTime));
            }
            
            // Create a conflicting visit
            Visit conflictingVisit = new Visit();
            conflictingVisit.setVisitDate(futureWorkingTime);
            conflictingVisit.setNotes("Conflicting appointment");
            conflictingVisit.setVisitType(VisitType.WELLNESS_EXAM);
            conflictingVisit.setCost(BigDecimal.valueOf(75.00));
            conflictingVisit.setPet(testPet);
            conflictingVisit.setVeterinarian(savedVet);
            
            // Save the conflicting visit
            visitRepository.save(conflictingVisit);
            
            // Test availability at the same time as the conflicting visit
            boolean availableWithConflict = veterinarianService.isVeterinarianAvailable(
                savedVet.getId(), futureWorkingTime);
            
            // Should not be available when there's a scheduling conflict
            assertFalse(availableWithConflict, 
                String.format("Veterinarian should not be available at time (%s) when there's a conflicting appointment", 
                    futureWorkingTime));
            
            // Test availability 1 hour after the conflicting visit
            LocalDateTime afterConflict = futureWorkingTime.plusHours(1);
            boolean availableAfterConflict = veterinarianService.isVeterinarianAvailable(
                savedVet.getId(), afterConflict);
            
            // Should be available after the conflict if still within working hours
            if (isWithinWorkingHours(afterConflict) || savedVet.canHandleEmergencies()) {
                assertTrue(availableAfterConflict, 
                    String.format("Veterinarian should be available after conflicting appointment (%s)", 
                        afterConflict));
            }
            
            // Test findAvailable method
            List<Veterinarian> availableVets = veterinarianService.findAvailable(workingHourTime);
            
            // All returned veterinarians should be available at the requested time
            for (Veterinarian vet : availableVets) {
                boolean isAvailable = veterinarianService.isVeterinarianAvailable(vet.getId(), workingHourTime);
                assertTrue(isAvailable, 
                    String.format("Veterinarian '%s' returned by findAvailable should actually be available at %s", 
                        vet.getFullName(), workingHourTime));
            }
            
            // Test availability with null parameters
            assertFalse(veterinarianService.isVeterinarianAvailable(null, workingHourTime),
                       "Availability check with null veterinarian ID should return false");
            
            assertFalse(veterinarianService.isVeterinarianAvailable(savedVet.getId(), null),
                       "Availability check with null date time should return false");
            
            // Test availability for non-existent veterinarian
            Long nonExistentVetId = 999999L;
            assertFalse(veterinarianService.isVeterinarianAvailable(nonExistentVetId, workingHourTime),
                       "Availability check for non-existent veterinarian should return false");
        });
    }
    
    /**
     * Property 3: Referential Integrity Protection
     * For any entity with dependent relationships (Pet with Visits, Veterinarian with Visits), 
     * deletion attempts should be handled according to the database constraints
     * **Validates: Requirements 3.5**
     */
    @Test
    @DisplayName("Property 3: Referential Integrity Protection - Veterinarian deletion should handle visit relationships correctly")
    void testReferentialIntegrityProtection() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Given: Create veterinarians with and without visits
            String uniqueId = String.valueOf(System.currentTimeMillis() + random.nextInt(10000));
            
            Veterinarian vetWithoutVisits = createTestVeterinarian("NoVisits" + uniqueId, "Vet", 
                Set.of(Specialty.GENERAL_PRACTICE));
            Veterinarian savedVetWithoutVisits = veterinarianService.create(vetWithoutVisits);
            
            Veterinarian vetWithVisits = createTestVeterinarian("WithVisits" + uniqueId, "Vet", 
                Set.of(Specialty.GENERAL_PRACTICE));
            Veterinarian savedVetWithVisits = veterinarianService.create(vetWithVisits);
            
            // Add one or more visits to the veterinarian
            int numberOfVisits = 1 + random.nextInt(3); // 1-3 visits
            for (int i = 0; i < numberOfVisits; i++) {
                Visit visit = new Visit();
                visit.setVisitDate(validVisitDates().next());
                visit.setNotes(validDescriptions().next());
                visit.setVisitType(VisitType.values()[random.nextInt(VisitType.values().length)]);
                visit.setCost(BigDecimal.valueOf(50.00 + random.nextDouble() * 200.00));
                visit.setPet(testPet);
                visit.setVeterinarian(savedVetWithVisits);
                
                // Save the visit
                visitRepository.save(visit);
            }
            
            // Reload veterinarian to get updated visits
            Veterinarian vetWithVisitsReloaded = veterinarianRepository.findById(savedVetWithVisits.getId())
                .orElseThrow();
            
            // Verify the veterinarian has visits by checking the database directly
            List<Visit> veterinarianVisits = visitRepository.findByVeterinarianIdOrderByVisitDateDesc(savedVetWithVisits.getId());
            assertFalse(veterinarianVisits.isEmpty(), 
                       "Veterinarian should have visits");
            assertEquals(numberOfVisits, veterinarianVisits.size(),
                        "Veterinarian should have the expected number of visits");
            
            // When & Then: Veterinarian without visits should be deletable
            assertTrue(veterinarianService.canDeleteVeterinarian(savedVetWithoutVisits.getId()), 
                      "Veterinarian without visits should be deletable");
            
            assertDoesNotThrow(() -> veterinarianService.deleteById(savedVetWithoutVisits.getId()),
                              "Deleting veterinarian without visits should not throw exception");
            
            // Verify veterinarian was actually deleted
            Optional<Veterinarian> deletedVetWithoutVisits = veterinarianService.findById(savedVetWithoutVisits.getId());
            assertFalse(deletedVetWithoutVisits.isPresent(), 
                       "Veterinarian without visits should be deleted from database");
            
            // When & Then: Test referential integrity behavior for veterinarian with visits
            // The database foreign key constraint should prevent deletion
            
            DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> veterinarianService.deleteById(savedVetWithVisits.getId()),
                "Deleting veterinarian with visits should throw DataIntegrityViolationException due to foreign key constraint");
            
            assertTrue(exception.getMessage().contains("Referential integrity constraint violation"),
                      "Exception message should indicate referential integrity constraint violation");
            assertTrue(exception.getMessage().contains("FOREIGN KEY"),
                      "Exception message should mention foreign key constraint");
            
            // Verify veterinarian still exists after failed deletion
            Optional<Veterinarian> existingVet = veterinarianService.findById(savedVetWithVisits.getId());
            assertTrue(existingVet.isPresent(), 
                      "Veterinarian with visits should still exist in database after failed deletion");
            
            // Verify visits are still associated with the veterinarian after failed deletion
            List<Visit> visitsAfterFailedDeletion = visitRepository.findByVeterinarianIdOrderByVisitDateDesc(savedVetWithVisits.getId());
            assertEquals(numberOfVisits, visitsAfterFailedDeletion.size(),
                        "All visits should still be associated with the veterinarian after failed deletion");
            
            for (Visit visit : visitsAfterFailedDeletion) {
                assertEquals(savedVetWithVisits.getId(), visit.getVeterinarian().getId(),
                           "Visit should still reference the correct veterinarian");
                assertNotNull(visit.getPet(), "Visit should still have associated pet");
                assertEquals(testPet.getId(), visit.getPet().getId(),
                           "Visit should reference the correct pet");
            }
            
            // Test edge cases
            
            // Test canDeleteVeterinarian with null ID
            assertFalse(veterinarianService.canDeleteVeterinarian(null),
                       "canDeleteVeterinarian with null ID should return false");
            
            // Test canDeleteVeterinarian with non-existent ID
            Long nonExistentId = 999999L;
            assertFalse(veterinarianService.canDeleteVeterinarian(nonExistentId),
                       "canDeleteVeterinarian with non-existent ID should return false");
            
            // Test deleteById with non-existent ID
            EntityNotFoundException notFoundException = assertThrows(EntityNotFoundException.class,
                () -> veterinarianService.deleteById(nonExistentId),
                "Deleting non-existent veterinarian should throw EntityNotFoundException");
            
            assertTrue(notFoundException.getMessage().contains("Veterinarian"),
                      "Exception message should mention Veterinarian entity");
            assertTrue(notFoundException.getMessage().contains(nonExistentId.toString()),
                      "Exception message should include the non-existent ID");
        });
    }
    
    // Helper methods for generating test data
    
    private Veterinarian createTestVeterinarian(String firstName, String lastName, Set<Specialty> specialties) {
        Veterinarian vet = new Veterinarian();
        vet.setFirstName(firstName);
        vet.setLastName(lastName);
        vet.setLicenseNumber(validLicenseNumbers().next());
        vet.setSpecialtySet(specialties);
        return vet;
    }
    
    private LocalDateTime generateWorkingHourTime() {
        // Generate time during working hours (Monday-Friday, 8 AM - 6 PM)
        LocalDate date = LocalDate.now().plusDays(1 + random.nextInt(30)); // 1-30 days from now
        
        // Ensure it's a weekday
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        
        // Generate time between 8 AM and 5 PM (to avoid edge cases)
        int hour = 8 + random.nextInt(9); // 8-16 (8 AM to 4 PM)
        int minute = random.nextInt(60);
        
        return LocalDateTime.of(date, LocalTime.of(hour, minute));
    }
    
    private LocalDateTime generateNonWorkingHourTime() {
        // Generate time during non-working hours
        LocalDate date = LocalDate.now().plusDays(1 + random.nextInt(30));
        
        LocalTime time;
        if (random.nextBoolean()) {
            // Weekend
            while (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                date = date.plusDays(1);
            }
            time = LocalTime.of(8 + random.nextInt(10), random.nextInt(60)); // Any time on weekend
        } else {
            // Weekday but outside working hours
            while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                date = date.plusDays(1);
            }
            
            if (random.nextBoolean()) {
                // Early morning (before 8 AM)
                time = LocalTime.of(random.nextInt(8), random.nextInt(60));
            } else {
                // Evening (after 6 PM)
                time = LocalTime.of(18 + random.nextInt(6), random.nextInt(60));
            }
        }
        
        return LocalDateTime.of(date, time);
    }
    
    private LocalDateTime generateFutureWorkingHourTime() {
        // Generate time in the future during working hours for scheduling conflicts
        LocalDate date = LocalDate.now().plusDays(7 + random.nextInt(30)); // 1-5 weeks from now
        
        // Ensure it's a weekday
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        
        // Generate time between 9 AM and 4 PM
        int hour = 9 + random.nextInt(7); // 9-15 (9 AM to 3 PM)
        int minute = random.nextInt(60);
        
        return LocalDateTime.of(date, LocalTime.of(hour, minute));
    }
    
    private boolean isWithinWorkingHours(LocalDateTime dateTime) {
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        LocalTime time = dateTime.toLocalTime();
        
        // Standard working hours: Monday-Friday, 8 AM - 6 PM
        boolean isWeekday = dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
        boolean isWorkingTime = !time.isBefore(LocalTime.of(8, 0)) && !time.isAfter(LocalTime.of(18, 0));
        
        return isWeekday && isWorkingTime;
    }
    
    // Generator methods for veterinarian-specific data
    
    protected Generator<String> validVeterinarianFirstNames() {
        String[] names = {"John", "Jane", "Michael", "Sarah", "David", "Emily", "Robert", "Lisa", "James", "Maria"};
        return () -> names[random.nextInt(names.length)];
    }
    
    protected Generator<String> validVeterinarianLastNames() {
        String[] names = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"};
        return () -> names[random.nextInt(names.length)];
    }
    
    protected Generator<Set<Specialty>> validSpecialtySet() {
        return () -> {
            Set<Specialty> specialties = new HashSet<>();
            Specialty[] allSpecialties = Specialty.values();
            
            // Generate 1-3 random specialties
            int count = 1 + random.nextInt(3);
            for (int i = 0; i < count; i++) {
                specialties.add(allSpecialties[random.nextInt(allSpecialties.length)]);
            }
            
            return specialties;
        };
    }
}