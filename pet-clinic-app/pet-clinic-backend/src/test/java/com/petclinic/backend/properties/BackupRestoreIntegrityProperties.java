package com.petclinic.backend.properties;

import com.petclinic.backend.model.*;
import com.petclinic.backend.repository.*;
import com.petclinic.backend.service.BackupService;
import net.java.quickcheck.Generator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for backup and restore integrity.
 * **Property 22: Backup and Restore Integrity**
 * **Validates: Requirements 8.5**
 * 
 * For any backup operation followed by restore, the restored data should be identical to the original data.
 */
@SpringBootTest
@ActiveProfiles("test")
class BackupRestoreIntegrityProperties extends PropertyTestBase {
    
    @Autowired
    private BackupService backupService;
    
    @Autowired
    private OwnerRepository ownerRepository;
    
    @Autowired
    private PetRepository petRepository;
    
    @Autowired
    private VeterinarianRepository veterinarianRepository;
    
    @Autowired
    private VisitRepository visitRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    void setUp() {
        super.setUp();
        // Clean database before each property test
        visitRepository.deleteAll();
        petRepository.deleteAll();
        veterinarianRepository.deleteAll();
        ownerRepository.deleteAll();
        userRepository.deleteAll();
    }
    
    @Test
    @Transactional
    void backupAndRestorePreservesOwnerData() {
        runPropertyTest(50, () -> {
            // Given: Generate and save owner data
            List<Owner> owners = generateOwners();
            List<Owner> savedOwners = ownerRepository.saveAll(owners);
            long originalCount = ownerRepository.count();
            
            // When: Create backup and restore
            File backupFile = backupService.createBackup("property_test_owners");
            assertTrue(backupService.validateBackup(backupFile), "Backup should be valid");
            
            // Clear data and restore
            ownerRepository.deleteAll();
            assertEquals(0, ownerRepository.count(), "Data should be cleared");
            
            backupService.restoreFromBackup(backupFile);
            
            // Then: Verify data integrity
            assertEquals(originalCount, ownerRepository.count(), "Owner count should match after restore");
            
            List<Owner> restoredOwners = ownerRepository.findAll();
            assertEquals(savedOwners.size(), restoredOwners.size(), "Number of owners should match");
            
            // Verify each owner's data
            for (Owner original : savedOwners) {
                Owner restored = restoredOwners.stream()
                        .filter(o -> o.getFirstName().equals(original.getFirstName()) && 
                                   o.getLastName().equals(original.getLastName()))
                        .findFirst()
                        .orElse(null);
                
                assertNotNull(restored, "Restored owner should exist: " + original.getFirstName() + " " + original.getLastName());
                assertEquals(original.getAddress(), restored.getAddress(), "Address should match");
                assertEquals(original.getCity(), restored.getCity(), "City should match");
                assertEquals(original.getTelephone(), restored.getTelephone(), "Telephone should match");
            }
            
            // Clean up
            backupService.deleteBackup(backupFile);
            
            // Clean for next iteration
            ownerRepository.deleteAll();
        });
    }
    
    @Test
    @Transactional
    void backupAndRestorePreservesPetData() {
        runPropertyTest(50, () -> {
            // Given: Generate and save pet data with owners
            List<Owner> owners = generateOwners();
            ownerRepository.saveAll(owners);
            
            List<Pet> pets = generatePetsWithOwners(owners);
            List<Pet> savedPets = petRepository.saveAll(pets);
            long originalCount = petRepository.count();
            
            // When: Create backup and restore
            File backupFile = backupService.createBackup("property_test_pets");
            assertTrue(backupService.validateBackup(backupFile), "Backup should be valid");
            
            // Clear data and restore
            petRepository.deleteAll();
            ownerRepository.deleteAll();
            assertEquals(0, petRepository.count(), "Pet data should be cleared");
            
            backupService.restoreFromBackup(backupFile);
            
            // Then: Verify data integrity
            assertEquals(originalCount, petRepository.count(), "Pet count should match after restore");
            
            List<Pet> restoredPets = petRepository.findAll();
            assertEquals(savedPets.size(), restoredPets.size(), "Number of pets should match");
            
            // Verify each pet's data
            for (Pet original : savedPets) {
                Pet restored = restoredPets.stream()
                        .filter(p -> p.getName().equals(original.getName()) && 
                                   p.getSpecies().equals(original.getSpecies()))
                        .findFirst()
                        .orElse(null);
                
                assertNotNull(restored, "Restored pet should exist: " + original.getName());
                assertEquals(original.getBreed(), restored.getBreed(), "Breed should match");
                assertEquals(original.getBirthDate(), restored.getBirthDate(), "Birth date should match");
                
                // Verify owner relationship is preserved
                if (original.getOwner() != null) {
                    assertNotNull(restored.getOwner(), "Owner relationship should be preserved");
                    assertEquals(original.getOwner().getFirstName(), restored.getOwner().getFirstName(), 
                               "Owner first name should match");
                    assertEquals(original.getOwner().getLastName(), restored.getOwner().getLastName(), 
                               "Owner last name should match");
                }
            }
            
            // Clean up
            backupService.deleteBackup(backupFile);
            
            // Clean for next iteration
            petRepository.deleteAll();
            ownerRepository.deleteAll();
        });
    }
    
