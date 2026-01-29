package com.petclinic.backend.service.impl;

import com.petclinic.backend.exception.BusinessRuleException;
import com.petclinic.backend.exception.EntityNotFoundException;
import com.petclinic.backend.exception.ValidationException;
import com.petclinic.backend.model.Specialty;
import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.model.VisitType;
import com.petclinic.backend.repository.VeterinarianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VeterinarianServiceImpl
 * Tests CRUD operations, availability tracking, and specialty-based filtering
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VeterinarianServiceImpl Tests")
class VeterinarianServiceImplTest {
    
    @Mock
    private VeterinarianRepository veterinarianRepository;
    
    @InjectMocks
    private VeterinarianServiceImpl veterinarianService;
    
    private Veterinarian testVeterinarian;
    private Veterinarian testVeterinarianWithSpecialties;
    
    @BeforeEach
    void setUp() {
        testVeterinarian = new Veterinarian();
        testVeterinarian.setId(1L);
        testVeterinarian.setFirstName("John");
        testVeterinarian.setLastName("Smith");
        testVeterinarian.setLicenseNumber("VET123456");
        testVeterinarian.setSpecialties("General Practice");
        
        testVeterinarianWithSpecialties = new Veterinarian();
        testVeterinarianWithSpecialties.setId(2L);
        testVeterinarianWithSpecialties.setFirstName("Jane");
        testVeterinarianWithSpecialties.setLastName("Doe");
        testVeterinarianWithSpecialties.setLicenseNumber("VET789012");
        Set<Specialty> specialties = new HashSet<>();
        specialties.add(Specialty.SURGERY);
        specialties.add(Specialty.EMERGENCY_MEDICINE);
        testVeterinarianWithSpecialties.setSpecialtySet(specialties);
    }
    
    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperations {
        
        @Test
        @DisplayName("Should create veterinarian successfully")
        void shouldCreateVeterinarianSuccessfully() {
            // Given
            when(veterinarianRepository.existsByLicenseNumber(testVeterinarian.getLicenseNumber()))
                .thenReturn(false);
            when(veterinarianRepository.save(testVeterinarian))
                .thenReturn(testVeterinarian);
            
            // When
            Veterinarian result = veterinarianService.create(testVeterinarian);
            
            // Then
            assertThat(result).isEqualTo(testVeterinarian);
            verify(veterinarianRepository).existsByLicenseNumber(testVeterinarian.getLicenseNumber());
            verify(veterinarianRepository).save(testVeterinarian);
        }
        
        @Test
        @DisplayName("Should throw ValidationException when creating veterinarian with duplicate license")
        void shouldThrowValidationExceptionForDuplicateLicense() {
            // Given
            when(veterinarianRepository.existsByLicenseNumber(testVeterinarian.getLicenseNumber()))
                .thenReturn(true);
            
            // When & Then
            assertThatThrownBy(() -> veterinarianService.create(testVeterinarian))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("License number 'VET123456' already exists");
            
            verify(veterinarianRepository).existsByLicenseNumber(testVeterinarian.getLicenseNumber());
            verify(veterinarianRepository, never()).save(any());
        }
        
        @Test
        @DisplayName("Should throw ValidationException when creating null veterinarian")
        void shouldThrowValidationExceptionForNullVeterinarian() {
            // When & Then
            assertThatThrownBy(() -> veterinarianService.create(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Veterinarian cannot be null");
        }
        
        @Test
        @DisplayName("Should throw ValidationException when creating veterinarian without required fields")
        void shouldThrowValidationExceptionForMissingRequiredFields() {
            // Given
            Veterinarian invalidVet = new Veterinarian();
            
            // When & Then
            assertThatThrownBy(() -> veterinarianService.create(invalidVet))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("First name is required");
        }
        
        @Test
        @DisplayName("Should update veterinarian successfully")
        void shouldUpdateVeterinarianSuccessfully() {
            // Given
            Veterinarian updatedVet = new Veterinarian();
            updatedVet.setFirstName("John Updated");
            updatedVet.setLastName("Smith Updated");
            updatedVet.setLicenseNumber("VET123456");
            
            when(veterinarianRepository.findById(1L))
                .thenReturn(Optional.of(testVeterinarian));
            when(veterinarianRepository.save(testVeterinarian))
                .thenReturn(testVeterinarian);
            
            // When
            Veterinarian result = veterinarianService.update(1L, updatedVet);
            
            // Then
            assertThat(result).isEqualTo(testVeterinarian);
            assertThat(testVeterinarian.getFirstName()).isEqualTo("John Updated");
            assertThat(testVeterinarian.getLastName()).isEqualTo("Smith Updated");
            verify(veterinarianRepository).findById(1L);
            verify(veterinarianRepository).save(testVeterinarian);
        }
        
        @Test
        @DisplayName("Should throw EntityNotFoundException when updating non-existent veterinarian")
        void shouldThrowEntityNotFoundExceptionForNonExistentVeterinarian() {
            // Given
            when(veterinarianRepository.findById(999L))
                .thenReturn(Optional.empty());
            
            // When & Then
            assertThatThrownBy(() -> veterinarianService.update(999L, testVeterinarian))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Veterinarian");
        }
        
        @Test
        @DisplayName("Should find veterinarian by ID")
        void shouldFindVeterinarianById() {
            // Given
            when(veterinarianRepository.findById(1L))
                .thenReturn(Optional.of(testVeterinarian));
            
            // When
            Optional<Veterinarian> result = veterinarianService.findById(1L);
            
            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(testVeterinarian);
            verify(veterinarianRepository).findById(1L);
        }
        
        @Test
        @DisplayName("Should return empty when veterinarian not found by ID")
        void shouldReturnEmptyWhenVeterinarianNotFoundById() {
            // Given
            when(veterinarianRepository.findById(999L))
                .thenReturn(Optional.empty());
            
            // When
            Optional<Veterinarian> result = veterinarianService.findById(999L);
            
            // Then
            assertThat(result).isEmpty();
            verify(veterinarianRepository).findById(999L);
        }
        
        @Test
        @DisplayName("Should find all veterinarians")
        void shouldFindAllVeterinarians() {
            // Given
            List<Veterinarian> veterinarians = Arrays.asList(testVeterinarian, testVeterinarianWithSpecialties);
            when(veterinarianRepository.findAll(Sort.by(Sort.Direction.ASC, "lastName", "firstName")))
                .thenReturn(veterinarians);
            
            // When
            List<Veterinarian> result = veterinarianService.findAll();
            
            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(testVeterinarian, testVeterinarianWithSpecialties);
            verify(veterinarianRepository).findAll(Sort.by(Sort.Direction.ASC, "lastName", "firstName"));
        }
        
        @Test
        @DisplayName("Should delete veterinarian successfully")
        void shouldDeleteVeterinarianSuccessfully() {
            // Given
            testVeterinarian.setVisits(new ArrayList<>()); // No visits
            when(veterinarianRepository.findById(1L))
                .thenReturn(Optional.of(testVeterinarian));
            
            // When
            veterinarianService.deleteById(1L);
            
            // Then
            verify(veterinarianRepository, times(2)).findById(1L); // Called twice: once in deleteById, once in canDeleteVeterinarian
            verify(veterinarianRepository).deleteById(1L);
        }
        
        @Test
        @DisplayName("Should throw BusinessRuleException when deleting veterinarian with visits")
        void shouldThrowBusinessRuleExceptionWhenDeletingVeterinarianWithVisits() {
            // Given
            Visit visit = new Visit();
            testVeterinarian.setVisits(Arrays.asList(visit));
            when(veterinarianRepository.findById(1L))
                .thenReturn(Optional.of(testVeterinarian));
            
            // When & Then
            assertThatThrownBy(() -> veterinarianService.deleteById(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot delete veterinarian with existing visits");
            
            verify(veterinarianRepository, times(2)).findById(1L); // Called twice: once in deleteById, once in canDeleteVeterinarian
            verify(veterinarianRepository, never()).deleteById(any());
        }
    }
    
    @Nested
    @DisplayName("Specialty-Based Operations")
    class SpecialtyBasedOperations {
        
        @Test
        @DisplayName("Should find veterinarians by specialty enum")
        void shouldFindVeterinariansBySpecialtyEnum() {
            // Given
            List<Veterinarian> surgeons = Arrays.asList(testVeterinarianWithSpecialties);
            when(veterinarianRepository.findBySpecialtyEnum(Specialty.SURGERY))
                .thenReturn(surgeons);
            
            // When
            List<Veterinarian> result = veterinarianService.findBySpecialty("Surgery");
            
            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(testVeterinarianWithSpecialties);
            verify(veterinarianRepository).findBySpecialtyEnum(Specialty.SURGERY);
        }
        
        @Test
        @DisplayName("Should fallback to string-based specialty search")
        void shouldFallbackToStringBasedSpecialtySearch() {
            // Given - "Custom Specialty" doesn't match any enum, so it should fallback to string search
            when(veterinarianRepository.findBySpecialty("Custom Specialty"))
                .thenReturn(Arrays.asList(testVeterinarian));
            
            // When
            List<Veterinarian> result = veterinarianService.findBySpecialty("Custom Specialty");
            
            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(testVeterinarian);
            verify(veterinarianRepository).findBySpecialty("Custom Specialty");
        }
        
        @Test
        @DisplayName("Should throw ValidationException for null specialty")
        void shouldThrowValidationExceptionForNullSpecialty() {
            // When & Then
            assertThatThrownBy(() -> veterinarianService.findBySpecialty(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Specialty cannot be null or empty");
        }
        
        @Test
        @DisplayName("Should find veterinarians available for visit type")
        void shouldFindVeterinariansAvailableForVisitType() {
            // Given
            LocalDateTime appointmentTime = LocalDateTime.of(2024, 1, 15, 10, 0);
            List<Veterinarian> surgeons = Arrays.asList(testVeterinarianWithSpecialties);
            
            when(veterinarianRepository.findBySpecialtyEnum(Specialty.SURGERY))
                .thenReturn(surgeons);
            when(veterinarianRepository.findById(2L))
                .thenReturn(Optional.of(testVeterinarianWithSpecialties));
            when(veterinarianRepository.isVeterinarianAvailable(eq(2L), any(), any()))
                .thenReturn(true);
            
            // When
            List<Veterinarian> result = veterinarianService.findAvailableForVisitType(VisitType.SURGERY, appointmentTime);
            
            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(testVeterinarianWithSpecialties);
        }
    }
    
    @Nested
    @DisplayName("Availability Tracking")
    class AvailabilityTracking {
        
        @Test
        @DisplayName("Should find available veterinarians at specific time")
        void shouldFindAvailableVeterinariansAtSpecificTime() {
            // Given
            LocalDateTime appointmentTime = LocalDateTime.of(2024, 1, 15, 10, 0); // Monday 10 AM
            List<Veterinarian> allVets = Arrays.asList(testVeterinarian, testVeterinarianWithSpecialties);
            
            when(veterinarianRepository.findAll(Sort.by(Sort.Direction.ASC, "lastName", "firstName")))
                .thenReturn(allVets);
            when(veterinarianRepository.findById(1L))
                .thenReturn(Optional.of(testVeterinarian));
            when(veterinarianRepository.findById(2L))
                .thenReturn(Optional.of(testVeterinarianWithSpecialties));
            when(veterinarianRepository.isVeterinarianAvailable(eq(1L), any(), any()))
                .thenReturn(true);
            when(veterinarianRepository.isVeterinarianAvailable(eq(2L), any(), any()))
                .thenReturn(false);
            
            // When
            List<Veterinarian> result = veterinarianService.findAvailable(appointmentTime);
            
            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(testVeterinarian);
        }
        
        @Test
        @DisplayName("Should check veterinarian availability correctly")
        void shouldCheckVeterinarianAvailabilityCorrectly() {
            // Given
            LocalDateTime appointmentTime = LocalDateTime.of(2024, 1, 15, 10, 0); // Monday 10 AM
            when(veterinarianRepository.findById(1L))
                .thenReturn(Optional.of(testVeterinarian));
            when(veterinarianRepository.isVeterinarianAvailable(eq(1L), any(), any()))
                .thenReturn(true);
            
            // When
            boolean result = veterinarianService.isVeterinarianAvailable(1L, appointmentTime);
            
            // Then
            assertThat(result).isTrue();
            verify(veterinarianRepository).findById(1L);
            verify(veterinarianRepository).isVeterinarianAvailable(eq(1L), any(), any());
        }
        
        @Test
        @DisplayName("Should return false for non-existent veterinarian availability")
        void shouldReturnFalseForNonExistentVeterinarianAvailability() {
            // Given
            LocalDateTime appointmentTime = LocalDateTime.of(2024, 1, 15, 10, 0);
            when(veterinarianRepository.findById(999L))
                .thenReturn(Optional.empty());
            
            // When
            boolean result = veterinarianService.isVeterinarianAvailable(999L, appointmentTime);
            
            // Then
            assertThat(result).isFalse();
            verify(veterinarianRepository).findById(999L);
        }
        
        @Test
        @DisplayName("Should return false for null parameters in availability check")
        void shouldReturnFalseForNullParametersInAvailabilityCheck() {
            // When & Then
            assertThat(veterinarianService.isVeterinarianAvailable(null, LocalDateTime.now())).isFalse();
            assertThat(veterinarianService.isVeterinarianAvailable(1L, null)).isFalse();
        }
        
        @Test
        @DisplayName("Should throw ValidationException for null dateTime in findAvailable")
        void shouldThrowValidationExceptionForNullDateTimeInFindAvailable() {
            // When & Then
            assertThatThrownBy(() -> veterinarianService.findAvailable(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("DateTime cannot be null");
        }
    }
    
    @Nested
    @DisplayName("Search and Filtering")
    class SearchAndFiltering {
        
        @Test
        @DisplayName("Should find veterinarian by license number")
        void shouldFindVeterinarianByLicenseNumber() {
            // Given
            when(veterinarianRepository.findByLicenseNumber("VET123456"))
                .thenReturn(Optional.of(testVeterinarian));
            
            // When
            Veterinarian result = veterinarianService.findByLicenseNumber("VET123456");
            
            // Then
            assertThat(result).isEqualTo(testVeterinarian);
            verify(veterinarianRepository).findByLicenseNumber("VET123456");
        }
        
        @Test
        @DisplayName("Should throw EntityNotFoundException for non-existent license number")
        void shouldThrowEntityNotFoundExceptionForNonExistentLicenseNumber() {
            // Given
            when(veterinarianRepository.findByLicenseNumber("INVALID"))
                .thenReturn(Optional.empty());
            
            // When & Then
            assertThatThrownBy(() -> veterinarianService.findByLicenseNumber("INVALID"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Veterinarian with license number: INVALID");
        }
        
        @Test
        @DisplayName("Should search veterinarians by name")
        void shouldSearchVeterinariansByName() {
            // Given
            when(veterinarianRepository.findByFullNameContaining("John"))
                .thenReturn(Arrays.asList(testVeterinarian));
            
            // When
            List<Veterinarian> result = veterinarianService.searchByName("John");
            
            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(testVeterinarian);
            verify(veterinarianRepository).findByFullNameContaining("John");
        }
        
        @Test
        @DisplayName("Should return all veterinarians for empty search term")
        void shouldReturnAllVeterinariansForEmptySearchTerm() {
            // Given
            List<Veterinarian> allVets = Arrays.asList(testVeterinarian, testVeterinarianWithSpecialties);
            when(veterinarianRepository.findAll(Sort.by(Sort.Direction.ASC, "lastName", "firstName")))
                .thenReturn(allVets);
            
            // When
            List<Veterinarian> result = veterinarianService.searchByName("");
            
            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(testVeterinarian, testVeterinarianWithSpecialties);
        }
        
        @Test
        @DisplayName("Should find most active veterinarians")
        void shouldFindMostActiveVeterinarians() {
            // Given
            List<Veterinarian> activeVets = Arrays.asList(testVeterinarian);
            when(veterinarianRepository.findVeterinariansByVisitCount(PageRequest.of(0, 5)))
                .thenReturn(activeVets);
            
            // When
            List<Veterinarian> result = veterinarianService.findMostActiveVeterinarians(5);
            
            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(testVeterinarian);
            verify(veterinarianRepository).findVeterinariansByVisitCount(PageRequest.of(0, 5));
        }
        
        @Test
        @DisplayName("Should throw ValidationException for invalid limit in findMostActiveVeterinarians")
        void shouldThrowValidationExceptionForInvalidLimit() {
            // When & Then
            assertThatThrownBy(() -> veterinarianService.findMostActiveVeterinarians(0))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Limit must be greater than 0");
        }
    }
    
    @Nested
    @DisplayName("Business Logic")
    class BusinessLogic {
        
        @Test
        @DisplayName("Should check if veterinarian can be deleted")
        void shouldCheckIfVeterinarianCanBeDeleted() {
            // Given
            testVeterinarian.setVisits(new ArrayList<>()); // No visits
            when(veterinarianRepository.findById(1L))
                .thenReturn(Optional.of(testVeterinarian));
            
            // When
            boolean result = veterinarianService.canDeleteVeterinarian(1L);
            
            // Then
            assertThat(result).isTrue();
            verify(veterinarianRepository).findById(1L);
        }
        
        @Test
        @DisplayName("Should return false when veterinarian has visits")
        void shouldReturnFalseWhenVeterinarianHasVisits() {
            // Given
            Visit visit = new Visit();
            testVeterinarian.setVisits(Arrays.asList(visit));
            when(veterinarianRepository.findById(1L))
                .thenReturn(Optional.of(testVeterinarian));
            
            // When
            boolean result = veterinarianService.canDeleteVeterinarian(1L);
            
            // Then
            assertThat(result).isFalse();
            verify(veterinarianRepository).findById(1L);
        }
        
        @Test
        @DisplayName("Should return false for null ID in canDeleteVeterinarian")
        void shouldReturnFalseForNullIdInCanDeleteVeterinarian() {
            // When
            boolean result = veterinarianService.canDeleteVeterinarian(null);
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("Should get veterinarian workload statistics")
        void shouldGetVeterinarianWorkloadStatistics() {
            // Given
            Visit completedVisit = new Visit();
            completedVisit.setDiagnosis("Test diagnosis");
            completedVisit.setTreatment("Test treatment");
            completedVisit.setVisitDate(LocalDateTime.now().minusDays(1));
            
            Visit upcomingVisit = new Visit();
            upcomingVisit.setVisitDate(LocalDateTime.now().plusDays(1));
            
            testVeterinarian.setVisits(Arrays.asList(completedVisit, upcomingVisit));
            when(veterinarianRepository.findById(1L))
                .thenReturn(Optional.of(testVeterinarian));
            
            // When
            Map<String, Object> result = veterinarianService.getVeterinarianWorkload(1L);
            
            // Then
            assertThat(result).containsEntry("veterinarianId", 1L);
            assertThat(result).containsEntry("fullName", "Dr. John Smith");
            assertThat(result).containsEntry("totalVisits", 2);
            assertThat(result).containsEntry("upcomingVisits", 1L);
            assertThat(result).containsEntry("completedVisits", 1L);
        }
        
        @Test
        @DisplayName("Should find veterinarians with light workload")
        void shouldFindVeterinariansWithLightWorkload() {
            // Given
            List<Veterinarian> lightWorkloadVets = Arrays.asList(testVeterinarian);
            when(veterinarianRepository.findAvailableVeterinarians(10))
                .thenReturn(lightWorkloadVets);
            
            // When
            List<Veterinarian> result = veterinarianService.findVeterinariansWithLightWorkload(10);
            
            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(testVeterinarian);
            verify(veterinarianRepository).findAvailableVeterinarians(10);
        }
        
        @Test
        @DisplayName("Should get specialty distribution")
        void shouldGetSpecialtyDistribution() {
            // Given
            List<Object[]> specialtyData = Arrays.asList(
                new Object[]{Specialty.SURGERY, 3L},
                new Object[]{Specialty.GENERAL_PRACTICE, 5L}
            );
            when(veterinarianRepository.countVeterinariansBySpecialtyEnum())
                .thenReturn(specialtyData);
            
            // When
            Map<String, Long> result = veterinarianService.getSpecialtyDistribution();
            
            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsEntry("Surgery", 3L);
            assertThat(result).containsEntry("General Practice", 5L);
        }
    }
    
    @Nested
    @DisplayName("Emergency and Surgical Operations")
    class EmergencyAndSurgicalOperations {
        
        @Test
        @DisplayName("Should find emergency veterinarians")
        void shouldFindEmergencyVeterinarians() {
            // Given
            LocalDateTime emergencyTime = LocalDateTime.of(2024, 1, 15, 22, 0); // 10 PM
            List<Veterinarian> emergencyVets = Arrays.asList(testVeterinarianWithSpecialties);
            
            when(veterinarianRepository.findEmergencyVeterinarians(anyList()))
                .thenReturn(emergencyVets);
            when(veterinarianRepository.findById(2L))
                .thenReturn(Optional.of(testVeterinarianWithSpecialties));
            when(veterinarianRepository.isVeterinarianAvailable(eq(2L), any(), any()))
                .thenReturn(true);
            
            // When
            List<Veterinarian> result = veterinarianService.findEmergencyVeterinarians(emergencyTime);
            
            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(testVeterinarianWithSpecialties);
        }
        
        @Test
        @DisplayName("Should find surgical veterinarians")
        void shouldFindSurgicalVeterinarians() {
            // Given
            LocalDateTime surgeryTime = LocalDateTime.of(2024, 1, 15, 14, 0); // 2 PM
            List<Veterinarian> surgicalVets = Arrays.asList(testVeterinarianWithSpecialties);
            
            when(veterinarianRepository.findSurgicalVeterinarians(anyList()))
                .thenReturn(surgicalVets);
            when(veterinarianRepository.findById(2L))
                .thenReturn(Optional.of(testVeterinarianWithSpecialties));
            when(veterinarianRepository.isVeterinarianAvailable(eq(2L), any(), any()))
                .thenReturn(true);
            
            // When
            List<Veterinarian> result = veterinarianService.findSurgicalVeterinarians(surgeryTime);
            
            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(testVeterinarianWithSpecialties);
        }
    }
}