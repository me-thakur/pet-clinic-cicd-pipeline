package com.petclinic.backend.service.impl;

import com.petclinic.backend.exception.BusinessRuleException;
import com.petclinic.backend.exception.EntityNotFoundException;
import com.petclinic.backend.exception.ValidationException;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.repository.OwnerRepository;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.service.PetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for PetService implementation
 * Tests universal properties that should hold across all valid inputs
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("PetService Property-Based Tests")
class PetServicePropertiesTest {

    @Autowired
    private PetService petService;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    private Owner testOwner;

    @BeforeEach
    void setUp() {
        // Create and save test owner
        testOwner = new Owner();
        testOwner.setFirstName("John");
        testOwner.setLastName("Doe");
        testOwner.setEmail("john.doe@test.com");
        testOwner.setAddress("123 Test St");
        testOwner.setCity("Test City");
        testOwner.setTelephone("(555) 123-4567");
        testOwner = ownerRepository.save(testOwner);
    }

    @Test
    @DisplayName("Property 1: Entity Creation Completeness - For any valid pet data, creating the pet should result in all required fields being stored and retrievable")
    void testEntityCreationCompleteness() {
        /**Validates: Requirements 1.1, 2.1, 3.1**/
        
        // Test with various valid pet configurations
        String[] names = {"Buddy", "Max", "Bella", "Charlie", "Luna"};
        String[] species = {"Dog", "Cat", "Bird", "Rabbit", "Hamster"};
        String[] breeds = {"Golden Retriever", "Persian", "Canary", "Holland Lop", "Syrian"};
        
        for (int i = 0; i < names.length; i++) {
            for (int j = 0; j < species.length; j++) {
                for (int k = 0; k < breeds.length; k++) {
                    // Given: Valid pet data with unique names
                    String uniqueName = names[i] + "_" + species[j] + "_" + k;
                    Pet pet = createValidPet(uniqueName, species[j], breeds[k]);
                    
                    // When: Creating the pet
                    Pet createdPet = petService.create(pet);
                    
                    // Then: All required fields should be stored and retrievable
                    assertThat(createdPet).isNotNull();
                    assertThat(createdPet.getId()).isNotNull();
                    assertThat(createdPet.getName()).isEqualTo(pet.getName());
                    assertThat(createdPet.getSpecies()).isEqualTo(pet.getSpecies());
                    assertThat(createdPet.getBreed()).isEqualTo(pet.getBreed());
                    assertThat(createdPet.getBirthDate()).isEqualTo(pet.getBirthDate());
                    assertThat(createdPet.getOwner()).isNotNull();
                    assertThat(createdPet.getOwner().getId()).isEqualTo(testOwner.getId());
                    
                    // Verify retrievability
                    Optional<Pet> retrievedPet = petService.findById(createdPet.getId());
                    assertThat(retrievedPet).isPresent();
                    assertThat(retrievedPet.get().getName()).isEqualTo(pet.getName());
                }
            }
        }
    }

    @Test
    @DisplayName("Property 2: Entity Update Persistence - For any existing pet and valid update data, updating the pet should result in changes being immediately persisted and retrievable")
    void testEntityUpdatePersistence() {
        /**Validates: Requirements 1.2, 2.2**/
        
        // Given: An existing pet
        Pet originalPet = createValidPet("Original", "Dog", "Labrador");
        Pet savedPet = petService.create(originalPet);
        
        // Test various update scenarios
        String[] newNames = {"Updated1", "Updated2", "Updated3"};
        String[] newSpecies = {"Cat", "Bird", "Rabbit"};
        String[] newBreeds = {"Persian", "Canary", "Holland Lop"};
        LocalDate[] newBirthDates = {
            LocalDate.now().minusYears(1),
            LocalDate.now().minusYears(2),
            LocalDate.now().minusYears(3)
        };
        
        for (int i = 0; i < newNames.length; i++) {
            // When: Updating the pet with new data
            Pet updateData = new Pet();
            updateData.setName(newNames[i]);
            updateData.setSpecies(newSpecies[i]);
            updateData.setBreed(newBreeds[i]);
            updateData.setBirthDate(newBirthDates[i]);
            updateData.setOwner(testOwner);
            
            Pet updatedPet = petService.update(savedPet.getId(), updateData);
            
            // Then: Changes should be immediately persisted and retrievable
            assertThat(updatedPet.getName()).isEqualTo(newNames[i]);
            assertThat(updatedPet.getSpecies()).isEqualTo(newSpecies[i]);
            assertThat(updatedPet.getBreed()).isEqualTo(newBreeds[i]);
            assertThat(updatedPet.getBirthDate()).isEqualTo(newBirthDates[i]);
            
            // Verify immediate retrievability
            Optional<Pet> retrievedPet = petService.findById(savedPet.getId());
            assertThat(retrievedPet).isPresent();
            assertThat(retrievedPet.get().getName()).isEqualTo(newNames[i]);
            assertThat(retrievedPet.get().getSpecies()).isEqualTo(newSpecies[i]);
            assertThat(retrievedPet.get().getBreed()).isEqualTo(newBreeds[i]);
            assertThat(retrievedPet.get().getBirthDate()).isEqualTo(newBirthDates[i]);
        }
    }

