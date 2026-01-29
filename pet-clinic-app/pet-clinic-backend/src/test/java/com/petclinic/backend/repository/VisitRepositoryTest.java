package com.petclinic.backend.repository;

import com.petclinic.backend.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for VisitRepository
 * Tests the repository methods and database interactions
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Visit Repository Tests")
class VisitRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private VisitRepository visitRepository;

    private Pet testPet;
    private Veterinarian testVet;
    private Owner testOwner;

    @BeforeEach
    void setUp() {
        // Create test owner
        testOwner = new Owner();
        testOwner.setFirstName("John");
        testOwner.setLastName("Doe");
        testOwner.setAddress("123 Main St");
        testOwner.setCity("Springfield");
        testOwner.setTelephone("555-1234");
        testOwner = entityManager.persistAndFlush(testOwner);

        // Create test pet
        testPet = new Pet();
        testPet.setName("Buddy");
        testPet.setSpecies("Dog");
        testPet.setBreed("Golden Retriever");
        testPet.setBirthDate(LocalDate.of(2020, 1, 1));
        testPet.setOwner(testOwner);
        testPet = entityManager.persistAndFlush(testPet);

        // Create test veterinarian
        testVet = new Veterinarian();
        testVet.setFirstName("Dr. Jane");
        testVet.setLastName("Smith");
        testVet.setLicenseNumber("VET123456");
        testVet.addSpecialty(Specialty.GENERAL_PRACTICE);
        testVet = entityManager.persistAndFlush(testVet);
    }

    @Test
    @DisplayName("Should find visits by pet ID")
    void shouldFindVisitsByPetId() {
        // Given
        Visit visit1 = createTestVisit(LocalDateTime.now().minusDays(1), VisitType.WELLNESS_EXAM);
        Visit visit2 = createTestVisit(LocalDateTime.now().minusDays(2), VisitType.VACCINATION);
        entityManager.persistAndFlush(visit1);
        entityManager.persistAndFlush(visit2);

        // When
        List<Visit> visits = visitRepository.findByPetId(testPet.getId());

        // Then
        assertEquals(2, visits.size());
        assertTrue(visits.stream().allMatch(v -> v.getPet().getId().equals(testPet.getId())));
    }

    @Test
    @DisplayName("Should find visits by veterinarian ID")
    void shouldFindVisitsByVeterinarianId() {
        // Given
        Visit visit1 = createTestVisit(LocalDateTime.now().minusDays(1), VisitType.WELLNESS_EXAM);
        Visit visit2 = createTestVisit(LocalDateTime.now().minusDays(2), VisitType.VACCINATION);
        entityManager.persistAndFlush(visit1);
        entityManager.persistAndFlush(visit2);

        // When
        List<Visit> visits = visitRepository.findByVeterinarianId(testVet.getId());

        // Then
        assertEquals(2, visits.size());
        assertTrue(visits.stream().allMatch(v -> v.getVeterinarian().getId().equals(testVet.getId())));
    }

    @Test
    @DisplayName("Should find visits by date range")
    void shouldFindVisitsByDateRange() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(3);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        Visit pastVisit = createTestVisit(LocalDateTime.now().minusDays(2), VisitType.WELLNESS_EXAM);
        Visit futureVisit = createTestVisit(LocalDateTime.now().plusDays(2), VisitType.VACCINATION);
        Visit inRangeVisit = createTestVisit(LocalDateTime.now().minusDays(1), VisitType.EMERGENCY);
        
        entityManager.persistAndFlush(pastVisit);
        entityManager.persistAndFlush(futureVisit);
        entityManager.persistAndFlush(inRangeVisit);

        // When
        List<Visit> visits = visitRepository.findByVisitDateBetween(start, end);

        // Then
        assertEquals(2, visits.size()); // pastVisit and inRangeVisit
        assertTrue(visits.stream().allMatch(v -> 
            v.getVisitDate().isAfter(start.minusSeconds(1)) && 
            v.getVisitDate().isBefore(end.plusSeconds(1))));
    }

    @Test
    @DisplayName("Should find visits by pet ID and date range")
    void shouldFindVisitsByPetIdAndDateRange() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(3);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        Visit inRangeVisit = createTestVisit(LocalDateTime.now().minusDays(1), VisitType.WELLNESS_EXAM);
        Visit outOfRangeVisit = createTestVisit(LocalDateTime.now().plusDays(2), VisitType.VACCINATION);
        
        entityManager.persistAndFlush(inRangeVisit);
        entityManager.persistAndFlush(outOfRangeVisit);

        // When
        List<Visit> visits = visitRepository.findByPetIdAndVisitDateBetween(testPet.getId(), start, end);

        // Then
        assertEquals(1, visits.size());
        assertEquals(inRangeVisit.getId(), visits.get(0).getId());
    }

    @Test
    @DisplayName("Should find upcoming visits")
    void shouldFindUpcomingVisits() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Visit pastVisit = createTestVisit(now.minusDays(1), VisitType.WELLNESS_EXAM);
        Visit futureVisit = createTestVisit(now.plusDays(1), VisitType.VACCINATION);
        
        entityManager.persistAndFlush(pastVisit);
        entityManager.persistAndFlush(futureVisit);

        // When
        List<Visit> upcomingVisits = visitRepository.findUpcomingVisits(now);

        // Then
        assertEquals(1, upcomingVisits.size());
        assertEquals(futureVisit.getId(), upcomingVisits.get(0).getId());
    }

    @Test
    @DisplayName("Should count visits by veterinarian and date")
    void shouldCountVisitsByVeterinarianAndDate() {
        // Given
        LocalDate today = LocalDate.now();
        Visit todayVisit1 = createTestVisit(today.atTime(10, 0), VisitType.WELLNESS_EXAM);
        Visit todayVisit2 = createTestVisit(today.atTime(14, 0), VisitType.VACCINATION);
        Visit yesterdayVisit = createTestVisit(today.minusDays(1).atTime(10, 0), VisitType.EMERGENCY);
        
        entityManager.persistAndFlush(todayVisit1);
        entityManager.persistAndFlush(todayVisit2);
        entityManager.persistAndFlush(yesterdayVisit);

        // When
        long count = visitRepository.countVisitsByVeterinarianAndDate(testVet.getId(), today);

        // Then
        assertEquals(2, count);
    }

    @Test
    @DisplayName("Should find visits by visit type")
    void shouldFindVisitsByVisitType() {
        // Given
        Visit wellnessVisit = createTestVisit(LocalDateTime.now().minusDays(1), VisitType.WELLNESS_EXAM);
        Visit vaccinationVisit = createTestVisit(LocalDateTime.now().minusDays(2), VisitType.VACCINATION);
        Visit emergencyVisit = createTestVisit(LocalDateTime.now().minusDays(3), VisitType.EMERGENCY);
        
        entityManager.persistAndFlush(wellnessVisit);
        entityManager.persistAndFlush(vaccinationVisit);
        entityManager.persistAndFlush(emergencyVisit);

        // When
        List<Visit> wellnessVisits = visitRepository.findByVisitType(VisitType.WELLNESS_EXAM);

        // Then
        assertEquals(1, wellnessVisits.size());
        assertEquals(VisitType.WELLNESS_EXAM, wellnessVisits.get(0).getVisitType());
    }

    @Test
    @DisplayName("Should save and retrieve visit with all fields")
    void shouldSaveAndRetrieveVisitWithAllFields() {
        // Given
        Visit visit = new Visit();
        visit.setVisitDate(LocalDateTime.now().plusDays(1));
        visit.setVisitType(VisitType.SURGERY);
        visit.setDuration(120);
        visit.setDiagnosis("Requires surgery");
        visit.setTreatment("Surgical procedure");
        visit.setNotes("Patient is stable");
        visit.setCost(BigDecimal.valueOf(500.00));
        visit.setPet(testPet);
        visit.setVeterinarian(testVet);

        // When
        Visit savedVisit = visitRepository.save(visit);
        Visit retrievedVisit = visitRepository.findById(savedVisit.getId()).orElse(null);

        // Then
        assertNotNull(retrievedVisit);
        assertEquals(visit.getVisitDate(), retrievedVisit.getVisitDate());
        assertEquals(visit.getVisitType(), retrievedVisit.getVisitType());
        assertEquals(visit.getDuration(), retrievedVisit.getDuration());
        assertEquals(visit.getDiagnosis(), retrievedVisit.getDiagnosis());
        assertEquals(visit.getTreatment(), retrievedVisit.getTreatment());
        assertEquals(visit.getNotes(), retrievedVisit.getNotes());
        assertEquals(visit.getCost(), retrievedVisit.getCost());
        assertEquals(testPet.getId(), retrievedVisit.getPet().getId());
        assertEquals(testVet.getId(), retrievedVisit.getVeterinarian().getId());
        assertNotNull(retrievedVisit.getCreatedAt());
        assertNotNull(retrievedVisit.getUpdatedAt());
    }

    private Visit createTestVisit(LocalDateTime visitDate, VisitType visitType) {
        Visit visit = new Visit();
        visit.setVisitDate(visitDate);
        visit.setVisitType(visitType);
        visit.setDuration(visitType.getDefaultDurationMinutes());
        visit.setPet(testPet);
        visit.setVeterinarian(testVet);
        visit.setCost(BigDecimal.valueOf(100.00));
        return visit;
    }
}