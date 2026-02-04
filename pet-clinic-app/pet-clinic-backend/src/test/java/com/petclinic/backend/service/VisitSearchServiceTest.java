package com.petclinic.backend.service;

import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.service.impl.VisitSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VisitSearchService
 * Tests specific search scenarios and edge cases
 * Requirements: 1.4, 1.5
 */
@ExtendWith(MockitoExtension.class)
class VisitSearchServiceTest {

    @Mock
    private VisitService visitService;

    @InjectMocks
    private VisitSearchServiceImpl visitSearchService;

    private List<Visit> testVisits;
    private Pet testPet;

    @BeforeEach
    void setUp() {
        testPet = new Pet();
        testPet.setId(1L);
        testPet.setName("Buddy");

        // Create test visits with different treatments, diagnoses, and notes
        Visit visit1 = new Visit();
        visit1.setId(1L);
        visit1.setVisitDate(LocalDateTime.now());
        visit1.setTreatment("Vaccination");
        visit1.setDiagnosis("Healthy");
        visit1.setNotes("Annual checkup completed");
        visit1.setPet(testPet);
        visit1.setCost(new BigDecimal("50.00"));

        Visit visit2 = new Visit();
        visit2.setId(2L);
        visit2.setVisitDate(LocalDateTime.now().minusDays(1));
        visit2.setTreatment("Surgery");
        visit2.setDiagnosis("Broken leg");
        visit2.setNotes("Emergency surgery performed");
        visit2.setPet(testPet);
        visit2.setCost(new BigDecimal("500.00"));

        Visit visit3 = new Visit();
        visit3.setId(3L);
        visit3.setVisitDate(LocalDateTime.now().minusDays(2));
        visit3.setTreatment("Medication");
        visit3.setDiagnosis("Infection");
        visit3.setNotes("Prescribed antibiotics");
        visit3.setPet(testPet);
        visit3.setCost(new BigDecimal("75.00"));

        Visit visit4 = new Visit();
        visit4.setId(4L);
        visit4.setVisitDate(LocalDateTime.now().minusDays(3));
        visit4.setTreatment("Dental cleaning");
        visit4.setDiagnosis("Dental plaque");
        visit4.setNotes("Routine dental maintenance");
        visit4.setPet(testPet);
        visit4.setCost(new BigDecimal("120.00"));

        testVisits = Arrays.asList(visit1, visit2, visit3, visit4);
    }

    @Test
    void testSearchByTreatment_ValidSearch_ReturnsMatchingVisits() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByTreatment("vaccination", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Vaccination", result.getContent().get(0).getTreatment());
        assertEquals(1L, result.getPage().getTotalElements());
    }

    @Test
    void testSearchByTreatment_CaseInsensitive_ReturnsMatchingVisits() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByTreatment("SURGERY", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Surgery", result.getContent().get(0).getTreatment());
    }

    @Test
    void testSearchByTreatment_PartialMatch_ReturnsMatchingVisits() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByTreatment("med", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Medication", result.getContent().get(0).getTreatment());
    }

    @Test
    void testSearchByTreatment_EmptyText_ReturnsEmptyResult() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByTreatment("", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        assertEquals(0L, result.getPage().getTotalElements());
        verify(visitService, never()).findAll();
    }

    @Test
    void testSearchByTreatment_NullText_ReturnsEmptyResult() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByTreatment(null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        assertEquals(0L, result.getPage().getTotalElements());
        verify(visitService, never()).findAll();
    }

    @Test
    void testSearchByTreatment_NoMatches_ReturnsEmptyResult() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByTreatment("nonexistent", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        assertEquals(0L, result.getPage().getTotalElements());
    }

    @Test
    void testSearchByDiagnosis_ValidSearch_ReturnsMatchingVisits() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByDiagnosis("infection", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Infection", result.getContent().get(0).getDiagnosis());
    }

    @Test
    void testSearchByDiagnosis_PartialMatch_ReturnsMatchingVisits() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByDiagnosis("broken", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Broken leg", result.getContent().get(0).getDiagnosis());
    }

