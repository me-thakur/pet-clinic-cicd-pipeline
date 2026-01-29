package com.petclinic.backend.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Visit entity
 * Tests the basic functionality of the Visit entity
 */
@DisplayName("Visit Entity Tests")
class VisitEntityTest {

    @Test
    @DisplayName("Should create Visit with required fields")
    void shouldCreateVisitWithRequiredFields() {
        // Given
        LocalDateTime visitDate = LocalDateTime.now().plusDays(1);
        Pet pet = new Pet();
        pet.setId(1L);
        pet.setName("Buddy");
        pet.setSpecies("Dog");

        // When
        Visit visit = new Visit(visitDate, pet);

        // Then
        assertNotNull(visit);
        assertEquals(visitDate, visit.getVisitDate());
        assertEquals(pet, visit.getPet());
        assertEquals(30, visit.getDuration()); // Default duration
        assertNotNull(visit.getCreatedAt());
        assertNotNull(visit.getUpdatedAt());
    }

    @Test
    @DisplayName("Should create Visit with visit type and auto-calculate duration")
    void shouldCreateVisitWithVisitTypeAndDuration() {
        // Given
        LocalDateTime visitDate = LocalDateTime.now().plusDays(1);
        VisitType visitType = VisitType.WELLNESS_EXAM;
        Pet pet = new Pet();
        pet.setId(1L);

        // When
        Visit visit = new Visit(visitDate, visitType, pet);

        // Then
        assertEquals(visitType, visit.getVisitType());
        assertEquals(visitType.getDefaultDurationMinutes(), visit.getDuration());
    }

    @Test
    @DisplayName("Should set visit type and update duration")
    void shouldSetVisitTypeAndUpdateDuration() {
        // Given
        Visit visit = new Visit();
        VisitType visitType = VisitType.SURGERY;

        // When
        visit.setVisitType(visitType);

        // Then
        assertEquals(visitType, visit.getVisitType());
        assertEquals(visitType.getDefaultDurationMinutes(), visit.getDuration());
    }

    @Test
    @DisplayName("Should calculate end time correctly")
    void shouldCalculateEndTimeCorrectly() {
        // Given
        LocalDateTime visitDate = LocalDateTime.of(2024, 1, 15, 10, 0);
        Visit visit = new Visit();
        visit.setVisitDate(visitDate);
        visit.setDuration(60);

        // When
        LocalDateTime endTime = visit.getEndTime();

        // Then
        assertEquals(LocalDateTime.of(2024, 1, 15, 11, 0), endTime);
    }

    @Test
    @DisplayName("Should identify emergency visits correctly")
    void shouldIdentifyEmergencyVisits() {
        // Given
        Visit emergencyVisit = new Visit();
        emergencyVisit.setVisitType(VisitType.EMERGENCY);

        Visit regularVisit = new Visit();
        regularVisit.setVisitType(VisitType.WELLNESS_EXAM);

        // Then
        assertTrue(emergencyVisit.isEmergencyVisit());
        assertFalse(regularVisit.isEmergencyVisit());
    }

    @Test
    @DisplayName("Should identify completed visits correctly")
    void shouldIdentifyCompletedVisits() {
        // Given
        Visit completedVisit = new Visit();
        completedVisit.setDiagnosis("Healthy");
        completedVisit.setTreatment("Vaccination");

        Visit incompleteVisit = new Visit();
        incompleteVisit.setDiagnosis("Healthy");
        // No treatment set

        // Then
        assertTrue(completedVisit.isCompleted());
        assertFalse(incompleteVisit.isCompleted());
    }

    @Test
    @DisplayName("Should identify visits with cost correctly")
    void shouldIdentifyVisitsWithCost() {
        // Given
        Visit visitWithCost = new Visit();
        visitWithCost.setCost(BigDecimal.valueOf(100.00));

        Visit visitWithoutCost = new Visit();
        visitWithoutCost.setCost(BigDecimal.ZERO);

        Visit visitWithNullCost = new Visit();

        // Then
        assertTrue(visitWithCost.hasCost());
        assertFalse(visitWithoutCost.hasCost());
        assertFalse(visitWithNullCost.hasCost());
    }

    @Test
    @DisplayName("Should generate visit summary correctly")
    void shouldGenerateVisitSummaryCorrectly() {
        // Given
        LocalDateTime visitDate = LocalDateTime.of(2024, 1, 15, 10, 0);
        Visit visit = new Visit();
        visit.setVisitDate(visitDate);
        visit.setVisitType(VisitType.WELLNESS_EXAM);
        visit.setDiagnosis("Healthy pet");

        Veterinarian vet = new Veterinarian();
        vet.setLastName("Smith");
        visit.setVeterinarian(vet);

        // When
        String summary = visit.getVisitSummary();

        // Then
        assertTrue(summary.contains("2024-01-15"));
        assertTrue(summary.contains("Dr. Smith"));
        assertTrue(summary.contains("Wellness Examination"));
        assertTrue(summary.contains("Healthy pet"));
    }

    @Test
    @DisplayName("Should identify visits requiring specialist correctly")
    void shouldIdentifyVisitsRequiringSpecialist() {
        // Given
        Visit surgeryVisit = new Visit();
        surgeryVisit.setVisitType(VisitType.SURGERY);

        Visit regularVisit = new Visit();
        regularVisit.setVisitType(VisitType.WELLNESS_EXAM);

        // Then
        assertTrue(surgeryVisit.requiresSpecialist());
        assertFalse(regularVisit.requiresSpecialist());
    }

    @Test
    @DisplayName("Should identify preventive care visits correctly")
    void shouldIdentifyPreventiveCareVisits() {
        // Given
        Visit preventiveVisit = new Visit();
        preventiveVisit.setVisitType(VisitType.VACCINATION);

        Visit treatmentVisit = new Visit();
        treatmentVisit.setVisitType(VisitType.SURGERY);

        // Then
        assertTrue(preventiveVisit.isPreventiveCare());
        assertFalse(treatmentVisit.isPreventiveCare());
    }

    @Test
    @DisplayName("Should get recommended specialty correctly")
    void shouldGetRecommendedSpecialtyCorrectly() {
        // Given
        Visit cardiologyVisit = new Visit();
        cardiologyVisit.setVisitType(VisitType.CARDIOLOGY_EXAM);

        // Then
        assertEquals(Specialty.CARDIOLOGY, cardiologyVisit.getRecommendedSpecialty());
    }
}