package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.SearchResult;
import com.petclinic.backend.dto.SearchResultSummary;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.repository.OwnerRepository;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.repository.VeterinarianRepository;
import com.petclinic.backend.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GlobalSearchServiceImpl
 * Validates: Requirements 4.1, 4.3, 4.4
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalSearchService Tests")
class GlobalSearchServiceImplTest {
    
    @Mock
    private PetRepository petRepository;
    
    @Mock
    private VisitRepository visitRepository;
    
    @Mock
    private VeterinarianRepository veterinarianRepository;
    
    @Mock
    private OwnerRepository ownerRepository;
    
    @InjectMocks
    private GlobalSearchServiceImpl globalSearchService;
    
    private Pet testPet;
    private Visit testVisit;
    private Veterinarian testVeterinarian;
    private Owner testOwner;
    
    @BeforeEach
    void setUp() {
        // Create test owner
        testOwner = new Owner();
        testOwner.setId(1L);
        testOwner.setFirstName("John");
        testOwner.setLastName("Doe");
        testOwner.setEmail("john.doe@example.com");
        testOwner.setTelephone("555-1234");
        
        // Create test pet
        testPet = new Pet();
        testPet.setId(1L);
        testPet.setName("Buddy");
        testPet.setSpecies("Dog");
        testPet.setBreed("Golden Retriever");
        testPet.setBirthDate(LocalDate.of(2020, 1, 1));
        testPet.setOwner(testOwner);
        
        // Create test veterinarian
        testVeterinarian = new Veterinarian();
        testVeterinarian.setId(1L);
        testVeterinarian.setFirstName("Dr. Jane");
        testVeterinarian.setLastName("Smith");
        testVeterinarian.setLicenseNumber("VET123456");
        testVeterinarian.setSpecialties("Surgery, Cardiology");
        
        // Create test visit
        testVisit = new Visit();
        testVisit.setId(1L);
        testVisit.setVisitDate(LocalDateTime.now());
        testVisit.setDiagnosis("Routine checkup");
        testVisit.setTreatment("Vaccination");
        testVisit.setNotes("Healthy pet");
        testVisit.setPet(testPet);
        testVisit.setVeterinarian(testVeterinarian);
    }
    
    @Test
    @DisplayName("Should perform global search successfully")
    void testGlobalSearch_Success() {
        // Given
        String query = "Buddy";
        
        when(petRepository.findByNameContainingIgnoreCase(query))
                .thenReturn(Arrays.asList(testPet));
        when(petRepository.findBySpeciesIgnoreCase(query))
                .thenReturn(new ArrayList<>());
        when(petRepository.findByBreedContainingIgnoreCase(query))
                .thenReturn(new ArrayList<>());
        when(petRepository.findByMedicalHistoryContainingIgnoreCase(query))
                .thenReturn(new ArrayList<>());
        when(petRepository.findByOwnerNameContaining(query))
                .thenReturn(new ArrayList<>());
        
        when(visitRepository.findByDiagnosisContainingIgnoreCase(query))
                .thenReturn(new ArrayList<>());
        when(visitRepository.findByTreatmentContainingIgnoreCase(query))
                .thenReturn(new ArrayList<>());
        when(visitRepository.findByNotesContainingIgnoreCase(query))
                .thenReturn(new ArrayList<>());
        
        when(veterinarianRepository.findByFirstNameContainingIgnoreCase(query))
                .thenReturn(new ArrayList<>());
        when(veterinarianRepository.findByLastNameContainingIgnoreCase(query))
                .thenReturn(new ArrayList<>());
        when(veterinarianRepository.findByLicenseNumberContainingIgnoreCase(query))
                .thenReturn(new ArrayList<>());
        when(veterinarianRepository.findBySpecialtiesContainingIgnoreCase(query))
                .thenReturn(new ArrayList<>());
        
        when(ownerRepository.findByFirstNameContainingIgnoreCase(query))
                .thenReturn(new ArrayList<>());
        when(ownerRepository.findByLastNameContainingIgnoreCase(query))
                .thenReturn(new ArrayList<>());
        when(ownerRepository.findByEmailContainingIgnoreCase(query))
                .thenReturn(new ArrayList<>());
        when(ownerRepository.findByAddressContainingIgnoreCase(query))
                .thenReturn(new ArrayList<>());
        when(ownerRepository.findByTelephoneContaining(query))
                .thenReturn(new ArrayList<>());
        
        // When
        SearchResultSummary result = globalSearchService.globalSearch(query);
        
        // Then
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertEquals(1, result.getTotalResults());
        assertEquals(1, result.getPetCount());
        assertEquals(0, result.getVisitCount());
        assertEquals(0, result.getVeterinarianCount());
        assertEquals(0, result.getOwnerCount());
        
        List<SearchResult> results = result.getResults();
        assertEquals(1, results.size());
        
        SearchResult petResult = results.get(0);
        assertEquals("Pet", petResult.getEntityType());
        assertEquals(testPet.getId(), petResult.getEntityId());
        assertTrue(petResult.getTitle().contains("Buddy"));
        assertTrue(petResult.getMatchedFields().contains("name"));
    }
    