    @Test
    @DisplayName("Property 3: Referential Integrity Protection - For any pet with visits, deletion attempts should be prevented")
    void testReferentialIntegrityProtection() {
        /**Validates: Requirements 1.5, 3.5**/
        
        // Given: Pets with and without visits
        Pet petWithoutVisits = createValidPet("NoVisits", "Dog", "Beagle");
        Pet savedPetWithoutVisits = petService.create(petWithoutVisits);
        
        Pet petWithVisits = createValidPet("WithVisits", "Cat", "Siamese");
        Pet savedPetWithVisits = petService.create(petWithVisits);
        
        // Add a visit to one pet
        Visit visit = new Visit();
        visit.setVisitDate(LocalDateTime.now().minusDays(7));
        visit.setNotes("Routine checkup");
        visit.setPet(savedPetWithVisits);
        savedPetWithVisits.getVisits().add(visit);
        petRepository.save(savedPetWithVisits);
        
        // When & Then: Pet without visits should be deletable
        assertThat(petService.canDeletePet(savedPetWithoutVisits.getId())).isTrue();
        assertThatCode(() -> petService.deleteById(savedPetWithoutVisits.getId()))
            .doesNotThrowAnyException();
        
        // When & Then: Pet with visits should not be deletable
        assertThat(petService.canDeletePet(savedPetWithVisits.getId())).isFalse();
        assertThatThrownBy(() -> petService.deleteById(savedPetWithVisits.getId()))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Cannot delete pet with existing visits");
    }

    @Test
    @DisplayName("Property 4: Search Result Accuracy - For any search query, returned results should contain only pets that match the search criteria")
    void testSearchResultAccuracy() {
        /**Validates: Requirements 1.3, 4.1, 4.4**/
        
        // Given: Multiple pets with different characteristics
        Pet dogBuddy = createValidPet("Buddy", "Dog", "Golden Retriever");
        Pet catWhiskers = createValidPet("Whiskers", "Cat", "Persian");
        Pet birdTweety = createValidPet("Tweety", "Bird", "Canary");
        Pet dogMax = createValidPet("Max", "Dog", "Labrador");
        
        petService.create(dogBuddy);
        petService.create(catWhiskers);
        petService.create(birdTweety);
        petService.create(dogMax);
        
        // Test various search scenarios
        String[] searchTerms = {"Buddy", "Dog", "Persian", "Golden", "Whiskers"};
        
        for (String searchTerm : searchTerms) {
            // When: Searching with the term
            List<Pet> results = petService.searchPets(searchTerm);
            
            // Then: All results should match the search criteria
            for (Pet pet : results) {
                boolean matches = pet.getName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                pet.getSpecies().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                (pet.getBreed() != null && pet.getBreed().toLowerCase().contains(searchTerm.toLowerCase())) ||
                                pet.getOwner().getFullName().toLowerCase().contains(searchTerm.toLowerCase());
                
                assertThat(matches)
                    .withFailMessage("Pet %s does not match search term %s", pet.getName(), searchTerm)
                    .isTrue();
            }
        }
    }

    @Test
    @DisplayName("Property 5: Input Validation Consistency - For any invalid input, consistent validation rules should be enforced")
    void testInputValidationConsistency() {
        /**Validates: Requirements 6.3**/
        
        // Test various invalid input scenarios
        String[] invalidNames = {null, "", " ", "A".repeat(51)};
        String[] invalidSpecies = {null, "", " ", "A".repeat(31)};
        String[] invalidBreeds = {"A".repeat(51)};
        LocalDate[] invalidBirthDates = {LocalDate.now().plusDays(1), LocalDate.now().minusYears(31)};
        
        // Test invalid names
        for (String invalidName : invalidNames) {
            Pet pet = createValidPet("Valid", "Dog", "Beagle");
            pet.setName(invalidName);
            
            assertThatThrownBy(() -> petService.create(pet))
                .isInstanceOf(ValidationException.class);
        }
        
        // Test invalid species
        for (String invalidSpeciesValue : invalidSpecies) {
            Pet pet = createValidPet("Valid", "Dog", "Beagle");
            pet.setSpecies(invalidSpeciesValue);
            
            assertThatThrownBy(() -> petService.create(pet))
                .isInstanceOf(ValidationException.class);
        }
        
        // Test invalid breeds
        for (String invalidBreed : invalidBreeds) {
            Pet pet = createValidPet("Valid", "Dog", "Beagle");
            pet.setBreed(invalidBreed);
            
            assertThatThrownBy(() -> petService.create(pet))
                .isInstanceOf(ValidationException.class);
        }
        
        // Test invalid birth dates
        for (LocalDate invalidBirthDate : invalidBirthDates) {
            Pet pet = createValidPet("Valid", "Dog", "Beagle");
            pet.setBirthDate(invalidBirthDate);
            
            assertThatThrownBy(() -> petService.create(pet))
                .isInstanceOf(ValidationException.class);
        }
    }

