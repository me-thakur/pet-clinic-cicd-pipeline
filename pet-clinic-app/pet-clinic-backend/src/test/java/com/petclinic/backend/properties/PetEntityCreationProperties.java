package com.petclinic.backend.properties;

import com.petclinic.backend.model.Owner;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.repository.OwnerRepository;
import com.petclinic.backend.repository.PetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for Pet entity creation
 * **Validates: Requirements 1.1**
 * 
 * Tests universal properties that should hold for all valid Pet entity creation scenarios
 * Uses H2 test database to verify persistence and Pet-Owner relationship mapping
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PetEntityCreationProperties extends PropertyTestBase {
    
    @Autowired
    private PetRepository petRepository;
    
    @Autowired
    private OwnerRepository ownerRepository;
    
    /**
     * Property 1: Entity Creation Completeness
     * For any valid pet data, creating the pet should result in all required fields being stored and retrievable
     * **Validates: Requirements 1.1**
     */
    @Test
    void testPetCreationCompleteness() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate valid pet data
            String name = validPetNames().next();
            String species = validSpecies().next();
            String breed = validBreeds().next();
            LocalDate birthDate = validBirthDates().next();
            
            // Create and persist owner (required for pet)
            Owner owner = new Owner();
            owner.setFirstName(validOwnerNames().next());
            owner.setLastName(validOwnerNames().next());
            owner.setEmail(validEmails().next());
            owner.setAddress("123 Test Street");
            owner.setCity("Test City");
            owner.setTelephone(validPhoneNumbers().next());
            Owner savedOwner = ownerRepository.save(owner);
            
            // Create pet with generated data
            Pet pet = new Pet(name, species, breed, birthDate, savedOwner);
            
            // Persist pet to database
            Pet savedPet = petRepository.save(pet);
            
            // Retrieve pet from database to verify persistence
            Optional<Pet> retrievedPetOpt = petRepository.findById(savedPet.getId());
            assertTrue(retrievedPetOpt.isPresent(), "Pet should be retrievable from database");
            Pet retrievedPet = retrievedPetOpt.get();
            
            // Verify all required fields are stored and retrievable
            assertNotNull(retrievedPet, "Pet should be created successfully");
            assertEquals(name, retrievedPet.getName(), "Pet name should be stored correctly");
            assertEquals(species, retrievedPet.getSpecies(), "Pet species should be stored correctly");
            assertEquals(breed, retrievedPet.getBreed(), "Pet breed should be stored correctly");
            assertEquals(birthDate, retrievedPet.getBirthDate(), "Pet birth date should be stored correctly");
            
            // Verify Pet-Owner relationship mapping
            assertNotNull(retrievedPet.getOwner(), "Pet owner should be stored correctly");
            assertEquals(savedOwner.getId(), retrievedPet.getOwner().getId(), "Pet should reference correct owner ID");
            assertEquals(savedOwner.getFirstName(), retrievedPet.getOwner().getFirstName(), "Owner first name should match");
            assertEquals(savedOwner.getLastName(), retrievedPet.getOwner().getLastName(), "Owner last name should match");
            assertEquals(savedOwner.getEmail(), retrievedPet.getOwner().getEmail(), "Owner email should match");
            
            // Verify timestamps are set
            assertNotNull(retrievedPet.getCreatedAt(), "Created timestamp should be set");
            assertNotNull(retrievedPet.getUpdatedAt(), "Updated timestamp should be set");
            
            // Verify business methods work correctly
            assertTrue(retrievedPet.getAge() >= 0, "Pet age should be non-negative");
            assertNotNull(retrievedPet.toString(), "toString should return non-null value");
            
            // Verify bidirectional relationship consistency
            savedOwner.addPet(retrievedPet);
            Owner updatedOwner = ownerRepository.save(savedOwner);
            assertTrue(updatedOwner.getPets().contains(retrievedPet), "Owner should contain the pet in their pets list");
            assertEquals(updatedOwner.getId(), retrievedPet.getOwner().getId(), "Pet should still reference the owner after adding to owner's list");
        });
    }
    
    /**
     * Property: Pet Name Validation with Database Persistence
     * For any valid pet name, the pet should accept and store it correctly in the database
     */
    @Test
    void testPetNameValidationWithPersistence() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String name = validPetNames().next();
            
            // Ensure name meets validation criteria
            assumeTrue(name != null && !name.trim().isEmpty() && name.length() <= 50);
            
            // Create and persist owner
            Owner owner = new Owner();
            owner.setFirstName("Test");
            owner.setLastName("Owner");
            owner.setEmail(validEmails().next());
            owner.setAddress("123 Test Street");
            owner.setCity("Test City");
            owner.setTelephone(validPhoneNumbers().next());
            Owner savedOwner = ownerRepository.save(owner);
            
            // Create and persist pet
            Pet pet = new Pet();
            pet.setName(name);
            pet.setSpecies("Dog");
            pet.setOwner(savedOwner);
            Pet savedPet = petRepository.save(pet);
            
            // Retrieve and verify
            Optional<Pet> retrievedPet = petRepository.findById(savedPet.getId());
            assertTrue(retrievedPet.isPresent(), "Pet should be retrievable from database");
            assertEquals(name, retrievedPet.get().getName(), "Pet name should be stored exactly as provided");
        });
    }
    
    /**
     * Property: Pet Species Validation with Database Persistence
     * For any valid species, the pet should accept and store it correctly in the database
     */
    @Test
    void testPetSpeciesValidationWithPersistence() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String species = validSpecies().next();
            
            // Create and persist owner
            Owner owner = new Owner();
            owner.setFirstName("Test");
            owner.setLastName("Owner");
            owner.setEmail(validEmails().next());
            owner.setAddress("123 Test Street");
            owner.setCity("Test City");
            owner.setTelephone(validPhoneNumbers().next());
            Owner savedOwner = ownerRepository.save(owner);
            
            // Create and persist pet
            Pet pet = new Pet();
            pet.setName("TestPet");
            pet.setSpecies(species);
            pet.setOwner(savedOwner);
            Pet savedPet = petRepository.save(pet);
            
            // Retrieve and verify
            Optional<Pet> retrievedPet = petRepository.findById(savedPet.getId());
            assertTrue(retrievedPet.isPresent(), "Pet should be retrievable from database");
            assertEquals(species, retrievedPet.get().getSpecies(), "Pet species should be stored exactly as provided");
        });
    }
    
    /**
     * Property: Pet Age Calculation with Database Persistence
     * For any valid birth date in the past, the calculated age should be consistent and non-negative after database persistence
     */
    @Test
    void testPetAgeCalculationWithPersistence() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            LocalDate birthDate = validBirthDates().next();
            
            // Ensure birth date is in the past
            assumeTrue(birthDate.isBefore(LocalDate.now()));
            
            // Create and persist owner
            Owner owner = new Owner();
            owner.setFirstName("Test");
            owner.setLastName("Owner");
            owner.setEmail(validEmails().next());
            owner.setAddress("123 Test Street");
            owner.setCity("Test City");
            owner.setTelephone(validPhoneNumbers().next());
            Owner savedOwner = ownerRepository.save(owner);
            
            // Create and persist pet
            Pet pet = new Pet();
            pet.setName("TestPet");
            pet.setSpecies("Dog");
            pet.setBirthDate(birthDate);
            pet.setOwner(savedOwner);
            Pet savedPet = petRepository.save(pet);
            
            // Retrieve and verify age calculation
            Optional<Pet> retrievedPetOpt = petRepository.findById(savedPet.getId());
            assertTrue(retrievedPetOpt.isPresent(), "Pet should be retrievable from database");
            Pet retrievedPet = retrievedPetOpt.get();
            
            int age = retrievedPet.getAge();
            int expectedAge = LocalDate.now().getYear() - birthDate.getYear();
            
            assertTrue(age >= 0, "Pet age should be non-negative");
            assertTrue(age <= expectedAge, "Pet age should not exceed calculated years");
            
            // Test age-based classifications
            if (age < 2) {
                assertTrue(retrievedPet.isYoungPet(), "Pet under 2 years should be classified as young");
            }
            if (age > 7) {
                assertTrue(retrievedPet.isSeniorPet(), "Pet over 7 years should be classified as senior");
            }
        });
    }
    
    /**
     * Property: Pet-Owner Relationship Persistence
     * For any valid pet-owner pair, the relationship should be bidirectional, consistent, and persist correctly in the database
     */
    @Test
    void testPetOwnerRelationshipPersistence() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Create and persist owner
            Owner owner = new Owner();
            owner.setFirstName(validOwnerNames().next());
            owner.setLastName(validOwnerNames().next());
            owner.setEmail(validEmails().next());
            owner.setAddress("123 Test Street");
            owner.setCity("Test City");
            owner.setTelephone(validPhoneNumbers().next());
            Owner savedOwner = ownerRepository.save(owner);
            
            // Create and persist pet
            Pet pet = new Pet();
            pet.setName(validPetNames().next());
            pet.setSpecies(validSpecies().next());
            pet.setOwner(savedOwner);
            Pet savedPet = petRepository.save(pet);
            
            // Retrieve pet and verify relationship
            Optional<Pet> retrievedPetOpt = petRepository.findById(savedPet.getId());
            assertTrue(retrievedPetOpt.isPresent(), "Pet should be retrievable from database");
            Pet retrievedPet = retrievedPetOpt.get();
            
            // Test relationship consistency
            assertEquals(savedOwner.getId(), retrievedPet.getOwner().getId(), "Pet should reference the correct owner");
            assertEquals(savedOwner.getFirstName(), retrievedPet.getOwner().getFirstName(), "Owner first name should match");
            assertEquals(savedOwner.getLastName(), retrievedPet.getOwner().getLastName(), "Owner last name should match");
            assertEquals(savedOwner.getEmail(), retrievedPet.getOwner().getEmail(), "Owner email should match");
            
            // Test bidirectional relationship when using helper methods
            savedOwner.addPet(retrievedPet);
            Owner updatedOwner = ownerRepository.save(savedOwner);
            
            // Retrieve updated owner and verify bidirectional relationship
            Optional<Owner> retrievedOwnerOpt = ownerRepository.findById(updatedOwner.getId());
            assertTrue(retrievedOwnerOpt.isPresent(), "Owner should be retrievable from database");
            Owner retrievedOwner = retrievedOwnerOpt.get();
            
            assertTrue(retrievedOwner.getPets().stream().anyMatch(p -> p.getId().equals(retrievedPet.getId())), 
                      "Owner should contain the pet in their pets list");
            assertEquals(retrievedOwner.getId(), retrievedPet.getOwner().getId(), 
                        "Pet should still reference the owner after adding to owner's list");
            
            // Test repository queries for relationship
            var petsByOwner = petRepository.findByOwnerId(savedOwner.getId());
            assertTrue(petsByOwner.stream().anyMatch(p -> p.getId().equals(savedPet.getId())), 
                      "Pet should be findable by owner ID");
        });
    }
    
    /**
     * Property: Pet Creation with Edge Cases
     * For any valid pet data including edge cases, creation should succeed and data should persist correctly
     */
    @Test
    void testPetCreationEdgeCases() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Test edge cases for pet names (minimum and maximum lengths)
            String[] edgeCaseNames = {"A", "B", "C", "X".repeat(50)}; // 1 char and 50 chars
            String name = edgeCaseNames[random.nextInt(edgeCaseNames.length)];
            
            // Test edge cases for birth dates (very old pets)
            LocalDate[] edgeCaseDates = {
                LocalDate.now().minusYears(20),  // Very old pet
                LocalDate.now().minusYears(1),   // Young pet
                LocalDate.now().minusDays(1)     // Very young pet
            };
            LocalDate birthDate = edgeCaseDates[random.nextInt(edgeCaseDates.length)];
            
            // Create and persist owner with unique email
            Owner owner = new Owner();
            owner.setFirstName("Edge");
            owner.setLastName("Case");
            owner.setEmail(validEmails().next()); // Use generator for unique email
            owner.setAddress("123 Edge Street");
            owner.setCity("Edge City");
            owner.setTelephone("5555550000");
            Owner savedOwner = ownerRepository.save(owner);
            
            // Create pet with edge case data
            Pet pet = new Pet();
            pet.setName(name);
            pet.setSpecies(validSpecies().next());
            pet.setBirthDate(birthDate);
            pet.setOwner(savedOwner);
            
            // Persist and verify
            Pet savedPet = petRepository.save(pet);
            Optional<Pet> retrievedPet = petRepository.findById(savedPet.getId());
            
            assertTrue(retrievedPet.isPresent(), "Pet with edge case data should be retrievable");
            assertEquals(name, retrievedPet.get().getName(), "Edge case name should be stored correctly");
            assertEquals(birthDate, retrievedPet.get().getBirthDate(), "Edge case birth date should be stored correctly");
            assertTrue(retrievedPet.get().getAge() >= 0, "Age calculation should work for edge case dates");
        });
    }
    
    // Helper method for assumptions in property tests
    private void assumeTrue(boolean condition) {
        if (!condition) {
            // Skip this iteration if assumption is not met
            return;
        }
    }
}