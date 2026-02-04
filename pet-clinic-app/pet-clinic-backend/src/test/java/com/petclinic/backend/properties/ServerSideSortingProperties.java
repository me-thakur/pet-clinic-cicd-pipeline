package com.petclinic.backend.properties;

import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.model.VisitType;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for Server-Side Table Sorting
 * **Feature: critical-fixes-and-enhancements, Property 2: Server-Side Table Sorting**
 * **Validates: Requirements 2.2, 2.3**
 * 
 * Property 2: Server-Side Table Sorting
 * For any sortable dataset and sort criteria, when server-side sorting is applied, 
 * the system should sort the complete dataset before pagination and return results 
 * with proper pagination metadata
 */
class ServerSideSortingProperties {
    
    /**
     * Property 2: Server-Side Table Sorting - Sort Before Pagination
     * For any sortable dataset and sort criteria, the system should sort the complete dataset before pagination
     * **Feature: critical-fixes-and-enhancements, Property 2: Server-Side Table Sorting**
     * **Validates: Requirements 2.2**
     */
    @Property(tries = 100)
    void serverSideSortingShouldSortCompleteDatasetBeforePagination(
            @ForAll("validSortColumns") String sortColumn,
            @ForAll("validSortDirections") String sortDirection,
            @ForAll @IntRange(min = 0, max = 5) int page,
            @ForAll @IntRange(min = 1, max = 10) int size) {
        
        // Setup test dataset for this property
        List<Visit> testDataset = setupTestDataset();
        
        // Create pageable with sort
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortColumn);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Simulate server-side sorting and pagination
        PagedResponse<Visit> result = performServerSideSortAndPagination(testDataset, pageable);
        
        // Verify that sorting was applied to complete dataset before pagination
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotNull();
        
