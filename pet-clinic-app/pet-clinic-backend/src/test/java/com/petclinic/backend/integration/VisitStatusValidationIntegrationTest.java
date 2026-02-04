package com.petclinic.backend.integration;

import com.petclinic.backend.model.Owner;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.repository.OwnerRepository;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.repository.VisitRepository;
import com.petclinic.backend.service.VisitStatusValidationService;
import com.petclinic.backend.service.VisitStatusValidationService.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for VisitStatusValidationService
 * Tests the validation utility with real database data
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VisitStatusValidationIntegrationTest {
    
    @Autowired
    private VisitStatusValidationService validationService;
    
    @Autowired
    private VisitRepository visitRepository;
    
    @Autowired
    private PetRepository petRepository;
    
    @Autowired
    private OwnerRepository ownerRepository;
    
    private Owner testOwner;
    private Pet testPet;
    
    @BeforeEach
    void setUp() {
        // Clean up any existing data
        visitRepository.deleteAll();
        petRepository.deleteAll();
        ownerRepository.deleteAll();
        
        // Create test owner
        testOwner = new Owner();
        testOwner.setFirstName("John");
        testOwner.setLastName("Doe");
        testOwner.setAddress("123 Main St");
        testOwner.setCity("Anytown");
        testOwner.setTelephone("555-1234");
        testOwner = ownerRepository.save(testOwner);
        
        // Create test pet
        testPet = new Pet();
        testPet.setName("Buddy");
        testPet.setSpecies("Dog");
        testPet.setBirthDate(LocalDateTime.now().minusYears(2).toLocalDate());
        testPet.setOwner(testOwner);
        testPet = petRepository.save(testPet);
    }
    
    @Test
    void testValidateAllVisitStatuses_WithConsistentData() {
        // Given - create visits with consistent status
        Visit completedVisit = new Visit();
        completedVisit.setVisitDate(LocalDateTime.now().minusDays(1));
        completedVisit.setDiagnosis("Routine checkup completed");
        completedVisit.setTreatment("Vaccinations administered");
        completedVisit.setPet(testPet);
        visitRepository.save(completedVisit);
        
        Visit incompleteVisit = new Visit();
        incompleteVisit.setVisitDate(LocalDateTime.now().minusDays(2));
        incompleteVisit.setDiagnosis("Examination in progress");
        incompleteVisit.setTreatment(null); // Incomplete as expected
        incompleteVisit.setPet(testPet);
        visitRepository.save(incompleteVisit);
        
        // When
        ValidationResult result = validationService.validateAllVisitStatuses();
        
        // Then
        assertNotNull(result);
        assertFalse(result.hasInconsistencies());
        assertEquals(2, result.getTotalVisitsChecked());
        assertEquals(0, result.getInconsistentVisitsCount());
        assertTrue(result.getInconsistencies().isEmpty());
    }
    
    @Test
    void testValidateVisitStatus_SingleVisit() {
        // Given - create a completed visit
        Visit visit = new Visit();
        visit.setVisitDate(LocalDateTime.now().minusDays(1));
        visit.setDiagnosis("Routine checkup completed");
        visit.setTreatment("Vaccinations administered");
        visit.setPet(testPet);
        visit = visitRepository.save(visit);
        
        // When
        ValidationResult result = validationService.validateVisitStatus(visit.getId());
        
        // Then
        assertNotNull(result);
        assertFalse(result.hasInconsistencies());
        assertEquals(1, result.getTotalVisitsChecked());
        assertEquals(0, result.getInconsistentVisitsCount());
        assertTrue(result.getInconsistencies().isEmpty());
    }
    
    @Test
    void testValidateVisitStatusesByPet() {
        // Given - create multiple visits for the same pet
        Visit visit1 = new Visit();
        visit1.setVisitDate(LocalDateTime.now().minusDays(1));
        visit1.setDiagnosis("Routine checkup");
        visit1.setTreatment("Vaccinations");
        visit1.setPet(testPet);
        visitRepository.save(visit1);
        
        Visit visit2 = new Visit();
        visit2.setVisitDate(LocalDateTime.now().minusDays(2));
        visit2.setDiagnosis(""); // Empty diagnosis
        visit2.setTreatment(""); // Empty treatment
        visit2.setPet(testPet);
        visitRepository.save(visit2);
        
        // When
        ValidationResult result = validationService.validateVisitStatusesByPet(testPet.getId());
        
        // Then
        assertNotNull(result);
        assertFalse(result.hasInconsistencies());
        assertEquals(2, result.getTotalVisitsChecked());
        assertEquals(0, result.getInconsistentVisitsCount());
        assertTrue(result.getInconsistencies().isEmpty());
    }
    
    @Test
    void testIsVisitStatusConsistent_CompletedVisit() {
        // Given - create a completed visit
        Visit visit = new Visit();
        visit.setVisitDate(LocalDateTime.now().minusDays(1));
        visit.setDiagnosis("Routine checkup completed");
        visit.setTreatment("Vaccinations administered");
        visit.setPet(testPet);
        
        // When
        boolean isConsistent = validationService.isVisitStatusConsistent(visit);
        boolean expectedStatus = validationService.getExpectedCompletionStatus(visit);
        
        // Then
        assertTrue(isConsistent);
        assertTrue(expectedStatus);
        assertTrue(visit.isCompleted());
    }
    
    @Test
    void testIsVisitStatusConsistent_IncompleteVisit() {
        // Given - create an incomplete visit
        Visit visit = new Visit();
        visit.setVisitDate(LocalDateTime.now().minusDays(1));
        visit.setDiagnosis("Examination in progress");
        visit.setTreatment(null); // No treatment yet
        visit.setPet(testPet);
        
        // When
        boolean isConsistent = validationService.isVisitStatusConsistent(visit);
        boolean expectedStatus = validationService.getExpectedCompletionStatus(visit);
        
        // Then
        assertTrue(isConsistent);
        assertFalse(expectedStatus);
        assertFalse(visit.isCompleted());
    }
    
    @Test
    void testGetExpectedCompletionStatus_WithWhitespaceFields() {
        // Given - create a visit with whitespace-only fields
        Visit visit = new Visit();
        visit.setVisitDate(LocalDateTime.now().minusDays(1));
        visit.setDiagnosis("   "); // Whitespace only
        visit.setTreatment("\t\n"); // Whitespace only
        visit.setPet(testPet);
        
        // When
        boolean expectedStatus = validationService.getExpectedCompletionStatus(visit);
        
        // Then
        assertFalse(expectedStatus);
        assertFalse(visit.isCompleted());
    }
    
    @Test
    void testValidateAllVisitStatuses_EmptyDatabase() {
        // Given - no visits in database (already cleaned up in setUp)
        
        // When
        ValidationResult result = validationService.validateAllVisitStatuses();
        
        // Then
        assertNotNull(result);
        assertFalse(result.hasInconsistencies());
        assertEquals(0, result.getTotalVisitsChecked());
        assertEquals(0, result.getInconsistentVisitsCount());
        assertTrue(result.getInconsistencies().isEmpty());
    }
}