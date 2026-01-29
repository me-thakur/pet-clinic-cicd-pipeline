package com.petclinic.backend.properties;

import com.petclinic.backend.model.*;
import com.petclinic.backend.repository.OwnerRepository;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.repository.VeterinarianRepository;
import com.petclinic.backend.repository.VisitRepository;
import net.java.quickcheck.Generator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for Visit entity creation
 * **Validates: Requirements 2.1, 2.5**
 * 
 * Tests universal properties that should hold for all valid Visit entity creation scenarios
 * Uses H2 test database to verify persistence and Visit-Pet-Veterinarian relationship mapping
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VisitEntityCreationProperties extends PropertyTestBase {
    
    @Autowired
    private VisitRepository visitRepository;
    
    @Autowired
    private PetRepository petRepository;
    
    @Autowired
    private VeterinarianRepository veterinarianRepository;
    
    @Autowired
    private OwnerRepository ownerRepository;
    
    /**
     * Property 1: Entity Creation Completeness
     * For any valid visit data, creating the visit should result in all required fields being stored and retrievable
     * **Validates: Requirements 2.1**
     */
    @Test
    void testVisitCreationCompleteness() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate valid visit data
            LocalDateTime visitDate = validVisitDates().next();
            VisitType visitType = validVisitTypes().next();
            String diagnosis = validDiagnoses().next();
            String treatment = validTreatments().next();
            String notes = validNotes().next();
            BigDecimal cost = validCosts().next();
            
            // Create and persist owner (required for pet)
            Owner owner = new Owner();
            owner.setFirstName(validOwnerNames().next());
            owner.setLastName(validOwnerNames().next());
            owner.setEmail(validEmails().next());
            owner.setAddress("123 Test Street");
            owner.setCity("Test City");
            owner.setTelephone(validPhoneNumbers().next());
            Owner savedOwner = ownerRepository.save(owner);
            
            // Create and persist pet (required for visit)
            Pet pet = new Pet();
            pet.setName(validPetNames().next());
            pet.setSpecies(validSpecies().next());
            pet.setBirthDate(validBirthDates().next());
            pet.setOwner(savedOwner);
            Pet savedPet = petRepository.save(pet);
            
            // Create and persist veterinarian (required for visit)
            Veterinarian veterinarian = new Veterinarian();
            veterinarian.setFirstName(validVeterinarianFirstNames().next());
            veterinarian.setLastName(validVeterinarianLastNames().next());
            veterinarian.setLicenseNumber(validLicenseNumbers().next());
            veterinarian.setSpecialties(validStringSpecialties().next());
            veterinarian.setSpecialtySet(validEnumSpecialties().next());
            Veterinarian savedVeterinarian = veterinarianRepository.save(veterinarian);
            
            // Create visit with generated data
            Visit visit = new Visit();
            visit.setVisitDate(visitDate);
            visit.setVisitType(visitType);
            visit.setDiagnosis(diagnosis);
            visit.setTreatment(treatment);
            visit.setNotes(notes);
            visit.setCost(cost);
            visit.setPet(savedPet);
            visit.setVeterinarian(savedVeterinarian);
            
            // Persist visit to database
            Visit savedVisit = visitRepository.save(visit);
            
            // Retrieve visit from database to verify persistence
            Optional<Visit> retrievedVisitOpt = visitRepository.findById(savedVisit.getId());
            assertTrue(retrievedVisitOpt.isPresent(), "Visit should be retrievable from database");
            Visit retrievedVisit = retrievedVisitOpt.get();
            
            // Verify all required fields are stored and retrievable
            assertNotNull(retrievedVisit, "Visit should be created successfully");
            assertEquals(visitDate, retrievedVisit.getVisitDate(), "Visit date should be stored correctly");
            assertEquals(visitType, retrievedVisit.getVisitType(), "Visit type should be stored correctly");
            assertEquals(diagnosis, retrievedVisit.getDiagnosis(), "Diagnosis should be stored correctly");
            assertEquals(treatment, retrievedVisit.getTreatment(), "Treatment should be stored correctly");
            assertEquals(notes, retrievedVisit.getNotes(), "Notes should be stored correctly");
            assertEquals(0, cost.compareTo(retrievedVisit.getCost()), "Cost should be stored correctly");
            
            // Verify Pet relationship mapping
            assertNotNull(retrievedVisit.getPet(), "Visit pet should be stored correctly");
            assertEquals(savedPet.getId(), retrievedVisit.getPet().getId(), "Visit should reference correct pet ID");
            assertEquals(savedPet.getName(), retrievedVisit.getPet().getName(), "Pet name should match");
            assertEquals(savedPet.getSpecies(), retrievedVisit.getPet().getSpecies(), "Pet species should match");
            
            // Verify Veterinarian relationship mapping
            assertNotNull(retrievedVisit.getVeterinarian(), "Visit veterinarian should be stored correctly");
            assertEquals(savedVeterinarian.getId(), retrievedVisit.getVeterinarian().getId(), "Visit should reference correct veterinarian ID");
            assertEquals(savedVeterinarian.getFirstName(), retrievedVisit.getVeterinarian().getFirstName(), "Veterinarian first name should match");
            assertEquals(savedVeterinarian.getLastName(), retrievedVisit.getVeterinarian().getLastName(), "Veterinarian last name should match");
            assertEquals(savedVeterinarian.getLicenseNumber(), retrievedVisit.getVeterinarian().getLicenseNumber(), "Veterinarian license number should match");
            
            // Verify BaseEntity fields (id, createdAt, updatedAt) are properly initialized
            assertNotNull(retrievedVisit.getId(), "Visit ID should be set");
            assertNotNull(retrievedVisit.getCreatedAt(), "Created timestamp should be set");
            assertNotNull(retrievedVisit.getUpdatedAt(), "Updated timestamp should be set");
            assertTrue(retrievedVisit.getId() > 0, "Visit ID should be positive");
            
            // Verify business methods work correctly
            assertNotNull(retrievedVisit.getVisitSummary(), "Visit summary should be generated");
            assertTrue(retrievedVisit.getVisitSummary().contains(visitDate.toLocalDate().toString()), "Visit summary should contain visit date");
            
            if (diagnosis != null && !diagnosis.trim().isEmpty() && treatment != null && !treatment.trim().isEmpty()) {
                assertTrue(retrievedVisit.isCompleted(), "Visit with diagnosis and treatment should be marked as completed");
            }
            
            if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
                assertTrue(retrievedVisit.hasCost(), "Visit with positive cost should be identified as having cost");
            }
            
            // Verify visit type-specific business logic
            if (visitType != null) {
                assertEquals(visitType.isEmergency(), retrievedVisit.isEmergencyVisit(), "Emergency status should match visit type");
                assertEquals(visitType.isPreventiveCare(), retrievedVisit.isPreventiveCare(), "Preventive care status should match visit type");
                assertEquals(visitType.requiresAnesthesia(), retrievedVisit.requiresAnesthesia(), "Anesthesia requirement should match visit type");
                assertEquals(visitType.requiresSpecialist(), retrievedVisit.requiresSpecialist(), "Specialist requirement should match visit type");
                assertEquals(visitType.getRecommendedSpecialty(), retrievedVisit.getRecommendedSpecialty(), "Recommended specialty should match visit type");
            }
            
            // Verify toString method works
            assertNotNull(retrievedVisit.toString(), "toString should return non-null value");
            assertTrue(retrievedVisit.toString().contains(retrievedVisit.getId().toString()), "toString should contain visit ID");
        });
    }
    
    /**
     * Property 8: Visit Duration Calculation
     * For any visit type, the system should automatically calculate and assign the correct duration based on predefined visit type rules
     * **Validates: Requirements 2.5**
     */
    @Test
    void testVisitDurationCalculation() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate valid visit data with specific visit type
            LocalDateTime visitDate = validVisitDates().next();
            VisitType visitType = validVisitTypes().next();
            
            // Create and persist owner and pet (required for visit)
            Owner owner = createAndSaveOwner();
            Pet pet = createAndSavePet(owner);
            Veterinarian veterinarian = createAndSaveVeterinarian();
            
            // Test 1: Visit created with visit type should automatically get default duration
            Visit visitWithType = new Visit(visitDate, visitType, pet);
            visitWithType.setVeterinarian(veterinarian);
            
            // Verify duration is set based on visit type
            assertEquals(visitType.getDefaultDurationMinutes(), visitWithType.getDuration(), 
                        "Duration should be automatically set to visit type default");
            
            // Persist and retrieve to verify duration calculation persists
            Visit savedVisitWithType = visitRepository.save(visitWithType);
            Optional<Visit> retrievedVisitWithTypeOpt = visitRepository.findById(savedVisitWithType.getId());
            assertTrue(retrievedVisitWithTypeOpt.isPresent(), "Visit with type should be retrievable from database");
            Visit retrievedVisitWithType = retrievedVisitWithTypeOpt.get();
            
            assertEquals(visitType.getDefaultDurationMinutes(), retrievedVisitWithType.getDuration(), 
                        "Duration should persist correctly based on visit type");
            
            // Test 2: Setting visit type on existing visit should update duration (if not already set)
            Visit visitWithoutType = new Visit();
            visitWithoutType.setVisitDate(visitDate);
            visitWithoutType.setPet(pet);
            visitWithoutType.setVeterinarian(veterinarian);
            
            // Initially no duration set
            assertNull(visitWithoutType.getDuration(), "Duration should be null initially");
            
            // Set visit type - should automatically set duration
            visitWithoutType.setVisitType(visitType);
            assertEquals(visitType.getDefaultDurationMinutes(), visitWithoutType.getDuration(), 
                        "Setting visit type should automatically set duration");
            
            // Test 3: Custom duration can override the default when explicitly set
            Integer customDuration = validCustomDurations().next();
            Visit visitWithCustomDuration = new Visit(visitDate, visitType, pet);
            visitWithCustomDuration.setVeterinarian(veterinarian);
            visitWithCustomDuration.setDuration(customDuration); // Override default
            
            assertEquals(customDuration, visitWithCustomDuration.getDuration(), 
                        "Custom duration should override visit type default");
            
            // Persist and verify custom duration is preserved
            Visit savedCustomVisit = visitRepository.save(visitWithCustomDuration);
            Optional<Visit> retrievedCustomVisitOpt = visitRepository.findById(savedCustomVisit.getId());
            assertTrue(retrievedCustomVisitOpt.isPresent(), "Visit with custom duration should be retrievable");
            Visit retrievedCustomVisit = retrievedCustomVisitOpt.get();
            
            assertEquals(customDuration, retrievedCustomVisit.getDuration(), 
                        "Custom duration should persist correctly");
            
            // Test 4: Duration calculation works for all VisitType enum values
            for (VisitType testVisitType : VisitType.values()) {
                Visit testVisit = new Visit(visitDate, testVisitType, pet);
                testVisit.setVeterinarian(veterinarian);
                
                assertEquals(testVisitType.getDefaultDurationMinutes(), testVisit.getDuration(), 
                            "Duration should match default for visit type: " + testVisitType);
                
                // Verify duration is positive and reasonable
                assertTrue(testVisit.getDuration() > 0, 
                          "Duration should be positive for visit type: " + testVisitType);
                assertTrue(testVisit.getDuration() <= 480, // 8 hours max
                          "Duration should be reasonable (≤ 8 hours) for visit type: " + testVisitType);
            }
            
            // Test 5: getEndTime() method correctly calculates end time based on visitDate + duration
            Visit visitForEndTime = new Visit(visitDate, visitType, pet);
            visitForEndTime.setVeterinarian(veterinarian);
            
            LocalDateTime expectedEndTime = visitDate.plusMinutes(visitType.getDefaultDurationMinutes());
            assertEquals(expectedEndTime, visitForEndTime.getEndTime(), 
                        "End time should be calculated as visit date + duration");
            
            // Test with custom duration
            visitForEndTime.setDuration(customDuration);
            LocalDateTime expectedCustomEndTime = visitDate.plusMinutes(customDuration);
            assertEquals(expectedCustomEndTime, visitForEndTime.getEndTime(), 
                        "End time should be calculated correctly with custom duration");
            
            // Test edge case: null duration should return visit date
            visitForEndTime.setDuration(null);
            assertEquals(visitDate, visitForEndTime.getEndTime(), 
                        "End time should return visit date when duration is null");
        });
    }
    
    /**
     * Property: Visit Type Validation and Business Logic
     * For any visit type assignment, all visit type-related business logic should work correctly
     * **Validates: Requirements 2.1, 2.5**
     */
    @Test
    void testVisitTypeValidationAndBusinessLogic() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate valid visit data
            LocalDateTime visitDate = validVisitDates().next();
            VisitType visitType = validVisitTypes().next();
            
            // Create required entities
            Owner owner = createAndSaveOwner();
            Pet pet = createAndSavePet(owner);
            Veterinarian veterinarian = createAndSaveVeterinarian();
            
            // Create visit with visit type
            Visit visit = new Visit(visitDate, visitType, pet);
            visit.setVeterinarian(veterinarian);
            
            // Persist and retrieve
            Visit savedVisit = visitRepository.save(visit);
            Optional<Visit> retrievedVisitOpt = visitRepository.findById(savedVisit.getId());
            assertTrue(retrievedVisitOpt.isPresent(), "Visit should be retrievable from database");
            Visit retrievedVisit = retrievedVisitOpt.get();
            
            // Verify visit type is stored correctly
            assertEquals(visitType, retrievedVisit.getVisitType(), "Visit type should be stored correctly");
            
            // Verify all visit type business logic methods work correctly
            assertEquals(visitType.isEmergency(), retrievedVisit.isEmergencyVisit(), 
                        "Emergency status should match visit type");
            assertEquals(visitType.isPreventiveCare(), retrievedVisit.isPreventiveCare(), 
                        "Preventive care status should match visit type");
            assertEquals(visitType.requiresAnesthesia(), retrievedVisit.requiresAnesthesia(), 
                        "Anesthesia requirement should match visit type");
            assertEquals(visitType.requiresSpecialist(), retrievedVisit.requiresSpecialist(), 
                        "Specialist requirement should match visit type");
            assertEquals(visitType.getRecommendedSpecialty(), retrievedVisit.getRecommendedSpecialty(), 
                        "Recommended specialty should match visit type");
            
            // Verify visit type display name is accessible
            assertNotNull(visitType.getDisplayName(), "Visit type should have display name");
            assertFalse(visitType.getDisplayName().trim().isEmpty(), "Visit type display name should not be empty");
            
            // Verify visit type duration is reasonable
            assertTrue(visitType.getDefaultDurationMinutes() > 0, "Visit type duration should be positive");
            assertTrue(visitType.getDefaultDurationMinutes() <= 480, "Visit type duration should be reasonable (≤ 8 hours)");
            
            // Verify visit summary includes visit type information
            String visitSummary = retrievedVisit.getVisitSummary();
            assertNotNull(visitSummary, "Visit summary should not be null");
            assertTrue(visitSummary.contains(visitType.getDisplayName()), 
                      "Visit summary should contain visit type display name");
        });
    }
    
    /**
     * Property: Visit Relationship Integrity
     * For any valid visit with pet and veterinarian relationships, the relationships should be bidirectional and consistent
     * **Validates: Requirements 2.1**
     */
    @Test
    void testVisitRelationshipIntegrity() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Create required entities
            Owner owner = createAndSaveOwner();
            Pet pet = createAndSavePet(owner);
            Veterinarian veterinarian = createAndSaveVeterinarian();
            
            // Generate visit data
            LocalDateTime visitDate = validVisitDates().next();
            VisitType visitType = validVisitTypes().next();
            
            // Create visit with relationships
            Visit visit = new Visit(visitDate, visitType, pet);
            visit.setVeterinarian(veterinarian);
            visit.setDiagnosis(validDiagnoses().next());
            visit.setTreatment(validTreatments().next());
            
            // Persist visit
            Visit savedVisit = visitRepository.save(visit);
            
            // Retrieve and verify relationships
            Optional<Visit> retrievedVisitOpt = visitRepository.findById(savedVisit.getId());
            assertTrue(retrievedVisitOpt.isPresent(), "Visit should be retrievable from database");
            Visit retrievedVisit = retrievedVisitOpt.get();
            
            // Verify Pet relationship
            assertNotNull(retrievedVisit.getPet(), "Visit should have pet relationship");
            assertEquals(pet.getId(), retrievedVisit.getPet().getId(), "Pet ID should match");
            assertEquals(pet.getName(), retrievedVisit.getPet().getName(), "Pet name should match");
            assertEquals(pet.getSpecies(), retrievedVisit.getPet().getSpecies(), "Pet species should match");
            
            // Verify Owner relationship through Pet
            assertNotNull(retrievedVisit.getPet().getOwner(), "Pet should have owner relationship");
            assertEquals(owner.getId(), retrievedVisit.getPet().getOwner().getId(), "Owner ID should match through pet");
            
            // Verify Veterinarian relationship
            assertNotNull(retrievedVisit.getVeterinarian(), "Visit should have veterinarian relationship");
            assertEquals(veterinarian.getId(), retrievedVisit.getVeterinarian().getId(), "Veterinarian ID should match");
            assertEquals(veterinarian.getFirstName(), retrievedVisit.getVeterinarian().getFirstName(), "Veterinarian first name should match");
            assertEquals(veterinarian.getLastName(), retrievedVisit.getVeterinarian().getLastName(), "Veterinarian last name should match");
            
            // Test repository queries work with relationships
            var visitsByPet = visitRepository.findByPetId(pet.getId());
            assertTrue(visitsByPet.stream().anyMatch(v -> v.getId().equals(savedVisit.getId())), 
                      "Visit should be findable by pet ID");
            
            var visitsByVeterinarian = visitRepository.findByVeterinarianId(veterinarian.getId());
            assertTrue(visitsByVeterinarian.stream().anyMatch(v -> v.getId().equals(savedVisit.getId())), 
                      "Visit should be findable by veterinarian ID");
            
            // Test date range queries
            LocalDateTime startRange = visitDate.minusHours(1);
            LocalDateTime endRange = visitDate.plusHours(1);
            var visitsInRange = visitRepository.findByVisitDateBetween(startRange, endRange);
            assertTrue(visitsInRange.stream().anyMatch(v -> v.getId().equals(savedVisit.getId())), 
                      "Visit should be findable in date range");
        });
    }
    
    // Generator methods for visit-specific data
    
    protected Generator<VisitType> validVisitTypes() {
        return () -> {
            VisitType[] types = VisitType.values();
            return types[random.nextInt(types.length)];
        };
    }
    
    protected Generator<String> validDiagnoses() {
        String[] diagnoses = {
            "Healthy - routine checkup",
            "Ear infection - bacterial",
            "Skin allergy - environmental",
            "Dental disease - tartar buildup",
            "Upper respiratory infection",
            "Gastrointestinal upset",
            "Arthritis - age-related",
            "Wound - minor laceration",
            "Eye irritation - conjunctivitis",
            "Vaccination reaction - mild",
            "Parasites - fleas detected",
            "Behavioral issues - anxiety",
            "" // Empty diagnosis for incomplete visits
        };
        return () -> diagnoses[random.nextInt(diagnoses.length)];
    }
    
    protected Generator<String> validTreatments() {
        String[] treatments = {
            "Prescribed antibiotics - 7 day course",
            "Topical medication applied",
            "Dental cleaning performed",
            "Wound cleaned and bandaged",
            "Eye drops prescribed",
            "Anti-inflammatory medication",
            "Dietary recommendations provided",
            "Follow-up appointment scheduled",
            "Vaccination administered",
            "Flea treatment applied",
            "Behavioral training recommended",
            "No treatment required",
            "" // Empty treatment for incomplete visits
        };
        return () -> treatments[random.nextInt(treatments.length)];
    }
    
    protected Generator<String> validNotes() {
        String[] notes = {
            "Patient was cooperative during examination",
            "Owner reported symptoms started 3 days ago",
            "Recommend follow-up in 2 weeks",
            "Patient showed improvement since last visit",
            "Owner educated on medication administration",
            "Discussed preventive care options",
            "Patient was anxious but manageable",
            "No adverse reactions observed",
            "Owner satisfied with treatment plan",
            "Referred to specialist for further evaluation",
            null, // Null notes
            "" // Empty notes
        };
        return () -> notes[random.nextInt(notes.length)];
    }
    
    protected Generator<BigDecimal> validCosts() {
        return () -> {
            // Generate costs between $0 and $500
            double cost = random.nextDouble() * 500.0;
            return BigDecimal.valueOf(Math.round(cost * 100.0) / 100.0); // Round to 2 decimal places
        };
    }
    
    protected Generator<Integer> validCustomDurations() {
        return () -> {
            int[] durations = {10, 15, 20, 25, 30, 45, 60, 75, 90, 105, 120, 150, 180, 240};
            return durations[random.nextInt(durations.length)];
        };
    }
    
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
    
    // Helper methods for creating and saving entities
    
    private Owner createAndSaveOwner() {
        Owner owner = new Owner();
        owner.setFirstName(validOwnerNames().next());
        owner.setLastName(validOwnerNames().next());
        owner.setEmail(validEmails().next());
        owner.setAddress("123 Test Street");
        owner.setCity("Test City");
        owner.setTelephone(validPhoneNumbers().next());
        return ownerRepository.save(owner);
    }
    
    private Pet createAndSavePet(Owner owner) {
        Pet pet = new Pet();
        pet.setName(validPetNames().next());
        pet.setSpecies(validSpecies().next());
        pet.setBirthDate(validBirthDates().next());
        pet.setOwner(owner);
        return petRepository.save(pet);
    }
    
    private Veterinarian createAndSaveVeterinarian() {
        Veterinarian veterinarian = new Veterinarian();
        veterinarian.setFirstName(validVeterinarianFirstNames().next());
        veterinarian.setLastName(validVeterinarianLastNames().next());
        veterinarian.setLicenseNumber(validLicenseNumbers().next());
        veterinarian.setSpecialties(validStringSpecialties().next());
        veterinarian.setSpecialtySet(validEnumSpecialties().next());
        return veterinarianRepository.save(veterinarian);
    }
    
    // Helper method for assumptions in property tests
    private void assumeTrue(boolean condition) {
        if (!condition) {
            // Skip this iteration if assumption is not met
            return;
        }
    }
}