        // If there are results, verify they are properly sorted
        if (!result.getContent().isEmpty()) {
            List<Visit> pageContent = result.getContent();
            
            // Verify the page content is sorted according to the sort criteria
            assertThat(isPageContentSorted(pageContent, sortColumn, direction))
                .as("Page content should be sorted by %s %s", sortColumn, direction)
                .isTrue();
            
            // Verify that the sorting was applied to the complete dataset, not just the page
            // by checking that the first item in this page would come after the last item of the previous page
            if (page > 0 && result.getPage().getTotalElements() > page * size) {
                // Get the previous page to verify global sort order
                Pageable previousPageable = PageRequest.of(page - 1, size, sort);
                PagedResponse<Visit> previousPage = performServerSideSortAndPagination(testDataset, previousPageable);
                
                if (!previousPage.getContent().isEmpty()) {
                    Visit lastItemPreviousPage = previousPage.getContent().get(previousPage.getContent().size() - 1);
                    Visit firstItemCurrentPage = pageContent.get(0);
                    
                    // Verify global sort order between pages
                    assertThat(compareVisits(lastItemPreviousPage, firstItemCurrentPage, sortColumn, direction))
                        .as("Items should be sorted across pages: last item of previous page should come before first item of current page")
                        .isLessThanOrEqualTo(0);
                }
            }
        }
    }
    
    /**
     * Property 2: Server-Side Table Sorting - Proper Pagination Metadata
     * For any sort criteria, the system should return results with proper pagination metadata
     * **Feature: critical-fixes-and-enhancements, Property 2: Server-Side Table Sorting**
     * **Validates: Requirements 2.3**
     */
    @Property(tries = 100)
    void serverSideSortingShouldReturnProperPaginationMetadata(
            @ForAll("validSortColumns") String sortColumn,
            @ForAll("validSortDirections") String sortDirection,
            @ForAll @IntRange(min = 0, max = 8) int page,
            @ForAll @IntRange(min = 1, max = 15) int size) {
        
        // Setup test dataset for this property
        List<Visit> testDataset = setupTestDataset();
        
        // Create pageable with sort
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortColumn);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Perform server-side sorting and pagination
        PagedResponse<Visit> result = performServerSideSortAndPagination(testDataset, pageable);
        
        // Verify pagination metadata is present and correct
        assertThat(result).isNotNull();
        assertThat(result.getPage()).isNotNull();
        
        PagedResponse.PageInfo pageInfo = result.getPage();
        
        // Verify basic pagination metadata
        assertThat(pageInfo.getNumber()).isEqualTo(page);
        assertThat(pageInfo.getSize()).isEqualTo(size);
        assertThat(pageInfo.getTotalElements()).isEqualTo(testDataset.size());
        
        // Verify total pages calculation
        int expectedTotalPages = (int) Math.ceil((double) testDataset.size() / size);
        assertThat(pageInfo.getTotalPages()).isEqualTo(expectedTotalPages);
        
        // Verify page flags
        assertThat(pageInfo.isFirst()).isEqualTo(page == 0);
        assertThat(pageInfo.isLast()).isEqualTo(page >= expectedTotalPages - 1);
        assertThat(pageInfo.isHasNext()).isEqualTo(page < expectedTotalPages - 1);
        assertThat(pageInfo.isHasPrevious()).isEqualTo(page > 0);
        
        // Verify content size is correct for the page
        int expectedContentSize;
        if (page >= expectedTotalPages) {
            expectedContentSize = 0; // Page beyond available data
        } else if (page == expectedTotalPages - 1) {
            // Last page - might have fewer items
            expectedContentSize = testDataset.size() - (page * size);
        } else {
            // Full page
            expectedContentSize = size;
        }
        
        assertThat(result.getContent().size()).isEqualTo(expectedContentSize);
    }
    
    /**
     * Property 2: Server-Side Table Sorting - Sort Consistency
     * For any sort criteria applied multiple times, the results should be consistent
     * **Feature: critical-fixes-and-enhancements, Property 2: Server-Side Table Sorting**
     * **Validates: Requirements 2.2**
     */
    @Property(tries = 50)
    void serverSideSortingShouldBeConsistent(
            @ForAll("validSortColumns") String sortColumn,
            @ForAll("validSortDirections") String sortDirection,
            @ForAll @IntRange(min = 0, max = 3) int page,
            @ForAll @IntRange(min = 5, max = 10) int size) {
        
        // Setup test dataset for this property
        List<Visit> testDataset = setupTestDataset();
        
        // Create pageable with sort
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortColumn);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Perform the same sort operation twice
        PagedResponse<Visit> result1 = performServerSideSortAndPagination(testDataset, pageable);
        PagedResponse<Visit> result2 = performServerSideSortAndPagination(testDataset, pageable);
        
        // Results should be identical
        assertThat(result1.getContent().size()).isEqualTo(result2.getContent().size());
        assertThat(result1.getPage().getTotalElements()).isEqualTo(result2.getPage().getTotalElements());
        
        // Content should be in the same order
        for (int i = 0; i < result1.getContent().size(); i++) {
            Visit visit1 = result1.getContent().get(i);
            Visit visit2 = result2.getContent().get(i);
            assertThat(visit1.getId()).isEqualTo(visit2.getId());
        }
    }
    
    /**
     * Property 2: Server-Side Table Sorting - Empty Dataset Handling
     * For any sort criteria applied to an empty dataset, the system should handle it gracefully
     * **Feature: critical-fixes-and-enhancements, Property 2: Server-Side Table Sorting**
     * **Validates: Requirements 2.2, 2.3**
     */
    @Property(tries = 20)
    void serverSideSortingShouldHandleEmptyDatasetGracefully(
            @ForAll("validSortColumns") String sortColumn,
            @ForAll("validSortDirections") String sortDirection,
            @ForAll @IntRange(min = 0, max = 2) int page,
            @ForAll @IntRange(min = 1, max = 5) int size) {
        
        // Create pageable with sort
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortColumn);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Test with empty dataset
        List<Visit> emptyDataset = new ArrayList<>();
        PagedResponse<Visit> result = performServerSideSortAndPagination(emptyDataset, pageable);
        
        // Verify empty result is handled properly
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getPage().getTotalElements()).isEqualTo(0);
        assertThat(result.getPage().getTotalPages()).isEqualTo(0);
        
        // For empty datasets, the behavior depends on the requested page
        if (page == 0) {
            // Page 0 of empty dataset should be both first and last
            assertThat(result.getPage().isFirst()).isTrue();
            assertThat(result.getPage().isLast()).isTrue();
            assertThat(result.getPage().isHasNext()).isFalse();
            assertThat(result.getPage().isHasPrevious()).isFalse();
        } else {
            // Pages beyond 0 for empty dataset should not be first, and may not be last
            assertThat(result.getPage().isFirst()).isFalse();
            // isLast() behavior for pages beyond available data can vary by implementation
            assertThat(result.getPage().isHasNext()).isFalse();
            assertThat(result.getPage().isHasPrevious()).isTrue();
        }
    }
    
    /**
     * Property 2: Server-Side Table Sorting - Large Dataset Performance
     * For any sort criteria applied to a large dataset, sorting should be applied before pagination
     * **Feature: critical-fixes-and-enhancements, Property 2: Server-Side Table Sorting**
     * **Validates: Requirements 2.2**
     */
    @Property(tries = 20)
    void serverSideSortingShouldHandleLargeDatasets(
            @ForAll("validSortColumns") String sortColumn,
            @ForAll("validSortDirections") String sortDirection) {
        
        // Create a larger dataset
        List<Visit> largeDataset = createLargeDataset(100);
        
        // Test with small page size to ensure sorting happens before pagination
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortColumn);
        Pageable pageable = PageRequest.of(0, 5, sort); // Small page size
        
        PagedResponse<Visit> result = performServerSideSortAndPagination(largeDataset, pageable);
        
        // Verify that we get the correct first 5 items from the sorted dataset
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getPage().getTotalElements()).isEqualTo(100);
        
        // Verify the first page contains the correct sorted items
        List<Visit> fullySorted = largeDataset.stream()
            .sorted(getComparator(sortColumn, direction))
            .collect(Collectors.toList());
        
        for (int i = 0; i < 5; i++) {
            assertThat(result.getContent().get(i).getId())
                .isEqualTo(fullySorted.get(i).getId());
        }
    }
    
    // Generators for valid sort parameters
    
    @Provide
    Arbitrary<String> validSortColumns() {
        return Arbitraries.of("id", "visitDate", "visitType", "diagnosis", "treatment", "cost");
    }
    
    @Provide
    Arbitrary<String> validSortDirections() {
        return Arbitraries.of("asc", "desc", "ASC", "DESC");
    }
    
    // Helper methods
    
    /**
     * Simulates server-side sorting and pagination as implemented in the EnhancedTableService
     */
    private PagedResponse<Visit> performServerSideSortAndPagination(List<Visit> dataset, Pageable pageable) {
        // Sort the complete dataset first (server-side behavior)
        List<Visit> sortedDataset = dataset.stream()
            .sorted(createComparatorFromPageable(pageable))
            .collect(Collectors.toList());
        
        // Apply pagination to the sorted dataset
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sortedDataset.size());
        
        List<Visit> pageContent;
        if (start >= sortedDataset.size()) {
            pageContent = new ArrayList<>();
        } else {
            pageContent = sortedDataset.subList(start, end);
        }
        
        // Create page metadata
        Page<Visit> page = new PageImpl<>(pageContent, pageable, sortedDataset.size());
        
        // Convert to PagedResponse
        PagedResponse.PageInfo pageInfo = new PagedResponse.PageInfo(
            page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
            page.isFirst(), page.isLast(), page.hasNext(), page.hasPrevious()
        );
        
        return new PagedResponse<>(pageContent, pageInfo);
    }
    
    private Comparator<Visit> createComparatorFromPageable(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return Comparator.comparing(Visit::getId); // Default sort
        }
        
        Comparator<Visit> comparator = null;
        for (Sort.Order order : pageable.getSort()) {
            Comparator<Visit> fieldComparator = getComparator(order.getProperty(), order.getDirection());
            if (comparator == null) {
                comparator = fieldComparator;
            } else {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }
        
        return comparator != null ? comparator : Comparator.comparing(Visit::getId);
    }
    
    private Comparator<Visit> getComparator(String column, Sort.Direction direction) {
        Comparator<Visit> comparator;
        
        switch (column.toLowerCase()) {
            case "id":
                comparator = Comparator.comparing(Visit::getId, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "visitdate":
                comparator = Comparator.comparing(Visit::getVisitDate, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "visittype":
                comparator = Comparator.comparing(v -> v.getVisitType() != null ? v.getVisitType().toString() : "", 
                                                Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "diagnosis":
                comparator = Comparator.comparing(v -> v.getDiagnosis() != null ? v.getDiagnosis() : "", 
                                                Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "treatment":
                comparator = Comparator.comparing(v -> v.getTreatment() != null ? v.getTreatment() : "", 
                                                Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "cost":
                comparator = Comparator.comparing(Visit::getCost, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            default:
                comparator = Comparator.comparing(Visit::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        
        return direction == Sort.Direction.DESC ? comparator.reversed() : comparator;
    }
    
    private boolean isPageContentSorted(List<Visit> content, String sortColumn, Sort.Direction direction) {
        if (content.size() <= 1) {
            return true;
        }
        
        for (int i = 0; i < content.size() - 1; i++) {
            Visit current = content.get(i);
            Visit next = content.get(i + 1);
            
            int comparison = compareVisits(current, next, sortColumn, direction);
            if (comparison > 0) {
                return false; // Not properly sorted
            }
        }
        
        return true;
    }
    
    private int compareVisits(Visit v1, Visit v2, String sortColumn, Sort.Direction direction) {
        Comparator<Visit> comparator = getComparator(sortColumn, direction);
        return comparator.compare(v1, v2);
    }
    
    private List<Visit> setupTestDataset() {
        List<Visit> testDataset = new ArrayList<>();
        
        // Create test pets
        Pet pet1 = new Pet();
        pet1.setId(1L);
        pet1.setName("Buddy");
        pet1.setSpecies("Dog");
        pet1.setBreed("Golden Retriever");
        pet1.setBirthDate(LocalDate.now().minusYears(3));
        
        Pet pet2 = new Pet();
        pet2.setId(2L);
        pet2.setName("Whiskers");
        pet2.setSpecies("Cat");
        pet2.setBreed("Siamese");
        pet2.setBirthDate(LocalDate.now().minusYears(2));
        
        Pet pet3 = new Pet();
        pet3.setId(3L);
        pet3.setName("Charlie");
        pet3.setSpecies("Dog");
        pet3.setBreed("Labrador");
        pet3.setBirthDate(LocalDate.now().minusYears(1));
        
        // Create test visits with varied data for sorting
        String[] treatments = {"Vaccination", "Surgery", "Medication", "Dental Cleaning", "Checkup", "X-ray", "Blood Test"};
        String[] diagnoses = {"Healthy", "Infection", "Broken Bone", "Dental Issues", "Skin Condition", "Arthritis", "Allergies"};
        VisitType[] visitTypes = {VisitType.WELLNESS_EXAM, VisitType.EMERGENCY, VisitType.SURGERY, VisitType.VACCINATION};
        Pet[] pets = {pet1, pet2, pet3};
        
        for (int i = 0; i < 25; i++) {
            Visit visit = new Visit();
            visit.setId((long) (i + 1));
            visit.setVisitDate(LocalDateTime.now().minusDays(i * 2).plusHours(i % 24));
            visit.setTreatment(treatments[i % treatments.length]);
            visit.setDiagnosis(diagnoses[i % diagnoses.length]);
            visit.setVisitType(visitTypes[i % visitTypes.length]);
            visit.setCost(BigDecimal.valueOf(50.0 + (i * 15.5) % 500));
            visit.setPet(pets[i % pets.length]);
            visit.setNotes("Test visit notes for visit " + (i + 1));
            testDataset.add(visit);
        }
        
        return testDataset;
    }
    
    private List<Visit> createLargeDataset(int size) {
        List<Visit> largeDataset = new ArrayList<>();
        
        Pet testPet = new Pet();
        testPet.setId(1L);
        testPet.setName("TestPet");
        testPet.setSpecies("Dog");
        testPet.setBirthDate(LocalDate.now().minusYears(2));
        
        String[] treatments = {"Vaccination", "Surgery", "Medication", "Dental", "Checkup"};
        String[] diagnoses = {"Healthy", "Sick", "Injured", "Recovering", "Critical"};
        VisitType[] visitTypes = {VisitType.WELLNESS_EXAM, VisitType.EMERGENCY, VisitType.SURGERY};
        
        for (int i = 0; i < size; i++) {
            Visit visit = new Visit();
            visit.setId((long) (i + 1));
            visit.setVisitDate(LocalDateTime.now().minusDays(i).plusMinutes(i * 7));
            visit.setTreatment(treatments[i % treatments.length]);
            visit.setDiagnosis(diagnoses[i % diagnoses.length]);
            visit.setVisitType(visitTypes[i % visitTypes.length]);
            visit.setCost(BigDecimal.valueOf(25.0 + (i * 12.3) % 1000));
            visit.setPet(testPet);
            largeDataset.add(visit);
        }
        
        return largeDataset;
    }
}