    @Test
    void testSearchByDescription_ValidSearch_ReturnsMatchingVisits() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);
        Pageable pageable = PageRequest.of(0, 10);

        // Act - searching in notes field (description field doesn't exist)
        PagedResponse<Visit> result = visitSearchService.searchByDescription("emergency", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Emergency surgery performed", result.getContent().get(0).getNotes());
    }

    @Test
    void testSearchByDescription_MultipleMatches_ReturnsAllMatches() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);
        Pageable pageable = PageRequest.of(0, 10);

        // Act - search for common word in notes
        PagedResponse<Visit> result = visitSearchService.searchByDescription("routine", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).getNotes().toLowerCase().contains("routine"));
    }

    @Test
    void testSearchByNotes_ValidSearch_ReturnsMatchingVisits() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByNotes("antibiotics", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Prescribed antibiotics", result.getContent().get(0).getNotes());
    }

    @Test
    void testSearchWithPagination_FirstPage_ReturnsCorrectPage() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);
        Pageable pageable = PageRequest.of(0, 2); // First page, 2 items per page

        // Act - search for something that matches multiple visits
        PagedResponse<Visit> result = visitSearchService.searchByTreatment("a", pageable); // Should match "Vaccination" and "Medication"

        // Assert
        assertNotNull(result);
        assertTrue(result.getContent().size() <= 2);
        assertEquals(0, result.getPage().getNumber());
        assertEquals(2, result.getPage().getSize());
    }

    @Test
    void testSearchWithPagination_SecondPage_ReturnsCorrectPage() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);
        Pageable pageable = PageRequest.of(1, 1); // Second page, 1 item per page

        // Act - search for something that matches multiple visits
        PagedResponse<Visit> result = visitSearchService.searchByTreatment("n", pageable); // Should match "Vaccination", "Medication", "Dental cleaning"

        // Assert
        assertNotNull(result);
        assertTrue(result.getContent().size() <= 1);
        assertEquals(1, result.getPage().getNumber());
        assertEquals(1, result.getPage().getSize());
    }

    @Test
    void testCountByTreatment_ValidSearch_ReturnsCorrectCount() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);

        // Act
        long count = visitSearchService.countByTreatment("tion"); // Should match "Vaccination" and "Medication"

        // Assert
        assertEquals(2, count);
    }

    @Test
    void testCountByDiagnosis_ValidSearch_ReturnsCorrectCount() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);

        // Act
        long count = visitSearchService.countByDiagnosis("dental"); // Should match "Dental plaque"

        // Assert
        assertEquals(1, count);
    }

    @Test
    void testCountByDescription_ValidSearch_ReturnsCorrectCount() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);

        // Act
        long count = visitSearchService.countByDescription("performed"); // Should match notes with "performed"

        // Assert
        assertEquals(1, count);
    }

    @Test
    void testSearchByTreatment_ServiceException_ReturnsEmptyResult() {
        // Arrange
        when(visitService.findAll()).thenThrow(new RuntimeException("Database error"));
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByTreatment("vaccination", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        assertEquals(0L, result.getPage().getTotalElements());
    }

    @Test
    void testSearchByTreatment_NullTreatmentField_HandledGracefully() {
        // Arrange
        Visit visitWithNullTreatment = new Visit();
        visitWithNullTreatment.setId(5L);
        visitWithNullTreatment.setVisitDate(LocalDateTime.now());
        visitWithNullTreatment.setTreatment(null);
        visitWithNullTreatment.setDiagnosis("Test");
        visitWithNullTreatment.setPet(testPet);

        List<Visit> visitsWithNull = Arrays.asList(visitWithNullTreatment);
        when(visitService.findAll()).thenReturn(visitsWithNull);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByTreatment("test", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getContent().size()); // Should not match null treatment
    }

    @Test
    void testSearchByDiagnosis_NullDiagnosisField_HandledGracefully() {
        // Arrange
        Visit visitWithNullDiagnosis = new Visit();
        visitWithNullDiagnosis.setId(5L);
        visitWithNullDiagnosis.setVisitDate(LocalDateTime.now());
        visitWithNullDiagnosis.setTreatment("Test");
        visitWithNullDiagnosis.setDiagnosis(null);
        visitWithNullDiagnosis.setPet(testPet);

        List<Visit> visitsWithNull = Arrays.asList(visitWithNullDiagnosis);
        when(visitService.findAll()).thenReturn(visitsWithNull);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByDiagnosis("test", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getContent().size()); // Should not match null diagnosis
    }

    @Test
    void testSearchByNotes_NullNotesField_HandledGracefully() {
        // Arrange
        Visit visitWithNullNotes = new Visit();
        visitWithNullNotes.setId(5L);
        visitWithNullNotes.setVisitDate(LocalDateTime.now());
        visitWithNullNotes.setTreatment("Test");
        visitWithNullNotes.setDiagnosis("Test");
        visitWithNullNotes.setNotes(null);
        visitWithNullNotes.setPet(testPet);

        List<Visit> visitsWithNull = Arrays.asList(visitWithNullNotes);
        when(visitService.findAll()).thenReturn(visitsWithNull);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByNotes("test", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getContent().size()); // Should not match null notes
    }

    // Additional Edge Case Tests for Requirements 1.4, 1.5

    @Test
    void testSearchByTreatment_SpecialCharacters_HandledSafely() {
        // Arrange
        Visit visitWithSpecialChars = new Visit();
        visitWithSpecialChars.setId(5L);
        visitWithSpecialChars.setVisitDate(LocalDateTime.now());
        visitWithSpecialChars.setTreatment("X-ray & MRI scan");
        visitWithSpecialChars.setDiagnosis("Test");
        visitWithSpecialChars.setNotes("Test notes");
        visitWithSpecialChars.setPet(testPet);

        List<Visit> visitsWithSpecialChars = Arrays.asList(visitWithSpecialChars);
        when(visitService.findAll()).thenReturn(visitsWithSpecialChars);
        Pageable pageable = PageRequest.of(0, 10);

        // Act - search with special characters
        PagedResponse<Visit> result = visitSearchService.searchByTreatment("x-ray & mri", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("X-ray & MRI scan", result.getContent().get(0).getTreatment());
    }

    @Test
    void testSearchByTreatment_WhitespaceOnlyText_ReturnsEmptyResult() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByTreatment("   ", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        assertEquals(0L, result.getPage().getTotalElements());
        verify(visitService, never()).findAll();
    }

    @Test
    void testSearchByTreatment_LeadingTrailingWhitespace_TrimsCorrectly() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByTreatment("  vaccination  ", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Vaccination", result.getContent().get(0).getTreatment());
    }

    @Test
    void testSearchByTreatment_VeryLongSearchString_HandledGracefully() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);
        Pageable pageable = PageRequest.of(0, 10);
        String veryLongString = "a".repeat(1000); // 1000 character string

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByTreatment(veryLongString, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getContent().size()); // Should not match anything
        assertEquals(0L, result.getPage().getTotalElements());
    }

    @Test
    void testSearchByTreatment_UnicodeCharacters_HandledCorrectly() {
        // Arrange
        Visit visitWithUnicode = new Visit();
        visitWithUnicode.setId(5L);
        visitWithUnicode.setVisitDate(LocalDateTime.now());
        visitWithUnicode.setTreatment("Médication spéciale");
        visitWithUnicode.setDiagnosis("Test");
        visitWithUnicode.setNotes("Test notes");
        visitWithUnicode.setPet(testPet);

        List<Visit> visitsWithUnicode = Arrays.asList(visitWithUnicode);
        when(visitService.findAll()).thenReturn(visitsWithUnicode);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByTreatment("médication", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Médication spéciale", result.getContent().get(0).getTreatment());
    }

    @Test
    void testSearchByDiagnosis_EmptyResultSet_HandledCorrectly() {
        // Arrange
        when(visitService.findAll()).thenReturn(Arrays.asList()); // Empty list
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByDiagnosis("anything", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        assertEquals(0L, result.getPage().getTotalElements());
    }

    @Test
    void testSearchWithPagination_PageBeyondResults_ReturnsEmptyPage() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);
        Pageable pageable = PageRequest.of(10, 10); // Page 10, way beyond available data

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByTreatment("a", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        assertEquals(10, result.getPage().getNumber());
        assertTrue(result.getPage().getTotalElements() > 0); // Total elements should still be correct
    }

    @Test
    void testSearchByDescription_SqlInjectionAttempt_HandledSafely() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);
        Pageable pageable = PageRequest.of(0, 10);
        String sqlInjectionAttempt = "'; DROP TABLE visits; --";

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByDescription(sqlInjectionAttempt, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getContent().size()); // Should not match anything and not cause errors
        verify(visitService, times(1)).findAll(); // Should still call the service safely
    }

    @Test
    void testCountMethods_ConsistencyWithSearchMethods() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);

        // Act
        long treatmentCount = visitSearchService.countByTreatment("tion");
        PagedResponse<Visit> treatmentSearch = visitSearchService.searchByTreatment("tion", PageRequest.of(0, 100));

        // Assert - count should match total elements in search
        assertEquals(treatmentCount, treatmentSearch.getPage().getTotalElements());
    }

    @Test
    void testSearchByNotes_CaseInsensitiveWithMixedCase_ReturnsCorrectResults() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByNotes("EMERGENCY", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).getNotes().toLowerCase().contains("emergency"));
    }

    @Test
    void testSearchByTreatment_SingleCharacterSearch_HandledCorrectly() {
        // Arrange
        when(visitService.findAll()).thenReturn(testVisits);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        PagedResponse<Visit> result = visitSearchService.searchByTreatment("s", pageable);

        // Assert
        assertNotNull(result);
        // Should match "Surgery" 
        assertTrue(result.getContent().size() >= 1);
        assertTrue(result.getContent().stream()
                .anyMatch(visit -> visit.getTreatment().toLowerCase().contains("s")));
    }

    @Test
    void testAllCountMethods_WithEmptyText_ReturnZero() {
        // Act & Assert
        assertEquals(0, visitSearchService.countByTreatment(""));
        assertEquals(0, visitSearchService.countByTreatment(null));
        assertEquals(0, visitSearchService.countByDiagnosis(""));
        assertEquals(0, visitSearchService.countByDiagnosis(null));
        assertEquals(0, visitSearchService.countByDescription(""));
        assertEquals(0, visitSearchService.countByDescription(null));
        assertEquals(0, visitSearchService.countByNotes(""));
        assertEquals(0, visitSearchService.countByNotes(null));
        
        // Verify service is never called for empty/null searches
        verify(visitService, never()).findAll();
    }

    @Test
    void testAllCountMethods_WithServiceException_ReturnZero() {
        // Arrange
        when(visitService.findAll()).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertEquals(0, visitSearchService.countByTreatment("test"));
        assertEquals(0, visitSearchService.countByDiagnosis("test"));
        assertEquals(0, visitSearchService.countByDescription("test"));
        assertEquals(0, visitSearchService.countByNotes("test"));
    }
}