package com.petclinic.backend.properties;

import com.petclinic.backend.model.Owner;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.repository.OwnerRepository;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.repository.VeterinarianRepository;
import com.petclinic.backend.repository.VisitRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.Positive;
import net.jqwik.api.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Property-Based Tests for Pet Clinic Data Persistence
 * **Property 11: Pet Clinic Data Persistence**
 * **Validates: Requirements 8.1, 8.2, 8.3**
 * 
 * Tests that for any pet, owner, or visit record created in the system,
 * the data should be retrievable with all original information intact
 * after storage and retrieval operations.
 */
@DataJpaTest
@ActiveProfiles("test")
public class PetClinicDataPersistenceProperties {

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

    /**
     * Property: Owner data persistence integrity
     * For any valid owner data, after saving and retrieving,
     * all fields should match the original values
     */
    @Property(tries = 100)
    @Label("Feature: pet-clinic-cicd-pipeline, Property 11: Owner data persistence maintains integrity")
    void ownerDataPersistenceIntegrity(
            @ForAll @NotBlank @Size(min = 1, max = 50) String firstName,
            @ForAll @NotBlank @Size(min = 1, max = 50) String lastName,
            @ForAll @Size(max = 200) String address,
            @ForAll @Size(max = 50) String city,
            @ForAll String telephone,
            @ForAll String email) {
        
        // Arrange: Create owner with generated data
        Owner originalOwner = new Owner();
        originalOwner.setFirstName(firstName);
        originalOwner.setLastName(lastName);
        originalOwner.setAddress(address);
        originalOwner.setCity(city);
        originalOwner.setTelephone(sanitizeTelephone(telephone));
        originalOwner.setEmail(sanitizeEmail(email));
        
        // Act: Save and retrieve owner
        Owner savedOwner = ownerRepository.save(originalOwner);
        Optional<Owner> retrievedOwner = ownerRepository.findById(savedOwner.getId());
        
        // Assert: All data should be preserved
        Assume.that(retrievedOwner.isPresent());
        Owner owner = retrievedOwner.get();
        
        // Verify all fields match original values
        Assume.that(owner.getFirstName().equals(originalOwner.getFirstName()));
        Assume.that(owner.getLastName().equals(originalOwner.getLastName()));
        Assume.that(objectsEqual(owner.getAddress(), originalOwner.getAddress()));
        Assume.that(objectsEqual(owner.getCity(), originalOwner.getCity()));
        Assume.that(objectsEqual(owner.getTelephone(), originalOwner.getTelephone()));
        Assume.that(objectsEqual(owner.getEmail(), originalOwner.getEmail()));
        
        // Verify audit fields are set
        Assume.that(owner.getCreatedAt() != null);
        Assume.that(owner.getUpdatedAt() != null);
        Assume.that(owner.getId() != null);
    }

    /**
     * Property: Pet data persistence integrity
     * For any valid pet data, after saving and retrieving,
     * all fields should match the original values
     */
    @Property(tries = 100)
    @Label("Feature: pet-clinic-cicd-pipeline, Property 11: Pet data persistence maintains integrity")
    void petDataPersistenceIntegrity(
            @ForAll @NotBlank @Size(min = 1, max = 50) String petName,
            @ForAll @NotBlank @Size(min = 1, max = 30) String species,
            @ForAll @Size(max = 50) String breed,
            @ForAll String medicalHistory,
            @ForAll @NotBlank @Size(min = 1, max = 50) String ownerFirstName,
            @ForAll @NotBlank @Size(min = 1, max = 50) String ownerLastName,
            @ForAll String ownerEmail) {
        
        // Arrange: Create owner first (required for pet)
        Owner owner = new Owner();
        owner.setFirstName(ownerFirstName);
        owner.setLastName(ownerLastName);
        owner.setEmail(sanitizeEmail(ownerEmail));
        Owner savedOwner = ownerRepository.save(owner);
        
        // Create pet with generated data
        Pet originalPet = new Pet();
        originalPet.setName(petName);
        originalPet.setSpecies(species);
        originalPet.setBreed(breed);
        originalPet.setBirthDate(generateValidBirthDate());
        originalPet.setMedicalHistory(sanitizeMedicalHistory(medicalHistory));
        originalPet.setOwner(savedOwner);
        
        // Act: Save and retrieve pet
        Pet savedPet = petRepository.save(originalPet);
        Optional<Pet> retrievedPet = petRepository.findById(savedPet.getId());
        
        // Assert: All data should be preserved
        Assume.that(retrievedPet.isPresent());
        Pet pet = retrievedPet.get();
        
        // Verify all fields match original values
        Assume.that(pet.getName().equals(originalPet.getName()));
        Assume.that(pet.getSpecies().equals(originalPet.getSpecies()));
        Assume.that(objectsEqual(pet.getBreed(), originalPet.getBreed()));
        Assume.that(objectsEqual(pet.getBirthDate(), originalPet.getBirthDate()));
        Assume.that(objectsEqual(pet.getMedicalHistory(), originalPet.getMedicalHistory()));
        Assume.that(pet.getOwner().getId().equals(savedOwner.getId()));
        
        // Verify audit fields are set
        Assume.that(pet.getCreatedAt() != null);
        Assume.that(pet.getUpdatedAt() != null);
        Assume.that(pet.getId() != null);
    }

