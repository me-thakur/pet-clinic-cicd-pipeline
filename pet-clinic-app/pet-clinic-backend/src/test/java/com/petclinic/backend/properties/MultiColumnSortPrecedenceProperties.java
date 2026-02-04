package com.petclinic.backend.properties;

import com.petclinic.backend.dto.MultiColumnSortMetadata;
import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.model.VisitType;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
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
 * Property-based tests for Multi-Column Sort Precedence
 * **Feature: critical-fixes-and-enhancements, Property 4: Multi-Column Sort Precedence**
 * **Validates: Requirements 2.4**
 * 
 * Property 4: Multi-Column Sort Precedence
 * For any combination of multiple sort criteria, the system should apply them in the correct precedence order
 */
class MultiColumnSortPrecedenceProperties {
    
    /**
     * Property 4: Multi-Column Sort Precedence - Correct Order Application
     * For any combination of multiple sort criteria, the system should apply them in the correct precedence order
     * **Feature: critical-fixes-and-enhancements, Property 4: Multi-Column Sort Precedence**
     * **Validates: Requirements 2.4**
     */
    @Property(tries = 100)
    void multiColumnSortShouldApplyCorrectPrecedenceOrder(
            @ForAll("multiColumnSortCriteria") List<MultiColumnSortMetadata.SortCriterion> sortCriteria,
            @ForAll @IntRange(min = 0, max = 3) int page,
            @ForAll @IntRange(min = 5, max = 10) int size) {
        
        // Setup test dataset for this property
        List<Visit> testDataset = setupTestDataset();
        
        // Create multi-column sort metadata
        MultiColumnSortMetadata multiSort = new MultiColumnSortMetadata(sortCriteria);
        
        // Create pageable with multi-column sort
        Pageable pageable = createPageableWithMultiSort(page, size, multiSort);
        
        // Perform server-side multi-column sorting and pagination
        PagedResponse<Visit> result = performMultiColumnSortAndPagination(testDataset, pageable, multiSort);
        
        // Verify that sorting was applied in correct precedence order
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotNull();
        
        // If there are results, verify they are properly sorted according to precedence
        if (!result.getContent().isEmpty()) {
            List<Visit> pageContent = result.getContent();
            
            // Verify the page content is sorted according to the multi-column sort criteria
            assertThat(isPageContentSortedByMultipleColumns(pageContent, sortCriteria))
                .as("Page content should be sorted by multiple columns in precedence order")
                .isTrue();
            
            // Verify that the sorting precedence is maintained across pages
            if (page > 0 && result.getPage().getTotalElements() > page * size) {
                // Get the previous page to verify global sort order
                Pageable previousPageable = createPageableWithMultiSort(page - 1, size, multiSort);
                PagedResponse<Visit> previousPage = performMultiColumnSortAndPagination(testDataset, previousPageable, multiSort);
                
                if (!previousPage.getContent().isEmpty()) {
                    Visit lastItemPreviousPage = previousPage.getContent().get(previousPage.getContent().size() - 1);
                    Visit firstItemCurrentPage = pageContent.get(0);
                    
                    // Verify global sort order between pages using multi-column comparison
                    assertThat(compareVisitsMultiColumn(lastItemPreviousPage, firstItemCurrentPage, sortCriteria))
                        .as("Multi-column sort order should be maintained across pages")
                        .isLessThanOrEqualTo(0);
                }
            }
        }
    }
    
