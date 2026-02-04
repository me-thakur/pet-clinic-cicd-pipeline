package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.FilterCriteria;
import com.petclinic.backend.dto.FilterResult;
import com.petclinic.backend.dto.SearchResult;
import com.petclinic.backend.exception.ValidationException;
import com.petclinic.backend.model.*;
import com.petclinic.backend.repository.*;
import com.petclinic.backend.service.GlobalSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FilterServiceImpl
 * Validates: Requirements 4.2, 4.5
 */
@ExtendWith(MockitoExtension.class)
class FilterServiceImplTest {
    
    @Mock
    private PetRepository petRepository;
    
    @Mock
    private VisitRepository visitRepository;
    
    @Mock
    private VeterinarianRepository veterinarianRepository;
    
    @Mock
    private OwnerRepository ownerRepository;
    
    @Mock
    private GlobalSearchService globalSearchService;
    
    @InjectMocks
    private FilterServiceImpl filterService;
    
    private Owner testOwner;
    private Pet testPet;
    private Veterinarian testVeterinarian;
    private Visit testVisit;
    
    @BeforeEach
    void setUp() {
        // Create test data
        testOwner = new Owner("John", "Doe", "123 Main St", "Springfield", "IL", "62701", "555-1234", "+1-555-1234", "john.doe@email.com");
        testOwner.setId(1L);
        
        testPet = new Pet("Buddy", "Dog", "Golden Retriever", LocalDate.of(2020, 1, 15), testOwner);
        testPet.setId(1L);
        
        testVeterinarian = new Veterinarian("Jane", "Smith", "General Practice", "VET123456");
        testVeterinarian.setId(1L);
        
        testVisit = new Visit(LocalDateTime.of(2024, 1, 15, 10, 0), VisitType.WELLNESS_EXAM, 
                             "Healthy", "Vaccination", "Regular checkup", 
                             new BigDecimal("75.00"), testPet, testVeterinarian);
        testVisit.setId(1L);
    }
    