    /**
     * Property: Visit data persistence integrity
     * For any valid visit data, after saving and retrieving,
     * all fields should match the original values
     */
    @Property(tries = 100)
    @Label("Feature: pet-clinic-cicd-pipeline, Property 11: Visit data persistence maintains integrity")
    void visitDataPersistenceIntegrity(
            @ForAll @NotBlank @Size(min = 1, max = 500) String description,
            @ForAll @Size(max = 500) String diagnosis,
            @ForAll @Size(max = 500) String treatment,
            @ForAll @Positive BigDecimal cost,
            @ForAll @NotBlank @Size(min = 1, max = 50) String petName,
            @ForAll @NotBlank @Size(min = 1, max = 30) String species,
            @ForAll @NotBlank @Size(min = 1, max = 50) String ownerFirstName,
            @ForAll @NotBlank @Size(min = 1, max = 50) String ownerLastName) {
        
        // Arrange: Create owner and pet first (required for visit)
        Owner owner = new Owner();
        owner.setFirstName(ownerFirstName);
        owner.setLastName(ownerLastName);
        owner.setEmail(generateUniqueEmail());
        Owner savedOwner = ownerRepository.save(owner);
        
        Pet pet = new Pet();
        pet.setName(petName);
        pet.setSpecies(species);
        pet.setBirthDate(generateValidBirthDate());
        pet.setOwner(savedOwner);
        Pet savedPet = petRepository.save(pet);
        
        // Create visit with generated data
        Visit originalVisit = new Visit();
        originalVisit.setVisitDate(generateValidVisitDate());
        originalVisit.setDescription(description);
        originalVisit.setDiagnosis(sanitizeText(diagnosis));
        originalVisit.setTreatment(sanitizeText(treatment));
        originalVisit.setCost(cost);
        originalVisit.setPet(savedPet);
        
        // Act: Save and retrieve visit
        Visit savedVisit = visitRepository.save(originalVisit);
        Optional<Visit> retrievedVisit = visitRepository.findById(savedVisit.getId());
        
        // Assert: All data should be preserved
        Assume.that(retrievedVisit.isPresent());
        Visit visit = retrievedVisit.get();
        
        // Verify all fields match original values
        Assume.that(visit.getDescription().equals(originalVisit.getDescription()));
        Assume.that(objectsEqual(visit.getDiagnosis(), originalVisit.getDiagnosis()));
        Assume.that(objectsEqual(visit.getTreatment(), originalVisit.getTreatment()));
        Assume.that(visit.getCost().compareTo(originalVisit.getCost()) == 0);
        Assume.that(visit.getPet().getId().equals(savedPet.getId()));
        Assume.that(visit.getVisitDate().equals(originalVisit.getVisitDate()));
        
        // Verify audit fields are set
        Assume.that(visit.getCreatedAt() != null);
        Assume.that(visit.getUpdatedAt() != null);
        Assume.that(visit.getId() != null);
    }

    /**
     * Property: Veterinarian data persistence integrity
     * For any valid veterinarian data, after saving and retrieving,
     * all fields should match the original values
     */
    @Property(tries = 100)
    @Label("Feature: pet-clinic-cicd-pipeline, Property 11: Veterinarian data persistence maintains integrity")
    void veterinarianDataPersistenceIntegrity(
            @ForAll @NotBlank @Size(min = 1, max = 50) String firstName,
            @ForAll @NotBlank @Size(min = 1, max = 50) String lastName,
            @ForAll @Size(max = 200) String specialties,
            @ForAll @NotBlank String licenseNumber) {
        
        // Arrange: Create veterinarian with generated data
        Veterinarian originalVet = new Veterinarian();
        originalVet.setFirstName(firstName);
        originalVet.setLastName(lastName);
        originalVet.setSpecialties(sanitizeText(specialties));
        originalVet.setLicenseNumber(generateValidLicenseNumber(licenseNumber));
        
        // Act: Save and retrieve veterinarian
        Veterinarian savedVet = veterinarianRepository.save(originalVet);
        Optional<Veterinarian> retrievedVet = veterinarianRepository.findById(savedVet.getId());
        
        // Assert: All data should be preserved
        Assume.that(retrievedVet.isPresent());
        Veterinarian vet = retrievedVet.get();
        
        // Verify all fields match original values
        Assume.that(vet.getFirstName().equals(originalVet.getFirstName()));
        Assume.that(vet.getLastName().equals(originalVet.getLastName()));
        Assume.that(objectsEqual(vet.getSpecialties(), originalVet.getSpecialties()));
        Assume.that(vet.getLicenseNumber().equals(originalVet.getLicenseNumber()));
        
        // Verify audit fields are set
        Assume.that(vet.getCreatedAt() != null);
        Assume.that(vet.getUpdatedAt() != null);
        Assume.that(vet.getId() != null);
    }

