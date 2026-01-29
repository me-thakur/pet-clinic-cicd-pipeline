package com.petclinic.frontend.properties;

import com.petclinic.frontend.model.Owner;
import com.petclinic.frontend.model.Pet;
import com.petclinic.frontend.model.Veterinarian;
import com.petclinic.frontend.model.Visit;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for form validation consistency
 * **Validates: Requirements 6.3**
 * 
 * Tests universal properties that should hold for all form input validation scenarios
 * across the pet clinic application. Ensures validation rules are applied consistently
 * and that invalid inputs are properly rejected while valid inputs are accepted.
 */
@DisplayName("Form Validation Consistency Property-Based Tests")
class FormValidationConsistencyProperties {
    
    private Validator validator;
    private Random random;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        random = new Random();
    }
    
    /**
     * Property 12: Input Validation Consistency
     * For any form input across the system, validation should enforce consistent rules 
     * for data types, formats, and constraints
     * **Validates: Requirements 6.3**
     */
    @Test
    @DisplayName("Property 12: Input Validation Consistency - Validation rules should be consistent across all forms")
    void testInputValidationConsistency() {
        runPropertyTest(100, () -> {
            // Test Pet form validation consistency
            testPetValidationConsistency();
            
            // Test Visit form validation consistency
            testVisitValidationConsistency();
            
            // Test Owner form validation consistency
            testOwnerValidationConsistency();
            
            // Test Veterinarian form validation consistency
            testVeterinarianValidationConsistency();
            
            // Test cross-form validation consistency
            testCrossFormValidationConsistency();
        });
    }
    
    /**
     * Test Pet form validation consistency
     */
    private void testPetValidationConsistency() {
        // Test required field validation consistency
        Pet emptyPet = new Pet();
        Set<ConstraintViolation<Pet>> violations = validator.validate(emptyPet);
        
        // Should have violations for required fields
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")),
                  "Pet name should be required");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("species")),
                  "Pet species should be required");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("birthDate")),
                  "Pet birth date should be required");
        
        // Test valid pet passes validation
        Pet validPet = createValidPet();
        Set<ConstraintViolation<Pet>> validViolations = validator.validate(validPet);
        assertTrue(validViolations.isEmpty(), 
                  "Valid pet should pass validation. Violations: " + validViolations);
        
        // Test string length constraints consistency
        testPetStringLengthConstraints();
        
        // Test date constraints consistency
        testPetDateConstraints();
    }
    
    /**
     * Test Pet string length constraints
     */
    private void testPetStringLengthConstraints() {
        // Test name length constraints
        Pet petWithLongName = createValidPet();
        petWithLongName.setName("A".repeat(51)); // Exceeds 50 character limit
        Set<ConstraintViolation<Pet>> nameViolations = validator.validate(petWithLongName);
        assertTrue(nameViolations.stream().anyMatch(v -> 
                  v.getPropertyPath().toString().equals("name") && 
                  v.getMessage().contains("50")),
                  "Pet name should be limited to 50 characters");
        
        // Test species length constraints
        Pet petWithLongSpecies = createValidPet();
        petWithLongSpecies.setSpecies("A".repeat(31)); // Exceeds 30 character limit
        Set<ConstraintViolation<Pet>> speciesViolations = validator.validate(petWithLongSpecies);
        assertTrue(speciesViolations.stream().anyMatch(v -> 
                  v.getPropertyPath().toString().equals("species") && 
                  v.getMessage().contains("30")),
                  "Pet species should be limited to 30 characters");
        
        // Test breed length constraints
        Pet petWithLongBreed = createValidPet();
        petWithLongBreed.setBreed("A".repeat(51)); // Exceeds 50 character limit
        Set<ConstraintViolation<Pet>> breedViolations = validator.validate(petWithLongBreed);
        assertTrue(breedViolations.stream().anyMatch(v -> 
                  v.getPropertyPath().toString().equals("breed") && 
                  v.getMessage().contains("50")),
                  "Pet breed should be limited to 50 characters");
        
        // Test medical history length constraints
        Pet petWithLongHistory = createValidPet();
        petWithLongHistory.setMedicalHistory("A".repeat(1001)); // Exceeds 1000 character limit
        Set<ConstraintViolation<Pet>> historyViolations = validator.validate(petWithLongHistory);
        assertTrue(historyViolations.stream().anyMatch(v -> 
                  v.getPropertyPath().toString().equals("medicalHistory") && 
                  v.getMessage().contains("1000")),
                  "Pet medical history should be limited to 1000 characters");
    }
    
    /**
     * Test Pet date constraints
     */
    private void testPetDateConstraints() {
        // Test future birth date is invalid
        Pet petWithFutureBirthDate = createValidPet();
        petWithFutureBirthDate.setBirthDate(LocalDate.now().plusDays(1));
        Set<ConstraintViolation<Pet>> futureDateViolations = validator.validate(petWithFutureBirthDate);
        assertTrue(futureDateViolations.stream().anyMatch(v -> 
                  v.getPropertyPath().toString().equals("birthDate") && 
                  v.getMessage().contains("past")),
                  "Pet birth date should be in the past");
        
        // Test past birth date is valid
        Pet petWithPastBirthDate = createValidPet();
        petWithPastBirthDate.setBirthDate(LocalDate.now().minusYears(5));
        Set<ConstraintViolation<Pet>> pastDateViolations = validator.validate(petWithPastBirthDate);
        assertFalse(pastDateViolations.stream().anyMatch(v -> 
                   v.getPropertyPath().toString().equals("birthDate")),
                   "Pet with past birth date should be valid");
    }
    
    /**
     * Test Visit form validation consistency
     */
    private void testVisitValidationConsistency() {
        // Test required field validation consistency
        Visit emptyVisit = new Visit();
        Set<ConstraintViolation<Visit>> violations = validator.validate(emptyVisit);
        
        // Should have violations for required fields
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("visitDate")),
                  "Visit date should be required");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")),
                  "Visit description should be required");
        
        // Test valid visit passes validation
        Visit validVisit = createValidVisit();
        Set<ConstraintViolation<Visit>> validViolations = validator.validate(validVisit);
        assertTrue(validViolations.isEmpty(), 
                  "Valid visit should pass validation. Violations: " + validViolations);
        
        // Test string length constraints consistency
        testVisitStringLengthConstraints();
        
        // Test numeric constraints consistency
        testVisitNumericConstraints();
    }
    
    /**
     * Test Visit string length constraints
     */
    private void testVisitStringLengthConstraints() {
        // Test description length constraints
        Visit visitWithLongDescription = createValidVisit();
        visitWithLongDescription.setDescription("A".repeat(501)); // Exceeds 500 character limit
        Set<ConstraintViolation<Visit>> descViolations = validator.validate(visitWithLongDescription);
        assertTrue(descViolations.stream().anyMatch(v -> 
                  v.getPropertyPath().toString().equals("description") && 
                  v.getMessage().contains("500")),
                  "Visit description should be limited to 500 characters");
        
        // Test diagnosis length constraints
        Visit visitWithLongDiagnosis = createValidVisit();
        visitWithLongDiagnosis.setDiagnosis("A".repeat(501)); // Exceeds 500 character limit
        Set<ConstraintViolation<Visit>> diagViolations = validator.validate(visitWithLongDiagnosis);
        assertTrue(diagViolations.stream().anyMatch(v -> 
                  v.getPropertyPath().toString().equals("diagnosis") && 
                  v.getMessage().contains("500")),
                  "Visit diagnosis should be limited to 500 characters");
        
        // Test treatment length constraints
        Visit visitWithLongTreatment = createValidVisit();
        visitWithLongTreatment.setTreatment("A".repeat(501)); // Exceeds 500 character limit
        Set<ConstraintViolation<Visit>> treatViolations = validator.validate(visitWithLongTreatment);
        assertTrue(treatViolations.stream().anyMatch(v -> 
                  v.getPropertyPath().toString().equals("treatment") && 
                  v.getMessage().contains("500")),
                  "Visit treatment should be limited to 500 characters");
    }
    
    /**
     * Test Visit numeric constraints
     */
    private void testVisitNumericConstraints() {
        // Test negative cost is invalid
        Visit visitWithNegativeCost = createValidVisit();
        visitWithNegativeCost.setCost(BigDecimal.valueOf(-10.00));
        Set<ConstraintViolation<Visit>> negativeViolations = validator.validate(visitWithNegativeCost);
        assertTrue(negativeViolations.stream().anyMatch(v -> 
                  v.getPropertyPath().toString().equals("cost") && 
                  v.getMessage().contains("positive")),
                  "Visit cost should be positive");
        
        // Test positive cost is valid
        Visit visitWithPositiveCost = createValidVisit();
        visitWithPositiveCost.setCost(BigDecimal.valueOf(75.50));
        Set<ConstraintViolation<Visit>> positiveViolations = validator.validate(visitWithPositiveCost);
        assertFalse(positiveViolations.stream().anyMatch(v -> 
                   v.getPropertyPath().toString().equals("cost")),
                   "Visit with positive cost should be valid");
    }
    
    /**
     * Test Owner form validation consistency
     */
    private void testOwnerValidationConsistency() {
        // Test required field validation consistency
        Owner emptyOwner = new Owner();
        Set<ConstraintViolation<Owner>> violations = validator.validate(emptyOwner);
        
        // Should have violations for required fields
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("firstName")),
                  "Owner first name should be required");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("lastName")),
                  "Owner last name should be required");
        
        // Test valid owner passes validation
        Owner validOwner = createValidOwner();
        Set<ConstraintViolation<Owner>> validViolations = validator.validate(validOwner);
        assertTrue(validViolations.isEmpty(), 
                  "Valid owner should pass validation. Violations: " + validViolations);
        
        // Test string length constraints consistency
        testOwnerStringLengthConstraints();
        
        // Test email format constraints
        testOwnerEmailConstraints();
    }
    
    /**
     * Test Owner string length constraints
     */
    private void testOwnerStringLengthConstraints() {
        // Test first name length constraints
        Owner ownerWithLongFirstName = createValidOwner();
        ownerWithLongFirstName.setFirstName("A".repeat(51)); // Exceeds 50 character limit
        Set<ConstraintViolation<Owner>> firstNameViolations = validator.validate(ownerWithLongFirstName);
        assertTrue(firstNameViolations.stream().anyMatch(v -> 
                  v.getPropertyPath().toString().equals("firstName") && 
                  v.getMessage().contains("50")),
                  "Owner first name should be limited to 50 characters");
        
        // Test last name length constraints
        Owner ownerWithLongLastName = createValidOwner();
        ownerWithLongLastName.setLastName("A".repeat(51)); // Exceeds 50 character limit
        Set<ConstraintViolation<Owner>> lastNameViolations = validator.validate(ownerWithLongLastName);
        assertTrue(lastNameViolations.stream().anyMatch(v -> 
                  v.getPropertyPath().toString().equals("lastName") && 
                  v.getMessage().contains("50")),
                  "Owner last name should be limited to 50 characters");
        
        // Test address length constraints
        Owner ownerWithLongAddress = createValidOwner();
        ownerWithLongAddress.setAddress("A".repeat(201)); // Exceeds 200 character limit
        Set<ConstraintViolation<Owner>> addressViolations = validator.validate(ownerWithLongAddress);
        assertTrue(addressViolations.stream().anyMatch(v -> 
                  v.getPropertyPath().toString().equals("address") && 
                  v.getMessage().contains("200")),
                  "Owner address should be limited to 200 characters");
        
        // Test city length constraints
        Owner ownerWithLongCity = createValidOwner();
        ownerWithLongCity.setCity("A".repeat(51)); // Exceeds 50 character limit
        Set<ConstraintViolation<Owner>> cityViolations = validator.validate(ownerWithLongCity);
        assertTrue(cityViolations.stream().anyMatch(v -> 
                  v.getPropertyPath().toString().equals("city") && 
                  v.getMessage().contains("50")),
                  "Owner city should be limited to 50 characters");
        
        // Test telephone length constraints
        Owner ownerWithLongTelephone = createValidOwner();
        ownerWithLongTelephone.setTelephone("A".repeat(16)); // Exceeds 15 character limit
        Set<ConstraintViolation<Owner>> telephoneViolations = validator.validate(ownerWithLongTelephone);
        assertTrue(telephoneViolations.stream().anyMatch(v -> 
                  v.getPropertyPath().toString().equals("telephone") && 
                  v.getMessage().contains("15")),
                  "Owner telephone should be limited to 15 characters");
        
        // Test email length constraints
        Owner ownerWithLongEmail = createValidOwner();
        ownerWithLongEmail.setEmail("a".repeat(85) + "@test.com"); // Total 94 characters, under 100 limit
        Set<ConstraintViolation<Owner>> emailViolations = validator.validate(ownerWithLongEmail);
        // This should pass since it's under 100 characters
        assertFalse(emailViolations.stream().anyMatch(v -> 
                   v.getPropertyPath().toString().equals("email") && 
                   v.getMessage().contains("100")),
                   "Email under 100 characters should be valid");
        
        // Test email that actually exceeds 100 characters
        Owner ownerWithVeryLongEmail = createValidOwner();
        ownerWithVeryLongEmail.setEmail("a".repeat(95) + "@test.com"); // Total 104 characters, exceeds 100 limit
        Set<ConstraintViolation<Owner>> longEmailViolations = validator.validate(ownerWithVeryLongEmail);
        assertTrue(longEmailViolations.stream().anyMatch(v -> 
                  v.getPropertyPath().toString().equals("email") && 
                  v.getMessage().contains("100")),
                  "Owner email should be limited to 100 characters");
    }
    
    /**
     * Test Owner email format constraints
     */
    private void testOwnerEmailConstraints() {
        // Test invalid email format
        Owner ownerWithInvalidEmail = createValidOwner();
        ownerWithInvalidEmail.setEmail("invalid-email");
        Set<ConstraintViolation<Owner>> invalidEmailViolations = validator.validate(ownerWithInvalidEmail);
        assertTrue(invalidEmailViolations.stream().anyMatch(v -> 
                  v.getPropertyPath().toString().equals("email") && 
                  v.getMessage().toLowerCase().contains("email")),
                  "Invalid email format should be rejected");
        
        // Test valid email format
        Owner ownerWithValidEmail = createValidOwner();
        ownerWithValidEmail.setEmail("valid@example.com");
        Set<ConstraintViolation<Owner>> validEmailViolations = validator.validate(ownerWithValidEmail);
        assertFalse(validEmailViolations.stream().anyMatch(v -> 
                   v.getPropertyPath().toString().equals("email")),
                   "Valid email format should be accepted");
    }
    
    /**
     * Test Veterinarian form validation consistency
     */
    private void testVeterinarianValidationConsistency() {
        // Test required field validation consistency
        Veterinarian emptyVet = new Veterinarian();
        Set<ConstraintViolation<Veterinarian>> violations = validator.validate(emptyVet);
        
        // Should have violations for required fields
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("firstName")),
                  "Veterinarian first name should be required");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("lastName")),
                  "Veterinarian last name should be required");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("licenseNumber")),
                  "Veterinarian license number should be required");
        
        // Test valid veterinarian passes validation
        Veterinarian validVet = createValidVeterinarian();
        Set<ConstraintViolation<Veterinarian>> validViolations = validator.validate(validVet);
        assertTrue(validViolations.isEmpty(), 
                  "Valid veterinarian should pass validation. Violations: " + validViolations);
        
        // Test string length constraints consistency
        testVeterinarianStringLengthConstraints();
    }
    
    /**
     * Test Veterinarian string length constraints
     */
    private void testVeterinarianStringLengthConstraints() {
        // Test first name length constraints
        Veterinarian vetWithLongFirstName = createValidVeterinarian();
        vetWithLongFirstName.setFirstName("A".repeat(51)); // Exceeds 50 character limit
        Set<ConstraintViolation<Veterinarian>> firstNameViolations = validator.validate(vetWithLongFirstName);
        assertTrue(firstNameViolations.stream().anyMatch(v -> 
                  v.getPropertyPath().toString().equals("firstName") && 
                  v.getMessage().contains("50")),
                  "Veterinarian first name should be limited to 50 characters");
        
        // Test last name length constraints
        Veterinarian vetWithLongLastName = createValidVeterinarian();
        vetWithLongLastName.setLastName("A".repeat(51)); // Exceeds 50 character limit
        Set<ConstraintViolation<Veterinarian>> lastNameViolations = validator.validate(vetWithLongLastName);
        assertTrue(lastNameViolations.stream().anyMatch(v -> 
                  v.getPropertyPath().toString().equals("lastName") && 
                  v.getMessage().contains("50")),
                  "Veterinarian last name should be limited to 50 characters");
        
        // Test specialties length constraints
        Veterinarian vetWithLongSpecialties = createValidVeterinarian();
        vetWithLongSpecialties.setSpecialties("A".repeat(201)); // Exceeds 200 character limit
        Set<ConstraintViolation<Veterinarian>> specialtiesViolations = validator.validate(vetWithLongSpecialties);
        assertTrue(specialtiesViolations.stream().anyMatch(v -> 
                  v.getPropertyPath().toString().equals("specialties") && 
                  v.getMessage().contains("200")),
                  "Veterinarian specialties should be limited to 200 characters");
        
        // Test license number length constraints
        Veterinarian vetWithLongLicense = createValidVeterinarian();
        vetWithLongLicense.setLicenseNumber("A".repeat(21)); // Exceeds 20 character limit
        Set<ConstraintViolation<Veterinarian>> licenseViolations = validator.validate(vetWithLongLicense);
        assertTrue(licenseViolations.stream().anyMatch(v -> 
                  v.getPropertyPath().toString().equals("licenseNumber") && 
                  v.getMessage().contains("20")),
                  "Veterinarian license number should be limited to 20 characters");
    }
    
    /**
     * Test cross-form validation consistency
     * Ensures that similar fields across different forms have consistent validation rules
     */
    private void testCrossFormValidationConsistency() {
        // Test that first name validation is consistent across Owner and Veterinarian
        testFirstNameConsistency();
        
        // Test that last name validation is consistent across Owner and Veterinarian
        testLastNameConsistency();
        
        // Test that required field behavior is consistent
        testRequiredFieldConsistency();
        
        // Test that string length validation patterns are consistent
        testStringLengthConsistency();
    }
    
    /**
     * Test first name validation consistency across forms
     */
    private void testFirstNameConsistency() {
        String longFirstName = "A".repeat(51); // Exceeds 50 character limit
        
        // Test Owner first name validation
        Owner ownerWithLongFirstName = createValidOwner();
        ownerWithLongFirstName.setFirstName(longFirstName);
        Set<ConstraintViolation<Owner>> ownerViolations = validator.validate(ownerWithLongFirstName);
        boolean ownerHasFirstNameViolation = ownerViolations.stream().anyMatch(v -> 
                v.getPropertyPath().toString().equals("firstName") && 
                v.getMessage().contains("50"));
        
        // Test Veterinarian first name validation
        Veterinarian vetWithLongFirstName = createValidVeterinarian();
        vetWithLongFirstName.setFirstName(longFirstName);
        Set<ConstraintViolation<Veterinarian>> vetViolations = validator.validate(vetWithLongFirstName);
        boolean vetHasFirstNameViolation = vetViolations.stream().anyMatch(v -> 
                v.getPropertyPath().toString().equals("firstName") && 
                v.getMessage().contains("50"));
        
        // Both should have consistent validation behavior
        assertEquals(ownerHasFirstNameViolation, vetHasFirstNameViolation,
                    "First name validation should be consistent across Owner and Veterinarian forms");
        assertTrue(ownerHasFirstNameViolation && vetHasFirstNameViolation,
                  "Both Owner and Veterinarian should reject first names exceeding 50 characters");
    }
    
    /**
     * Test last name validation consistency across forms
     */
    private void testLastNameConsistency() {
        String longLastName = "A".repeat(51); // Exceeds 50 character limit
        
        // Test Owner last name validation
        Owner ownerWithLongLastName = createValidOwner();
        ownerWithLongLastName.setLastName(longLastName);
        Set<ConstraintViolation<Owner>> ownerViolations = validator.validate(ownerWithLongLastName);
        boolean ownerHasLastNameViolation = ownerViolations.stream().anyMatch(v -> 
                v.getPropertyPath().toString().equals("lastName") && 
                v.getMessage().contains("50"));
        
        // Test Veterinarian last name validation
        Veterinarian vetWithLongLastName = createValidVeterinarian();
        vetWithLongLastName.setLastName(longLastName);
        Set<ConstraintViolation<Veterinarian>> vetViolations = validator.validate(vetWithLongLastName);
        boolean vetHasLastNameViolation = vetViolations.stream().anyMatch(v -> 
                v.getPropertyPath().toString().equals("lastName") && 
                v.getMessage().contains("50"));
        
        // Both should have consistent validation behavior
        assertEquals(ownerHasLastNameViolation, vetHasLastNameViolation,
                    "Last name validation should be consistent across Owner and Veterinarian forms");
        assertTrue(ownerHasLastNameViolation && vetHasLastNameViolation,
                  "Both Owner and Veterinarian should reject last names exceeding 50 characters");
    }
    
    /**
     * Test required field validation consistency
     */
    private void testRequiredFieldConsistency() {
        // Test that all forms consistently reject empty required fields
        Pet emptyPet = new Pet();
        Visit emptyVisit = new Visit();
        Owner emptyOwner = new Owner();
        Veterinarian emptyVet = new Veterinarian();
        
        Set<ConstraintViolation<Pet>> petViolations = validator.validate(emptyPet);
        Set<ConstraintViolation<Visit>> visitViolations = validator.validate(emptyVisit);
        Set<ConstraintViolation<Owner>> ownerViolations = validator.validate(emptyOwner);
        Set<ConstraintViolation<Veterinarian>> vetViolations = validator.validate(emptyVet);
        
        // All forms should have validation violations for required fields
        assertFalse(petViolations.isEmpty(), "Pet form should have validation violations for required fields");
        assertFalse(visitViolations.isEmpty(), "Visit form should have validation violations for required fields");
        assertFalse(ownerViolations.isEmpty(), "Owner form should have validation violations for required fields");
        assertFalse(vetViolations.isEmpty(), "Veterinarian form should have validation violations for required fields");
        
        // All violations should have meaningful error messages
        petViolations.forEach(v -> assertNotNull(v.getMessage(), "Pet validation message should not be null"));
        visitViolations.forEach(v -> assertNotNull(v.getMessage(), "Visit validation message should not be null"));
        ownerViolations.forEach(v -> assertNotNull(v.getMessage(), "Owner validation message should not be null"));
        vetViolations.forEach(v -> assertNotNull(v.getMessage(), "Veterinarian validation message should not be null"));
    }
    
    /**
     * Test string length validation consistency
     */
    private void testStringLengthConsistency() {
        // Test that similar field types have consistent length limits
        // Names should be limited to 50 characters across all forms
        String longName = "A".repeat(51);
        
        Pet petWithLongName = createValidPet();
        petWithLongName.setName(longName);
        boolean petNameViolation = !validator.validate(petWithLongName).isEmpty();
        
        Owner ownerWithLongFirstName = createValidOwner();
        ownerWithLongFirstName.setFirstName(longName);
        boolean ownerFirstNameViolation = !validator.validate(ownerWithLongFirstName).isEmpty();
        
        Owner ownerWithLongLastName = createValidOwner();
        ownerWithLongLastName.setLastName(longName);
        boolean ownerLastNameViolation = !validator.validate(ownerWithLongLastName).isEmpty();
        
        Veterinarian vetWithLongFirstName = createValidVeterinarian();
        vetWithLongFirstName.setFirstName(longName);
        boolean vetFirstNameViolation = !validator.validate(vetWithLongFirstName).isEmpty();
        
        Veterinarian vetWithLongLastName = createValidVeterinarian();
        vetWithLongLastName.setLastName(longName);
        boolean vetLastNameViolation = !validator.validate(vetWithLongLastName).isEmpty();
        
        // All name fields should consistently reject strings exceeding 50 characters
        assertTrue(petNameViolation, "Pet name should reject strings exceeding 50 characters");
        assertTrue(ownerFirstNameViolation, "Owner first name should reject strings exceeding 50 characters");
        assertTrue(ownerLastNameViolation, "Owner last name should reject strings exceeding 50 characters");
        assertTrue(vetFirstNameViolation, "Veterinarian first name should reject strings exceeding 50 characters");
        assertTrue(vetLastNameViolation, "Veterinarian last name should reject strings exceeding 50 characters");
    }
    
    // Helper methods for creating valid test objects
    
    private Pet createValidPet() {
        Pet pet = new Pet();
        pet.setName(generateValidName());
        pet.setSpecies(generateValidSpecies());
        pet.setBreed(generateValidBreed());
        pet.setBirthDate(generateValidPastDate());
        pet.setMedicalHistory(generateValidMedicalHistory());
        return pet;
    }
    
    private Visit createValidVisit() {
        Visit visit = new Visit();
        visit.setVisitDate(generateValidFutureDateTime());
        visit.setDescription(generateValidDescription());
        visit.setDiagnosis(generateValidDiagnosis());
        visit.setTreatment(generateValidTreatment());
        visit.setCost(generateValidCost());
        visit.setEmergencyVisit(false);
        visit.setCompleted(false);
        return visit;
    }
    
    private Owner createValidOwner() {
        Owner owner = new Owner();
        owner.setFirstName(generateValidName());
        owner.setLastName(generateValidName());
        owner.setAddress(generateValidAddress());
        owner.setCity(generateValidCity());
        owner.setTelephone(generateValidTelephone());
        owner.setEmail(generateValidEmail());
        return owner;
    }
    
    private Veterinarian createValidVeterinarian() {
        Veterinarian vet = new Veterinarian();
        vet.setFirstName(generateValidName());
        vet.setLastName(generateValidName());
        vet.setSpecialties(generateValidSpecialties());
        vet.setLicenseNumber(generateValidLicenseNumber());
        return vet;
    }
    
    // Generators for valid test data
    
    private String generateValidName() {
        String[] names = {"John", "Jane", "Michael", "Sarah", "David", "Emily", "Robert", "Lisa"};
        return names[random.nextInt(names.length)];
    }
    
    private String generateValidSpecies() {
        String[] species = {"Dog", "Cat", "Bird", "Rabbit", "Hamster", "Fish"};
        return species[random.nextInt(species.length)];
    }
    
    private String generateValidBreed() {
        String[] breeds = {"Golden Retriever", "Siamese", "Parakeet", "Holland Lop", "Syrian", "Goldfish"};
        return breeds[random.nextInt(breeds.length)];
    }
    
    private LocalDate generateValidPastDate() {
        return LocalDate.now().minusDays(1 + random.nextInt(3650)); // 1 day to 10 years ago
    }
    
    private LocalDateTime generateValidFutureDateTime() {
        return LocalDateTime.now().plusDays(1 + random.nextInt(30)); // 1 to 30 days from now
    }
    
    private String generateValidMedicalHistory() {
        String[] histories = {"No known allergies", "Vaccinated", "Previous surgery", "Healthy"};
        return histories[random.nextInt(histories.length)];
    }
    
    private String generateValidDescription() {
        String[] descriptions = {"Annual checkup", "Vaccination", "Emergency visit", "Follow-up"};
        return descriptions[random.nextInt(descriptions.length)];
    }
    
    private String generateValidDiagnosis() {
        String[] diagnoses = {"Healthy", "Minor infection", "Routine care", "Preventive treatment"};
        return diagnoses[random.nextInt(diagnoses.length)];
    }
    
    private String generateValidTreatment() {
        String[] treatments = {"Vaccination", "Medication prescribed", "Observation", "Follow-up recommended"};
        return treatments[random.nextInt(treatments.length)];
    }
    
    private BigDecimal generateValidCost() {
        return BigDecimal.valueOf(25.00 + random.nextDouble() * 200.00); // $25 to $225
    }
    
    private String generateValidAddress() {
        return (100 + random.nextInt(9900)) + " Main Street";
    }
    
    private String generateValidCity() {
        String[] cities = {"Springfield", "Madison", "Franklin", "Georgetown", "Clinton"};
        return cities[random.nextInt(cities.length)];
    }
    
    private String generateValidTelephone() {
        return String.format("555-%03d-%04d", random.nextInt(1000), random.nextInt(10000));
    }
    
    private String generateValidEmail() {
        String[] domains = {"example.com", "test.org", "sample.net"};
        return "user" + random.nextInt(10000) + "@" + domains[random.nextInt(domains.length)];
    }
    
    private String generateValidSpecialties() {
        String[] specialties = {"Surgery", "Cardiology", "Dermatology", "Orthopedics", "Emergency Medicine"};
        return specialties[random.nextInt(specialties.length)];
    }
    
    private String generateValidLicenseNumber() {
        StringBuilder license = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (random.nextBoolean()) {
                license.append((char) ('A' + random.nextInt(26)));
            } else {
                license.append(random.nextInt(10));
            }
        }
        return license.toString();
    }
    
    /**
     * Run a property test with specified number of iterations
     */
    private void runPropertyTest(int iterations, Runnable property) {
        for (int i = 0; i < iterations; i++) {
            try {
                property.run();
            } catch (Exception e) {
                throw new AssertionError("Property test failed on iteration " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
    }
}