    @Test
    @Transactional
    void backupAndRestorePreservesVeterinarianData() {
        runPropertyTest(50, () -> {
            // Given: Generate and save veterinarian data
            List<Veterinarian> veterinarians = generateVeterinarians();
            List<Veterinarian> savedVets = veterinarianRepository.saveAll(veterinarians);
            long originalCount = veterinarianRepository.count();
            
            // When: Create backup and restore
            File backupFile = backupService.createBackup("property_test_vets");
            assertTrue(backupService.validateBackup(backupFile), "Backup should be valid");
            
            // Clear data and restore
            veterinarianRepository.deleteAll();
            assertEquals(0, veterinarianRepository.count(), "Veterinarian data should be cleared");
            
            backupService.restoreFromBackup(backupFile);
            
            // Then: Verify data integrity
            assertEquals(originalCount, veterinarianRepository.count(), "Veterinarian count should match after restore");
            
            List<Veterinarian> restoredVets = veterinarianRepository.findAll();
            assertEquals(savedVets.size(), restoredVets.size(), "Number of veterinarians should match");
            
            // Verify each veterinarian's data
            for (Veterinarian original : savedVets) {
                Veterinarian restored = restoredVets.stream()
                        .filter(v -> v.getFirstName().equals(original.getFirstName()) && 
                                   v.getLastName().equals(original.getLastName()) &&
                                   v.getLicenseNumber().equals(original.getLicenseNumber()))
                        .findFirst()
                        .orElse(null);
                
                assertNotNull(restored, "Restored veterinarian should exist: " + original.getFirstName() + " " + original.getLastName());
                assertEquals(original.getSpecialtySet(), restored.getSpecialtySet(), "Specialties should match");
            }
            
            // Clean up
            backupService.deleteBackup(backupFile);
            
            // Clean for next iteration
            veterinarianRepository.deleteAll();
        });
    }
    
    @Test
    @Transactional
    void backupAndRestorePreservesCompleteDataSet() {
        runPropertyTest(30, () -> {
            // Given: Generate complete data set with relationships
            List<Owner> owners = generateOwners();
            List<Owner> savedOwners = ownerRepository.saveAll(owners);
            
            List<Veterinarian> veterinarians = generateVeterinarians();
            List<Veterinarian> savedVets = veterinarianRepository.saveAll(veterinarians);
            
            List<Pet> pets = generatePetsWithOwners(savedOwners);
            List<Pet> savedPets = petRepository.saveAll(pets);
            
            List<Visit> visits = generateVisitsWithPetsAndVets(savedPets, savedVets);
            List<Visit> savedVisits = visitRepository.saveAll(visits);
            
            long originalOwnerCount = ownerRepository.count();
            long originalVetCount = veterinarianRepository.count();
            long originalPetCount = petRepository.count();
            long originalVisitCount = visitRepository.count();
            
            // When: Create backup and restore
            File backupFile = backupService.createBackup("property_test_complete");
            assertTrue(backupService.validateBackup(backupFile), "Backup should be valid");
            
            // Clear all data
            visitRepository.deleteAll();
            petRepository.deleteAll();
            veterinarianRepository.deleteAll();
            ownerRepository.deleteAll();
            
            assertEquals(0, ownerRepository.count(), "All data should be cleared");
            assertEquals(0, veterinarianRepository.count(), "All data should be cleared");
            assertEquals(0, petRepository.count(), "All data should be cleared");
            assertEquals(0, visitRepository.count(), "All data should be cleared");
            
            backupService.restoreFromBackup(backupFile);
            
            // Then: Verify complete data integrity
            assertEquals(originalOwnerCount, ownerRepository.count(), "Owner count should match");
            assertEquals(originalVetCount, veterinarianRepository.count(), "Veterinarian count should match");
            assertEquals(originalPetCount, petRepository.count(), "Pet count should match");
            assertEquals(originalVisitCount, visitRepository.count(), "Visit count should match");
            
            // Verify relationships are preserved
            List<Visit> restoredVisits = visitRepository.findAll();
            for (Visit originalVisit : savedVisits) {
                Visit restoredVisit = restoredVisits.stream()
                        .filter(v -> v.getVisitDate().equals(originalVisit.getVisitDate()) &&
                                   v.getVisitType() == originalVisit.getVisitType())
                        .findFirst()
                        .orElse(null);
                
                assertNotNull(restoredVisit, "Visit should be restored");
                
                // Verify pet relationship
                if (originalVisit.getPet() != null) {
                    assertNotNull(restoredVisit.getPet(), "Pet relationship should be preserved");
                    assertEquals(originalVisit.getPet().getName(), restoredVisit.getPet().getName(), 
                               "Pet name should match");
                }
                
                // Verify veterinarian relationship
                if (originalVisit.getVeterinarian() != null) {
                    assertNotNull(restoredVisit.getVeterinarian(), "Veterinarian relationship should be preserved");
                    assertEquals(originalVisit.getVeterinarian().getLicenseNumber(), 
                               restoredVisit.getVeterinarian().getLicenseNumber(), 
                               "Veterinarian license should match");
                }
            }
            
            // Clean up
            backupService.deleteBackup(backupFile);
            
            // Clean for next iteration
            visitRepository.deleteAll();
            petRepository.deleteAll();
            veterinarianRepository.deleteAll();
            ownerRepository.deleteAll();
        });
    }
    