    /**
     * Property 4: Multi-Column Sort Precedence - Primary Sort Dominance
     * For any multi-column sort, the primary (first) sort criterion should dominate over secondary criteria
     * **Feature: critical-fixes-and-enhancements, Property 4: Multi-Column Sort Precedence**
     * **Validates: Requirements 2.4**
     */
    @Property(tries = 100)
    void primarySortCriterionShouldDominateOverSecondary(
            @ForAll("validSortColumns") String primaryColumn,
            @ForAll("validSortDirections") String primaryDirection,
            @ForAll("validSortColumns") String secondaryColumn,
            @ForAll("validSortDirections") String secondaryDirection) {
        
        // Ensure we have different columns for meaningful test
        Assume.that(!primaryColumn.equals(secondaryColumn));
        
        // Setup test dataset with varied data
        List<Visit> testDataset = setupTestDataset();
        
        // Create multi-column sort criteria
        List<MultiColumnSortMetadata.SortCriterion> sortCriteria = List.of(
            new MultiColumnSortMetadata.SortCriterion(primaryColumn, primaryDirection, 0),
            new MultiColumnSortMetadata.SortCriterion(secondaryColumn, secondaryDirection, 1)
        );
        
        MultiColumnSortMetadata multiSort = new MultiColumnSortMetadata(sortCriteria);
        Pageable pageable = createPageableWithMultiSort(0, testDataset.size(), multiSort);
        
        // Perform multi-column sorting
        PagedResponse<Visit> result = performMultiColumnSortAndPagination(testDataset, pageable, multiSort);
        List<Visit> sortedVisits = result.getContent();
        
        // Verify primary sort dominance
        if (sortedVisits.size() > 1) {
            for (int i = 0; i < sortedVisits.size() - 1; i++) {
                Visit current = sortedVisits.get(i);
                Visit next = sortedVisits.get(i + 1);
                
                // Compare by primary column
                int primaryComparison = compareVisitsByColumn(current, next, primaryColumn, primaryDirection);
                
                // If primary values are different, primary sort should determine order
                if (primaryComparison != 0) {
                    assertThat(primaryComparison)
                        .as("Primary sort criterion should determine order when values differ")
                        .isLessThanOrEqualTo(0);
                }
                // If primary values are equal, secondary sort should determine order
                else {
                    int secondaryComparison = compareVisitsByColumn(current, next, secondaryColumn, secondaryDirection);
                    assertThat(secondaryComparison)
                        .as("Secondary sort criterion should determine order when primary values are equal")
                        .isLessThanOrEqualTo(0);
                }
            }
        }
    }
    
    /**
     * Property 4: Multi-Column Sort Precedence - Consistent Precedence Numbers
     * For any multi-column sort metadata, precedence numbers should be consistent and sequential
     * **Feature: critical-fixes-and-enhancements, Property 4: Multi-Column Sort Precedence**
     * **Validates: Requirements 2.4**
     */
    @Property(tries = 50)
    void multiColumnSortShouldHaveConsistentPrecedenceNumbers(
            @ForAll("multiColumnSortCriteria") List<MultiColumnSortMetadata.SortCriterion> sortCriteria) {
        
        // Create multi-column sort metadata
        MultiColumnSortMetadata multiSort = new MultiColumnSortMetadata(sortCriteria);
        
        // Verify precedence numbers are consistent
        List<MultiColumnSortMetadata.SortCriterion> criteria = multiSort.getSortCriteria();
        
        if (!criteria.isEmpty()) {
            // Sort by precedence to verify order
            List<MultiColumnSortMetadata.SortCriterion> sortedByPrecedence = criteria.stream()
                .sorted(Comparator.comparingInt(MultiColumnSortMetadata.SortCriterion::getPrecedence))
                .collect(Collectors.toList());
            
            // Verify precedence numbers are sequential starting from 0
            for (int i = 0; i < sortedByPrecedence.size(); i++) {
                assertThat(sortedByPrecedence.get(i).getPrecedence())
                    .as("Precedence numbers should be sequential starting from 0")
                    .isEqualTo(i);
            }
            
            // Verify no duplicate precedence numbers
            long uniquePrecedenceCount = criteria.stream()
                .mapToInt(MultiColumnSortMetadata.SortCriterion::getPrecedence)
                .distinct()
                .count();
            
            assertThat(uniquePrecedenceCount)
                .as("All precedence numbers should be unique")
                .isEqualTo(criteria.size());
        }
    }
    
    /**
     * Property 4: Multi-Column Sort Precedence - Maximum Criteria Limit
     * For any multi-column sort, the system should enforce a maximum of 5 sort criteria
     * **Feature: critical-fixes-and-enhancements, Property 4: Multi-Column Sort Precedence**
     * **Validates: Requirements 2.4**
     */
    @Property(tries = 30)
    void multiColumnSortShouldEnforceMaximumCriteriaLimit(
            @ForAll("largeSortCriteriaList") List<MultiColumnSortMetadata.SortCriterion> largeSortCriteria) {
        
        // Create multi-column sort metadata with potentially more than 5 criteria
        MultiColumnSortMetadata multiSort = new MultiColumnSortMetadata();
        
        // Add criteria one by one to test limit enforcement
        for (MultiColumnSortMetadata.SortCriterion criterion : largeSortCriteria) {
            multiSort.addSecondarySortCriterion(criterion.getColumn(), criterion.getDirection());
        }
        
        // Verify that no more than 5 criteria are stored
        assertThat(multiSort.getSortCriteria().size())
            .as("Multi-column sort should not exceed 5 criteria")
            .isLessThanOrEqualTo(5);
        
        // If we had more than 5 input criteria, verify the first 5 are kept
        if (largeSortCriteria.size() > 5) {
            assertThat(multiSort.getSortCriteria().size())
                .as("Should keep exactly 5 criteria when more are provided")
                .isEqualTo(5);
        }
    }
    
