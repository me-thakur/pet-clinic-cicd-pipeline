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
import com.petclinic.backend.service.GlobalSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of GlobalSearchService providing cross-entity search functionality
 * Validates: Requirements 4.1, 4.3, 4.4
 */
@Service
@Transactional(readOnly = true)
public class GlobalSearchServiceImpl implements GlobalSearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalSearchServiceImpl.class);
    
    private final PetRepository petRepository;
    private final VisitRepository visitRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final OwnerRepository ownerRepository;
    
    // In-memory storage for search history and analytics (in production, use Redis or database)
    private final Map<Long, List<String>> userSearchHistory = new ConcurrentHashMap<>();
    private final Map<String, Integer> searchTermFrequency = new ConcurrentHashMap<>();
    private final Map<String, Long> searchAnalytics = new ConcurrentHashMap<>();
    
    private static final String HIGHLIGHT_START = "<mark>";
    private static final String HIGHLIGHT_END = "</mark>";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_SUGGESTIONS = 10;
    
    @Autowired
    public GlobalSearchServiceImpl(PetRepository petRepository,
                                 VisitRepository visitRepository,
                                 VeterinarianRepository veterinarianRepository,
                                 OwnerRepository ownerRepository) {
        this.petRepository = petRepository;
        this.visitRepository = visitRepository;
        this.veterinarianRepository = veterinarianRepository;
        this.ownerRepository = ownerRepository;
    }
    
    @Override
    public SearchResultSummary globalSearch(String query) {
        return globalSearch(query, 0, DEFAULT_PAGE_SIZE);
    }
    
    @Override
    public SearchResultSummary globalSearch(String query, int page, int size) {
        return globalSearch(query, page, size, "relevance", "desc");
    }
    
    @Override
    public SearchResultSummary globalSearch(String query, int page, int size, String sortBy, String sortDirection) {
        logger.debug("Performing global search for query: '{}', page: {}, size: {}, sortBy: {}, sortDirection: {}", 
                    query, page, size, sortBy, sortDirection);
        
        long startTime = System.currentTimeMillis();
        
        // Validate and sanitize input
        if (query == null || query.trim().isEmpty()) {
            return createEmptySearchResult(query, page, size, sortBy, sortDirection);
        }
        
        String sanitizedQuery = sanitizeQuery(query.trim());
        
        // Track search analytics
        trackSearchQuery(sanitizedQuery);
        
        try {
            // Search across all entity types
            List<SearchResult> petResults = searchPets(sanitizedQuery);
            List<SearchResult> visitResults = searchVisits(sanitizedQuery);
            List<SearchResult> veterinarianResults = searchVeterinarians(sanitizedQuery);
            List<SearchResult> ownerResults = searchOwners(sanitizedQuery);
            
            // Combine all results
            List<SearchResult> allResults = new ArrayList<>();
            allResults.addAll(petResults);
            allResults.addAll(visitResults);
            allResults.addAll(veterinarianResults);
            allResults.addAll(ownerResults);
            
            // Sort results
            sortResults(allResults, sortBy, sortDirection);
            
            // Create result counts by type
            Map<String, Integer> resultsByType = new HashMap<>();
            resultsByType.put("Pet", petResults.size());
            resultsByType.put("Visit", visitResults.size());
            resultsByType.put("Veterinarian", veterinarianResults.size());
            resultsByType.put("Owner", ownerResults.size());
            
            // Apply pagination
            int totalResults = allResults.size();
            int totalPages = (int) Math.ceil((double) totalResults / size);
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalResults);
            
            List<SearchResult> paginatedResults = startIndex < totalResults ? 
                allResults.subList(startIndex, endIndex) : new ArrayList<>();
            
            // Create summary
            SearchResultSummary summary = new SearchResultSummary(sanitizedQuery, paginatedResults, resultsByType, page, size, totalPages);
            summary.setSortBy(sortBy);
            summary.setSortDirection(sortDirection);
            summary.setTotalResults(totalResults);
            
            long executionTime = System.currentTimeMillis() - startTime;
            summary.setExecutionTimeMs(executionTime);
            
            logger.debug("Global search completed in {}ms, found {} results", executionTime, totalResults);
            
            return summary;
            
        } catch (Exception e) {
            logger.error("Error performing global search for query: '{}'", sanitizedQuery, e);
            return createEmptySearchResult(sanitizedQuery, page, size, sortBy, sortDirection);
        }
    }
    
    @Override
    public List<SearchResult> searchPets(String query) {
        logger.debug("Searching pets for query: '{}'", query);
        
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String sanitizedQuery = sanitizeQuery(query.trim());
        List<SearchResult> results = new ArrayList<>();
        
        try {
            // Search by name
            List<Pet> petsByName = petRepository.findByNameContainingIgnoreCase(sanitizedQuery);
            for (Pet pet : petsByName) {
                SearchResult result = createPetSearchResult(pet, sanitizedQuery);
                result.getMatchedFields().add("name");
                results.add(result);
            }
            
            // Search by species
            List<Pet> petsBySpecies = petRepository.findBySpeciesIgnoreCase(sanitizedQuery);
            for (Pet pet : petsBySpecies) {
                if (!containsResult(results, "Pet", pet.getId())) {
                    SearchResult result = createPetSearchResult(pet, sanitizedQuery);
                    result.getMatchedFields().add("species");
                    results.add(result);
                }
            }
            
            // Search by breed
            List<Pet> petsByBreed = petRepository.findByBreedContainingIgnoreCase(sanitizedQuery);
            for (Pet pet : petsByBreed) {
                SearchResult existingResult = findResult(results, "Pet", pet.getId());
                if (existingResult != null) {
                    existingResult.getMatchedFields().add("breed");
                    existingResult.setRelevanceScore(existingResult.getRelevanceScore() + 0.5);
                } else {
                    SearchResult result = createPetSearchResult(pet, sanitizedQuery);
                    result.getMatchedFields().add("breed");
                    results.add(result);
                }
            }
            
            // Search by medical history
            List<Pet> petsByMedicalHistory = petRepository.findByMedicalHistoryContainingIgnoreCase(sanitizedQuery);
            for (Pet pet : petsByMedicalHistory) {
                SearchResult existingResult = findResult(results, "Pet", pet.getId());
                if (existingResult != null) {
                    existingResult.getMatchedFields().add("medicalHistory");
                    existingResult.setRelevanceScore(existingResult.getRelevanceScore() + 0.3);
                } else {
                    SearchResult result = createPetSearchResult(pet, sanitizedQuery);
                    result.getMatchedFields().add("medicalHistory");
                    results.add(result);
                }
            }
            
            // Search by owner name
            List<Pet> petsByOwnerName = petRepository.findByOwnerNameContaining(sanitizedQuery);
            for (Pet pet : petsByOwnerName) {
                SearchResult existingResult = findResult(results, "Pet", pet.getId());
                if (existingResult != null) {
                    existingResult.getMatchedFields().add("owner");
                    existingResult.setRelevanceScore(existingResult.getRelevanceScore() + 0.4);
                } else {
                    SearchResult result = createPetSearchResult(pet, sanitizedQuery);
                    result.getMatchedFields().add("owner");
                    results.add(result);
                }
            }
            
            logger.debug("Found {} pet results for query: '{}'", results.size(), sanitizedQuery);
            
        } catch (Exception e) {
            logger.error("Error searching pets for query: '{}'", sanitizedQuery, e);
        }
        
        return results;
    }
    
    @Override
    public List<SearchResult> searchVisits(String query) {
        logger.debug("Searching visits for query: '{}'", query);
        
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String sanitizedQuery = sanitizeQuery(query.trim());
        List<SearchResult> results = new ArrayList<>();
        
        try {
            // Search by diagnosis
            List<Visit> visitsByDiagnosis = visitRepository.findByDiagnosisContainingIgnoreCase(sanitizedQuery);
            for (Visit visit : visitsByDiagnosis) {
                SearchResult result = createVisitSearchResult(visit, sanitizedQuery);
                result.getMatchedFields().add("diagnosis");
                results.add(result);
            }
            
            // Search by treatment
            List<Visit> visitsByTreatment = visitRepository.findByTreatmentContainingIgnoreCase(sanitizedQuery);
            for (Visit visit : visitsByTreatment) {
                SearchResult existingResult = findResult(results, "Visit", visit.getId());
                if (existingResult != null) {
                    existingResult.getMatchedFields().add("treatment");
                    existingResult.setRelevanceScore(existingResult.getRelevanceScore() + 0.5);
                } else {
                    SearchResult result = createVisitSearchResult(visit, sanitizedQuery);
                    result.getMatchedFields().add("treatment");
                    results.add(result);
                }
            }
            
            // Search by notes
            List<Visit> visitsByNotes = visitRepository.findByNotesContainingIgnoreCase(sanitizedQuery);
            for (Visit visit : visitsByNotes) {
                SearchResult existingResult = findResult(results, "Visit", visit.getId());
                if (existingResult != null) {
                    existingResult.getMatchedFields().add("notes");
                    existingResult.setRelevanceScore(existingResult.getRelevanceScore() + 0.3);
                } else {
                    SearchResult result = createVisitSearchResult(visit, sanitizedQuery);
                    result.getMatchedFields().add("notes");
                    results.add(result);
                }
            }
            
            logger.debug("Found {} visit results for query: '{}'", results.size(), sanitizedQuery);
            
        } catch (Exception e) {
            logger.error("Error searching visits for query: '{}'", sanitizedQuery, e);
        }
        
        return results;
    }
    
    @Override
    public List<SearchResult> searchVeterinarians(String query) {
        logger.debug("Searching veterinarians for query: '{}'", query);
        
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String sanitizedQuery = sanitizeQuery(query.trim());
        List<SearchResult> results = new ArrayList<>();
        
        try {
            // Search by first name
            List<Veterinarian> vetsByFirstName = veterinarianRepository.findByFirstNameContainingIgnoreCase(sanitizedQuery);
            for (Veterinarian vet : vetsByFirstName) {
                SearchResult result = createVeterinarianSearchResult(vet, sanitizedQuery);
                result.getMatchedFields().add("firstName");
                results.add(result);
            }
            
            // Search by last name
            List<Veterinarian> vetsByLastName = veterinarianRepository.findByLastNameContainingIgnoreCase(sanitizedQuery);
            for (Veterinarian vet : vetsByLastName) {
                SearchResult existingResult = findResult(results, "Veterinarian", vet.getId());
                if (existingResult != null) {
                    existingResult.getMatchedFields().add("lastName");
                    existingResult.setRelevanceScore(existingResult.getRelevanceScore() + 0.5);
                } else {
                    SearchResult result = createVeterinarianSearchResult(vet, sanitizedQuery);
                    result.getMatchedFields().add("lastName");
                    results.add(result);
                }
            }
            
            // Search by license number
            List<Veterinarian> vetsByLicense = veterinarianRepository.findByLicenseNumberContainingIgnoreCase(sanitizedQuery);
            for (Veterinarian vet : vetsByLicense) {
                SearchResult existingResult = findResult(results, "Veterinarian", vet.getId());
                if (existingResult != null) {
                    existingResult.getMatchedFields().add("licenseNumber");
                    existingResult.setRelevanceScore(existingResult.getRelevanceScore() + 0.8);
                } else {
                    SearchResult result = createVeterinarianSearchResult(vet, sanitizedQuery);
                    result.getMatchedFields().add("licenseNumber");
                    results.add(result);
                }
            }
            
            // Search by specialties
            List<Veterinarian> vetsBySpecialties = veterinarianRepository.findBySpecialtiesContainingIgnoreCase(sanitizedQuery);
            for (Veterinarian vet : vetsBySpecialties) {
                SearchResult existingResult = findResult(results, "Veterinarian", vet.getId());
                if (existingResult != null) {
                    existingResult.getMatchedFields().add("specialties");
                    existingResult.setRelevanceScore(existingResult.getRelevanceScore() + 0.6);
                } else {
                    SearchResult result = createVeterinarianSearchResult(vet, sanitizedQuery);
                    result.getMatchedFields().add("specialties");
                    results.add(result);
                }
            }
            
            logger.debug("Found {} veterinarian results for query: '{}'", results.size(), sanitizedQuery);
            
        } catch (Exception e) {
            logger.error("Error searching veterinarians for query: '{}'", sanitizedQuery, e);
        }
        
        return results;
    }
    
    @Override
    public List<SearchResult> searchOwners(String query) {
        logger.debug("Searching owners for query: '{}'", query);
        
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String sanitizedQuery = sanitizeQuery(query.trim());
        List<SearchResult> results = new ArrayList<>();
        
        try {
            // Search by first name
            List<Owner> ownersByFirstName = ownerRepository.findByFirstNameContainingIgnoreCase(sanitizedQuery);
            for (Owner owner : ownersByFirstName) {
                SearchResult result = createOwnerSearchResult(owner, sanitizedQuery);
                result.getMatchedFields().add("firstName");
                results.add(result);
            }
            
            // Search by last name
            List<Owner> ownersByLastName = ownerRepository.findByLastNameContainingIgnoreCase(sanitizedQuery);
            for (Owner owner : ownersByLastName) {
                SearchResult existingResult = findResult(results, "Owner", owner.getId());
                if (existingResult != null) {
                    existingResult.getMatchedFields().add("lastName");
                    existingResult.setRelevanceScore(existingResult.getRelevanceScore() + 0.5);
                } else {
                    SearchResult result = createOwnerSearchResult(owner, sanitizedQuery);
                    result.getMatchedFields().add("lastName");
                    results.add(result);
                }
            }
            
            // Search by email
            List<Owner> ownersByEmail = ownerRepository.findByEmailContainingIgnoreCase(sanitizedQuery);
            for (Owner owner : ownersByEmail) {
                SearchResult existingResult = findResult(results, "Owner", owner.getId());
                if (existingResult != null) {
                    existingResult.getMatchedFields().add("email");
                    existingResult.setRelevanceScore(existingResult.getRelevanceScore() + 0.7);
                } else {
                    SearchResult result = createOwnerSearchResult(owner, sanitizedQuery);
                    result.getMatchedFields().add("email");
                    results.add(result);
                }
            }
            
            // Search by address
            List<Owner> ownersByAddress = ownerRepository.findByAddressContainingIgnoreCase(sanitizedQuery);
            for (Owner owner : ownersByAddress) {
                SearchResult existingResult = findResult(results, "Owner", owner.getId());
                if (existingResult != null) {
                    existingResult.getMatchedFields().add("address");
                    existingResult.setRelevanceScore(existingResult.getRelevanceScore() + 0.3);
                } else {
                    SearchResult result = createOwnerSearchResult(owner, sanitizedQuery);
                    result.getMatchedFields().add("address");
                    results.add(result);
                }
            }
            
            // Search by phone
            List<Owner> ownersByPhone = ownerRepository.findByTelephoneContaining(sanitizedQuery);
            for (Owner owner : ownersByPhone) {
                SearchResult existingResult = findResult(results, "Owner", owner.getId());
                if (existingResult != null) {
                    existingResult.getMatchedFields().add("telephone");
                    existingResult.setRelevanceScore(existingResult.getRelevanceScore() + 0.6);
                } else {
                    SearchResult result = createOwnerSearchResult(owner, sanitizedQuery);
                    result.getMatchedFields().add("telephone");
                    results.add(result);
                }
            }
            
            logger.debug("Found {} owner results for query: '{}'", results.size(), sanitizedQuery);
            
        } catch (Exception e) {
            logger.error("Error searching owners for query: '{}'", sanitizedQuery, e);
        }
        
        return results;
    }
    
    // Helper methods continue in next part...
    
    @Override
    public List<String> getSearchSuggestions(String partialQuery, int maxSuggestions) {
        logger.debug("Getting search suggestions for partial query: '{}'", partialQuery);
        
        if (partialQuery == null || partialQuery.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String sanitizedQuery = sanitizeQuery(partialQuery.trim().toLowerCase());
        Set<String> suggestions = new HashSet<>();
        
        try {
            // Get suggestions from pet names
            List<Pet> pets = petRepository.findByNameContainingIgnoreCase(sanitizedQuery);
            pets.stream().limit(maxSuggestions / 4).forEach(pet -> suggestions.add(pet.getName()));
            
            // Get suggestions from species
            List<Pet> petsBySpecies = petRepository.findBySpeciesIgnoreCase(sanitizedQuery);
            petsBySpecies.stream().limit(maxSuggestions / 4).forEach(pet -> suggestions.add(pet.getSpecies()));
            
            // Get suggestions from veterinarian names
            List<Veterinarian> vets = veterinarianRepository.findByFirstNameContainingIgnoreCase(sanitizedQuery);
            vets.stream().limit(maxSuggestions / 4).forEach(vet -> suggestions.add(vet.getFullName()));
            
            // Get suggestions from owner names
            List<Owner> owners = ownerRepository.findByFirstNameContainingIgnoreCase(sanitizedQuery);
            owners.stream().limit(maxSuggestions / 4).forEach(owner -> suggestions.add(owner.getFullName()));
            
        } catch (Exception e) {
            logger.error("Error getting search suggestions for query: '{}'", sanitizedQuery, e);
        }
        
        return suggestions.stream()
                .limit(maxSuggestions)
                .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Integer> getSearchResultCounts(String query) {
        logger.debug("Getting search result counts for query: '{}'", query);
        
        Map<String, Integer> counts = new HashMap<>();
        
        if (query == null || query.trim().isEmpty()) {
            counts.put("Pet", 0);
            counts.put("Visit", 0);
            counts.put("Veterinarian", 0);
            counts.put("Owner", 0);
            return counts;
        }
        
        String sanitizedQuery = sanitizeQuery(query.trim());
        
        try {
            counts.put("Pet", searchPets(sanitizedQuery).size());
            counts.put("Visit", searchVisits(sanitizedQuery).size());
            counts.put("Veterinarian", searchVeterinarians(sanitizedQuery).size());
            counts.put("Owner", searchOwners(sanitizedQuery).size());
        } catch (Exception e) {
            logger.error("Error getting search result counts for query: '{}'", sanitizedQuery, e);
            counts.put("Pet", 0);
            counts.put("Visit", 0);
            counts.put("Veterinarian", 0);
            counts.put("Owner", 0);
        }
        
        return counts;
    }
    
    @Override
    public Page<SearchResult> searchByEntityType(String query, String entityType, int page, int size) {
        logger.debug("Searching by entity type: '{}' for query: '{}'", entityType, query);
        
        if (query == null || query.trim().isEmpty() || entityType == null) {
            return new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), 0);
        }
        
        String sanitizedQuery = sanitizeQuery(query.trim());
        List<SearchResult> results = new ArrayList<>();
        
        try {
            switch (entityType.toLowerCase()) {
                case "pet":
                    results = searchPets(sanitizedQuery);
                    break;
                case "visit":
                    results = searchVisits(sanitizedQuery);
                    break;
                case "veterinarian":
                    results = searchVeterinarians(sanitizedQuery);
                    break;
                case "owner":
                    results = searchOwners(sanitizedQuery);
                    break;
                default:
                    logger.warn("Unknown entity type: '{}'", entityType);
                    return new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), 0);
            }
            
            // Apply pagination
            int totalResults = results.size();
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalResults);
            
            List<SearchResult> paginatedResults = startIndex < totalResults ? 
                results.subList(startIndex, endIndex) : new ArrayList<>();
            
            return new PageImpl<>(paginatedResults, PageRequest.of(page, size), totalResults);
            
        } catch (Exception e) {
            logger.error("Error searching by entity type: '{}' for query: '{}'", entityType, sanitizedQuery, e);
            return new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), 0);
        }
    }
    
    @Override
    public SearchResultSummary advancedSearch(String query, Map<String, Object> filters, int page, int size) {
        logger.debug("Performing advanced search for query: '{}' with filters: {}", query, filters);
        
        // For now, implement basic advanced search - can be extended with more complex filtering
        SearchResultSummary basicResults = globalSearch(query, page, size);
        
        if (filters == null || filters.isEmpty()) {
            return basicResults;
        }
        
        // Apply filters to results
        List<SearchResult> filteredResults = basicResults.getResults().stream()
                .filter(result -> matchesFilters(result, filters))
                .collect(Collectors.toList());
        
        // Update counts
        Map<String, Integer> filteredCounts = new HashMap<>();
        filteredResults.forEach(result -> 
            filteredCounts.merge(result.getEntityType(), 1, Integer::sum));
        
        SearchResultSummary filteredSummary = new SearchResultSummary(query, filteredResults, filteredCounts, page, size, 
                (int) Math.ceil((double) filteredResults.size() / size));
        filteredSummary.setExecutionTimeMs(basicResults.getExecutionTimeMs());
        
        return filteredSummary;
    }
    
    @Override
    public List<String> getRecentSearches(Long userId, int maxResults) {
        if (userId == null) {
            return new ArrayList<>();
        }
        
        List<String> userSearches = userSearchHistory.get(userId);
        if (userSearches == null || userSearches.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Return most recent searches (list is maintained in reverse chronological order)
        return userSearches.stream()
                .limit(maxResults)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void saveSearchQuery(Long userId, String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        
        String sanitizedQuery = sanitizeQuery(query.trim());
        
        // Update search term frequency
        searchTermFrequency.merge(sanitizedQuery, 1, Integer::sum);
        
        // Save to user search history if userId is provided
        if (userId != null) {
            userSearchHistory.computeIfAbsent(userId, k -> new ArrayList<>());
            List<String> userSearches = userSearchHistory.get(userId);
            
            // Remove if already exists to avoid duplicates
            userSearches.remove(sanitizedQuery);
            
            // Add to beginning of list (most recent first)
            userSearches.add(0, sanitizedQuery);
            
            // Keep only last 50 searches
            if (userSearches.size() > 50) {
                userSearches.subList(50, userSearches.size()).clear();
            }
        }
        
        logger.debug("Saved search query: '{}' for user: {}", sanitizedQuery, userId);
    }
    
    @Override
    public List<String> getPopularSearchTerms(int maxResults) {
        return searchTermFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void clearSearchHistory(Long userId) {
        if (userId != null) {
            userSearchHistory.remove(userId);
            logger.debug("Cleared search history for user: {}", userId);
        }
    }
    
    @Override
    public Map<String, Object> getSearchAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        
        analytics.put("totalSearches", searchAnalytics.getOrDefault("totalSearches", 0L));
        analytics.put("uniqueSearchTerms", searchTermFrequency.size());
        analytics.put("activeUsers", userSearchHistory.size());
        analytics.put("popularTerms", getPopularSearchTerms(10));
        analytics.put("averageResultsPerSearch", searchAnalytics.getOrDefault("averageResults", 0L));
        
        return analytics;
    }
    
    // Helper methods
    
    private SearchResult createPetSearchResult(Pet pet, String query) {
        String title = pet.getName() + " (" + pet.getSpecies() + ")";
        String description = String.format("%s, %s - Owner: %s", 
                pet.getBreed() != null ? pet.getBreed() : "Mixed breed",
                pet.getAge() + " years old",
                pet.getOwner().getFullName());
        
        SearchResult result = new SearchResult("Pet", pet.getId(), title, description);
        result.setMatchedFields(new ArrayList<>());
        result.setLastModified(pet.getUpdatedAt().atStartOfDay());
        result.setUrl("/pets/" + pet.getId());
        
        // Apply highlighting
        result.setHighlightedTitle(highlightText(title, query));
        result.setHighlightedDescription(highlightText(description, query));
        
        // Calculate relevance score based on match quality
        result.setRelevanceScore(calculateRelevanceScore(pet, query));
        
        return result;
    }
    
    private SearchResult createVisitSearchResult(Visit visit, String query) {
        String title = "Visit - " + visit.getPet().getName();
        if (visit.getVeterinarian() != null) {
            title += " with " + visit.getVeterinarian().getFullName();
        }
        
        String description = String.format("Date: %s", visit.getVisitDate().toLocalDate());
        if (visit.getDiagnosis() != null && !visit.getDiagnosis().trim().isEmpty()) {
            description += " - " + visit.getDiagnosis();
        }
        if (visit.getVisitType() != null) {
            description += " (" + visit.getVisitType().getDisplayName() + ")";
        }
        
        SearchResult result = new SearchResult("Visit", visit.getId(), title, description);
        result.setMatchedFields(new ArrayList<>());
        result.setLastModified(visit.getVisitDate());
        result.setUrl("/visits/" + visit.getId());
        
        // Apply highlighting
        result.setHighlightedTitle(highlightText(title, query));
        result.setHighlightedDescription(highlightText(description, query));
        
        // Calculate relevance score
        result.setRelevanceScore(calculateRelevanceScore(visit, query));
        
        return result;
    }
    
    private SearchResult createVeterinarianSearchResult(Veterinarian vet, String query) {
        String title = vet.getFullName();
        String description = "License: " + vet.getLicenseNumber();
        if (vet.isSpecialist()) {
            description += " - Specialties: " + String.join(", ", vet.getSpecialtyList());
        }
        description += " - " + vet.getVisitCount() + " visits";
        
        SearchResult result = new SearchResult("Veterinarian", vet.getId(), title, description);
        result.setMatchedFields(new ArrayList<>());
        result.setLastModified(vet.getUpdatedAt().atStartOfDay());
        result.setUrl("/veterinarians/" + vet.getId());
        
        // Apply highlighting
        result.setHighlightedTitle(highlightText(title, query));
        result.setHighlightedDescription(highlightText(description, query));
        
        // Calculate relevance score
        result.setRelevanceScore(calculateRelevanceScore(vet, query));
        
        return result;
    }
    
    private SearchResult createOwnerSearchResult(Owner owner, String query) {
        String title = owner.getFullName();
        String description = "";
        if (owner.getEmail() != null) {
            description += owner.getEmail();
        }
        if (owner.getTelephone() != null) {
            description += (description.isEmpty() ? "" : " - ") + owner.getTelephone();
        }
        description += " - " + owner.getPetCount() + " pets";
        
        SearchResult result = new SearchResult("Owner", owner.getId(), title, description);
        result.setMatchedFields(new ArrayList<>());
        result.setLastModified(owner.getUpdatedAt().atStartOfDay());
        result.setUrl("/owners/" + owner.getId());
        
        // Apply highlighting
        result.setHighlightedTitle(highlightText(title, query));
        result.setHighlightedDescription(highlightText(description, query));
        
        // Calculate relevance score
        result.setRelevanceScore(calculateRelevanceScore(owner, query));
        
        return result;
    }
    
    private String highlightText(String text, String query) {
        if (text == null || query == null || query.trim().isEmpty()) {
            return text;
        }
        
        String sanitizedQuery = sanitizeQuery(query.trim());
        Pattern pattern = Pattern.compile(Pattern.quote(sanitizedQuery), Pattern.CASE_INSENSITIVE);
        return pattern.matcher(text).replaceAll(HIGHLIGHT_START + "$0" + HIGHLIGHT_END);
    }
    
    private double calculateRelevanceScore(Object entity, String query) {
        // Base score
        double score = 1.0;
        
        if (entity instanceof Pet) {
            Pet pet = (Pet) entity;
            if (pet.getName().toLowerCase().contains(query.toLowerCase())) {
                score += 2.0; // Name match is highly relevant
            }
            if (pet.getSpecies().toLowerCase().equals(query.toLowerCase())) {
                score += 1.5; // Exact species match
            }
        } else if (entity instanceof Visit) {
            Visit visit = (Visit) entity;
            if (visit.getDiagnosis() != null && visit.getDiagnosis().toLowerCase().contains(query.toLowerCase())) {
                score += 1.8; // Diagnosis match is important
            }
        } else if (entity instanceof Veterinarian) {
            Veterinarian vet = (Veterinarian) entity;
            if (vet.getLastName().toLowerCase().contains(query.toLowerCase())) {
                score += 2.0; // Name match is highly relevant
            }
            if (vet.getLicenseNumber().toLowerCase().contains(query.toLowerCase())) {
                score += 2.5; // License number match is very specific
            }
        } else if (entity instanceof Owner) {
            Owner owner = (Owner) entity;
            if (owner.getLastName().toLowerCase().contains(query.toLowerCase())) {
                score += 2.0; // Name match is highly relevant
            }
            if (owner.getEmail() != null && owner.getEmail().toLowerCase().contains(query.toLowerCase())) {
                score += 1.8; // Email match is specific
            }
        }
        
        return score;
    }
    
    private void sortResults(List<SearchResult> results, String sortBy, String sortDirection) {
        if (results == null || results.isEmpty()) {
            return;
        }
        
        Comparator<SearchResult> comparator;
        
        switch (sortBy.toLowerCase()) {
            case "date":
                comparator = Comparator.comparing(SearchResult::getLastModified, 
                        Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "name":
                comparator = Comparator.comparing(SearchResult::getTitle, 
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
            case "type":
                comparator = Comparator.comparing(SearchResult::getEntityType);
                break;
            case "relevance":
            default:
                comparator = Comparator.comparing(SearchResult::getRelevanceScore, 
                        Comparator.nullsLast(Comparator.reverseOrder()));
                break;
        }
        
        if ("asc".equalsIgnoreCase(sortDirection)) {
            results.sort(comparator);
        } else {
            results.sort(comparator.reversed());
        }
    }
    
    private boolean containsResult(List<SearchResult> results, String entityType, Long entityId) {
        return results.stream().anyMatch(r -> 
                entityType.equals(r.getEntityType()) && entityId.equals(r.getEntityId()));
    }
    
    private SearchResult findResult(List<SearchResult> results, String entityType, Long entityId) {
        return results.stream()
                .filter(r -> entityType.equals(r.getEntityType()) && entityId.equals(r.getEntityId()))
                .findFirst()
                .orElse(null);
    }
    
    private boolean matchesFilters(SearchResult result, Map<String, Object> filters) {
        // Implement filter matching logic based on your requirements
        // This is a basic implementation that can be extended
        
        if (filters.containsKey("entityType")) {
            String filterType = (String) filters.get("entityType");
            if (!result.getEntityType().equalsIgnoreCase(filterType)) {
                return false;
            }
        }
        
        if (filters.containsKey("minRelevance")) {
            Double minRelevance = (Double) filters.get("minRelevance");
            if (result.getRelevanceScore() < minRelevance) {
                return false;
            }
        }
        
        return true;
    }
    
    private String sanitizeQuery(String query) {
        if (query == null) {
            return "";
        }
        
        // Remove potentially harmful characters and normalize
        return query.replaceAll("[<>\"'&]", "").trim();
    }
    
    private void trackSearchQuery(String query) {
        searchAnalytics.merge("totalSearches", 1L, Long::sum);
        searchTermFrequency.merge(query, 1, Integer::sum);
    }
    
    private SearchResultSummary createEmptySearchResult(String query, int page, int size, String sortBy, String sortDirection) {
        Map<String, Integer> emptyResults = new HashMap<>();
        emptyResults.put("Pet", 0);
        emptyResults.put("Visit", 0);
        emptyResults.put("Veterinarian", 0);
        emptyResults.put("Owner", 0);
        
        SearchResultSummary summary = new SearchResultSummary(query, new ArrayList<>(), emptyResults, page, size, 0);
        summary.setSortBy(sortBy);
        summary.setSortDirection(sortDirection);
        summary.setExecutionTimeMs(0);
        
        return summary;
    }
}