    @Test
    void backupValidationDetectsCorruptedFiles() {
        runPropertyTest(20, () -> {
            // Given: Create a valid backup
            List<Owner> owners = generateOwners();
            ownerRepository.saveAll(owners);
            File backupFile = backupService.createBackup("validation_test");
            
            // Verify original backup is valid
            assertTrue(backupService.validateBackup(backupFile), "Original backup should be valid");
            
            // When: Simulate file corruption by creating an invalid file with same name
            File corruptedFile = new File(backupFile.getParent(), "corrupted_" + backupFile.getName());
            try {
                corruptedFile.createNewFile();
                // Write invalid content
                java.nio.file.Files.write(corruptedFile.toPath(), "This is not a valid SQL backup".getBytes());
                
                // Then: Validation should detect corruption
                assertFalse(backupService.validateBackup(corruptedFile), "Corrupted backup should be invalid");
                
            } catch (Exception e) {
                fail("Test setup failed: " + e.getMessage());
            } finally {
                // Clean up
                backupService.deleteBackup(backupFile);
                if (corruptedFile.exists()) {
                    corruptedFile.delete();
                }
                ownerRepository.deleteAll();
            }
        });
    }
    
    // Data generators using the existing framework
    
    private List<Owner> generateOwners() {
        List<Owner> owners = new ArrayList<>();
        int count = 1 + random.nextInt(4); // 1-4 owners
        
        for (int i = 0; i < count; i++) {
            Owner owner = new Owner();
            owner.setFirstName(validOwnerNames().next());
            owner.setLastName(validOwnerNames().next());
            owner.setAddress(generateAddress());
            owner.setCity(generateCity());
            owner.setTelephone(validPhoneNumbers().next());
            owners.add(owner);
        }
        
        return owners;
    }
    
    private List<Pet> generatePetsWithOwners(List<Owner> owners) {
        List<Pet> pets = new ArrayList<>();
        int count = 1 + random.nextInt(3); // 1-3 pets
        
        for (int i = 0; i < count; i++) {
            Pet pet = new Pet();
            pet.setName(validPetNames().next());
            pet.setSpecies(validSpecies().next());
            pet.setBreed(validBreeds().next());
            pet.setBirthDate(validBirthDates().next());
            pet.setOwner(owners.get(random.nextInt(owners.size())));
            pets.add(pet);
        }
        
        return pets;
    }
    
    private List<Veterinarian> generateVeterinarians() {
        List<Veterinarian> vets = new ArrayList<>();
        int count = 1 + random.nextInt(3); // 1-3 veterinarians
        
        for (int i = 0; i < count; i++) {
            Veterinarian vet = new Veterinarian();
            vet.setFirstName("Dr. " + validOwnerNames().next());
            vet.setLastName(validOwnerNames().next());
            vet.setLicenseNumber(validLicenseNumbers().next());
            vet.setSpecialtySet(generateSpecialties());
            vets.add(vet);
        }
        
        return vets;
    }
    
    private List<Visit> generateVisitsWithPetsAndVets(List<Pet> pets, List<Veterinarian> vets) {
        List<Visit> visits = new ArrayList<>();
        int count = 1 + random.nextInt(2); // 1-2 visits
        
        for (int i = 0; i < count; i++) {
            Visit visit = new Visit();
            visit.setVisitDate(validVisitDates().next());
            visit.setVisitType(VisitType.valueOf(generateVisitType()));
            visit.setPet(pets.get(random.nextInt(pets.size())));
            visit.setVeterinarian(vets.get(random.nextInt(vets.size())));
            visits.add(visit);
        }
        
        return visits;
    }
    
    private String generateAddress() {
        String[] streets = {"Main St", "Oak Ave", "Pine Rd", "Elm Dr", "Maple Ln"};
        int number = 100 + random.nextInt(900);
        String street = streets[random.nextInt(streets.length)];
        return number + " " + street;
    }
    
    private String generateCity() {
        String[] cities = {"Springfield", "Shelbyville", "Capital City", "Ogdenville", "North Haverbrook"};
        return cities[random.nextInt(cities.length)];
    }
    
    private Set<Specialty> generateSpecialties() {
        Specialty[] specialties = Specialty.values();
        Set<Specialty> result = new java.util.HashSet<>();
        int count = 1 + random.nextInt(2); // 1-2 specialties
        
        for (int i = 0; i < count; i++) {
            result.add(specialties[random.nextInt(specialties.length)]);
        }
        
        return result;
    }
    
    private String generateVisitType() {
        VisitType[] types = {VisitType.WELLNESS_EXAM, VisitType.VACCINATION, VisitType.EMERGENCY, VisitType.FOLLOW_UP};
        return types[random.nextInt(types.length)].name();
    }
}