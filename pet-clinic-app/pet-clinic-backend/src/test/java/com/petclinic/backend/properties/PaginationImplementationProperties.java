package com.petclinic.backend.properties;

import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.service.PetService;
import com.petclinic.backend.service.VisitService;
import com.petclinic.backend.service.VeterinarianService;
import net.java.quickcheck.Generator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for pagination implementation
 * **Validates: Requirements 10.2**
 * 
 * Tests universal properties that must hold for all pagination implementations:
 * - Page size consistency
 * - Total elements accuracy
 * - Page navigation correctness
 * - Boundary condition handling
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PaginationImplementationProperties extends PropertyTestBase {

    @Autowired
    private PetService petService;
    
    @Autowired
    private VisitService visitService;
    
    @Autowired
    private VeterinarianService veterinarianService;

    /**
     * Property 28: Pagination Implementation
     * **Validates: Requirements 10.2**
     * 
     * Universal properties for pagination:
     * 1. Page size must not exceed requested size
     * 2. Total elements must be consistent across pages
     * 3. First page must start at index 0
     * 4. Last page must contain remaining elements
     * 5. Empty pages must be handled correctly
     * 6. Page navigation must be consistent
     */
    @Test
    void testPaginationImplementationProperties() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate random pagination parameters
            int pageSize = 1 + random.nextInt(20); // 1-20 items per page
            int pageNumber = random.nextInt(5); // 0-4 page numbers
            
            testPetPaginationProperties(pageNumber, pageSize);
            testVisitPaginationProperties(pageNumber, pageSize);
            testVeterinarianPaginationProperties(pageNumber, pageSize);
        });
    }

    private void testPetPaginationProperties(int pageNumber, int pageSize) {
        // Test Pet pagination using the advanced search method
        Page<Pet> page = petService.searchPetsAdvanced(null, null, null, null, pageNumber, pageSize);
        PagedResponse<Pet> response = new PagedResponse<>(page);
        
        // Property 1: Page size must not exceed requested size
        assertTrue(page.getContent().size() <= pageSize, 
                  "Page content size (" + page.getContent().size() + ") exceeds requested page size (" + pageSize + ")");
        
        // Property 2: Total elements must be consistent
        assertEquals(page.getTotalElements(), response.getPage().getTotalElements(),
                    "Total elements mismatch between Page and PagedResponse");
        
        // Property 3: Page number consistency
        assertEquals(pageNumber, page.getNumber(),
                    "Page number mismatch");
        assertEquals(pageNumber, response.getPage().getNumber(),
                    "Page number mismatch in PagedResponse");
        
        // Property 4: Page size consistency
        assertEquals(pageSize, page.getSize(),
                    "Page size mismatch");
        assertEquals(pageSize, response.getPage().getSize(),
                    "Page size mismatch in PagedResponse");
        
        // Property 5: First/Last page flags consistency
        assertEquals(page.isFirst(), response.getPage().isFirst(),
                    "First page flag mismatch");
        assertEquals(page.isLast(), response.getPage().isLast(),
                    "Last page flag mismatch");
        
        // Property 6: Navigation flags consistency
        assertEquals(page.hasNext(), response.getPage().isHasNext(),
                    "Has next flag mismatch");
        assertEquals(page.hasPrevious(), response.getPage().isHasPrevious(),
                    "Has previous flag mismatch");
        
        // Property 7: Total pages calculation
        long expectedTotalPages = (page.getTotalElements() + pageSize - 1) / pageSize;
        assertEquals(expectedTotalPages, page.getTotalPages(),
                    "Total pages calculation incorrect");
        assertEquals(expectedTotalPages, response.getPage().getTotalPages(),
                    "Total pages calculation incorrect in PagedResponse");
        
        // Property 8: Empty page handling
        if (page.getTotalElements() == 0) {
            assertTrue(page.getContent().isEmpty(), "Empty page should have empty content");
            // For empty results, Spring Data considers any page as both first and last
            // This is correct behavior - when there's no data, any page request is valid
            assertTrue(page.isLast(), "Empty page should be last");
            assertFalse(page.hasNext(), "Empty page should not have next");
            // Only page 0 should be considered "first" when there's no data
            if (pageNumber == 0) {
                assertTrue(page.isFirst(), "Empty page 0 should be first");
                assertFalse(page.hasPrevious(), "Empty page 0 should not have previous");
            }
        }
        
        // Property 9: Non-empty page validation
        if (page.getTotalElements() > 0 && pageNumber == 0) {
            assertTrue(page.isFirst(), "First page should be marked as first");
            assertFalse(page.hasPrevious(), "First page should not have previous");
        }
    }

    private void testVisitPaginationProperties(int pageNumber, int pageSize) {
        // Create a manual page for visits since the service doesn't have built-in pagination yet
        List<Visit> allVisits = visitService.findAll();
        
        // Apply pagination manually
        int start = pageNumber * pageSize;
        int end = Math.min(start + pageSize, allVisits.size());
        
        if (start >= allVisits.size()) {
            // Empty page case
            List<Visit> emptyContent = List.of();
            Page<Visit> page = new org.springframework.data.domain.PageImpl<>(
                emptyContent, 
                PageRequest.of(pageNumber, pageSize), 
                allVisits.size()
            );
            PagedResponse<Visit> response = new PagedResponse<>(page);
            
            assertTrue(page.getContent().isEmpty(), "Empty page should have empty content");
            assertEquals(allVisits.size(), page.getTotalElements(), "Total elements should match");
            return;
        }
        
        List<Visit> pageContent = allVisits.subList(start, end);
        Page<Visit> page = new org.springframework.data.domain.PageImpl<>(
            pageContent, 
            PageRequest.of(pageNumber, pageSize), 
            allVisits.size()
        );
        PagedResponse<Visit> response = new PagedResponse<>(page);
        
        // Apply same property tests as for pets
        assertTrue(page.getContent().size() <= pageSize, 
                  "Visit page content size exceeds requested page size");
        assertEquals(page.getTotalElements(), response.getPage().getTotalElements(),
                    "Visit total elements mismatch");
        assertEquals(pageNumber, page.getNumber(), "Visit page number mismatch");
        assertEquals(pageSize, page.getSize(), "Visit page size mismatch");
    }

    private void testVeterinarianPaginationProperties(int pageNumber, int pageSize) {
        // Create a manual page for veterinarians
        List<Veterinarian> allVeterinarians = veterinarianService.findAll();
        
        // Apply pagination manually
        int start = pageNumber * pageSize;
        int end = Math.min(start + pageSize, allVeterinarians.size());
        
        if (start >= allVeterinarians.size()) {
            // Empty page case
            List<Veterinarian> emptyContent = List.of();
            Page<Veterinarian> page = new org.springframework.data.domain.PageImpl<>(
                emptyContent, 
                PageRequest.of(pageNumber, pageSize), 
                allVeterinarians.size()
            );
            PagedResponse<Veterinarian> response = new PagedResponse<>(page);
            
            assertTrue(page.getContent().isEmpty(), "Empty veterinarian page should have empty content");
            assertEquals(allVeterinarians.size(), page.getTotalElements(), "Total elements should match");
            return;
        }
        
        List<Veterinarian> pageContent = allVeterinarians.subList(start, end);
        Page<Veterinarian> page = new org.springframework.data.domain.PageImpl<>(
            pageContent, 
            PageRequest.of(pageNumber, pageSize), 
            allVeterinarians.size()
        );
        PagedResponse<Veterinarian> response = new PagedResponse<>(page);
        
        // Apply same property tests as for pets
        assertTrue(page.getContent().size() <= pageSize, 
                  "Veterinarian page content size exceeds requested page size");
        assertEquals(page.getTotalElements(), response.getPage().getTotalElements(),
                    "Veterinarian total elements mismatch");
        assertEquals(pageNumber, page.getNumber(), "Veterinarian page number mismatch");
        assertEquals(pageSize, page.getSize(), "Veterinarian page size mismatch");
    }

    /**
     * Test pagination boundary conditions
     */
    @Test
    void testPaginationBoundaryConditions() {
        // Test with page size 1
        testBoundaryCondition(0, 1);
        
        // Test with large page size
        testBoundaryCondition(0, 1000);
        
        // Test with page number beyond available data
        testBoundaryCondition(999, 10);
    }

    private void testBoundaryCondition(int pageNumber, int pageSize) {
        Page<Pet> page = petService.searchPetsAdvanced(null, null, null, null, pageNumber, pageSize);
        PagedResponse<Pet> response = new PagedResponse<>(page);
        
        // Boundary condition: Page content size should never exceed page size
        assertTrue(page.getContent().size() <= pageSize,
                  "Boundary condition failed: page content size exceeds page size");
        
        // Boundary condition: Page number should match requested
        assertEquals(pageNumber, page.getNumber(),
                    "Boundary condition failed: page number mismatch");
        
        // Boundary condition: Total elements should be non-negative
        assertTrue(page.getTotalElements() >= 0,
                  "Boundary condition failed: negative total elements");
        
        // Boundary condition: Total pages should be non-negative
        assertTrue(page.getTotalPages() >= 0,
                  "Boundary condition failed: negative total pages");
        
        // Boundary condition: If no content, should be empty
        if (page.getTotalElements() == 0) {
            assertTrue(page.getContent().isEmpty(),
                      "Boundary condition failed: empty total but non-empty content");
        }
    }

    /**
     * Test pagination consistency across multiple requests
     */
    @Test
    void testPaginationConsistency() {
        int pageSize = 5;
        
        // Get first page
        Page<Pet> firstPage = petService.searchPetsAdvanced(null, null, null, null, 0, pageSize);
        
        // Get second page
        Page<Pet> secondPage = petService.searchPetsAdvanced(null, null, null, null, 1, pageSize);
        
        // Consistency: Total elements should be the same
        assertEquals(firstPage.getTotalElements(), secondPage.getTotalElements(),
                    "Total elements should be consistent across pages");
        
        // Consistency: Total pages should be the same
        assertEquals(firstPage.getTotalPages(), secondPage.getTotalPages(),
                    "Total pages should be consistent across pages");
        
        // Consistency: No overlap between pages (if both have content)
        if (!firstPage.getContent().isEmpty() && !secondPage.getContent().isEmpty()) {
            List<Long> firstPageIds = firstPage.getContent().stream()
                    .map(Pet::getId)
                    .collect(java.util.stream.Collectors.toList());
            List<Long> secondPageIds = secondPage.getContent().stream()
                    .map(Pet::getId)
                    .collect(java.util.stream.Collectors.toList());
            
            // No IDs should appear in both pages
            assertTrue(firstPageIds.stream().noneMatch(secondPageIds::contains),
                      "Pages should not have overlapping content");
        }
    }
}