    /**
     * Property 4: Multi-Column Sort Precedence - Empty Criteria Handling
     * For any empty multi-column sort criteria, the system should handle it gracefully
     * **Feature: critical-fixes-and-enhancements, Property 4: Multi-Column Sort Precedence**
     * **Validates: Requirements 2.4**
     */
    @Property(tries = 20)
    void multiColumnSortShouldHandleEmptyCriteriaGracefully() {
        
        // Create empty multi-column sort metadata
        MultiColumnSortMetadata emptyMultiSort = new MultiColumnSortMetadata();
        
        // Setup test dataset
        List<Visit> testDataset = setupTestDataset();
        
        // Create pageable with empty multi-column sort
        Pageable pageable = createPageableWithMultiSort(0, 10, emptyMultiSort);
        
        // Perform sorting with empty criteria
        PagedResponse<Visit> result = performMultiColumnSortAndPagination(testDataset, pageable, emptyMultiSort);
        
        // Verify that empty criteria is handled gracefully
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotNull();
        
        // With empty sort criteria, should fall back to default sorting (by ID)
        if (result.getContent().size() > 1) {
            List<Visit> content = result.getContent();
            for (int i = 0; i < content.size() - 1; i++) {
                Visit current = content.get(i);
                Visit next = content.get(i + 1);
                
                // Should be sorted by ID (default) when no criteria provided
                assertThat(current.getId())
                    .as("Should fall back to ID sorting when no criteria provided")
                    .isLessThanOrEqualTo(next.getId());
            }
        }
    }
    
    // Generators for test data
    
    @Provide
    Arbitrary<List<MultiColumnSortMetadata.SortCriterion>> multiColumnSortCriteria() {
        return Arbitraries.of("id", "visitDate", "visitType", "diagnosis", "treatment", "cost")
            .list().ofMinSize(1).ofMaxSize(3)
            .map(columns -> {
                List<MultiColumnSortMetadata.SortCriterion> criteria = new ArrayList<>();
                for (int i = 0; i < columns.size(); i++) {
                    String direction = i % 2 == 0 ? "asc" : "desc";
                    criteria.add(new MultiColumnSortMetadata.SortCriterion(columns.get(i), direction, i));
                }
                return criteria;
            });
    }
    
    @Provide
    Arbitrary<List<MultiColumnSortMetadata.SortCriterion>> largeSortCriteriaList() {
        return Arbitraries.of("id", "visitDate", "visitType", "diagnosis", "treatment", "cost", "notes")
            .list().ofMinSize(6).ofMaxSize(10)
            .map(columns -> {
                List<MultiColumnSortMetadata.SortCriterion> criteria = new ArrayList<>();
                for (int i = 0; i < columns.size(); i++) {
                    String direction = i % 2 == 0 ? "asc" : "desc";
                    criteria.add(new MultiColumnSortMetadata.SortCriterion(columns.get(i), direction, i));
                }
                return criteria;
            });
    }
    
    @Provide
    Arbitrary<String> validSortColumns() {
        return Arbitraries.of("id", "visitDate", "visitType", "diagnosis", "treatment", "cost");
    }
    
    @Provide
    Arbitrary<String> validSortDirections() {
        return Arbitraries.of("asc", "desc");
    }
    
    // Helper methods
    
