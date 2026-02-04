package com.petclinic.backend.service;

import com.petclinic.backend.dto.*;
import com.petclinic.backend.service.impl.EnhancedTableServiceImpl;
import org.junit.jupiter.api.BeforeEach;
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
 * Unit tests for EnhancedTableService
 * Tests basic functionality of sort, filter, and bulk operations
 */
@ExtendWith(MockitoExtension.class)
class EnhancedTableServiceTest {

    @Mock
    private VisitService visitService;

    @Mock
    private PetService petService;

    @Mock
    private UserService userService;

    @Mock
    private VeterinarianService veterinarianService;

    @InjectMocks
    private EnhancedTableServiceImpl enhancedTableService;

    private Pageable pageable;
    private SortMetadata sortMetadata;
    private List<FilterCriteria> filters;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
        sortMetadata = new SortMetadata("id", "asc");
        filters = Arrays.asList(
            new FilterCriteria("visitType", "equals", "CHECKUP", "visits")
        );
    }

    @Test
    void testGetSortableColumns() {
        // Test visits
        List<String> visitColumns = enhancedTableService.getSortableColumns("visits");
        assertNotNull(visitColumns);
        assertTrue(visitColumns.contains("id"));
        assertTrue(visitColumns.contains("visitDate"));
        assertTrue(visitColumns.contains("visitType"));

        // Test owners
        List<String> ownerColumns = enhancedTableService.getSortableColumns("owners");
        assertNotNull(ownerColumns);
        assertTrue(ownerColumns.contains("firstName"));
        assertTrue(ownerColumns.contains("lastName"));

        // Test invalid entity type
        List<String> invalidColumns = enhancedTableService.getSortableColumns("invalid");
        assertTrue(invalidColumns.isEmpty());
    }

    @Test
    void testGetFilterableColumns() {
        // Test visits
        List<String> visitColumns = enhancedTableService.getFilterableColumns("visits");
        assertNotNull(visitColumns);
        assertTrue(visitColumns.contains("visitType"));
        assertTrue(visitColumns.contains("diagnosis"));

        // Test pets
        List<String> petColumns = enhancedTableService.getFilterableColumns("pets");
        assertNotNull(petColumns);
        assertTrue(petColumns.contains("name"));
        assertTrue(petColumns.contains("petType"));

        // Test invalid entity type
        List<String> invalidColumns = enhancedTableService.getFilterableColumns("invalid");
        assertTrue(invalidColumns.isEmpty());
    }

    @Test
    void testValidateSortCriteria() {
        // Valid sort criteria
        SortMetadata validSort = new SortMetadata("id", "asc");
        assertTrue(enhancedTableService.validateSortCriteria("visits", validSort));

        // Invalid column
        SortMetadata invalidColumn = new SortMetadata("invalidColumn", "asc");
        assertFalse(enhancedTableService.validateSortCriteria("visits", invalidColumn));

        // Invalid direction
        SortMetadata invalidDirection = new SortMetadata("id", "invalid");
        assertFalse(enhancedTableService.validateSortCriteria("visits", invalidDirection));

        // Null sort metadata
        assertFalse(enhancedTableService.validateSortCriteria("visits", null));
    }

    @Test
    void testValidateFilterCriteria() {
        // Valid filter criteria
        FilterCriteria validFilter = new FilterCriteria("visitType", "equals", "CHECKUP", "visits");
        assertTrue(enhancedTableService.validateFilterCriteria("visits", validFilter));

        // Invalid column
        FilterCriteria invalidColumn = new FilterCriteria("invalidColumn", "equals", "value", "visits");
        assertFalse(enhancedTableService.validateFilterCriteria("visits", invalidColumn));

        // Null filter criteria
        assertFalse(enhancedTableService.validateFilterCriteria("visits", null));
    }

    @Test
    void testBulkDeleteValidation() {
        // Valid bulk delete request
        BulkDeleteRequest validRequest = new BulkDeleteRequest(
            Arrays.asList(1L, 2L, 3L), 
            "visits"
        );
        assertTrue(validRequest.isValid());

        // Invalid entity type
        BulkDeleteRequest invalidEntity = new BulkDeleteRequest(
            Arrays.asList(1L, 2L, 3L), 
            "invalid"
        );
        assertFalse(invalidEntity.isValid());

        // Empty IDs
        BulkDeleteRequest emptyIds = new BulkDeleteRequest();
        emptyIds.setEntityType("visits");
        assertFalse(emptyIds.isValid());
    }

    @Test
    void testBulkOperationResult() {
        BulkOperationResult result = new BulkOperationResult(true, 5, 10, "visits");
        
        assertTrue(result.isSuccess());
        assertEquals(5, result.getDeletedCount());
        assertEquals(5, result.getFailedCount());
        assertEquals(10, result.getTotalRequested());
        assertTrue(result.isPartialSuccess());
        assertFalse(result.isCompleteSuccess());
        assertFalse(result.isCompleteFailure());
        assertEquals(50.0, result.getSuccessRate(), 0.01);
    }

    @Test
    void testSortMetadata() {
        SortMetadata sort = new SortMetadata("visitDate", "desc");
        
        assertEquals("visitDate", sort.getColumn());
        assertEquals("desc", sort.getDirection());
        assertTrue(sort.isGlobal());
        assertFalse(sort.isAscending());
        assertTrue(sort.isDescending());
        assertTrue(sort.isValidDirection());
        assertEquals("asc", sort.getOppositeDirection());
        assertEquals("Visit Date", sort.getDisplayName());
    }

    @Test
    void testFilterMetadata() {
        FilterMetadata filter = new FilterMetadata("visitType", "equals", "CHECKUP");
        
        assertEquals("visitType", filter.getColumn());
        assertEquals("equals", filter.getOperator());
        assertEquals("CHECKUP", filter.getValue());
        assertEquals("Visit Type", filter.getDisplayName());
        assertEquals("CHECKUP", filter.getDisplayValue());
        assertTrue(filter.isEqualsOperator());
        assertFalse(filter.isContainsOperator());
        assertTrue(filter.isSelectFilter());
    }
}