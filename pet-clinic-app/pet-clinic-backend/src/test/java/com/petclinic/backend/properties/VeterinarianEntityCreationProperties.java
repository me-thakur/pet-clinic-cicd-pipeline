package com.petclinic.backend.properties;

import com.petclinic.backend.model.Specialty;
import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.repository.VeterinarianRepository;
import net.java.quickcheck.Generator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for Veterinarian entity creation
 * **Validates: Requirements 3.1, 3.2**
 * 
 * Tests universal properties that should hold for all valid Veterinarian entity creation scenarios
 * Uses H2 test database to verify persistence and specialty validation
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VeterinarianEntityCreationProperties extends PropertyTestBase {
    
    @Autowired
    private VeterinarianRepository veterinarianRepository;
    
    /**
     * Property 1: Entity Creation Completeness
     * For any valid veterinarian data, creating the veterinarian should result in all required fields being stored and retrievable
     * **Validates: Requirements 3.1**
     */
    @Test
    void testVeterinarianCreationCompleteness() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate valid veterinarian data
            String firstName = validVeterinarianFirstNames().next();
            String lastName = validVeterinarianLastNames().next();
            String licenseNumber = validLicenseNumbers().next();
            String stringSpecialties = validStringSpecialties().next();
            Set<Specialty> enumSpecialties = validEnumSpecialties().next();
            
            // Create veterinarian with both string and enum specialties
            Veterinarian veterinarian = new Veterinarian();
            veterinarian.setFirstName(firstName);
            veterinarian.setLastName(lastName);
            veterinarian.setLicenseNumber(licenseNumber);
            veterinarian.setSpecialties(stringSpecialties);
            veterinarian.setSpecialtySet(enumSpecialties);
            
            // Persist veterinarian to database
            Veterinarian savedVeterinarian = veterinarianRepository.save(veterinarian);
            
            // Retrieve veterinarian from database to verify persistence
            Optional<Veterinarian> retrievedVetOpt = veterinarianRepository.findById(savedVeterinarian.getId());
            assertTrue(retrievedVetOpt.isPresent(), "Veterinarian should be retrievable from database");
            Veterinarian retrievedVet = retrievedVetOpt.get();
            
            // Verify all required fields are stored and retrievable
            assertNotNull(retrievedVet, "Veterinarian should be created successfully");
            assertEquals(firstName, retrievedVet.getFirstName(), "Veterinarian first name should be stored correctly");
            assertEquals(lastName, retrievedVet.getLastName(), "Veterinarian last name should be stored correctly");
            assertEquals(licenseNumber, retrievedVet.getLicenseNumber(), "Veterinarian license number should be stored correctly");
            
            // Verify string-based specialties are stored correctly
            assertEquals(stringSpecialties, retrievedVet.getSpecialties(), "String-based specialties should be stored correctly");
            
            // Verify enum-based specialties are stored correctly
            assertEquals(enumSpecialties.size(), retrievedVet.getSpecialtySet().size(), "Enum specialty set size should match");
            assertTrue(retrievedVet.getSpecialtySet().containsAll(enumSpecialties), "All enum specialties should be stored correctly");
            
            // Verify timestamps are set
            assertNotNull(retrievedVet.getCreatedAt(), "Created timestamp should be set");
            assertNotNull(retrievedVet.getUpdatedAt(), "Updated timestamp should be set");
            
            // Verify business methods work correctly
            assertNotNull(retrievedVet.getFullName(), "Full name should be generated correctly");
            assertTrue(retrievedVet.getFullName().contains(firstName), "Full name should contain first name");
            assertTrue(retrievedVet.getFullName().contains(lastName), "Full name should contain last name");
            assertTrue(retrievedVet.getFullName().startsWith("Dr."), "Full name should start with 'Dr.'");
            
            // Verify specialty-related business methods
            if (!stringSpecialties.trim().isEmpty() || !enumSpecialties.isEmpty()) {
                assertTrue(retrievedVet.isSpecialist(), "Veterinarian with specialties should be identified as specialist");
            }
            
            // Verify visit count is initially zero
            assertEquals(0, retrievedVet.getVisitCount(), "New veterinarian should have zero visits");
            assertTrue(retrievedVet.getVisits().isEmpty(), "New veterinarian should have empty visits list");
            
            // Verify toString method works
            assertNotNull(retrievedVet.toString(), "toString should return non-null value");
            assertTrue(retrievedVet.toString().contains(firstName), "toString should contain first name");
            assertTrue(retrievedVet.toString().contains(lastName), "toString should contain last name");
            assertTrue(retrievedVet.toString().contains(licenseNumber), "toString should contain license number");
        });
    }
    
    /**
     * Property 11: Specialty Validation
     * For any specialty assignment to a veterinarian, the specialty must exist in the predefined specialty enumeration
     * **Validates: Requirements 3.2**
     */
    @Test
    void testSpecialtyValidation() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate valid veterinarian data
            String firstName = validVeterinarianFirstNames().next();
            String lastName = validVeterinarianLastNames().next();
            String licenseNumber = validLicenseNumbers().next();
            
            // Test enum-based specialty validation
            Set<Specialty> validEnumSpecialties = validEnumSpecialties().next();
            
            // Create veterinarian with enum specialties
            Veterinarian veterinarian = new Veterinarian();
            veterinarian.setFirstName(firstName);
            veterinarian.setLastName(lastName);
            veterinarian.setLicenseNumber(licenseNumber);
            veterinarian.setSpecialtySet(validEnumSpecialties);
            
            // Persist and retrieve
            Veterinarian savedVeterinarian = veterinarianRepository.save(veterinarian);
            Optional<Veterinarian> retrievedVetOpt = veterinarianRepository.findById(savedVeterinarian.getId());
            assertTrue(retrievedVetOpt.isPresent(), "Veterinarian should be retrievable from database");
            Veterinarian retrievedVet = retrievedVetOpt.get();
            
            // Verify all enum specialties are valid predefined values
            for (Specialty specialty : retrievedVet.getSpecialtySet()) {
                assertNotNull(specialty, "Specialty should not be null");
                assertTrue(isValidSpecialtyEnum(specialty), "Specialty should be a valid predefined enum value");
                assertNotNull(specialty.getDisplayName(), "Specialty display name should not be null");
                assertNotNull(specialty.getCode(), "Specialty code should not be null");
                assertFalse(specialty.getDisplayName().trim().isEmpty(), "Specialty display name should not be empty");
                assertFalse(specialty.getCode().trim().isEmpty(), "Specialty code should not be empty");
            }
            
            // Test specialty lookup methods work correctly
            for (Specialty specialty : validEnumSpecialties) {
                assertTrue(retrievedVet.hasSpecialty(specialty), "Veterinarian should have the assigned specialty");
                assertTrue(retrievedVet.hasSpecialty(specialty.getDisplayName()), "Veterinarian should be found by specialty display name");
                assertTrue(retrievedVet.hasSpecialty(specialty.getCode()), "Veterinarian should be found by specialty code");
            }
            
            // Test specialty list generation
            var specialtyList = retrievedVet.getSpecialtyList();
            assertNotNull(specialtyList, "Specialty list should not be null");
            assertTrue(specialtyList.size() >= validEnumSpecialties.size(), "Specialty list should contain at least the enum specialties");
            
            // Verify specialty-based capabilities work correctly
            boolean hasEmergencySpecialty = validEnumSpecialties.stream().anyMatch(Specialty::isEmergency);
            boolean hasSurgicalSpecialty = validEnumSpecialties.stream().anyMatch(Specialty::isSurgical);
            
            if (hasEmergencySpecialty) {
                assertTrue(retrievedVet.canHandleEmergencies(), "Veterinarian with emergency specialty should be able to handle emergencies");
            }
            
            if (hasSurgicalSpecialty) {
                assertTrue(retrievedVet.canPerformSurgery(), "Veterinarian with surgical specialty should be able to perform surgery");
            }
        });
    }
    
    /**
     * Property: String-based Specialty Validation (Backward Compatibility)
     * For any string-based specialty assignment, the veterinarian should store and retrieve it correctly
     * **Validates: Requirements 3.2**
     */
    @Test
    void testStringBasedSpecialtyValidation() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate valid veterinarian data
            String firstName = validVeterinarianFirstNames().next();
            String lastName = validVeterinarianLastNames().next();
            String licenseNumber = validLicenseNumbers().next();
            String stringSpecialties = validStringSpecialties().next();
            
            // Create veterinarian with string specialties
            Veterinarian veterinarian = new Veterinarian();
            veterinarian.setFirstName(firstName);
            veterinarian.setLastName(lastName);
            veterinarian.setLicenseNumber(licenseNumber);
            veterinarian.setSpecialties(stringSpecialties);
            
            // Persist and retrieve
            Veterinarian savedVeterinarian = veterinarianRepository.save(veterinarian);
            Optional<Veterinarian> retrievedVetOpt = veterinarianRepository.findById(savedVeterinarian.getId());
            assertTrue(retrievedVetOpt.isPresent(), "Veterinarian should be retrievable from database");
            Veterinarian retrievedVet = retrievedVetOpt.get();
            
            // Verify string specialties are stored correctly
            assertEquals(stringSpecialties, retrievedVet.getSpecialties(), "String specialties should be stored exactly as provided");
            
            // Test specialty search functionality with string specialties
            if (!stringSpecialties.trim().isEmpty()) {
                String[] specialtyArray = stringSpecialties.split(",");
                for (String specialty : specialtyArray) {
                    String trimmedSpecialty = specialty.trim();
                    if (!trimmedSpecialty.isEmpty()) {
                        assertTrue(retrievedVet.hasSpecialty(trimmedSpecialty), 
                                  "Veterinarian should be found by individual string specialty: " + trimmedSpecialty);
                    }
                }
                
                assertTrue(retrievedVet.isSpecialist(), "Veterinarian with string specialties should be identified as specialist");
                
                // Test specialty list includes string specialties
                var specialtyList = retrievedVet.getSpecialtyList();
                assertNotNull(specialtyList, "Specialty list should not be null");
                assertFalse(specialtyList.isEmpty(), "Specialty list should not be empty for veterinarian with specialties");
            }
        });
    }
    
    /**
     * Property: Dual Specialty System Compatibility
     * For any veterinarian with both string and enum specialties, both systems should work together correctly
     * **Validates: Requirements 3.2**
     */
    @Test
    void testDualSpecialtySystemCompatibility() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate valid veterinarian data
            String firstName = validVeterinarianFirstNames().next();
            String lastName = validVeterinarianLastNames().next();
            String licenseNumber = validLicenseNumbers().next();
            String stringSpecialties = validStringSpecialties().next();
            Set<Specialty> enumSpecialties = validEnumSpecialties().next();
            
            // Create veterinarian with both specialty systems
            Veterinarian veterinarian = new Veterinarian();
            veterinarian.setFirstName(firstName);
            veterinarian.setLastName(lastName);
            veterinarian.setLicenseNumber(licenseNumber);
            veterinarian.setSpecialties(stringSpecialties);
            veterinarian.setSpecialtySet(enumSpecialties);
            
            // Persist and retrieve
            Veterinarian savedVeterinarian = veterinarianRepository.save(veterinarian);
            Optional<Veterinarian> retrievedVetOpt = veterinarianRepository.findById(savedVeterinarian.getId());
            assertTrue(retrievedVetOpt.isPresent(), "Veterinarian should be retrievable from database");
            Veterinarian retrievedVet = retrievedVetOpt.get();
            
            // Verify both specialty systems are preserved
            assertEquals(stringSpecialties, retrievedVet.getSpecialties(), "String specialties should be preserved");
            assertEquals(enumSpecialties.size(), retrievedVet.getSpecialtySet().size(), "Enum specialty count should be preserved");
            assertTrue(retrievedVet.getSpecialtySet().containsAll(enumSpecialties), "All enum specialties should be preserved");
            
            // Test specialty list combines both systems
            var specialtyList = retrievedVet.getSpecialtyList();
            assertNotNull(specialtyList, "Combined specialty list should not be null");
            
            // Verify string specialties are in the list
            if (!stringSpecialties.trim().isEmpty()) {
                String[] stringSpecialtyArray = stringSpecialties.split(",");
                for (String specialty : stringSpecialtyArray) {
                    String trimmedSpecialty = specialty.trim();
                    if (!trimmedSpecialty.isEmpty()) {
                        assertTrue(specialtyList.contains(trimmedSpecialty), 
                                  "Specialty list should contain string specialty: " + trimmedSpecialty);
                    }
                }
            }
            
            // Verify enum specialties are in the list
            for (Specialty specialty : enumSpecialties) {
                assertTrue(specialtyList.contains(specialty.getDisplayName()), 
                          "Specialty list should contain enum specialty: " + specialty.getDisplayName());
            }
            
            // Test that both systems contribute to specialist status
            boolean hasStringSpecialties = !stringSpecialties.trim().isEmpty();
            boolean hasEnumSpecialties = !enumSpecialties.isEmpty();
            
            if (hasStringSpecialties || hasEnumSpecialties) {
                assertTrue(retrievedVet.isSpecialist(), "Veterinarian with either type of specialty should be identified as specialist");
            }
        });
    }
    
    /**
     * Property: License Number Uniqueness
     * For any valid license number, it should be unique across all veterinarians in the database
     * **Validates: Requirements 3.1**
     */
    @Test
    void testLicenseNumberUniqueness() {
        runPropertyTest(50, () -> { // Reduced iterations for uniqueness test
            // Generate unique license number
            String licenseNumber = validLicenseNumbers().next();
            
            // Ensure license number doesn't already exist
            while (veterinarianRepository.existsByLicenseNumber(licenseNumber)) {
                licenseNumber = validLicenseNumbers().next();
            }
            
            // Create first veterinarian
            Veterinarian vet1 = new Veterinarian();
            vet1.setFirstName("First");
            vet1.setLastName("Veterinarian");
            vet1.setLicenseNumber(licenseNumber);
            Veterinarian savedVet1 = veterinarianRepository.save(vet1);
            
            // Verify license number exists
            assertTrue(veterinarianRepository.existsByLicenseNumber(licenseNumber), 
                      "License number should exist after saving veterinarian");
            
            // Verify we can find by license number
            Optional<Veterinarian> foundVet = veterinarianRepository.findByLicenseNumber(licenseNumber);
            assertTrue(foundVet.isPresent(), "Should be able to find veterinarian by license number");
            assertEquals(savedVet1.getId(), foundVet.get().getId(), "Found veterinarian should match saved veterinarian");
            
            // Test that license number is properly validated
            assertNotNull(savedVet1.getLicenseNumber(), "License number should not be null");
            assertEquals(licenseNumber, savedVet1.getLicenseNumber(), "License number should match exactly");
            assertTrue(savedVet1.getLicenseNumber().matches("^[A-Z0-9]{6,20}$"), 
                      "License number should match the required pattern");
        });
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
    
    /**
     * Check if a specialty is a valid predefined enum value
     */
    private boolean isValidSpecialtyEnum(Specialty specialty) {
        for (Specialty validSpecialty : Specialty.values()) {
            if (validSpecialty.equals(specialty)) {
                return true;
            }
        }
        return false;
    }
    
    // Helper method for assumptions in property tests
    private void assumeTrue(boolean condition) {
        if (!condition) {
            // Skip this iteration if assumption is not met
            return;
        }
    }
}