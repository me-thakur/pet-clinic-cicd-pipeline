package com.petclinic.backend.repository;

import com.petclinic.backend.model.Owner;
import com.petclinic.backend.model.Pet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Tests for Pet Clinic Search and Filter Functionality
 * **Property 12: Search and Filter Accuracy**
 * **Validates: Requirements 8.4**
 * 
 * Tests that search query and filter criteria applied to pets or owners
 * return accurate results that include all matching records and exclude
 * all non-matching records.
 */
@DataJpaTest
@ActiveProfiles("test")
public class PetClinicSearchFilterTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OwnerRepository ownerRepository;
    
    @Autowired
    private PetRepository petRepository;

    @Test
    void ownerFirstNameSearchReturnsAccurateResults() {
        // Arrange: Create owners with known first names
        Owner johnOwner = createOwner("John", "Doe", "john.doe@example.com");
        Owner janeOwner = createOwner("Jane", "Smith", "jane.smith@example.com");
        Owner bobOwner = createOwner("Bob", "Johnson", "bob.johnson@example.com");
        Owner johnnyOwner = createOwner("Johnny", "Walker", "johnny.walker@example.com");
        
        entityManager.flush();
        
        // Act: Search for owners with "John" in first name
        List<Owner> johnResults = ownerRepository.findByFirstNameContainingIgnoreCase("John");
        
        // Assert: Should include John and Johnny, exclude Jane and Bob
        assertThat(johnResults).hasSize(2);
        assertThat(johnResults).extracting(Owner::getFirstName)
            .containsExactlyInAnyOrder("John", "Johnny");
        
        // Act: Search for owners with "Jane" in first name
        List<Owner> janeResults = ownerRepository.findByFirstNameContainingIgnoreCase("Jane");
        
        // Assert: Should include only Jane
        assertThat(janeResults).hasSize(1);
        assertThat(janeResults.get(0).getFirstName()).isEqualTo("Jane");
        
        // Act: Search for non-existent name
        List<Owner> noResults = ownerRepository.findByFirstNameContainingIgnoreCase("NonExistent");
        
        // Assert: Should return empty list
        assertThat(noResults).isEmpty();
    }

    @Test
    void ownerLastNameSearchReturnsAccurateResults() {
        // Arrange: Create owners with known last names
        Owner smithOwner = createOwner("John", "Smith", "john.smith@example.com");
        Owner johnsonOwner = createOwner("Jane", "Johnson", "jane.johnson@example.com");
        Owner smithsonOwner = createOwner("Bob", "Smithson", "bob.smithson@example.com");
        
        entityManager.flush();
        
        // Act: Search for owners with "Smith" in last name
        List<Owner> smithResults = ownerRepository.findByLastNameContainingIgnoreCase("Smith");
        
        // Assert: Should include Smith and Smithson, exclude Johnson
        assertThat(smithResults).hasSize(2);
        assertThat(smithResults).extracting(Owner::getLastName)
            .containsExactlyInAnyOrder("Smith", "Smithson");
    }

    @Test
    void ownerCityFilterReturnsAccurateResults() {
        // Arrange: Create owners in different cities
        Owner springfieldOwner = createOwner("John", "Doe", "john@example.com", "Springfield");
        Owner shelbyvilleOwner = createOwner("Jane", "Smith", "jane@example.com", "Shelbyville");
        Owner springfieldOwner2 = createOwner("Bob", "Johnson", "bob@example.com", "Springfield");
        
        entityManager.flush();
        
        // Act: Filter by Springfield (case-insensitive)
        List<Owner> springfieldResults = ownerRepository.findByCityIgnoreCase("springfield");
        
        // Assert: Should include both Springfield owners
        assertThat(springfieldResults).hasSize(2);
        assertThat(springfieldResults).extracting(Owner::getFirstName)
            .containsExactlyInAnyOrder("John", "Bob");
        
        // Act: Filter by Shelbyville
        List<Owner> shelbyvilleResults = ownerRepository.findByCityIgnoreCase("Shelbyville");
        
        // Assert: Should include only Jane
        assertThat(shelbyvilleResults).hasSize(1);
        assertThat(shelbyvilleResults.get(0).getFirstName()).isEqualTo("Jane");
    }

    @Test
    void petNameSearchReturnsAccurateResults() {
        // Arrange: Create owner and pets with known names
        Owner owner = createOwner("Test", "Owner", "test@example.com");
        Pet buddyPet = createPet("Buddy", "Dog", owner);
        Pet maxPet = createPet("Max", "Cat", owner);
        Pet buddyBearPet = createPet("BuddyBear", "Dog", owner);
        
        entityManager.flush();
        
        // Act: Search for pets with "Buddy" in name
        List<Pet> buddyResults = petRepository.findByNameContainingIgnoreCase("Buddy");
        
        // Assert: Should include Buddy and BuddyBear, exclude Max
        assertThat(buddyResults).hasSize(2);
        assertThat(buddyResults).extracting(Pet::getName)
            .containsExactlyInAnyOrder("Buddy", "BuddyBear");
        
        // Act: Search for pets with "Max" in name
        List<Pet> maxResults = petRepository.findByNameContainingIgnoreCase("Max");
        
        // Assert: Should include only Max
        assertThat(maxResults).hasSize(1);
        assertThat(maxResults.get(0).getName()).isEqualTo("Max");
    }

    @Test
    void petSpeciesFilterReturnsAccurateResults() {
        // Arrange: Create owner and pets with different species
        Owner owner = createOwner("Test", "Owner", "test@example.com");
        Pet dogPet1 = createPet("Buddy", "Dog", owner);
        Pet catPet = createPet("Whiskers", "Cat", owner);
        Pet dogPet2 = createPet("Rex", "Dog", owner);
        Pet birdPet = createPet("Tweety", "Bird", owner);
        
        entityManager.flush();
        
        // Act: Filter by Dog species (case-insensitive)
        List<Pet> dogResults = petRepository.findBySpeciesIgnoreCase("dog");
        
        // Assert: Should include both dogs
        assertThat(dogResults).hasSize(2);
        assertThat(dogResults).extracting(Pet::getName)
            .containsExactlyInAnyOrder("Buddy", "Rex");
        
        // Act: Filter by Cat species
        List<Pet> catResults = petRepository.findBySpeciesIgnoreCase("Cat");
        
        // Assert: Should include only the cat
        assertThat(catResults).hasSize(1);
        assertThat(catResults.get(0).getName()).isEqualTo("Whiskers");
    }

    @Test
    void petBreedSearchReturnsAccurateResults() {
        // Arrange: Create owner and pets with different breeds
        Owner owner = createOwner("Test", "Owner", "test@example.com");
        Pet labradorPet = createPet("Buddy", "Dog", "Labrador", owner);
        Pet goldenPet = createPet("Max", "Dog", "Golden Retriever", owner);
        Pet labMixPet = createPet("Rex", "Dog", "Labrador Mix", owner);
        Pet poodlePet = createPet("Fluffy", "Dog", "Poodle", owner);
        
        entityManager.flush();
        
        // Act: Search for pets with "Labrador" in breed
        List<Pet> labradorResults = petRepository.findByBreedContainingIgnoreCase("Labrador");
        
        // Assert: Should include Labrador and Labrador Mix
        assertThat(labradorResults).hasSize(2);
        assertThat(labradorResults).extracting(Pet::getName)
            .containsExactlyInAnyOrder("Buddy", "Rex");
        
        // Act: Search for pets with "Golden" in breed
        List<Pet> goldenResults = petRepository.findByBreedContainingIgnoreCase("Golden");
        
        // Assert: Should include only Golden Retriever
        assertThat(goldenResults).hasSize(1);
        assertThat(goldenResults.get(0).getName()).isEqualTo("Max");
    }

    @Test
    void petAgeRangeFilterReturnsAccurateResults() {
        // Arrange: Create owner and pets with different ages
        Owner owner = createOwner("Test", "Owner", "test@example.com");
        LocalDate now = LocalDate.now();
        
        Pet youngPet = createPet("Young", "Dog", owner, now.minusYears(2)); // 2 years old
        Pet middlePet = createPet("Middle", "Dog", owner, now.minusYears(5)); // 5 years old
        Pet oldPet = createPet("Old", "Dog", owner, now.minusYears(10)); // 10 years old
        Pet veryOldPet = createPet("VeryOld", "Dog", owner, now.minusYears(15)); // 15 years old
        
        entityManager.flush();
        
        // Act: Search for pets between 3-8 years old (birth dates between now-8 and now-3)
        LocalDate maxBirthDate = now.minusYears(3);
        LocalDate minBirthDate = now.minusYears(8);
        List<Pet> ageRangeResults = petRepository.findByBirthDateBetween(minBirthDate, maxBirthDate);
        
        // Assert: Should include only the 5-year-old pet
        assertThat(ageRangeResults).hasSize(1);
        assertThat(ageRangeResults.get(0).getName()).isEqualTo("Middle");
        
        // Act: Search for senior pets (older than 7 years)
        LocalDate seniorCutoff = now.minusYears(7);
        List<Pet> seniorResults = petRepository.findSeniorPets(seniorCutoff);
        
        // Assert: Should include pets that are 10 and 15 years old
        assertThat(seniorResults).hasSize(2);
        assertThat(seniorResults).extracting(Pet::getName)
            .containsExactlyInAnyOrder("Old", "VeryOld");
    }

    @Test
    void searchReturnsEmptyResultsWhenNoMatches() {
        // Arrange: Create some test data
        Owner owner = createOwner("John", "Doe", "john@example.com");
        Pet pet = createPet("Buddy", "Dog", owner);
        
        entityManager.flush();
        
        // Act & Assert: Search for non-existent data should return empty results
        assertThat(ownerRepository.findByFirstNameContainingIgnoreCase("NonExistentName")).isEmpty();
        assertThat(ownerRepository.findByLastNameContainingIgnoreCase("NonExistentLastName")).isEmpty();
        assertThat(ownerRepository.findByCityIgnoreCase("NonExistentCity")).isEmpty();
        assertThat(petRepository.findByNameContainingIgnoreCase("NonExistentPetName")).isEmpty();
        assertThat(petRepository.findBySpeciesIgnoreCase("NonExistentSpecies")).isEmpty();
        assertThat(petRepository.findByBreedContainingIgnoreCase("NonExistentBreed")).isEmpty();
    }

    @Test
    void searchIsCaseInsensitive() {
        // Arrange: Create test data with mixed case
        Owner owner = createOwner("JoHn", "DoE", "john@example.com", "SpRiNgFiElD");
        Pet pet = createPet("BuDdY", "DoG", "LabraDor", owner);
        
        entityManager.flush();
        
        // Act & Assert: All searches should be case-insensitive
        assertThat(ownerRepository.findByFirstNameContainingIgnoreCase("john")).hasSize(1);
        assertThat(ownerRepository.findByFirstNameContainingIgnoreCase("JOHN")).hasSize(1);
        assertThat(ownerRepository.findByFirstNameContainingIgnoreCase("JoHn")).hasSize(1);
        
        assertThat(ownerRepository.findByCityIgnoreCase("springfield")).hasSize(1);
        assertThat(ownerRepository.findByCityIgnoreCase("SPRINGFIELD")).hasSize(1);
        
        assertThat(petRepository.findByNameContainingIgnoreCase("buddy")).hasSize(1);
        assertThat(petRepository.findByNameContainingIgnoreCase("BUDDY")).hasSize(1);
        
        assertThat(petRepository.findBySpeciesIgnoreCase("dog")).hasSize(1);
        assertThat(petRepository.findBySpeciesIgnoreCase("DOG")).hasSize(1);
        
        assertThat(petRepository.findByBreedContainingIgnoreCase("labrador")).hasSize(1);
        assertThat(petRepository.findByBreedContainingIgnoreCase("LABRADOR")).hasSize(1);
    }

    // Helper methods
    
    private Owner createOwner(String firstName, String lastName, String email) {
        return createOwner(firstName, lastName, email, null);
    }
    
    private Owner createOwner(String firstName, String lastName, String email, String city) {
        Owner owner = new Owner();
        owner.setFirstName(firstName);
        owner.setLastName(lastName);
        owner.setEmail(email);
        owner.setCity(city);
        return ownerRepository.save(owner);
    }
    
    private Pet createPet(String name, String species, Owner owner) {
        return createPet(name, species, null, owner, LocalDate.now().minusYears(3));
    }
    
    private Pet createPet(String name, String species, String breed, Owner owner) {
        return createPet(name, species, breed, owner, LocalDate.now().minusYears(3));
    }
    
    private Pet createPet(String name, String species, Owner owner, LocalDate birthDate) {
        return createPet(name, species, null, owner, birthDate);
    }
    
    private Pet createPet(String name, String species, String breed, Owner owner, LocalDate birthDate) {
        Pet pet = new Pet();
        pet.setName(name);
        pet.setSpecies(species);
        pet.setBreed(breed);
        pet.setBirthDate(birthDate);
        pet.setOwner(owner);
        return petRepository.save(pet);
    }
}