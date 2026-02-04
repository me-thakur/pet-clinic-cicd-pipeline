package com.petclinic.backend.service;

import com.petclinic.backend.dto.*;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.model.VisitType;
import com.petclinic.backend.repository.VisitRepository;
import com.petclinic.backend.service.impl.EnhancedVisitServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EnhancedVisitService
 * Tests the enhanced table functionality for visits
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Enhanced Visit Service Tests")
class EnhancedVisitServiceTest {

    @Mock
    private VisitService visitService;
    
    @Mock
    private VisitRepository visitRepository;

    @InjectMocks
    private EnhancedVisitServiceImpl enhancedVisitService;

    private Visit testVisit1;
    private Visit testVisit2;

    @BeforeEach
    void setUp() {
        testVisit1 = new Visit();
        testVisit1.setId(1L);
        testVisit1.setVisitType(VisitType.WELLNESS_EXAM);
        testVisit1.setDiagnosis("Healthy");
        testVisit1.setTreatment("Routine checkup");

        testVisit2 = new Visit();
        testVisit2.setId(2L);
        testVisit2.setVisitType(VisitType.EMERGENCY);
        testVisit2.setDiagnosis(null);
        testVisit2.setTreatment(null);
    }

    @Test
    @DisplayName("Should return available visit status values")
    void shouldReturnAvailableVisitStatusValues() {
        // When
        List<String> statusValues = enhancedVisitService.getAvailableVisitStatusValues();

        // Then
        assertNotNull(statusValues);
        assertEquals(2, statusValues.size());
        assertTrue(statusValues.contains("completed"));
        assertTrue(statusValues.contains("pending"));
    }

    @Test
    @DisplayName("Should return available visit type values")
    void shouldReturnAvailableVisitTypeValues() {
        // When
        List<String> visitTypeValues = enhancedVisitService.getAvailableVisitTypeValues();

        // Then
        assertNotNull(visitTypeValues);
        assertTrue(visitTypeValues.size() > 0);
        assertTrue(visitTypeValues.contains("WELLNESS_EXAM"));
        assertTrue(visitTypeValues.contains("EMERGENCY"));
    }

    @Test
    @DisplayName("Should return sortable visit columns")
    void shouldReturnSortableVisitColumns() {
        // When
        List<String> sortableColumns = enhancedVisitService.getSortableVisitColumns();

        // Then
        assertNotNull(sortableColumns);
        assertTrue(sortableColumns.contains("id"));
        assertTrue(sortableColumns.contains("visitDate"));
        assertTrue(sortableColumns.contains("visitType"));
        assertTrue(sortableColumns.contains("pet.name"));
        assertTrue(sortableColumns.contains("veterinarian.lastName"));
    }

    @Test
    @DisplayName("Should return filterable visit columns")
    void shouldReturnFilterableVisitColumns() {
        // When
        List<String> filterableColumns = enhancedVisitService.getFilterableVisitColumns();

        // Then
        assertNotNull(filterableColumns);
        assertTrue(filterableColumns.contains("status"));
        assertTrue(filterableColumns.contains("visitType"));
        assertTrue(filterableColumns.contains("pet.name"));
        assertTrue(filterableColumns.contains("veterinarian.lastName"));
    }

    @Test
    @DisplayName("Should validate visit sort criteria correctly")
    void shouldValidateVisitSortCriteria() {
        // Given
        SortMetadata validSort = new SortMetadata("visitDate", "asc", true);
        SortMetadata invalidSort = new SortMetadata("invalidColumn", "asc", true);

        // When & Then
        assertTrue(enhancedVisitService.validateVisitSortCriteria(validSort));
        assertFalse(enhancedVisitService.validateVisitSortCriteria(invalidSort));
        assertFalse(enhancedVisitService.validateVisitSortCriteria(null));
    }

    @Test
    @DisplayName("Should validate visit filter criteria correctly")
    void shouldValidateVisitFilterCriteria() {
        // Given
        FilterCriteria validFilter = new FilterCriteria("status", "equals", "completed", "visits");
        FilterCriteria invalidFilter = new FilterCriteria("invalidField", "equals", "value", "visits");

        // When & Then
        assertTrue(enhancedVisitService.validateVisitFilterCriteria(validFilter));
        assertFalse(enhancedVisitService.validateVisitFilterCriteria(invalidFilter));
        assertFalse(enhancedVisitService.validateVisitFilterCriteria(null));
    }

    @Test
    @DisplayName("Should find visits by completion status")
    void shouldFindVisitsByCompletionStatus() {
        // Given
        when(visitRepository.findAll()).thenReturn(Arrays.asList(testVisit1, testVisit2));

        // When
        List<Visit> completedVisits = enhancedVisitService.findVisitsByCompletionStatus(true);
        List<Visit> pendingVisits = enhancedVisitService.findVisitsByCompletionStatus(false);

        // Then
        assertNotNull(completedVisits);
        assertNotNull(pendingVisits);
        
        // testVisit1 should be completed (has diagnosis and treatment)
        // testVisit2 should be pending (no diagnosis and treatment)
        assertEquals(1, completedVisits.size());
        assertEquals(1, pendingVisits.size());
        assertEquals(testVisit1.getId(), completedVisits.get(0).getId());
        assertEquals(testVisit2.getId(), pendingVisits.get(0).getId());
    }

    @Test
    @DisplayName("Should find visits by type")
    void shouldFindVisitsByType() {
        // Given
        when(visitRepository.findAll()).thenReturn(Arrays.asList(testVisit1, testVisit2));

        // When
        List<Visit> wellnessVisits = enhancedVisitService.findVisitsByType("WELLNESS_EXAM");
        List<Visit> emergencyVisits = enhancedVisitService.findVisitsByType("EMERGENCY");

        // Then
        assertNotNull(wellnessVisits);
        assertNotNull(emergencyVisits);
        assertEquals(1, wellnessVisits.size());
        assertEquals(1, emergencyVisits.size());
        assertEquals(testVisit1.getId(), wellnessVisits.get(0).getId());
        assertEquals(testVisit2.getId(), emergencyVisits.get(0).getId());
    }

    @Test
    @DisplayName("Should handle invalid visit type gracefully")
    void shouldHandleInvalidVisitType() {
        // When
        List<Visit> visits = enhancedVisitService.findVisitsByType("INVALID_TYPE");

        // Then
        assertNotNull(visits);
        assertTrue(visits.isEmpty());
    }

    @Test
    @DisplayName("Should handle null or empty visit type")
    void shouldHandleNullOrEmptyVisitType() {
        // When
        List<Visit> nullVisits = enhancedVisitService.findVisitsByType(null);
        List<Visit> emptyVisits = enhancedVisitService.findVisitsByType("");

        // Then
        assertNotNull(nullVisits);
        assertNotNull(emptyVisits);
        assertTrue(nullVisits.isEmpty());
        assertTrue(emptyVisits.isEmpty());
    }
}