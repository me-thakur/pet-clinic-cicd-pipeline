package com.petclinic.backend.service.impl;

import com.petclinic.backend.exception.BusinessRuleException;
import com.petclinic.backend.exception.EntityNotFoundException;
import com.petclinic.backend.exception.ValidationException;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.repository.OwnerRepository;
import com.petclinic.backend.repository.PetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PetServiceImpl
 * Tests CRUD operations, search functionality, and business logic validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PetService Implementation Tests")
class PetServiceImplTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private com.petclinic.backend.service.AuditService auditService;

    @InjectMocks
    private PetServiceImpl petService;

    private Owner testOwner;
    private Pet testPet;
    private Pet testPetWithVisits;

    @BeforeEach
    void setUp() {
        // Create test owner
        testOwner = new Owner();
        testOwner.setId(1L);
        testOwner.setFirstName("John");
        testOwner.setLastName("Doe");
        testOwner.setEmail("john.doe@example.com");

        // Create test pet
        testPet = new Pet();
        testPet.setId(1L);
        testPet.setName("Buddy");
        testPet.setSpecies("Dog");
        testPet.setBreed("Golden Retriever");
        testPet.setBirthDate(LocalDate.now().minusYears(3));
        testPet.setOwner(testOwner);

        // Create test pet with visits
        testPetWithVisits = new Pet();
        testPetWithVisits.setId(2L);
        testPetWithVisits.setName("Whiskers");
        testPetWithVisits.setSpecies("Cat");
        testPetWithVisits.setBreed("Persian");
        testPetWithVisits.setBirthDate(LocalDate.now().minusYears(5));
        testPetWithVisits.setOwner(testOwner);
        
        Visit visit = new Visit();
        visit.setId(1L);
        visit.setVisitDate(LocalDateTime.now().minusDays(30));
        visit.setNotes("Routine checkup");
        visit.setPet(testPetWithVisits);
        testPetWithVisits.getVisits().add(visit);
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperations {

        @Test
        @DisplayName("Should create pet successfully with valid data")
        void shouldCreatePetSuccessfully() {
            // Given
            when(ownerRepository.existsById(1L)).thenReturn(true);
            when(petRepository.existsByNameIgnoreCaseAndOwnerId("Buddy", 1L)).thenReturn(false);
            when(petRepository.save(any(Pet.class))).thenReturn(testPet);

            // When
            Pet result = petService.create(testPet);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Buddy");
            assertThat(result.getSpecies()).isEqualTo("Dog");
            verify(petRepository).save(testPet);
        }

        @Test
        @DisplayName("Should throw ValidationException when creating pet with null data")
        void shouldThrowValidationExceptionForNullPet() {
            // When & Then
            assertThatThrownBy(() -> petService.create(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Pet cannot be null");
        }

        @Test
        @DisplayName("Should throw ValidationException when creating pet without name")
        void shouldThrowValidationExceptionForMissingName() {
            // Given
            testPet.setName(null);

            // When & Then
            assertThatThrownBy(() -> petService.create(testPet))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Pet name is required");
        }

        @Test
        @DisplayName("Should throw ValidationException when creating pet without species")
        void shouldThrowValidationExceptionForMissingSpecies() {
            // Given
            testPet.setSpecies(null);

            // When & Then
            assertThatThrownBy(() -> petService.create(testPet))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Pet species is required");
        }

        @Test
        @DisplayName("Should throw ValidationException when creating pet without owner")
        void shouldThrowValidationExceptionForMissingOwner() {
            // Given
            testPet.setOwner(null);

            // When & Then
            assertThatThrownBy(() -> petService.create(testPet))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Pet must have a valid owner");
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when owner does not exist")
        void shouldThrowEntityNotFoundExceptionForNonExistentOwner() {
            // Given
            when(ownerRepository.existsById(1L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> petService.create(testPet))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Owner with ID 1 not found");
        }

        @Test
        @DisplayName("Should throw ValidationException for duplicate pet name")
        void shouldThrowValidationExceptionForDuplicateName() {
            // Given
            when(ownerRepository.existsById(1L)).thenReturn(true);
            when(petRepository.existsByNameIgnoreCaseAndOwnerId("Buddy", 1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> petService.create(testPet))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Pet name 'Buddy' already exists for this owner");
        }

        @Test
        @DisplayName("Should throw ValidationException for future birth date")
        void shouldThrowValidationExceptionForFutureBirthDate() {
            // Given
            testPet.setBirthDate(LocalDate.now().plusDays(1));
            when(ownerRepository.existsById(1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> petService.create(testPet))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Birth date cannot be in the future");
        }

        @Test
        @DisplayName("Should update pet successfully")
        void shouldUpdatePetSuccessfully() {
            // Given
            Pet updatedPet = new Pet();
            updatedPet.setName("Buddy Updated");
            updatedPet.setSpecies("Dog");
            updatedPet.setBreed("Labrador");
            updatedPet.setOwner(testOwner);

            when(petRepository.findById(1L)).thenReturn(Optional.of(testPet));
            when(ownerRepository.existsById(1L)).thenReturn(true);
            when(petRepository.existsByNameIgnoreCaseAndOwnerId("Buddy Updated", 1L)).thenReturn(false);
            when(petRepository.save(any(Pet.class))).thenReturn(testPet);

            // When
            Pet result = petService.update(1L, updatedPet);

            // Then
            assertThat(result).isNotNull();
            verify(petRepository).save(testPet);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when updating non-existent pet")
        void shouldThrowEntityNotFoundExceptionForUpdateNonExistentPet() {
            // Given
            when(petRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> petService.update(1L, testPet))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Pet with ID 1 not found");
        }

        @Test
        @DisplayName("Should find pet by ID successfully")
        void shouldFindPetByIdSuccessfully() {
            // Given
            when(petRepository.findById(1L)).thenReturn(Optional.of(testPet));

            // When
            Optional<Pet> result = petService.findById(1L);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Buddy");
        }

        @Test
        @DisplayName("Should return empty when pet not found by ID")
        void shouldReturnEmptyWhenPetNotFoundById() {
            // Given
            when(petRepository.findById(1L)).thenReturn(Optional.empty());

            // When
            Optional<Pet> result = petService.findById(1L);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should find all pets successfully")
        void shouldFindAllPetsSuccessfully() {
            // Given
            List<Pet> pets = Arrays.asList(testPet, testPetWithVisits);
            when(petRepository.findAll(any(Sort.class))).thenReturn(pets);

            // When
            List<Pet> result = petService.findAll();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).contains(testPet, testPetWithVisits);
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should delete pet successfully when no visits exist")
        void shouldDeletePetSuccessfully() {
            // Given
            when(petRepository.findById(1L)).thenReturn(Optional.of(testPet));

            // When
            petService.deleteById(1L);

            // Then
            verify(petRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when deleting pet with visits")
        void shouldThrowBusinessRuleExceptionForPetWithVisits() {
            // Given
            when(petRepository.findById(2L)).thenReturn(Optional.of(testPetWithVisits));

            // When & Then
            assertThatThrownBy(() -> petService.deleteById(2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot delete pet with existing visits");
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when deleting non-existent pet")
        void shouldThrowEntityNotFoundExceptionForDeleteNonExistentPet() {
            // Given
            when(petRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> petService.deleteById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Pet with ID 1 not found");
        }

        @Test
        @DisplayName("Should return true when pet can be deleted")
        void shouldReturnTrueWhenPetCanBeDeleted() {
            // Given
            when(petRepository.findById(1L)).thenReturn(Optional.of(testPet));

            // When
            boolean result = petService.canDeletePet(1L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when pet cannot be deleted due to visits")
        void shouldReturnFalseWhenPetCannotBeDeleted() {
            // Given
            when(petRepository.findById(2L)).thenReturn(Optional.of(testPetWithVisits));

            // When
            boolean result = petService.canDeletePet(2L);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when pet does not exist")
        void shouldReturnFalseWhenPetDoesNotExist() {
            // Given
            when(petRepository.findById(1L)).thenReturn(Optional.empty());

            // When
            boolean result = petService.canDeletePet(1L);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Search Operations")
    class SearchOperations {

        @Test
        @DisplayName("Should find pets by owner successfully")
        void shouldFindPetsByOwnerSuccessfully() {
            // Given
            List<Pet> pets = Arrays.asList(testPet, testPetWithVisits);
            when(ownerRepository.existsById(1L)).thenReturn(true);
            when(petRepository.findByOwnerId(1L)).thenReturn(pets);

            // When
            List<Pet> result = petService.findByOwner(1L);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).contains(testPet, testPetWithVisits);
        }

        @Test
        @DisplayName("Should throw ValidationException when owner ID is null")
        void shouldThrowValidationExceptionForNullOwnerId() {
            // When & Then
            assertThatThrownBy(() -> petService.findByOwner(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Owner ID cannot be null");
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when owner does not exist")
        void shouldThrowEntityNotFoundExceptionForNonExistentOwnerInSearch() {
            // Given
            when(ownerRepository.existsById(1L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> petService.findByOwner(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Owner with ID 1 not found");
        }

        @Test
        @DisplayName("Should search pets by term successfully")
        void shouldSearchPetsByTermSuccessfully() {
            // Given
            String searchTerm = "Buddy";
            Page<Pet> page = new PageImpl<>(Arrays.asList(testPet));
            when(petRepository.searchPets(eq(searchTerm), isNull(), eq(searchTerm), isNull(), any(Pageable.class)))
                .thenReturn(page);
            when(petRepository.findByOwnerNameContaining(searchTerm)).thenReturn(Collections.emptyList());

            // When
            List<Pet> result = petService.searchPets(searchTerm);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).contains(testPet);
        }

        @Test
        @DisplayName("Should return all pets when search term is empty")
        void shouldReturnAllPetsWhenSearchTermIsEmpty() {
            // Given
            List<Pet> allPets = Arrays.asList(testPet, testPetWithVisits);
            when(petRepository.findAll(any(Sort.class))).thenReturn(allPets);

            // When
            List<Pet> result = petService.searchPets("");

            // Then
            assertThat(result).hasSize(2);
            verify(petRepository).findAll(any(Sort.class));
        }

        @Test
        @DisplayName("Should find pets by species successfully")
        void shouldFindPetsBySpeciesSuccessfully() {
            // Given
            when(petRepository.findBySpeciesIgnoreCase("Dog")).thenReturn(Arrays.asList(testPet));

            // When
            List<Pet> result = petService.findBySpecies("Dog");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSpecies()).isEqualTo("Dog");
        }

        @Test
        @DisplayName("Should throw ValidationException for empty species")
        void shouldThrowValidationExceptionForEmptySpecies() {
            // When & Then
            assertThatThrownBy(() -> petService.findBySpecies(""))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Species cannot be null or empty");
        }

        @Test
        @DisplayName("Should find pets by breed successfully")
        void shouldFindPetsByBreedSuccessfully() {
            // Given
            when(petRepository.findByBreedContainingIgnoreCase("Golden")).thenReturn(Arrays.asList(testPet));

            // When
            List<Pet> result = petService.findByBreed("Golden");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBreed()).contains("Golden");
        }

        @Test
        @DisplayName("Should find pets by age range successfully")
        void shouldFindPetsByAgeRangeSuccessfully() {
            // Given
            when(petRepository.findByBirthDateBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(testPet));

            // When
            List<Pet> result = petService.findByAgeRange(2, 5);

            // Then
            assertThat(result).hasSize(1);
            verify(petRepository).findByBirthDateBetween(any(LocalDate.class), any(LocalDate.class));
        }

        @Test
        @DisplayName("Should throw ValidationException for invalid age range")
        void shouldThrowValidationExceptionForInvalidAgeRange() {
            // When & Then
            assertThatThrownBy(() -> petService.findByAgeRange(5, 2))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid age range");
        }
    }

    @Nested
    @DisplayName("Business Logic Operations")
    class BusinessLogicOperations {

        @Test
        @DisplayName("Should find pets with upcoming visits")
        void shouldFindPetsWithUpcomingVisits() {
            // Given
            when(petRepository.findPetsWithVisitsBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(testPetWithVisits));

            // When
            List<Pet> result = petService.findPetsWithUpcomingVisits();

            // Then
            assertThat(result).hasSize(1);
            verify(petRepository).findPetsWithVisitsBetween(any(LocalDate.class), any(LocalDate.class));
        }

        @Test
        @DisplayName("Should check if pet exists by ID")
        void shouldCheckIfPetExistsById() {
            // Given
            when(petRepository.existsById(1L)).thenReturn(true);

            // When
            boolean result = petService.existsById(1L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should count total pets")
        void shouldCountTotalPets() {
            // Given
            when(petRepository.count()).thenReturn(5L);

            // When
            long result = petService.count();

            // Then
            assertThat(result).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("Validation Edge Cases")
    class ValidationEdgeCases {

        @Test
        @DisplayName("Should throw ValidationException for pet name too long")
        void shouldThrowValidationExceptionForLongName() {
            // Given
            testPet.setName("A".repeat(51)); // 51 characters
            when(ownerRepository.existsById(1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> petService.create(testPet))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Pet name cannot exceed 50 characters");
        }

        @Test
        @DisplayName("Should throw ValidationException for species too long")
        void shouldThrowValidationExceptionForLongSpecies() {
            // Given
            testPet.setSpecies("A".repeat(31)); // 31 characters
            when(ownerRepository.existsById(1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> petService.create(testPet))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Species cannot exceed 30 characters");
        }

        @Test
        @DisplayName("Should throw ValidationException for breed too long")
        void shouldThrowValidationExceptionForLongBreed() {
            // Given
            testPet.setBreed("A".repeat(51)); // 51 characters
            when(ownerRepository.existsById(1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> petService.create(testPet))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Breed cannot exceed 50 characters");
        }

        @Test
        @DisplayName("Should throw ValidationException for medical history too long")
        void shouldThrowValidationExceptionForLongMedicalHistory() {
            // Given
            testPet.setMedicalHistory("A".repeat(1001)); // 1001 characters
            when(ownerRepository.existsById(1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> petService.create(testPet))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Medical history cannot exceed 1000 characters");
        }

        @Test
        @DisplayName("Should throw ValidationException for birth date too old")
        void shouldThrowValidationExceptionForTooOldBirthDate() {
            // Given
            testPet.setBirthDate(LocalDate.now().minusYears(31)); // 31 years ago
            when(ownerRepository.existsById(1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> petService.create(testPet))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Birth date cannot be more than 30 years ago");
        }
    }
}