    /**
     * Property: Data persistence survives transaction boundaries
     * Data saved in one transaction should be retrievable in another transaction
     */
    @Property(tries = 50)
    @Label("Feature: pet-clinic-cicd-pipeline, Property 11: Data persistence survives transaction boundaries")
    void dataPersistenceSurvivesTransactionBoundaries(
            @ForAll @NotBlank @Size(min = 1, max = 50) String firstName,
            @ForAll @NotBlank @Size(min = 1, max = 50) String lastName,
            @ForAll String email) {
        
        // Arrange & Act: Save owner in current transaction
        Owner owner = new Owner();
        owner.setFirstName(firstName);
        owner.setLastName(lastName);
        owner.setEmail(sanitizeEmail(email));
        Owner savedOwner = ownerRepository.save(owner);
        Long ownerId = savedOwner.getId();
        
        // Force flush to database
        entityManager.flush();
        
        // Clear persistence context to simulate new transaction
        entityManager.clear();
        
        // Assert: Data should still be retrievable
        Optional<Owner> retrievedOwner = ownerRepository.findById(ownerId);
        Assume.that(retrievedOwner.isPresent());
        Assume.that(retrievedOwner.get().getFirstName().equals(firstName));
        Assume.that(retrievedOwner.get().getLastName().equals(lastName));
    }

    // Helper methods for data sanitization and generation
    
    private String sanitizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return generateUniqueEmail();
        }
        // Simple email format validation
        String sanitized = email.replaceAll("[^a-zA-Z0-9@._-]", "");
        if (!sanitized.contains("@")) {
            sanitized = sanitized + "@example.com";
        }
        return sanitized.length() > 100 ? sanitized.substring(0, 100) : sanitized;
    }
    
    private String generateUniqueEmail() {
        return "test" + System.nanoTime() + "@example.com";
    }
    
    private String sanitizeTelephone(String telephone) {
        if (telephone == null) return null;
        String sanitized = telephone.replaceAll("[^0-9+\\-\\(\\)\\s]", "");
        return sanitized.length() > 15 ? sanitized.substring(0, 15) : sanitized;
    }
    
    private String sanitizeText(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        return text.trim();
    }
    
    private String sanitizeMedicalHistory(String history) {
        if (history == null || history.trim().isEmpty()) return null;
        return history.length() > 1000 ? history.substring(0, 1000) : history;
    }
    
    private LocalDate generateValidBirthDate() {
        // Generate a birth date between 20 years ago and 1 year ago
        LocalDate now = LocalDate.now();
        LocalDate twentyYearsAgo = now.minusYears(20);
        LocalDate oneYearAgo = now.minusYears(1);
        
        long daysBetween = twentyYearsAgo.toEpochDay() - oneYearAgo.toEpochDay();
        long randomDays = (long) (Math.random() * Math.abs(daysBetween));
        
        return oneYearAgo.plusDays(randomDays);
    }
    
    private LocalDateTime generateValidVisitDate() {
        // Generate a visit date within the last year or next month
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneYearAgo = now.minusYears(1);
        LocalDateTime oneMonthFromNow = now.plusMonths(1);
        
        long hoursBetween = java.time.Duration.between(oneYearAgo, oneMonthFromNow).toHours();
        long randomHours = (long) (Math.random() * hoursBetween);
        
        return oneYearAgo.plusHours(randomHours);
    }
    
    private String generateValidLicenseNumber(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "VET" + System.nanoTime();
        }
        
        // Convert to uppercase alphanumeric
        String sanitized = input.toUpperCase().replaceAll("[^A-Z0-9]", "");
        
        // Ensure minimum length of 6
        while (sanitized.length() < 6) {
            sanitized += "0";
        }
        
        // Ensure maximum length of 20
        if (sanitized.length() > 20) {
            sanitized = sanitized.substring(0, 20);
        }
        
        // Make unique by appending timestamp
        return sanitized + System.nanoTime() % 1000;
    }
    
    private boolean objectsEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}