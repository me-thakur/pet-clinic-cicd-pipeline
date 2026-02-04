package com.petclinic.backend.service;

import com.petclinic.backend.exception.EntityNotFoundException;
import com.petclinic.backend.exception.ValidationException;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.repository.VisitRepository;
import com.petclinic.backend.service.VisitStatusValidationService.ValidationResult;
import com.petclinic.backend.service.VisitStatusValidationService.InconsistencyReport;
import com.petclinic.backend.service.VisitStatusValidationService.RecalculationResult;
import com.petclinic.backend.service.impl.VisitStatusValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VisitStatusValidationService
 * Tests validation logic for visit completion status consistency
 */
@ExtendWith(MockitoExtension.class)
class VisitStatusValidationServiceTest {
    
    @Mock
    private VisitRepository visitRepository;
    
    @InjectMocks
    private VisitStatusValidationServiceImpl validationService;
    
    private Visit completedVisit;
    private Visit incompleteVisit;
    private Pet testPet;
    private Owner testOwner;
    
    @BeforeEach
    void setUp() {
        // Create test owner
        testOwner = new Owner();
        testOwner.setId(1L);
        testOwner.setFirstName("John");
        testOwner.setLastName("Doe");
        
        // Create test pet
        testPet = new Pet();
        testPet.setId(1L);
        testPet.setName("Buddy");
        testPet.setSpecies("Dog");
        testPet.setOwner(testOwner);
        
        // Create completed visit (has both diagnosis and treatment)
        completedVisit = new Visit();
        completedVisit.setId(1L);
        completedVisit.setVisitDate(LocalDateTime.now().minusDays(1));
        completedVisit.setDiagnosis("Routine checkup completed");
        completedVisit.setTreatment("Vaccinations administered");
        completedVisit.setPet(testPet);
        
        // Create incomplete visit (missing treatment)
        incompleteVisit = new Visit();
        incompleteVisit.setId(2L);
        incompleteVisit.setVisitDate(LocalDateTime.now().minusDays(2));
        incompleteVisit.setDiagnosis("Examination in progress");
        incompleteVisit.setTreatment(null); // Missing treatment
        incompleteVisit.setPet(testPet);
    }
    
    @Test
    void testValidateAllVisitStatuses_NoInconsistencies() {
        // Given
        List<Visit> visits = Arrays.asList(completedVisit);
        when(visitRepository.findAll()).thenReturn(visits);
        
        // When
        ValidationResult result = validationService.validateAllVisitStatuses();
        
        // Then
        assertNotNull(result);
        assertFalse(result.hasInconsistencies());
        assertEquals(1, result.getTotalVisitsChecked());
        assertEquals(0, result.getInconsistentVisitsCount());
        assertTrue(result.getInconsistencies().isEmpty());
        
        verify(visitRepository).findAll();
    }
    
    @Test
    void testValidateAllVisitStatuses_WithInconsistencies() {
        // Given
        List<Visit> visits = Arrays.asList(completedVisit, incompleteVisit);
        when(visitRepository.findAll()).thenReturn(visits);
        
        // When
        ValidationResult result = validationService.validateAllVisitStatuses();
        
        // Then
        assertNotNull(result);
        assertFalse(result.hasInconsistencies()); // incompleteVisit is actually consistent (incomplete as expected)
        assertEquals(2, result.getTotalVisitsChecked());
        assertEquals(0, result.getInconsistentVisitsCount());
        
        verify(visitRepository).findAll();
    }
    
    @Test
    void testValidateAllVisitStatuses_WithActualInconsistency() {
        // Given - create a visit that has diagnosis and treatment but somehow shows as incomplete
        Visit inconsistentVisit = new Visit();
        inconsistentVisit.setId(3L);
        inconsistentVisit.setVisitDate(LocalDateTime.now().minusDays(1));
        inconsistentVisit.setDiagnosis("Complete diagnosis");
        inconsistentVisit.setTreatment("Complete treatment");
        inconsistentVisit.setPet(testPet);
        
        // Mock the isCompleted method to return false even though it has diagnosis and treatment
        Visit spyVisit = spy(inconsistentVisit);
        when(spyVisit.isCompleted()).thenReturn(false); // Force inconsistency
        
        List<Visit> visits = Arrays.asList(spyVisit);
        when(visitRepository.findAll()).thenReturn(visits);
        
        // When
        ValidationResult result = validationService.validateAllVisitStatuses();
        
        // Then
        assertNotNull(result);
        assertTrue(result.hasInconsistencies());
        assertEquals(1, result.getTotalVisitsChecked());
        assertEquals(1, result.getInconsistentVisitsCount());
        assertEquals(1, result.getInconsistencies().size());
        
        InconsistencyReport report = result.getInconsistencies().get(0);
        assertEquals(3L, report.getVisitId());
        assertEquals("Buddy", report.getPetName());
        assertEquals("Doe", report.getOwnerName());
        assertTrue(report.getExpectedCompletionStatus());
        
        verify(visitRepository).findAll();
    }
    
