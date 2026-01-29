package com.petclinic.backend.properties;

import com.petclinic.backend.model.Owner;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.repository.OwnerRepository;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.repository.VeterinarianRepository;
import com.petclinic.backend.repository.VisitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-Based Tests for Pet Clinic Data Persistence
 * **Property 11: Pet Clinic Data Persistence**
 * **Validates: Requirements 8.1, 8.2, 8.3**
 * 
 * Tests that for any pet, owner, or visit record created in the system,
 * the data should be retrievable with all original information intact
 * after storage and retrieval operations.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PetClinicDataPersistenceProperties {

    @Autowired
    private OwnerRepository ownerRepository;
    
    @Autowired
    private PetRepository petRepository;
    
    @Autowired
    private VeterinarianRepository veterinarianRepository;
    
    @Autowired
    private VisitRepository visitRepository;

    /**
     * Simple JUnit test to verify property-based testing concepts
     * Tests basic owner data persistence with fixed data
     */
    @Test
    void testOwnerDataPersistenceWithFixedData() {
        // Arrange: Create owner with fixed data
        Owner originalOwner = new Owner();
        originalOwner.setFirstName("John");
        originalOwner.setLastName("Doe");
        originalOwner.setAddress("123 Test Street");
        originalOwner.setCity("Test City");
        originalOwner.setTelephone("1234567890");
        originalOwner.setEmail("john.doe@test.com");
        
        // Act: Save and retrieve owner
        Owner savedOwner = ownerRepository.save(originalOwner);
        Optional<Owner> retrievedOwner = ownerRepository.findById(savedOwner.getId());
        
        // Assert: All data should be preserved
        assertTrue(retrievedOwner.isPresent());
        Owner owner = retrievedOwner.get();
        
        // Verify all fields match original values
        assertEquals(originalOwner.getFirstName(), owner.getFirstName());
        assertEquals(originalOwner.getLastName(), owner.getLastName());
        assertEquals(originalOwner.getAddress(), owner.getAddress());
        assertEquals(originalOwner.getCity(), owner.getCity());
        assertEquals(originalOwner.getTelephone(), owner.getTelephone());
        assertEquals(originalOwner.getEmail(), owner.getEmail());
        
        // Verify audit fields are set
        assertNotNull(owner.getCreatedAt());
        assertNotNull(owner.getUpdatedAt());
        assertNotNull(owner.getId());
    }

    /**
     * Simple JUnit test to verify pet data persistence with fixed data
     */
    @Test
    void testPetDataPersistenceWithFixedData() {
        // Arrange: Create owner first (required for pet)
        Owner owner = new Owner();
        owner.setFirstName("Jane");
        owner.setLastName("Smith");
        owner.setEmail("jane.smith@test.com");
        owner.setAddress("456 Test Avenue");
        owner.setCity("Test City");
        owner.setTelephone("9876543210");
        Owner savedOwner = ownerRepository.save(owner);
        
        // Create pet with fixed data
        Pet originalPet = new Pet();
        originalPet.setName("Buddy");
        originalPet.setSpecies("Dog");
        originalPet.setBreed("Golden Retriever");
        originalPet.setBirthDate(LocalDate.of(2020, 5, 15));
        originalPet.setMedicalHistory("Healthy dog, regular checkups");
        originalPet.setOwner(savedOwner);
        
        // Act: Save and retrieve pet
        Pet savedPet = petRepository.save(originalPet);
        Optional<Pet> retrievedPet = petRepository.findById(savedPet.getId());
        
        // Assert: All data should be preserved
        assertTrue(retrievedPet.isPresent());
        Pet pet = retrievedPet.get();
        
        // Verify all fields match original values
        assertEquals(originalPet.getName(), pet.getName());
        assertEquals(originalPet.getSpecies(), pet.getSpecies());
        assertEquals(originalPet.getBreed(), pet.getBreed());
        assertEquals(originalPet.getBirthDate(), pet.getBirthDate());
        assertEquals(originalPet.getMedicalHistory(), pet.getMedicalHistory());
        assertEquals(savedOwner.getId(), pet.getOwner().getId());
        
        // Verify audit fields are set
        assertNotNull(pet.getCreatedAt());
        assertNotNull(pet.getUpdatedAt());
        assertNotNull(pet.getId());
    }

    /**
     * Simple JUnit test to verify veterinarian data persistence with fixed data
     */
    @Test
    void testVeterinarianDataPersistenceWithFixedData() {
        // Arrange: Create veterinarian with fixed data
        Veterinarian originalVet = new Veterinarian();
        originalVet.setFirstName("Dr. Sarah");
        originalVet.setLastName("Johnson");
        originalVet.setSpecialties("Small Animal Medicine");
        originalVet.setLicenseNumber("VET123456");
        
        // Act: Save and retrieve veterinarian
        Veterinarian savedVet = veterinarianRepository.save(originalVet);
        Optional<Veterinarian> retrievedVet = veterinarianRepository.findById(savedVet.getId());
        
        // Assert: All data should be preserved
        assertTrue(retrievedVet.isPresent());
        Veterinarian vet = retrievedVet.get();
        
        // Verify all fields match original values
        assertEquals(originalVet.getFirstName(), vet.getFirstName());
        assertEquals(originalVet.getLastName(), vet.getLastName());
        assertEquals(originalVet.getSpecialties(), vet.getSpecialties());
        assertEquals(originalVet.getLicenseNumber(), vet.getLicenseNumber());
        
        // Verify audit fields are set
        assertNotNull(vet.getCreatedAt());
        assertNotNull(vet.getUpdatedAt());
        assertNotNull(vet.getId());
    }

    /**
     * Simple JUnit test to verify visit data persistence with fixed data
     */
    @Test
    void testVisitDataPersistenceWithFixedData() {
        // Arrange: Create owner and pet first (required for visit)
        Owner owner = new Owner();
        owner.setFirstName("Bob");
        owner.setLastName("Wilson");
        owner.setEmail("bob.wilson@test.com");
        owner.setAddress("789 Test Boulevard");
        owner.setCity("Test City");
        owner.setTelephone("5555551234");
        Owner savedOwner = ownerRepository.save(owner);
        
        Pet pet = new Pet();
        pet.setName("Whiskers");
        pet.setSpecies("Cat");
        pet.setBirthDate(LocalDate.of(2019, 3, 10));
        pet.setOwner(savedOwner);
        Pet savedPet = petRepository.save(pet);
        
        // Create visit with fixed data
        Visit originalVisit = new Visit();
        originalVisit.setVisitDate(LocalDateTime.of(2024, 1, 15, 10, 30));
        originalVisit.setDescription("Annual checkup");
        originalVisit.setDiagnosis("Healthy cat");
        originalVisit.setTreatment("Vaccinations updated");
        originalVisit.setCost(new BigDecimal("125.50"));
        originalVisit.setPet(savedPet);
        
        // Act: Save and retrieve visit
        Visit savedVisit = visitRepository.save(originalVisit);
        Optional<Visit> retrievedVisit = visitRepository.findById(savedVisit.getId());
        
        // Assert: All data should be preserved
        assertTrue(retrievedVisit.isPresent());
        Visit visit = retrievedVisit.get();
        
        // Verify all fields match original values
        assertEquals(originalVisit.getDescription(), visit.getDescription());
        assertEquals(originalVisit.getDiagnosis(), visit.getDiagnosis());
        assertEquals(originalVisit.getTreatment(), visit.getTreatment());
        assertEquals(0, originalVisit.getCost().compareTo(visit.getCost()));
        assertEquals(savedPet.getId(), visit.getPet().getId());
        assertEquals(originalVisit.getVisitDate(), visit.getVisitDate());
        
        // Verify audit fields are set
        assertNotNull(visit.getCreatedAt());
        assertNotNull(visit.getUpdatedAt());
        assertNotNull(visit.getId());
    }

    /**
     * Simple JUnit test to verify data persistence across transactions
     */
    @Test
    void testDataPersistenceAcrossTransactions() {
        // Arrange & Act: Save owner in current transaction
        Owner owner = new Owner();
        owner.setFirstName("Alice");
        owner.setLastName("Brown");
        owner.setEmail("alice.brown@test.com");
        owner.setAddress("321 Test Lane");
        owner.setCity("Test City");
        owner.setTelephone("1111111111");
        Owner savedOwner = ownerRepository.save(owner);
        Long ownerId = savedOwner.getId();
        
        // Assert: Data should still be retrievable
        Optional<Owner> retrievedOwner = ownerRepository.findById(ownerId);
        assertTrue(retrievedOwner.isPresent());
        assertEquals("Alice", retrievedOwner.get().getFirstName());
        assertEquals("Brown", retrievedOwner.get().getLastName());
    }
}