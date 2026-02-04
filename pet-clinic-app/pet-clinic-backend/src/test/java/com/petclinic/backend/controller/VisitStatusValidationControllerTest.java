package com.petclinic.backend.controller;

import com.petclinic.backend.service.VisitStatusValidationService;
import com.petclinic.backend.service.VisitStatusValidationService.ValidationResult;
import com.petclinic.backend.service.VisitStatusValidationService.InconsistencyReport;
import com.petclinic.backend.service.VisitStatusValidationService.RecalculationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for VisitStatusValidationController
 * Tests REST endpoints for visit status validation
 */
@ExtendWith(MockitoExtension.class)
class VisitStatusValidationControllerTest {
    
    @Mock
    private VisitStatusValidationService validationService;
    
    @InjectMocks
    private VisitStatusValidationController controller;
    
    @Test
    void testValidateAllVisits_NoInconsistencies() {
        // Given
        ValidationResult result = new ValidationResult(false, Collections.emptyList(), 5, 0);
        when(validationService.validateAllVisitStatuses()).thenReturn(result);
        
        // When
        ResponseEntity<ValidationResult> response = controller.validateAllVisits();
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().hasInconsistencies());
        assertEquals(5, response.getBody().getTotalVisitsChecked());
        assertEquals(0, response.getBody().getInconsistentVisitsCount());
        assertTrue(response.getBody().getInconsistencies().isEmpty());
    }
    
    @Test
    void testValidateAllVisits_WithInconsistencies() {
        // Given
        InconsistencyReport report = new InconsistencyReport(
            1L, "Buddy", "Doe", "Diagnosis", null, true, 
            "Visit should be marked as completed but shows as pending"
        );
        ValidationResult result = new ValidationResult(true, Arrays.asList(report), 5, 1);
        when(validationService.validateAllVisitStatuses()).thenReturn(result);
        
        // When
        ResponseEntity<ValidationResult> response = controller.validateAllVisits();
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().hasInconsistencies());
        assertEquals(5, response.getBody().getTotalVisitsChecked());
        assertEquals(1, response.getBody().getInconsistentVisitsCount());
        assertEquals(1, response.getBody().getInconsistencies().size());
        
        InconsistencyReport responseReport = response.getBody().getInconsistencies().get(0);
        assertEquals(1L, responseReport.getVisitId());
        assertEquals("Buddy", responseReport.getPetName());
        assertEquals("Doe", responseReport.getOwnerName());
    }
    
    @Test
    void testValidateVisitsByDateRange_ValidRange() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        ValidationResult result = new ValidationResult(false, Collections.emptyList(), 3, 0);
        when(validationService.validateVisitStatusesByDateRange(startDate, endDate)).thenReturn(result);
        
        // When
        ResponseEntity<ValidationResult> response = controller.validateVisitsByDateRange(startDate, endDate);
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().hasInconsistencies());
        assertEquals(3, response.getBody().getTotalVisitsChecked());
        assertEquals(0, response.getBody().getInconsistentVisitsCount());
    }
    
    @Test
    void testValidateVisit_ExistingVisit() {
        // Given
        Long visitId = 1L;
        ValidationResult result = new ValidationResult(false, Collections.emptyList(), 1, 0);
        when(validationService.validateVisitStatus(visitId)).thenReturn(result);
        
        // When
        ResponseEntity<ValidationResult> response = controller.validateVisit(visitId);
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().hasInconsistencies());
        assertEquals(1, response.getBody().getTotalVisitsChecked());
        assertEquals(0, response.getBody().getInconsistentVisitsCount());
    }
    
    @Test
    void testValidateVisitsByPet_ExistingPet() {
        // Given
        Long petId = 1L;
        ValidationResult result = new ValidationResult(false, Collections.emptyList(), 2, 0);
        when(validationService.validateVisitStatusesByPet(petId)).thenReturn(result);
        
        // When
        ResponseEntity<ValidationResult> response = controller.validateVisitsByPet(petId);
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().hasInconsistencies());
        assertEquals(2, response.getBody().getTotalVisitsChecked());
        assertEquals(0, response.getBody().getInconsistentVisitsCount());
    }
    
    @Test
    void testGetValidationSummary() {
        // When
        ResponseEntity<VisitStatusValidationController.ValidationSummary> response = controller.getValidationSummary();
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("Visit Status Validation Service", response.getBody().getServiceName());
        assertNotNull(response.getBody().getDescription());
        assertNotNull(response.getBody().getAvailableOperations());
        assertTrue(response.getBody().getAvailableOperations().length > 0);
        assertNotNull(response.getBody().getCompletionLogic());
    }
    
    // Batch Recalculation Controller Tests
    
    @Test
    void testRecalculateAllVisitStatuses_Success() {
        // Given
        RecalculationResult result = new RecalculationResult(
            10, 10, Arrays.asList(1L, 2L, 3L), Collections.emptyList(), 150L
        );
        when(validationService.recalculateAllVisitStatuses()).thenReturn(result);
        
        // When
        ResponseEntity<RecalculationResult> response = controller.recalculateAllVisitStatuses();
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(10, response.getBody().getTotalVisitsProcessed());
        assertEquals(10, response.getBody().getVisitsUpdated());
        assertFalse(response.getBody().hasErrors());
        assertTrue(response.getBody().getErrors().isEmpty());
        assertEquals(3, response.getBody().getUpdatedVisitIds().size());
        assertEquals(150L, response.getBody().getProcessingTimeMs());
    }
    
    @Test
    void testRecalculateAllVisitStatuses_WithErrors() {
        // Given
        RecalculationResult result = new RecalculationResult(
            10, 8, Arrays.asList(1L, 2L), Arrays.asList("Error processing visit 3", "Error processing visit 5"), 200L
        );
        when(validationService.recalculateAllVisitStatuses()).thenReturn(result);
        
        // When
        ResponseEntity<RecalculationResult> response = controller.recalculateAllVisitStatuses();
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(10, response.getBody().getTotalVisitsProcessed());
        assertEquals(8, response.getBody().getVisitsUpdated());
        assertTrue(response.getBody().hasErrors());
        assertEquals(2, response.getBody().getErrors().size());
        assertEquals(2, response.getBody().getUpdatedVisitIds().size());
        assertEquals(200L, response.getBody().getProcessingTimeMs());
    }
    
    @Test
    void testRecalculateVisitStatusesByDateRange_Success() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        RecalculationResult result = new RecalculationResult(
            5, 5, Arrays.asList(1L, 2L, 3L, 4L, 5L), Collections.emptyList(), 75L
        );
        when(validationService.recalculateVisitStatusesByDateRange(startDate, endDate)).thenReturn(result);
        
        // When
        ResponseEntity<RecalculationResult> response = controller.recalculateVisitStatusesByDateRange(startDate, endDate);
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().getTotalVisitsProcessed());
        assertEquals(5, response.getBody().getVisitsUpdated());
        assertFalse(response.getBody().hasErrors());
        assertEquals(5, response.getBody().getUpdatedVisitIds().size());
        assertEquals(75L, response.getBody().getProcessingTimeMs());
    }
    
    @Test
    void testRecalculateVisitStatusesByPet_Success() {
        // Given
        Long petId = 1L;
        RecalculationResult result = new RecalculationResult(
            3, 3, Arrays.asList(1L, 2L, 3L), Collections.emptyList(), 50L
        );
        when(validationService.recalculateVisitStatusesByPet(petId)).thenReturn(result);
        
        // When
        ResponseEntity<RecalculationResult> response = controller.recalculateVisitStatusesByPet(petId);
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().getTotalVisitsProcessed());
        assertEquals(3, response.getBody().getVisitsUpdated());
        assertFalse(response.getBody().hasErrors());
        assertEquals(3, response.getBody().getUpdatedVisitIds().size());
        assertEquals(50L, response.getBody().getProcessingTimeMs());
    }
    
    @Test
    void testRecalculateVisitStatusesByPet_NoVisits() {
        // Given
        Long petId = 999L;
        RecalculationResult result = new RecalculationResult(
            0, 0, Collections.emptyList(), Collections.emptyList(), 5L
        );
        when(validationService.recalculateVisitStatusesByPet(petId)).thenReturn(result);
        
        // When
        ResponseEntity<RecalculationResult> response = controller.recalculateVisitStatusesByPet(petId);
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getTotalVisitsProcessed());
        assertEquals(0, response.getBody().getVisitsUpdated());
        assertFalse(response.getBody().hasErrors());
        assertTrue(response.getBody().getUpdatedVisitIds().isEmpty());
        assertEquals(5L, response.getBody().getProcessingTimeMs());
    }
}