package com.petclinic.backend.repository;

import com.petclinic.backend.model.Owner;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.model.Visit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Tests for Pet Clinic Data Persistence
 * **Validates: Requirements 8.1, 8.2, 8.3**
 * 
 * Tests that pet, owner, and visit records can be saved and retrieved
 * with all original information intact after storage operations.
 */
@DataJpaTest
@ActiveProfiles("test")
public class PetClinicDataPersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OwnerRepository ownerRepository;
    
    @Autowired
    private PetRepository petRepository;
    
    @Autowired
    private VeterinarianRepository veterinarianRepository;
    
    @Autowired
    private VisitRepository visitRepository;

    @Test
    void ownerDataPersistenceIntegrity() {
        // Arrange: Create owner with test data
        Owner originalOwner = new Owner();
        originalOwner.setFirstName("John");
        originalOwner.setLastName("Doe");
        originalOwner.setAddress("123 Main St");
        originalOwner.setCity("Springfield");
        originalOwner.setTelephone("555-123-4567");
        originalOwner.setEmail("john.doe@example.com");
        
        // Act: Save and retrieve owner
        Owner savedOwner = ownerRepository.save(originalOwner);
        entityManager.flush();
        entityManager.clear();
        
        Optional<Owner> retrievedOwner = ownerRepository.findById(savedOwner.getId());
        
        // Assert: All data should be preserved
        assertThat(retrievedOwner).isPresent();
        Owner owner = retrievedOwner.get();
        
        assertThat(owner.getFirstName()).isEqualTo("John");
        assertThat(owner.getLastName()).isEqualTo("Doe");
        assertThat(owner.getAddress()).isEqualTo("123 Main St");
        assertThat(owner.getCity()).isEqualTo("Springfield");
        assertThat(owner.getTelephone()).isEqualTo("555-123-4567");
        assertThat(owner.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(owner.getCreatedAt()).isNotNull();
        assertThat(owner.getUpdatedAt()).isNotNull();
        assertThat(owner.getId()).isNotNull();
    }

    @Test
    void petDataPersistenceIntegrity() {
        // Arrange: Create owner first (required for pet)
        Owner owner = new Owner();
        owner.setFirstName("Jane");
        owner.setLastName("Smith");
        owner.setEmail("jane.smith@example.com");
        Owner savedOwner = ownerRepository.save(owner);
        
        // Create pet with test data
        Pet originalPet = new Pet();
        originalPet.setName("Buddy");
        originalPet.setSpecies("Dog");
        originalPet.setBreed("Golden Retriever");
        originalPet.setBirthDate(LocalDate.of(2020, 5, 15));
        originalPet.setMedicalHistory("Vaccinated, healthy");
        originalPet.setOwner(savedOwner);
        
        // Act: Save and retrieve pet
        Pet savedPet = petRepository.save(originalPet);
        entityManager.flush();
        entityManager.clear();
        
        Optional<Pet> retrievedPet = petRepository.findById(savedPet.getId());
        
        // Assert: All data should be preserved
        assertThat(retrievedPet).isPresent();
        Pet pet = retrievedPet.get();
        
        assertThat(pet.getName()).isEqualTo("Buddy");
        assertThat(pet.getSpecies()).isEqualTo("Dog");
        assertThat(pet.getBreed()).isEqualTo("Golden Retriever");
        assertThat(pet.getBirthDate()).isEqualTo(LocalDate.of(2020, 5, 15));
        assertThat(pet.getMedicalHistory()).isEqualTo("Vaccinated, healthy");
        assertThat(pet.getOwner().getId()).isEqualTo(savedOwner.getId());
        assertThat(pet.getCreatedAt()).isNotNull();
        assertThat(pet.getUpdatedAt()).isNotNull();
        assertThat(pet.getId()).isNotNull();
    }

    @Test
    void visitDataPersistenceIntegrity() {
        // Arrange: Create owner and pet first (required for visit)
        Owner owner = new Owner();
        owner.setFirstName("Bob");
        owner.setLastName("Johnson");
        owner.setEmail("bob.johnson@example.com");
        Owner savedOwner = ownerRepository.save(owner);
        
        Pet pet = new Pet();
        pet.setName("Max");
        pet.setSpecies("Cat");
        pet.setBirthDate(LocalDate.of(2019, 3, 10));
        pet.setOwner(savedOwner);
        Pet savedPet = petRepository.save(pet);
        
        // Create visit with test data
        Visit originalVisit = new Visit();
        originalVisit.setVisitDate(LocalDateTime.of(2023, 6, 15, 10, 30));
        originalVisit.setDescription("Annual checkup");
        originalVisit.setDiagnosis("Healthy");
        originalVisit.setTreatment("Vaccination");
        originalVisit.setCost(new BigDecimal("75.00"));
        originalVisit.setPet(savedPet);
        
        // Act: Save and retrieve visit
        Visit savedVisit = visitRepository.save(originalVisit);
        entityManager.flush();
        entityManager.clear();
        
        Optional<Visit> retrievedVisit = visitRepository.findById(savedVisit.getId());
        
        // Assert: All data should be preserved
        assertThat(retrievedVisit).isPresent();
        Visit visit = retrievedVisit.get();
        
        assertThat(visit.getDescription()).isEqualTo("Annual checkup");
        assertThat(visit.getDiagnosis()).isEqualTo("Healthy");
        assertThat(visit.getTreatment()).isEqualTo("Vaccination");
        assertThat(visit.getCost()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(visit.getPet().getId()).isEqualTo(savedPet.getId());
        assertThat(visit.getVisitDate()).isEqualTo(LocalDateTime.of(2023, 6, 15, 10, 30));
        assertThat(visit.getCreatedAt()).isNotNull();
        assertThat(visit.getUpdatedAt()).isNotNull();
        assertThat(visit.getId()).isNotNull();
    }

    @Test
    void veterinarianDataPersistenceIntegrity() {
        // Arrange: Create veterinarian with test data
        Veterinarian originalVet = new Veterinarian();
        originalVet.setFirstName("Dr. Sarah");
        originalVet.setLastName("Wilson");
        originalVet.setSpecialties("Surgery, Cardiology");
        originalVet.setLicenseNumber("VET123456");
        
        // Act: Save and retrieve veterinarian
        Veterinarian savedVet = veterinarianRepository.save(originalVet);
        entityManager.flush();
        entityManager.clear();
        
        Optional<Veterinarian> retrievedVet = veterinarianRepository.findById(savedVet.getId());
        
        // Assert: All data should be preserved
        assertThat(retrievedVet).isPresent();
        Veterinarian vet = retrievedVet.get();
        
        assertThat(vet.getFirstName()).isEqualTo("Dr. Sarah");
        assertThat(vet.getLastName()).isEqualTo("Wilson");
        assertThat(vet.getSpecialties()).isEqualTo("Surgery, Cardiology");
        assertThat(vet.getLicenseNumber()).isEqualTo("VET123456");
        assertThat(vet.getCreatedAt()).isNotNull();
        assertThat(vet.getUpdatedAt()).isNotNull();
        assertThat(vet.getId()).isNotNull();
    }

    @Test
    void dataPersistenceSurvivesTransactionBoundaries() {
        // Arrange & Act: Save owner in current transaction
        Owner owner = new Owner();
        owner.setFirstName("Alice");
        owner.setLastName("Brown");
        owner.setEmail("alice.brown@example.com");
        Owner savedOwner = ownerRepository.save(owner);
        Long ownerId = savedOwner.getId();
        
        // Force flush to database and clear persistence context
        entityManager.flush();
        entityManager.clear();
        
        // Assert: Data should still be retrievable
        Optional<Owner> retrievedOwner = ownerRepository.findById(ownerId);
        assertThat(retrievedOwner).isPresent();
        assertThat(retrievedOwner.get().getFirstName()).isEqualTo("Alice");
        assertThat(retrievedOwner.get().getLastName()).isEqualTo("Brown");
    }

    @Test
    void repositorySearchFunctionality() {
        // Arrange: Create test data
        Owner owner = new Owner();
        owner.setFirstName("Test");
        owner.setLastName("Owner");
        owner.setEmail("test.owner@example.com");
        Owner savedOwner = ownerRepository.save(owner);
        
        Pet pet = new Pet();
        pet.setName("TestPet");
        pet.setSpecies("Dog");
        pet.setBreed("Labrador");
        pet.setOwner(savedOwner);
        petRepository.save(pet);
        
        entityManager.flush();
        
        // Act & Assert: Test search functionality
        var ownersByName = ownerRepository.findByFirstNameContainingIgnoreCase("test");
        assertThat(ownersByName).hasSize(1);
        assertThat(ownersByName.get(0).getFirstName()).isEqualTo("Test");
        
        var petsBySpecies = petRepository.findBySpeciesIgnoreCase("dog");
        assertThat(petsBySpecies).hasSize(1);
        assertThat(petsBySpecies.get(0).getSpecies()).isEqualTo("Dog");
        
        var petsByOwner = petRepository.findByOwnerId(savedOwner.getId());
        assertThat(petsByOwner).hasSize(1);
        assertThat(petsByOwner.get(0).getName()).isEqualTo("TestPet");
    }
}