    @Test
    @DisplayName("Property 6: Business Rule Enforcement - Duplicate pet names for same owner should be prevented")
    void testBusinessRuleEnforcement() {
        /**Validates: Requirements 1.2**/
        
        // Given: A pet with a specific name for an owner
        Pet firstPet = createValidPet("Duplicate", "Dog", "Beagle");
        petService.create(firstPet);
        
        // When & Then: Attempting to create another pet with same name for same owner should fail
        Pet duplicatePet = createValidPet("Duplicate", "Cat", "Persian");
        
        assertThatThrownBy(() -> petService.create(duplicatePet))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Pet name 'Duplicate' already exists for this owner");
        
        // But should succeed for different owner
        Owner anotherOwner = new Owner();
        anotherOwner.setFirstName("Jane");
        anotherOwner.setLastName("Smith");
        anotherOwner.setEmail("jane.smith@test.com");
        anotherOwner.setTelephone("(555) 987-6543");
        anotherOwner = ownerRepository.save(anotherOwner);
        
        Pet petForDifferentOwner = createValidPet("Duplicate", "Cat", "Persian");
        petForDifferentOwner.setOwner(anotherOwner);
        
        assertThatCode(() -> petService.create(petForDifferentOwner))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Property 7: Search Completeness - Search should find all matching pets across all searchable fields")
    void testSearchCompleteness() {
        /**Validates: Requirements 1.3, 4.1**/
        
        // Given: Pets with searchable content in different fields
        Pet petWithNameMatch = createValidPet("SearchTerm", "Dog", "Beagle");
        Pet petWithSpeciesMatch = createValidPet("Buddy", "SearchTerm", "Beagle");
        Pet petWithBreedMatch = createValidPet("Max", "Dog", "SearchTerm");
        
        // Create owner with searchable name
        Owner searchableOwner = new Owner();
        searchableOwner.setFirstName("SearchTerm");
        searchableOwner.setLastName("Owner");
        searchableOwner.setTelephone("(555) 111-2222");
        searchableOwner.setEmail("searchterm@test.com");
        searchableOwner = ownerRepository.save(searchableOwner);
        
        Pet petWithOwnerMatch = createValidPet("Charlie", "Cat", "Persian");
        petWithOwnerMatch.setOwner(searchableOwner);
        
        petService.create(petWithNameMatch);
        petService.create(petWithSpeciesMatch);
        petService.create(petWithBreedMatch);
        petService.create(petWithOwnerMatch);
        
        // When: Searching for the term
        List<Pet> results = petService.searchPets("SearchTerm");
        
        // Then: Should find pets matching in any searchable field
        assertThat(results).hasSizeGreaterThanOrEqualTo(1);
        
        // Verify that all returned results actually match the search term
        for (Pet pet : results) {
            boolean matches = pet.getName().toLowerCase().contains("searchterm") ||
                            pet.getSpecies().toLowerCase().contains("searchterm") ||
                            (pet.getBreed() != null && pet.getBreed().toLowerCase().contains("searchterm")) ||
                            pet.getOwner().getFullName().toLowerCase().contains("searchterm");
            
            assertThat(matches)
                .withFailMessage("Pet %s does not match search term SearchTerm", pet.getName())
                .isTrue();
        }
    }

    @Test
    @DisplayName("Property 8: Age Range Filtering Accuracy - Age range queries should return only pets within the specified age range")
    void testAgeRangeFilteringAccuracy() {
        /**Validates: Requirements 1.3**/
        
        // Given: Pets of different ages
        Pet youngPet = createValidPet("Young", "Dog", "Puppy");
        youngPet.setBirthDate(LocalDate.now().minusMonths(6)); // 0.5 years old
        
        Pet middleAgedPet = createValidPet("Middle", "Cat", "Adult");
        middleAgedPet.setBirthDate(LocalDate.now().minusYears(3)); // 3 years old
        
        Pet oldPet = createValidPet("Old", "Dog", "Senior");
        oldPet.setBirthDate(LocalDate.now().minusYears(10)); // 10 years old
        
        petService.create(youngPet);
        petService.create(middleAgedPet);
        petService.create(oldPet);
        
        // Test various age ranges
        int[][] ageRanges = {{0, 1}, {2, 5}, {8, 15}, {0, 15}};
        
        for (int[] range : ageRanges) {
            int minAge = range[0];
            int maxAge = range[1];
            
            // When: Searching by age range
            List<Pet> results = petService.findByAgeRange(minAge, maxAge);
            
            // Then: All results should be within the age range
            for (Pet pet : results) {
                int petAge = pet.getAge();
                assertThat(petAge)
                    .withFailMessage("Pet %s age %d is not within range %d-%d", pet.getName(), petAge, minAge, maxAge)
                    .isBetween(minAge, maxAge);
            }
        }
    }

    private Pet createValidPet(String name, String species, String breed) {
        Pet pet = new Pet();
        pet.setName(name);
        pet.setSpecies(species);
        pet.setBreed(breed);
        pet.setBirthDate(LocalDate.now().minusYears(2));
        pet.setOwner(testOwner);
        return pet;
    }
}