    @Test
    void testValidateVisitStatusesByDateRange_ValidRange() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = Arrays.asList(completedVisit);
        when(visitRepository.findByVisitDateBetween(startDateTime, endDateTime)).thenReturn(visits);
        
        // When
        ValidationResult result = validationService.validateVisitStatusesByDateRange(startDate, endDate);
        
        // Then
        assertNotNull(result);
        assertFalse(result.hasInconsistencies());
        assertEquals(1, result.getTotalVisitsChecked());
        
        verify(visitRepository).findByVisitDateBetween(startDateTime, endDateTime);
    }
    
    @Test
    void testValidateVisitStatusesByDateRange_InvalidRange() {
        // Given
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().minusDays(1); // End before start
        
        // When & Then
        assertThrows(ValidationException.class, () -> {
            validationService.validateVisitStatusesByDateRange(startDate, endDate);
        });
        
        verifyNoInteractions(visitRepository);
    }
    
    @Test
    void testValidateVisitStatusesByDateRange_NullDates() {
        // When & Then
        assertThrows(ValidationException.class, () -> {
            validationService.validateVisitStatusesByDateRange(null, LocalDate.now());
        });
        
        assertThrows(ValidationException.class, () -> {
            validationService.validateVisitStatusesByDateRange(LocalDate.now(), null);
        });
        
        verifyNoInteractions(visitRepository);
    }
    
    @Test
    void testValidateVisitStatus_ExistingVisit() {
        // Given
        Long visitId = 1L;
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(completedVisit));
        
        // When
        ValidationResult result = validationService.validateVisitStatus(visitId);
        
        // Then
        assertNotNull(result);
        assertFalse(result.hasInconsistencies());
        assertEquals(1, result.getTotalVisitsChecked());
        
        verify(visitRepository).findById(visitId);
    }
    
    @Test
    void testValidateVisitStatus_NonExistentVisit() {
        // Given
        Long visitId = 999L;
        when(visitRepository.findById(visitId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(EntityNotFoundException.class, () -> {
            validationService.validateVisitStatus(visitId);
        });
        
        verify(visitRepository).findById(visitId);
    }
    
    @Test
    void testValidateVisitStatus_NullVisitId() {
        // When & Then
        assertThrows(ValidationException.class, () -> {
            validationService.validateVisitStatus(null);
        });
        
        verifyNoInteractions(visitRepository);
    }
    
    @Test
    void testValidateVisitStatusesByPet() {
        // Given
        Long petId = 1L;
        List<Visit> visits = Arrays.asList(completedVisit, incompleteVisit);
        when(visitRepository.findByPetId(petId)).thenReturn(visits);
        
        // When
        ValidationResult result = validationService.validateVisitStatusesByPet(petId);
        
        // Then
        assertNotNull(result);
        assertFalse(result.hasInconsistencies());
        assertEquals(2, result.getTotalVisitsChecked());
        
        verify(visitRepository).findByPetId(petId);
    }
    
    @Test
    void testValidateVisitStatusesByPet_NullPetId() {
        // When & Then
        assertThrows(ValidationException.class, () -> {
            validationService.validateVisitStatusesByPet(null);
        });
        
        verifyNoInteractions(visitRepository);
    }
    
    @Test
    void testIsVisitStatusConsistent_CompletedVisit() {
        // When
        boolean isConsistent = validationService.isVisitStatusConsistent(completedVisit);
        
        // Then
        assertTrue(isConsistent);
    }
    
    @Test
    void testIsVisitStatusConsistent_IncompleteVisit() {
        // When
        boolean isConsistent = validationService.isVisitStatusConsistent(incompleteVisit);
        
        // Then
        assertTrue(isConsistent); // Incomplete visit is consistent with its data
    }
    
    @Test
    void testIsVisitStatusConsistent_NullVisit() {
        // When
        boolean isConsistent = validationService.isVisitStatusConsistent(null);
        
        // Then
        assertFalse(isConsistent);
    }
    
    @Test
    void testGetExpectedCompletionStatus_CompletedVisit() {
        // When
        boolean expectedStatus = validationService.getExpectedCompletionStatus(completedVisit);
        
        // Then
        assertTrue(expectedStatus);
    }
    
    @Test
    void testGetExpectedCompletionStatus_IncompleteVisit() {
        // When
        boolean expectedStatus = validationService.getExpectedCompletionStatus(incompleteVisit);
        
        // Then
        assertFalse(expectedStatus);
    }
    
    @Test
    void testGetExpectedCompletionStatus_EmptyFields() {
        // Given
        Visit visitWithEmptyFields = new Visit();
        visitWithEmptyFields.setDiagnosis("");
        visitWithEmptyFields.setTreatment("   "); // Whitespace only
        
        // When
        boolean expectedStatus = validationService.getExpectedCompletionStatus(visitWithEmptyFields);
        
        // Then
        assertFalse(expectedStatus);
    }
    
    @Test
    void testGetExpectedCompletionStatus_NullVisit() {
        // When
        boolean expectedStatus = validationService.getExpectedCompletionStatus(null);
        
        // Then
        assertFalse(expectedStatus);
    }
    
    @Test
    void testValidateAllVisitStatuses_EmptyList() {
        // Given
        when(visitRepository.findAll()).thenReturn(Collections.emptyList());
        
        // When
        ValidationResult result = validationService.validateAllVisitStatuses();
        
        // Then
        assertNotNull(result);
        assertFalse(result.hasInconsistencies());
        assertEquals(0, result.getTotalVisitsChecked());
        assertEquals(0, result.getInconsistentVisitsCount());
        assertTrue(result.getInconsistencies().isEmpty());
        
        verify(visitRepository).findAll();
    }
    
    @Test
    void testValidationWithWhitespaceOnlyFields() {
        // Given - visit with whitespace-only diagnosis and treatment
        Visit whitespaceVisit = new Visit();
        whitespaceVisit.setId(4L);
        whitespaceVisit.setVisitDate(LocalDateTime.now().minusDays(1));
        whitespaceVisit.setDiagnosis("   "); // Whitespace only
        whitespaceVisit.setTreatment("\t\n"); // Whitespace only
        whitespaceVisit.setPet(testPet);
        
        List<Visit> visits = Arrays.asList(whitespaceVisit);
        when(visitRepository.findAll()).thenReturn(visits);
        
        // When
        ValidationResult result = validationService.validateAllVisitStatuses();
        
        // Then
        assertNotNull(result);
        assertFalse(result.hasInconsistencies()); // Should be consistent (incomplete as expected)
        assertEquals(1, result.getTotalVisitsChecked());
        assertEquals(0, result.getInconsistentVisitsCount());
        
        // Verify the expected status is false for whitespace-only fields
        assertFalse(validationService.getExpectedCompletionStatus(whitespaceVisit));
        
        verify(visitRepository).findAll();
    }
    
    // Batch Recalculation Tests
    
    @Test
    void testRecalculateAllVisitStatuses_Success() {
        // Given
        List<Visit> visits = Arrays.asList(completedVisit, incompleteVisit);
        when(visitRepository.findAll()).thenReturn(visits);
        when(visitRepository.save(any(Visit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        RecalculationResult result = validationService.recalculateAllVisitStatuses();
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalVisitsProcessed());
        assertEquals(2, result.getVisitsUpdated());
        assertFalse(result.hasErrors());
        assertTrue(result.getErrors().isEmpty());
        assertEquals(2, result.getUpdatedVisitIds().size());
        assertTrue(result.getUpdatedVisitIds().contains(1L));
        assertTrue(result.getUpdatedVisitIds().contains(2L));
        assertTrue(result.getProcessingTimeMs() >= 0);
        
        verify(visitRepository).findAll();
        verify(visitRepository, times(2)).save(any(Visit.class));
    }
    
    @Test
    void testRecalculateAllVisitStatuses_EmptyList() {
        // Given
        when(visitRepository.findAll()).thenReturn(Collections.emptyList());
        
        // When
        RecalculationResult result = validationService.recalculateAllVisitStatuses();
        
        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalVisitsProcessed());
        assertEquals(0, result.getVisitsUpdated());
        assertFalse(result.hasErrors());
        assertTrue(result.getErrors().isEmpty());
        assertTrue(result.getUpdatedVisitIds().isEmpty());
        
        verify(visitRepository).findAll();
        verify(visitRepository, never()).save(any(Visit.class));
    }
    
    @Test
    void testRecalculateAllVisitStatuses_WithErrors() {
        // Given
        List<Visit> visits = Arrays.asList(completedVisit, incompleteVisit);
        when(visitRepository.findAll()).thenReturn(visits);
        when(visitRepository.save(completedVisit)).thenReturn(completedVisit);
        when(visitRepository.save(incompleteVisit)).thenThrow(new RuntimeException("Database error"));
        
        // When
        RecalculationResult result = validationService.recalculateAllVisitStatuses();
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalVisitsProcessed());
        assertEquals(1, result.getVisitsUpdated()); // Only one succeeded
        assertTrue(result.hasErrors());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Error recalculating visit ID 2"));
        assertEquals(1, result.getUpdatedVisitIds().size());
        assertTrue(result.getUpdatedVisitIds().contains(1L));
        
        verify(visitRepository).findAll();
        verify(visitRepository, times(2)).save(any(Visit.class));
    }
    
    @Test
    void testRecalculateVisitStatusesByDateRange_Success() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = Arrays.asList(completedVisit);
        when(visitRepository.findByVisitDateBetween(startDateTime, endDateTime)).thenReturn(visits);
        when(visitRepository.save(any(Visit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        RecalculationResult result = validationService.recalculateVisitStatusesByDateRange(startDate, endDate);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalVisitsProcessed());
        assertEquals(1, result.getVisitsUpdated());
        assertFalse(result.hasErrors());
        
        verify(visitRepository).findByVisitDateBetween(startDateTime, endDateTime);
        verify(visitRepository).save(completedVisit);
    }
    
    @Test
    void testRecalculateVisitStatusesByDateRange_InvalidRange() {
        // Given
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().minusDays(1); // End before start
        
        // When & Then
        assertThrows(ValidationException.class, () -> {
            validationService.recalculateVisitStatusesByDateRange(startDate, endDate);
        });
        
        verifyNoInteractions(visitRepository);
    }
    
    @Test
    void testRecalculateVisitStatusesByDateRange_NullDates() {
        // When & Then
        assertThrows(ValidationException.class, () -> {
            validationService.recalculateVisitStatusesByDateRange(null, LocalDate.now());
        });
        
        assertThrows(ValidationException.class, () -> {
            validationService.recalculateVisitStatusesByDateRange(LocalDate.now(), null);
        });
        
        verifyNoInteractions(visitRepository);
    }
    
    @Test
    void testRecalculateVisitStatusesByPet_Success() {
        // Given
        Long petId = 1L;
        List<Visit> visits = Arrays.asList(completedVisit, incompleteVisit);
        when(visitRepository.findByPetId(petId)).thenReturn(visits);
        when(visitRepository.save(any(Visit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        RecalculationResult result = validationService.recalculateVisitStatusesByPet(petId);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalVisitsProcessed());
        assertEquals(2, result.getVisitsUpdated());
        assertFalse(result.hasErrors());
        
        verify(visitRepository).findByPetId(petId);
        verify(visitRepository, times(2)).save(any(Visit.class));
    }
    
    @Test
    void testRecalculateVisitStatusesByPet_NullPetId() {
        // When & Then
        assertThrows(ValidationException.class, () -> {
            validationService.recalculateVisitStatusesByPet(null);
        });
        
        verifyNoInteractions(visitRepository);
    }
    
    @Test
    void testRecalculateVisitStatusesByPet_NoVisits() {
        // Given
        Long petId = 1L;
        when(visitRepository.findByPetId(petId)).thenReturn(Collections.emptyList());
        
        // When
        RecalculationResult result = validationService.recalculateVisitStatusesByPet(petId);
        
        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalVisitsProcessed());
        assertEquals(0, result.getVisitsUpdated());
        assertFalse(result.hasErrors());
        assertTrue(result.getUpdatedVisitIds().isEmpty());
        
        verify(visitRepository).findByPetId(petId);
        verify(visitRepository, never()).save(any(Visit.class));
    }
}