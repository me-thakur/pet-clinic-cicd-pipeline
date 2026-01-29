package com.petclinic.backend.properties;

import com.petclinic.backend.dto.SearchResult;
import com.petclinic.backend.dto.SearchResultSummary;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Specialty;
import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.model.VisitType;
import com.petclinic.backend.repository.OwnerRepository;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.repository.VeterinarianRepository;
import com.petclinic.backend.repository.VisitRepository;
import com.petclinic.backend.service.GlobalSearchService;
import net.java.quickcheck.Generator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for Search Result Completeness
 * **Validates: Requirements 4.1, 4.3, 4.4**
 * 
 * Property 6: Search Result Completeness
 * If an entity contains the search query, it must appear in search results
 * Search across all entity types must return results from all applicable entities
 * Pagination must not lose or duplicate results
 * Result counts must match actual result quantities
 * Search suggestions must be relevant to the partial query
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Search Result Completeness Property-Based Tests")
class SearchResultCompletenessProperties extends PropertyTestBase {
    
    @Autowired
    private GlobalSearchService globalSearchService;
    
    @Autowired
    private PetRepository petRepository;
    
    @Autowired
    private OwnerRepository ownerRepository;
    
    @Autowired
    private VeterinarianRepository veterinarianRepository;
    
    @Autowired
    private VisitRepository visitRepository;
    
    private List<Owner> testOwners;
    private List<Pet> testPets;
    private List<Veterinarian> testVeterinarians;
    private List<Visit> testVisits;
    
    @BeforeEach
    void setUp() {
        super.setUp();
        setupTestData();
    }
    
    /**
     * Property 6: Search Result Completeness - Entity Inclusion
     * If an entity contains the search query, it must appear in search results
     * **Validates: Requirements 4.1, 4.3, 4.4**
     */
    @Test
    @DisplayName("Property 6: Search Result Completeness - Entities containing query must appear in results")
    void testEntityInclusionCompleteness() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Given: Create entities with known searchable content
            String uniqueSearchTerm = "UniqueSearchTerm" + System.currentTimeMillis() + random.nextInt(10000);
            
            // Create entities that should match the search
            Owner ownerWithMatch = createOwnerWithSearchTerm(uniqueSearchTerm);
            Pet petWithMatch = createPetWithSearchTerm(uniqueSearchTerm, ownerWithMatch);
            Veterinarian vetWithMatch = createVeterinarianWithSearchTerm(uniqueSearchTerm);
            Visit visitWithMatch = createVisitWithSearchTerm(uniqueSearchTerm, petWithMatch, vetWithMatch);
            
            // Save entities
            Owner savedOwner = ownerRepository.save(ownerWithMatch);
            Pet savedPet = petRepository.save(petWithMatch);
            Veterinarian savedVet = veterinarianRepository.save(vetWithMatch);
            Visit savedVisit = visitRepository.save(visitWithMatch);
            
            // When: Performing global search
            SearchResultSummary summary = globalSearchService.globalSearch(uniqueSearchTerm);
            
            // Then: All matching entities must appear in results
            assertNotNull(summary, "Search summary should not be null");
            assertNotNull(summary.getResults(), "Search results should not be null");
            
            // Verify each entity type appears in results
            boolean foundOwner = summary.getResults().stream()
                    .anyMatch(r -> "Owner".equals(r.getEntityType()) && savedOwner.getId().equals(r.getEntityId()));
            boolean foundPet = summary.getResults().stream()
                    .anyMatch(r -> "Pet".equals(r.getEntityType()) && savedPet.getId().equals(r.getEntityId()));
            boolean foundVet = summary.getResults().stream()
                    .anyMatch(r -> "Veterinarian".equals(r.getEntityType()) && savedVet.getId().equals(r.getEntityId()));
            boolean foundVisit = summary.getResults().stream()
                    .anyMatch(r -> "Visit".equals(r.getEntityType()) && savedVisit.getId().equals(r.getEntityId()));
            