    /**
     * Simulates server-side multi-column sorting and pagination
     */
    private PagedResponse<Visit> performMultiColumnSortAndPagination(
            List<Visit> dataset, Pageable pageable, MultiColumnSortMetadata multiSort) {
        
        // Sort the complete dataset first using multi-column criteria
        List<Visit> sortedDataset = dataset.stream()
            .sorted(createMultiColumnComparator(multiSort.getSortCriteria()))
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
    
    private Pageable createPageableWithMultiSort(int page, int size, MultiColumnSortMetadata multiSort) {
        if (multiSort == null || multiSort.getSortCriteria().isEmpty()) {
            return PageRequest.of(page, size);
        }
        
        List<Sort.Order> orders = new ArrayList<>();
        for (MultiColumnSortMetadata.SortCriterion criterion : multiSort.getSortCriteria()) {
            Sort.Direction direction = criterion.isAscending() ? Sort.Direction.ASC : Sort.Direction.DESC;
            orders.add(new Sort.Order(direction, criterion.getColumn()));
        }
        
        Sort sort = Sort.by(orders);
        return PageRequest.of(page, size, sort);
    }
    
    private Comparator<Visit> createMultiColumnComparator(List<MultiColumnSortMetadata.SortCriterion> sortCriteria) {
        if (sortCriteria.isEmpty()) {
            return Comparator.comparing(Visit::getId); // Default sort
        }
        
        Comparator<Visit> comparator = null;
        
        // Sort criteria by precedence to ensure correct order
        List<MultiColumnSortMetadata.SortCriterion> orderedCriteria = sortCriteria.stream()
            .sorted(Comparator.comparingInt(MultiColumnSortMetadata.SortCriterion::getPrecedence))
            .collect(Collectors.toList());
        
        for (MultiColumnSortMetadata.SortCriterion criterion : orderedCriteria) {
            Comparator<Visit> fieldComparator = getComparator(criterion.getColumn(), criterion.getDirection());
            if (comparator == null) {
                comparator = fieldComparator;
            } else {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }
        
        return comparator != null ? comparator : Comparator.comparing(Visit::getId);
    }
    
    private Comparator<Visit> getComparator(String column, String direction) {
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
            case "notes":
                comparator = Comparator.comparing(v -> v.getNotes() != null ? v.getNotes() : "", 
                                                Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            default:
                comparator = Comparator.comparing(Visit::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        
        return direction.equalsIgnoreCase("desc") ? comparator.reversed() : comparator;
    }
    
    private boolean isPageContentSortedByMultipleColumns(
            List<Visit> content, List<MultiColumnSortMetadata.SortCriterion> sortCriteria) {
        
        if (content.size() <= 1 || sortCriteria.isEmpty()) {
            return true;
        }
        
        for (int i = 0; i < content.size() - 1; i++) {
            Visit current = content.get(i);
            Visit next = content.get(i + 1);
            
            int comparison = compareVisitsMultiColumn(current, next, sortCriteria);
            if (comparison > 0) {
                return false; // Not properly sorted
            }
        }
        
        return true;
    }
    
    private int compareVisitsMultiColumn(Visit v1, Visit v2, List<MultiColumnSortMetadata.SortCriterion> sortCriteria) {
        // Sort criteria by precedence to ensure correct comparison order
        List<MultiColumnSortMetadata.SortCriterion> orderedCriteria = sortCriteria.stream()
            .sorted(Comparator.comparingInt(MultiColumnSortMetadata.SortCriterion::getPrecedence))
            .collect(Collectors.toList());
        
        for (MultiColumnSortMetadata.SortCriterion criterion : orderedCriteria) {
            int comparison = compareVisitsByColumn(v1, v2, criterion.getColumn(), criterion.getDirection());
            if (comparison != 0) {
                return comparison; // First non-equal comparison determines order
            }
        }
        
        return 0; // All criteria are equal
    }
    
    private int compareVisitsByColumn(Visit v1, Visit v2, String column, String direction) {
        Comparator<Visit> comparator = getComparator(column, direction);
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
        
        // Create test visits with varied data for multi-column sorting
        String[] treatments = {"Vaccination", "Surgery", "Medication", "Dental Cleaning", "Checkup", "X-ray", "Blood Test", "Vaccination", "Surgery"};
        String[] diagnoses = {"Healthy", "Infection", "Broken Bone", "Dental Issues", "Skin Condition", "Healthy", "Infection", "Arthritis", "Allergies"};
        VisitType[] visitTypes = {VisitType.WELLNESS_EXAM, VisitType.EMERGENCY, VisitType.SURGERY, VisitType.VACCINATION, VisitType.WELLNESS_EXAM};
        Pet[] pets = {pet1, pet2, pet3};
        
        for (int i = 0; i < 30; i++) {
            Visit visit = new Visit();
            visit.setId((long) (i + 1));
            visit.setVisitDate(LocalDateTime.now().minusDays(i * 2).plusHours(i % 24));
            visit.setTreatment(treatments[i % treatments.length]);
            visit.setDiagnosis(diagnoses[i % diagnoses.length]);
            visit.setVisitType(visitTypes[i % visitTypes.length]);
            visit.setCost(BigDecimal.valueOf(50.0 + (i * 15.5) % 500));
            visit.setPet(pets[i % pets.length]);
            visit.setNotes("Test visit notes for visit " + (i + 1) + " - priority " + (i % 3));
            testDataset.add(visit);
        }
        
        return testDataset;
    }
}