    @Test
    void testApplyFilters_WithValidFilters_ShouldReturnResults() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("name", "contains", "Buddy", "Pet"),
            new FilterCriteria("species", "eq", "Dog", "Pet")
        );
        
        when(petRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList(testPet));
        
        // Act
        FilterResult result = filterService.applyFilters(filters);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalResults());
        assertEquals(filters, result.getAppliedFilters());
        assertTrue(result.getExecutionTimeMs() >= 0);
        
        verify(petRepository).findAll(any(Specification.class));
    }
    
    @Test
    void testApplyFilters_WithPagination_ShouldReturnPaginatedResults() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("species", "eq", "Dog", "Pet")
        );
        
        List<Pet> pets = Arrays.asList(testPet, createTestPet("Max", "Dog"));
        when(petRepository.findAll(any(Specification.class))).thenReturn(pets);
        
        // Act
        FilterResult result = filterService.applyFilters(filters, 0, 1);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        assertEquals(2, result.getTotalResults());
        assertEquals(2, result.getTotalPages());
        assertEquals(0, result.getPage());
        assertEquals(1, result.getSize());
        assertTrue(result.isHasMore());
    }
    
    @Test
    void testApplyFilters_WithSorting_ShouldReturnSortedResults() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("species", "eq", "Dog", "Pet")
        );
        
        Pet pet1 = createTestPet("Alpha", "Dog");
        Pet pet2 = createTestPet("Zulu", "Dog");
        when(petRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList(pet2, pet1));
        
        // Act
        FilterResult result = filterService.applyFilters(filters, 0, 10, "name", "asc");
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.getResults().size());
        assertEquals("Alpha", result.getResults().get(0).getTitle());
        assertEquals("Zulu", result.getResults().get(1).getTitle());
        assertEquals("name", result.getSortBy());
        assertEquals("asc", result.getSortDirection());
    }
    
    @Test
    void testApplyFilters_WithInvalidFilters_ShouldThrowValidationException() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("", "invalid", null, "Pet")
        );
        
        // Act & Assert
        assertThrows(ValidationException.class, () -> filterService.applyFilters(filters));
    }
    
    @Test
    void testFilterPets_WithTextFilter_ShouldReturnMatchingPets() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("name", "contains", "Bud", "Pet")
        );
        
        when(petRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList(testPet));
        
        // Act
        List<SearchResult> results = filterService.filterPets(filters);
        
        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Pet", results.get(0).getEntityType());
        assertEquals("Buddy", results.get(0).getTitle());
        
        verify(petRepository).findAll(any(Specification.class));
    }
    
    @Test
    void testFilterVisits_WithDateFilter_ShouldReturnMatchingVisits() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("visitDate", "after", "2024-01-01T00:00:00", "Visit")
        );
        
        when(visitRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList(testVisit));
        
        // Act
        List<SearchResult> results = filterService.filterVisits(filters);
        
        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Visit", results.get(0).getEntityType());
        assertTrue(results.get(0).getTitle().contains("Visit on"));
        
        verify(visitRepository).findAll(any(Specification.class));
    }
    
    @Test
    void testFilterVeterinarians_WithSpecialtyFilter_ShouldReturnMatchingVeterinarians() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("specialties", "contains", "General", "Veterinarian")
        );
        
        when(veterinarianRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList(testVeterinarian));
        
        // Act
        List<SearchResult> results = filterService.filterVeterinarians(filters);
        
        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Veterinarian", results.get(0).getEntityType());
        assertTrue(results.get(0).getTitle().contains("Dr. Jane Smith"));
        
        verify(veterinarianRepository).findAll(any(Specification.class));
    }
    
    @Test
    void testFilterOwners_WithCityFilter_ShouldReturnMatchingOwners() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("city", "eq", "Springfield", "Owner")
        );
        
        when(ownerRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList(testOwner));
        
        // Act
        List<SearchResult> results = filterService.filterOwners(filters);
        
        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Owner", results.get(0).getEntityType());
        assertEquals("John Doe", results.get(0).getTitle());
        
        verify(ownerRepository).findAll(any(Specification.class));
    }
    
    @Test
    void testValidateFilters_WithValidFilters_ShouldReturnNoErrors() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("name", "contains", "test", "Pet"),
            new FilterCriteria("species", "eq", "Dog", "Pet")
        );
        
        // Act
        List<String> errors = filterService.validateFilters(filters);
        
        // Assert
        assertTrue(errors.isEmpty());
    }
    
    @Test
    void testValidateFilters_WithInvalidFilters_ShouldReturnErrors() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("", "invalid", null, "InvalidType"),
            new FilterCriteria("nonexistentField", "eq", "value", "Pet")
        );
        
        // Act
        List<String> errors = filterService.validateFilters(filters);
        
        // Assert
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(error -> error.contains("Field is required")));
        assertTrue(errors.stream().anyMatch(error -> error.contains("Invalid operator")));
        assertTrue(errors.stream().anyMatch(error -> error.contains("Value is required")));
        assertTrue(errors.stream().anyMatch(error -> error.contains("Invalid entity type")));
    }
    
    @Test
    void testSanitizeFilters_WithUnsafeInput_ShouldReturnSanitizedFilters() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("name<script>", "contains", "test'value", "Pet")
        );
        
        // Act
        List<FilterCriteria> sanitized = filterService.sanitizeFilters(filters);
        
        // Assert
        assertNotNull(sanitized);
        assertEquals(1, sanitized.size());
        assertEquals("namescript", sanitized.get(0).getField());
        assertEquals("testvalue", sanitized.get(0).getValue());
    }
    
    @Test
    void testGetAvailableFields_ForPet_ShouldReturnPetFields() {
        // Act
        Map<String, String> fields = filterService.getAvailableFields("Pet");
        
        // Assert
        assertNotNull(fields);
        assertFalse(fields.isEmpty());
        assertTrue(fields.containsKey("name"));
        assertTrue(fields.containsKey("species"));
        assertTrue(fields.containsKey("breed"));
        assertTrue(fields.containsKey("birthDate"));
        assertEquals("text", fields.get("name"));
        assertEquals("date", fields.get("birthDate"));
    }
    
    @Test
    void testGetAvailableFields_ForVisit_ShouldReturnVisitFields() {
        // Act
        Map<String, String> fields = filterService.getAvailableFields("Visit");
        
        // Assert
        assertNotNull(fields);
        assertFalse(fields.isEmpty());
        assertTrue(fields.containsKey("visitDate"));
        assertTrue(fields.containsKey("visitType"));
        assertTrue(fields.containsKey("diagnosis"));
        assertTrue(fields.containsKey("cost"));
        assertEquals("date", fields.get("visitDate"));
        assertEquals("enum", fields.get("visitType"));
        assertEquals("numeric", fields.get("cost"));
    }
    
    @Test
    void testGetAvailableOperators_ForTextFields_ShouldReturnTextOperators() {
        // Act
        List<String> operators = filterService.getAvailableOperators("text");
        
        // Assert
        assertNotNull(operators);
        assertFalse(operators.isEmpty());
        assertTrue(operators.contains("eq"));
        assertTrue(operators.contains("ne"));
        assertTrue(operators.contains("contains"));
        assertTrue(operators.contains("startswith"));
        assertTrue(operators.contains("endswith"));
    }
    
    @Test
    void testGetAvailableOperators_ForNumericFields_ShouldReturnNumericOperators() {
        // Act
        List<String> operators = filterService.getAvailableOperators("numeric");
        
        // Assert
        assertNotNull(operators);
        assertFalse(operators.isEmpty());
        assertTrue(operators.contains("eq"));
        assertTrue(operators.contains("ne"));
        assertTrue(operators.contains("gt"));
        assertTrue(operators.contains("lt"));
        assertTrue(operators.contains("gte"));
        assertTrue(operators.contains("lte"));
    }
    
    @Test
    void testSaveFilterCombination_ShouldReturnFilterId() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("name", "contains", "test", "Pet")
        );
        
        // Act
        Long filterId = filterService.saveFilterCombination(1L, "Test Filter", filters);
        
        // Assert
        assertNotNull(filterId);
        assertTrue(filterId > 0);
    }
    
    @Test
    void testGetSavedFilterCombinations_ShouldReturnSavedFilters() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("name", "contains", "test", "Pet")
        );
        Long filterId = filterService.saveFilterCombination(1L, "Test Filter", filters);
        
        // Act
        Map<Long, String> savedFilters = filterService.getSavedFilterCombinations(1L);
        
        // Assert
        assertNotNull(savedFilters);
        assertTrue(savedFilters.containsKey(filterId));
        assertEquals("Test Filter", savedFilters.get(filterId));
    }
    
    @Test
    void testLoadFilterCombination_WithValidId_ShouldReturnFilters() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("name", "contains", "test", "Pet")
        );
        Long filterId = filterService.saveFilterCombination(1L, "Test Filter", filters);
        
        // Act
        List<FilterCriteria> loadedFilters = filterService.loadFilterCombination(filterId);
        
        // Assert
        assertNotNull(loadedFilters);
        assertEquals(1, loadedFilters.size());
        assertEquals("name", loadedFilters.get(0).getField());
        assertEquals("contains", loadedFilters.get(0).getOperator());
        assertEquals("test", loadedFilters.get(0).getValue());
    }
    
    @Test
    void testDeleteFilterCombination_WithValidId_ShouldReturnTrue() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("name", "contains", "test", "Pet")
        );
        Long filterId = filterService.saveFilterCombination(1L, "Test Filter", filters);
        
        // Act
        boolean deleted = filterService.deleteFilterCombination(filterId, 1L);
        
        // Assert
        assertTrue(deleted);
        
        // Verify filter is actually deleted
        Map<Long, String> savedFilters = filterService.getSavedFilterCombinations(1L);
        assertFalse(savedFilters.containsKey(filterId));
    }
    
    @Test
    void testSaveFilterToHistory_ShouldAddToHistory() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("name", "contains", "test", "Pet")
        );
        
        // Act
        filterService.saveFilterToHistory(1L, filters);
        
        // Assert
        List<List<FilterCriteria>> history = filterService.getRecentFilterCombinations(1L, 10);
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals(filters, history.get(0));
    }
    
    @Test
    void testGetRecentFilterCombinations_ShouldReturnRecentFilters() {
        // Arrange
        List<FilterCriteria> filters1 = Arrays.asList(
            new FilterCriteria("name", "contains", "test1", "Pet")
        );
        List<FilterCriteria> filters2 = Arrays.asList(
            new FilterCriteria("name", "contains", "test2", "Pet")
        );
        
        filterService.saveFilterToHistory(1L, filters1);
        filterService.saveFilterToHistory(1L, filters2);
        
        // Act
        List<List<FilterCriteria>> history = filterService.getRecentFilterCombinations(1L, 10);
        
        // Assert
        assertNotNull(history);
        assertEquals(2, history.size());
        // Most recent should be first
        assertEquals(filters2, history.get(0));
        assertEquals(filters1, history.get(1));
    }
    
    @Test
    void testClearFilterHistory_ShouldClearHistory() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("name", "contains", "test", "Pet")
        );
        filterService.saveFilterToHistory(1L, filters);
        
        // Act
        filterService.clearFilterHistory(1L);
        
        // Assert
        List<List<FilterCriteria>> history = filterService.getRecentFilterCombinations(1L, 10);
        assertTrue(history.isEmpty());
    }
    
    @Test
    void testGetFilterAnalytics_ShouldReturnAnalytics() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("name", "contains", "test", "Pet")
        );
        filterService.saveFilterCombination(1L, "Test Filter", filters);
        
        // Act
        Map<String, Object> analytics = filterService.getFilterAnalytics();
        
        // Assert
        assertNotNull(analytics);
        assertTrue(analytics.containsKey("totalSavedCombinations"));
        assertTrue(analytics.containsKey("totalUsers"));
        assertTrue(analytics.containsKey("popularEntityTypes"));
        assertTrue(analytics.containsKey("popularOperators"));
    }
    
    @Test
    void testApplyFiltersToEntityType_ShouldReturnPagedResults() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("name", "contains", "test", "Pet")
        );
        
        when(petRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList(testPet));
        
        // Act
        Page<SearchResult> result = filterService.applyFiltersToEntityType(filters, "Pet", 0, 10);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("Pet", result.getContent().get(0).getEntityType());
    }
    
    @Test
    void testCombineSearchAndFilters_ShouldCombineResults() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("species", "eq", "Dog", "Pet")
        );
        
        when(petRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList(testPet));
        
        // Act
        FilterResult result = filterService.combineSearchAndFilters("Buddy", filters, 0, 10);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalResults());
        assertEquals("Buddy", result.getResults().get(0).getTitle());
    }
    
    @Test
    void testExportFilteredResults_ShouldReturnExportData() {
        // Arrange
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("name", "contains", "test", "Pet")
        );
        
        when(petRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList(testPet));
        
        // Act
        byte[] exportData = filterService.exportFilteredResults(filters, "csv");
        
        // Assert
        assertNotNull(exportData);
        assertTrue(exportData.length > 0);
        
        String exportString = new String(exportData);
        assertTrue(exportString.contains("Entity Type,ID,Title,Description"));
        assertTrue(exportString.contains("Pet"));
    }
    
    // Helper methods
    private Pet createTestPet(String name, String species) {
        Pet pet = new Pet(name, species, "Mixed", LocalDate.of(2021, 1, 1), testOwner);
        pet.setId(System.currentTimeMillis()); // Simple ID generation for test
        return pet;
    }
}