            assertTrue(foundOwner, "Owner containing search term should appear in results");
            assertTrue(foundPet, "Pet containing search term should appear in results");
            assertTrue(foundVet, "Veterinarian containing search term should appear in results");
            assertTrue(foundVisit, "Visit containing search term should appear in results");
            
            // Verify result counts match actual findings
            Map<String, Integer> resultsByType = summary.getResultsByType();
            assertTrue(resultsByType.get("Owner") >= 1, "Owner count should be at least 1");
            assertTrue(resultsByType.get("Pet") >= 1, "Pet count should be at least 1");
            assertTrue(resultsByType.get("Veterinarian") >= 1, "Veterinarian count should be at least 1");
            assertTrue(resultsByType.get("Visit") >= 1, "Visit count should be at least 1");
        });
    }
    
    /**
     * Property 6: Search Result Completeness - Cross-Entity Search Coverage
     * Search across all entity types must return results from all applicable entities
     * **Validates: Requirements 4.1, 4.3, 4.4**
     */
    @Test
    @DisplayName("Property 6: Search Result Completeness - Cross-entity search must cover all applicable entities")
    void testCrossEntitySearchCoverage() {
        runPropertyTest(50, () -> { // Reduced iterations for complex test
            // Given: Generate a search term that exists across multiple entity types
            String commonTerm = generateCommonSearchTerm().next();
            
            // When: Performing global search
            SearchResultSummary summary = globalSearchService.globalSearch(commonTerm);
            
            // Then: Verify comprehensive coverage
            assertNotNull(summary, "Search summary should not be null");
            Map<String, Integer> resultsByType = summary.getResultsByType();
            assertNotNull(resultsByType, "Results by type should not be null");
            
            // Verify all entity types are represented in the result map
            assertTrue(resultsByType.containsKey("Pet"), "Results should include Pet count");
            assertTrue(resultsByType.containsKey("Visit"), "Results should include Visit count");
            assertTrue(resultsByType.containsKey("Veterinarian"), "Results should include Veterinarian count");
            assertTrue(resultsByType.containsKey("Owner"), "Results should include Owner count");
            
            // Verify individual entity searches return consistent results
            List<SearchResult> petResults = globalSearchService.searchPets(commonTerm);
            List<SearchResult> visitResults = globalSearchService.searchVisits(commonTerm);
            List<SearchResult> vetResults = globalSearchService.searchVeterinarians(commonTerm);
            List<SearchResult> ownerResults = globalSearchService.searchOwners(commonTerm);
            
            // Verify counts match between global and individual searches
            assertEquals(petResults.size(), resultsByType.get("Pet").intValue(),
                        "Pet count should match between global and individual search");
            assertEquals(visitResults.size(), resultsByType.get("Visit").intValue(),
                        "Visit count should match between global and individual search");
            assertEquals(vetResults.size(), resultsByType.get("Veterinarian").intValue(),
                        "Veterinarian count should match between global and individual search");
            assertEquals(ownerResults.size(), resultsByType.get("Owner").intValue(),
                        "Owner count should match between global and individual search");
            
            // Verify total count consistency
            int totalFromIndividual = petResults.size() + visitResults.size() + 
                                    vetResults.size() + ownerResults.size();
            assertEquals(totalFromIndividual, summary.getResults().size(),
                        "Total results should match sum of individual entity searches");
        });
    }
    
    /**
     * Property 6: Search Result Completeness - Pagination Completeness
     * Pagination must not lose or duplicate results
     * **Validates: Requirements 4.1, 4.3, 4.4**
     */
    @Test
    @DisplayName("Property 6: Search Result Completeness - Pagination must not lose or duplicate results")
    void testPaginationCompleteness() {
        runPropertyTest(25, () -> { // Reduced iterations for pagination test
            // Given: Generate search query that returns multiple results
            String searchQuery = generateCommonSearchTerm().next();
            int pageSize = 3 + random.nextInt(5); // 3-7 results per page
            
            // When: Getting all results through pagination
            List<SearchResult> allPaginatedResults = new ArrayList<>();
            Set<String> seenResultIds = new HashSet<>();
            int currentPage = 0;
            SearchResultSummary currentPageResults;
            
            do {
                currentPageResults = globalSearchService.globalSearch(searchQuery, currentPage, pageSize);
                assertNotNull(currentPageResults, "Page results should not be null");
                
                // Verify page size constraints
                assertTrue(currentPageResults.getResults().size() <= pageSize,
                          "Page should not exceed specified page size");
                
                // Check for duplicates
                for (SearchResult result : currentPageResults.getResults()) {
                    String resultId = result.getEntityType() + ":" + result.getEntityId();
                    assertFalse(seenResultIds.contains(resultId),
                               "Result should not be duplicated across pages: " + resultId);
                    seenResultIds.add(resultId);
                    allPaginatedResults.add(result);
                }
                
                currentPage++;
                
                // Safety check to prevent infinite loops
                if (currentPage > 20) {
                    break;
                }
                
            } while (currentPage < currentPageResults.getTotalPages());
            
            // Then: Compare with non-paginated results
            SearchResultSummary allResults = globalSearchService.globalSearch(searchQuery, 0, 1000);
            
            // Verify total count consistency
            assertEquals(allResults.getResults().size(), allPaginatedResults.size(),
                        "Paginated results should contain same number of items as non-paginated");
            
            // Verify all results are present (order may differ)
            Set<String> allResultIds = new HashSet<>();
            for (SearchResult result : allResults.getResults()) {
                allResultIds.add(result.getEntityType() + ":" + result.getEntityId());
            }
            
            Set<String> paginatedResultIds = new HashSet<>();
            for (SearchResult result : allPaginatedResults) {
                paginatedResultIds.add(result.getEntityType() + ":" + result.getEntityId());
            }
            
            assertEquals(allResultIds, paginatedResultIds,
                        "Paginated results should contain exactly the same entities as non-paginated");
        });
    }
    
    /**
     * Property 6: Search Result Completeness - Result Count Accuracy
     * Result counts must match actual result quantities
     * **Validates: Requirements 4.1, 4.3, 4.4**
     */
    @Test
    @DisplayName("Property 6: Search Result Completeness - Result counts must match actual quantities")
    void testResultCountAccuracy() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Given: Generate search query
            String searchQuery = generateSearchQuery().next();
            
            // When: Performing search and getting counts
            SearchResultSummary summary = globalSearchService.globalSearch(searchQuery);
            Map<String, Integer> searchResultCounts = globalSearchService.getSearchResultCounts(searchQuery);
            
            // Then: Verify count consistency
            assertNotNull(summary, "Search summary should not be null");
            assertNotNull(searchResultCounts, "Search result counts should not be null");
            
            Map<String, Integer> summaryResultsByType = summary.getResultsByType();
            
            // Verify counts match between summary and dedicated count method
            assertEquals(summaryResultsByType.get("Pet"), searchResultCounts.get("Pet"),
                        "Pet counts should match between summary and count method");
            assertEquals(summaryResultsByType.get("Visit"), searchResultCounts.get("Visit"),
                        "Visit counts should match between summary and count method");
            assertEquals(summaryResultsByType.get("Veterinarian"), searchResultCounts.get("Veterinarian"),
                        "Veterinarian counts should match between summary and count method");
            assertEquals(summaryResultsByType.get("Owner"), searchResultCounts.get("Owner"),
                        "Owner counts should match between summary and count method");
            
            // Verify actual result count matches reported count
            Map<String, Integer> actualCounts = new HashMap<>();
            actualCounts.put("Pet", 0);
            actualCounts.put("Visit", 0);
            actualCounts.put("Veterinarian", 0);
            actualCounts.put("Owner", 0);
            
            for (SearchResult result : summary.getResults()) {
                actualCounts.merge(result.getEntityType(), 1, Integer::sum);
            }
            
            assertEquals(actualCounts.get("Pet"), summaryResultsByType.get("Pet"),
                        "Actual Pet count should match reported count");
            assertEquals(actualCounts.get("Visit"), summaryResultsByType.get("Visit"),
                        "Actual Visit count should match reported count");
            assertEquals(actualCounts.get("Veterinarian"), summaryResultsByType.get("Veterinarian"),
                        "Actual Veterinarian count should match reported count");
            assertEquals(actualCounts.get("Owner"), summaryResultsByType.get("Owner"),
                        "Actual Owner count should match reported count");
            
            // Verify total count
            int totalActual = actualCounts.values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(totalActual, summary.getResults().size(),
                        "Total actual count should match results size");
        });
    }
    
    /**
     * Property 6: Search Result Completeness - Search Suggestions Relevance
     * Search suggestions must be relevant to the partial query
     * **Validates: Requirements 4.1, 4.3, 4.4**
     */
    @Test
    @DisplayName("Property 6: Search Result Completeness - Search suggestions must be relevant")
    void testSearchSuggestionsRelevance() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Given: Generate partial query
            String partialQuery = generatePartialQuery().next();
            int maxSuggestions = 5 + random.nextInt(10); // 5-14 suggestions
            
            // When: Getting search suggestions
            List<String> suggestions = globalSearchService.getSearchSuggestions(partialQuery, maxSuggestions);
            
            // Then: Verify suggestions are relevant and within limits
            assertNotNull(suggestions, "Suggestions should not be null");
            assertTrue(suggestions.size() <= maxSuggestions,
                      "Suggestions should not exceed maximum requested");
            
            // Verify each suggestion is relevant to the partial query
            for (String suggestion : suggestions) {
                assertNotNull(suggestion, "Suggestion should not be null");
                assertFalse(suggestion.trim().isEmpty(), "Suggestion should not be empty");
                
                // For meaningful partial queries, suggestions should contain the partial query
                if (partialQuery != null && partialQuery.trim().length() > 1) {
                    assertTrue(suggestion.toLowerCase().contains(partialQuery.toLowerCase()),
                              String.format("Suggestion '%s' should contain partial query '%s'", 
                                          suggestion, partialQuery));
                }
            }
            
            // Verify suggestions are unique
            Set<String> uniqueSuggestions = new HashSet<>(suggestions);
            assertEquals(uniqueSuggestions.size(), suggestions.size(),
                        "Suggestions should be unique");
        });
    }
    
    /**
     * Property 6: Search Result Completeness - Entity Type Filtering
     * Search within specific entity types should return complete results for that type
     * **Validates: Requirements 4.1, 4.3, 4.4**
     */
    @Test
    @DisplayName("Property 6: Search Result Completeness - Entity type filtering should be complete")
    void testEntityTypeFilteringCompleteness() {
        runPropertyTest(50, () -> { // Reduced iterations for complex test
            // Given: Generate search query and entity type
            String searchQuery = generateSearchQuery().next();
            String entityType = generateEntityType().next();
            int pageSize = 10;
            
            // When: Searching within specific entity type with pagination
            Page<SearchResult> pagedResults = globalSearchService.searchByEntityType(
                searchQuery, entityType, 0, pageSize);
            
            // Also get results through individual entity search
            List<SearchResult> individualResults;
            switch (entityType) {
                case "Pet":
                    individualResults = globalSearchService.searchPets(searchQuery);
                    break;
                case "Visit":
                    individualResults = globalSearchService.searchVisits(searchQuery);
                    break;
                case "Veterinarian":
                    individualResults = globalSearchService.searchVeterinarians(searchQuery);
                    break;
                case "Owner":
                    individualResults = globalSearchService.searchOwners(searchQuery);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown entity type: " + entityType);
            }
            
            // Then: Verify completeness and consistency
            assertNotNull(pagedResults, "Paged results should not be null");
            assertNotNull(individualResults, "Individual results should not be null");
            
            // Verify all results are of correct entity type
            for (SearchResult result : pagedResults.getContent()) {
                assertEquals(entityType, result.getEntityType(),
                           "All results should be of requested entity type");
            }
            
            for (SearchResult result : individualResults) {
                assertEquals(entityType, result.getEntityType(),
                           "All individual results should be of requested entity type");
            }
            
            // Verify total count consistency
            assertEquals(individualResults.size(), (int) pagedResults.getTotalElements(),
                        "Total elements should match individual search results");
            
            // If there are results, verify first page contains expected results
            if (!individualResults.isEmpty()) {
                assertTrue(pagedResults.getContent().size() > 0,
                          "First page should contain results when individual search has results");
                
                // Verify first page results are subset of individual results
                Set<String> individualIds = new HashSet<>();
                for (SearchResult result : individualResults) {
                    individualIds.add(result.getEntityType() + ":" + result.getEntityId());
                }
                
                for (SearchResult result : pagedResults.getContent()) {
                    String resultId = result.getEntityType() + ":" + result.getEntityId();
                    assertTrue(individualIds.contains(resultId),
                              "Paged result should be present in individual results");
                }
            }
        });
    }
    
    // Helper methods
    
    private void setupTestData() {
        testOwners = new ArrayList<>();
        testPets = new ArrayList<>();
        testVeterinarians = new ArrayList<>();
        testVisits = new ArrayList<>();
        
        // Create test owners with searchable content
        String[] ownerNames = {"John Smith", "Jane Doe", "Bob Johnson", "Alice Brown", "Charlie Wilson"};
        for (int i = 0; i < ownerNames.length; i++) {
            Owner owner = new Owner();
            String[] nameParts = ownerNames[i].split(" ");
            owner.setFirstName(nameParts[0]);
            owner.setLastName(nameParts[1]);
            owner.setEmail(nameParts[0].toLowerCase() + "." + nameParts[1].toLowerCase() + "@test.com");
            owner.setAddress("123 Main Street " + i);
            owner.setCity("Test City");
            owner.setTelephone("555-010-" + String.format("%04d", i));
            testOwners.add(ownerRepository.save(owner));
        }
        
        // Create test pets with searchable content
        String[] petNames = {"Buddy", "Max", "Bella", "Charlie", "Lucy", "Cooper", "Daisy", "Rocky"};
        String[] species = {"Dog", "Cat", "Bird", "Rabbit"};
        String[] breeds = {"Golden Retriever", "Siamese Cat", "Parakeet", "Holland Lop"};
        
        for (int i = 0; i < petNames.length; i++) {
            Pet pet = new Pet();
            pet.setName(petNames[i]);
            pet.setSpecies(species[i % species.length]);
            pet.setBreed(breeds[i % breeds.length]);
            pet.setBirthDate(LocalDate.now().minusYears(1 + i % 8));
            pet.setOwner(testOwners.get(i % testOwners.size()));
            pet.setMedicalHistory("Medical history for " + petNames[i]);
            testPets.add(petRepository.save(pet));
        }
        
        // Create test veterinarians with searchable content
        String[] vetNames = {"Dr. Sarah Johnson", "Dr. Michael Brown", "Dr. Emily Davis"};
        for (int i = 0; i < vetNames.length; i++) {
            Veterinarian vet = new Veterinarian();
            String[] nameParts = vetNames[i].replace("Dr. ", "").split(" ");
            vet.setFirstName(nameParts[0]);
            vet.setLastName(nameParts[1]);
            vet.setLicenseNumber("VET" + String.format("%05d", 1000 + i));
            Set<Specialty> specialties = new HashSet<>();
            specialties.add(Specialty.values()[i % Specialty.values().length]);
            vet.setSpecialtySet(specialties);
            testVeterinarians.add(veterinarianRepository.save(vet));
        }
        
        // Create test visits with searchable content
        String[] diagnoses = {"Healthy checkup", "Skin allergy", "Dental cleaning", "Vaccination", "Injury treatment"};
        String[] treatments = {"Routine care", "Medication prescribed", "Dental procedure", "Vaccination given", "Wound care"};
        
        for (int i = 0; i < 10; i++) {
            Visit visit = new Visit();
            visit.setVisitDate(LocalDateTime.now().minusDays(i * 7));
            visit.setVisitType(VisitType.values()[i % VisitType.values().length]);
            visit.setDiagnosis(diagnoses[i % diagnoses.length]);
            visit.setTreatment(treatments[i % treatments.length]);
            visit.setNotes("Visit notes for " + testPets.get(i % testPets.size()).getName());
            visit.setCost(BigDecimal.valueOf(75.0 + i * 15));
            visit.setPet(testPets.get(i % testPets.size()));
            visit.setVeterinarian(testVeterinarians.get(i % testVeterinarians.size()));
            testVisits.add(visitRepository.save(visit));
        }
    }
    
    private Owner createOwnerWithSearchTerm(String searchTerm) {
        Owner owner = new Owner();
        owner.setFirstName(searchTerm + "Owner");
        owner.setLastName("TestLastName");
        owner.setEmail(searchTerm.toLowerCase() + "@test.com");
        owner.setAddress("123 Test Street");
        owner.setCity("Test City");
        owner.setTelephone("555-999-9999");
        return owner;
    }
    
    private Pet createPetWithSearchTerm(String searchTerm, Owner owner) {
        Pet pet = new Pet();
        pet.setName(searchTerm + "Pet");
        pet.setSpecies("Dog");
        pet.setBreed("Test Breed");
        pet.setBirthDate(LocalDate.now().minusYears(2));
        pet.setOwner(owner);
        pet.setMedicalHistory("Medical history with " + searchTerm);
        return pet;
    }
    
    private Veterinarian createVeterinarianWithSearchTerm(String searchTerm) {
        Veterinarian vet = new Veterinarian();
        vet.setFirstName(searchTerm + "Vet");
        vet.setLastName("TestVetLastName");
        // Ensure license number is valid: 6-20 uppercase alphanumeric characters
        String licenseBase = searchTerm.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (licenseBase.length() < 6) {
            licenseBase = licenseBase + "123456".substring(0, 6 - licenseBase.length());
        } else if (licenseBase.length() > 20) {
            licenseBase = licenseBase.substring(0, 20);
        }
        vet.setLicenseNumber(licenseBase);
        Set<Specialty> specialties = new HashSet<>();
        specialties.add(Specialty.SURGERY);
        vet.setSpecialtySet(specialties);
        return vet;
    }
    
    private Visit createVisitWithSearchTerm(String searchTerm, Pet pet, Veterinarian vet) {
        Visit visit = new Visit();
        visit.setVisitDate(LocalDateTime.now().minusDays(1));
        visit.setVisitType(VisitType.WELLNESS_EXAM);
        visit.setDiagnosis("Diagnosis with " + searchTerm);
        visit.setTreatment("Treatment with " + searchTerm);
        visit.setNotes("Notes with " + searchTerm);
        visit.setCost(BigDecimal.valueOf(100.0));
        visit.setPet(pet);
        visit.setVeterinarian(vet);
        return visit;
    }
    
    private Generator<String> generateSearchQuery() {
        return () -> {
            String[] queries = {
                "Test", "Dog", "Cat", "Dr", "Smith", "Johnson", "Healthy", 
                "Medical", "555", "test.com", "Main", "Street"
            };
            return queries[random.nextInt(queries.length)];
        };
    }
    
    private Generator<String> generateCommonSearchTerm() {
        return () -> {
            String[] commonTerms = {"Test", "Dr", "555", "Street", "Medical"};
            return commonTerms[random.nextInt(commonTerms.length)];
        };
    }
    
    private Generator<String> generateEntityType() {
        return () -> {
            String[] entityTypes = {"Pet", "Visit", "Veterinarian", "Owner"};
            return entityTypes[random.nextInt(entityTypes.length)];
        };
    }
    
    private Generator<String> generatePartialQuery() {
        return () -> {
            String[] partialQueries = {"Te", "Dr", "Jo", "55", "Ma", "Me", "St", "Do", "Ca"};
            return partialQueries[random.nextInt(partialQueries.length)];
        };
    }
}