    @Test
    @DisplayName("Should return empty results for empty query")
    void testGlobalSearch_EmptyQuery() {
        // When
        SearchResultSummary result = globalSearchService.globalSearch("");
        
        // Then
        assertNotNull(result);
        assertEquals("", result.getQuery());
        assertEquals(0, result.getTotalResults());
        assertTrue(result.getResults().isEmpty());
    }
    
    @Test
    @DisplayName("Should return empty results for null query")
    void testGlobalSearch_NullQuery() {
        // When
        SearchResultSummary result = globalSearchService.globalSearch(null);
        
        // Then
        assertNotNull(result);
        assertNull(result.getQuery());
        assertEquals(0, result.getTotalResults());
        assertTrue(result.getResults().isEmpty());
    }
    
    @Test
    @DisplayName("Should perform paginated search successfully")
    void testGlobalSearchPaginated_Success() {
        // Given
        String query = "test";
        int page = 0;
        int size = 10;
        
        // Mock empty results for simplicity
        when(petRepository.findByNameContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(petRepository.findBySpeciesIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(petRepository.findByBreedContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(petRepository.findByMedicalHistoryContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(petRepository.findByOwnerNameContaining(anyString()))
                .thenReturn(new ArrayList<>());
        
        when(visitRepository.findByDiagnosisContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(visitRepository.findByTreatmentContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(visitRepository.findByNotesContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        
        when(veterinarianRepository.findByFirstNameContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(veterinarianRepository.findByLastNameContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(veterinarianRepository.findByLicenseNumberContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(veterinarianRepository.findBySpecialtiesContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        
        when(ownerRepository.findByFirstNameContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(ownerRepository.findByLastNameContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(ownerRepository.findByEmailContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(ownerRepository.findByAddressContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(ownerRepository.findByTelephoneContaining(anyString()))
                .thenReturn(new ArrayList<>());
        
        // When
        SearchResultSummary result = globalSearchService.globalSearch(query, page, size);
        
        // Then
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertEquals(page, result.getPage());
        assertEquals(size, result.getSize());
        assertEquals(0, result.getTotalResults());
    }
    
    @Test
    @DisplayName("Should search pets successfully")
    void testSearchPets_Success() {
        // Given
        String query = "Buddy";
        
        when(petRepository.findByNameContainingIgnoreCase(query))
                .thenReturn(Arrays.asList(testPet));
        when(petRepository.findBySpeciesIgnoreCase(query))
                .thenReturn(new ArrayList<>());
        when(petRepository.findByBreedContainingIgnoreCase(query))
                .thenReturn(new ArrayList<>());
        when(petRepository.findByMedicalHistoryContainingIgnoreCase(query))
                .thenReturn(new ArrayList<>());
        when(petRepository.findByOwnerNameContaining(query))
                .thenReturn(new ArrayList<>());
        
        // When
        List<SearchResult> results = globalSearchService.searchPets(query);
        
        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        
        SearchResult result = results.get(0);
        assertEquals("Pet", result.getEntityType());
        assertEquals(testPet.getId(), result.getEntityId());
        assertTrue(result.getTitle().contains("Buddy"));
        assertTrue(result.getMatchedFields().contains("name"));
    }
    
    @Test
    @DisplayName("Should search by entity type successfully")
    void testSearchByEntityType_Success() {
        // Given
        String query = "Buddy";
        String entityType = "Pet";
        int page = 0;
        int size = 10;
        
        when(petRepository.findByNameContainingIgnoreCase(query))
                .thenReturn(Arrays.asList(testPet));
        when(petRepository.findBySpeciesIgnoreCase(query))
                .thenReturn(new ArrayList<>());
        when(petRepository.findByBreedContainingIgnoreCase(query))
                .thenReturn(new ArrayList<>());
        when(petRepository.findByMedicalHistoryContainingIgnoreCase(query))
                .thenReturn(new ArrayList<>());
        when(petRepository.findByOwnerNameContaining(query))
                .thenReturn(new ArrayList<>());
        
        // When
        Page<SearchResult> result = globalSearchService.searchByEntityType(query, entityType, page, size);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        
        SearchResult searchResult = result.getContent().get(0);
        assertEquals("Pet", searchResult.getEntityType());
        assertEquals(testPet.getId(), searchResult.getEntityId());
    }
    
    @Test
    @DisplayName("Should get search result counts successfully")
    void testGetSearchResultCounts_Success() {
        // Given
        String query = "test";
        
        when(petRepository.findByNameContainingIgnoreCase(anyString()))
                .thenReturn(Arrays.asList(testPet));
        when(petRepository.findBySpeciesIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(petRepository.findByBreedContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(petRepository.findByMedicalHistoryContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(petRepository.findByOwnerNameContaining(anyString()))
                .thenReturn(new ArrayList<>());
        
        when(visitRepository.findByDiagnosisContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(visitRepository.findByTreatmentContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(visitRepository.findByNotesContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        
        when(veterinarianRepository.findByFirstNameContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(veterinarianRepository.findByLastNameContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(veterinarianRepository.findByLicenseNumberContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(veterinarianRepository.findBySpecialtiesContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        
        when(ownerRepository.findByFirstNameContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(ownerRepository.findByLastNameContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(ownerRepository.findByEmailContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(ownerRepository.findByAddressContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(ownerRepository.findByTelephoneContaining(anyString()))
                .thenReturn(new ArrayList<>());
        
        // When
        Map<String, Integer> counts = globalSearchService.getSearchResultCounts(query);
        
        // Then
        assertNotNull(counts);
        assertEquals(4, counts.size());
        assertEquals(1, counts.get("Pet").intValue());
        assertEquals(0, counts.get("Visit").intValue());
        assertEquals(0, counts.get("Veterinarian").intValue());
        assertEquals(0, counts.get("Owner").intValue());
    }
    
    @Test
    @DisplayName("Should get search suggestions successfully")
    void testGetSearchSuggestions_Success() {
        // Given
        String partialQuery = "Bu";
        int maxSuggestions = 10;
        
        when(petRepository.findByNameContainingIgnoreCase(anyString()))
                .thenReturn(Arrays.asList(testPet));
        when(petRepository.findBySpeciesIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(veterinarianRepository.findByFirstNameContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        when(ownerRepository.findByFirstNameContainingIgnoreCase(anyString()))
                .thenReturn(new ArrayList<>());
        
        // When
        List<String> suggestions = globalSearchService.getSearchSuggestions(partialQuery, maxSuggestions);
        
        // Then
        assertNotNull(suggestions);
        assertTrue(suggestions.size() <= maxSuggestions);
        assertTrue(suggestions.contains("Buddy"));
    }
    
    @Test
    @DisplayName("Should save and retrieve search queries")
    void testSaveAndRetrieveSearchQueries() {
        // Given
        Long userId = 1L;
        String query1 = "test query 1";
        String query2 = "test query 2";
        
        // When
        globalSearchService.saveSearchQuery(userId, query1);
        globalSearchService.saveSearchQuery(userId, query2);
        
        List<String> recentSearches = globalSearchService.getRecentSearches(userId, 10);
        
        // Then
        assertNotNull(recentSearches);
        assertEquals(2, recentSearches.size());
        assertEquals(query2, recentSearches.get(0)); // Most recent first
        assertEquals(query1, recentSearches.get(1));
    }
    
    @Test
    @DisplayName("Should get popular search terms")
    void testGetPopularSearchTerms() {
        // Given
        String query1 = "popular query";
        String query2 = "another query";
        
        // When
        globalSearchService.saveSearchQuery(null, query1);
        globalSearchService.saveSearchQuery(null, query1); // Save twice to make it more popular
        globalSearchService.saveSearchQuery(null, query2);
        
        List<String> popularTerms = globalSearchService.getPopularSearchTerms(10);
        
        // Then
        assertNotNull(popularTerms);
        assertTrue(popularTerms.contains(query1));
        assertTrue(popularTerms.contains(query2));
        assertEquals(query1, popularTerms.get(0)); // Most popular first
    }
    
    @Test
    @DisplayName("Should get search analytics")
    void testGetSearchAnalytics() {
        // Given
        globalSearchService.saveSearchQuery(1L, "test query");
        
        // When
        Map<String, Object> analytics = globalSearchService.getSearchAnalytics();
        
        // Then
        assertNotNull(analytics);
        assertTrue(analytics.containsKey("totalSearches"));
        assertTrue(analytics.containsKey("uniqueSearchTerms"));
        assertTrue(analytics.containsKey("activeUsers"));
        assertTrue(analytics.containsKey("popularTerms"));
    }
    
    @Test
    @DisplayName("Should clear search history")
    void testClearSearchHistory() {
        // Given
        Long userId = 1L;
        globalSearchService.saveSearchQuery(userId, "test query");
        
        // When
        globalSearchService.clearSearchHistory(userId);
        List<String> recentSearches = globalSearchService.getRecentSearches(userId, 10);
        
        // Then
        assertNotNull(recentSearches);
        assertTrue(recentSearches.isEmpty());
    }
}