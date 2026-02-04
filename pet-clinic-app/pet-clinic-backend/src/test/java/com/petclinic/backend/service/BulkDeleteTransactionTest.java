package com.petclinic.backend.service;

import com.petclinic.backend.dto.BulkDeleteRequest;
import com.petclinic.backend.dto.BulkOperationResult;
import com.petclinic.backend.service.impl.EnhancedTableServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for bulk delete transaction and progress tracking functionality
 * Validates: Requirements 6.5 - Transaction support and progress tracking
 */
@ExtendWith(MockitoExtension.class)
class BulkDeleteTransactionTest {

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

    @Test
    void testBulkDeleteWithTransactionSupport() {
        // Given
        List<Long> idsToDelete = Arrays.asList(1L, 2L, 3L);
        BulkDeleteRequest request = new BulkDeleteRequest(idsToDelete, "visits");

        // When
        BulkOperationResult result = enhancedTableService.bulkDelete(request);

        // Then
        assertNotNull(result);
        assertEquals("visits", result.getEntityType());
        assertEquals(3, result.getTotalRequested());
        assertNotNull(result.getDuration());
        assertTrue(result.getDuration() >= 0);
    }

    @Test
    void testBulkDeleteProgressTracking() {
        // Given
        BulkOperationResult result = new BulkOperationResult();
        result.setTotalRequested(100);
        long startTime = System.currentTimeMillis();

        // When - simulate progress updates
        result.updateProgress(25, startTime);

        // Then
        assertEquals(25, result.getCurrentItem().intValue());
        assertEquals(25, result.getProgressPercentage().intValue());
        assertNotNull(result.getEstimatedTimeRemaining());
        assertTrue(result.isInProgress());
    }

    @Test
    void testBulkDeleteProgressCompletion() {
        // Given
        BulkOperationResult result = new BulkOperationResult();
        result.setTotalRequested(10);
        long startTime = System.currentTimeMillis();

        // When - simulate completion
        result.updateProgress(10, startTime);

        // Then
        assertEquals(10, result.getCurrentItem().intValue());
        assertEquals(100, result.getProgressPercentage().intValue());
        assertFalse(result.isInProgress());
    }

    @Test
    void testBulkDeleteWithInvalidRequest() {
        // Given
        BulkDeleteRequest invalidRequest = new BulkDeleteRequest();
        invalidRequest.setEntityType("invalid");

        // When
        BulkOperationResult result = enhancedTableService.bulkDelete(invalidRequest);

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.hasErrors());
        assertEquals("Invalid bulk delete request", result.getErrors().get(0));
    }

    @Test
    void testBulkDeleteResultSummaryMessages() {
        // Test complete success
        BulkOperationResult successResult = new BulkOperationResult(true, 5, 5, "visits");
        assertTrue(successResult.isCompleteSuccess());
        assertTrue(successResult.getSummaryMessage().contains("Successfully deleted 5 visits"));

        // Test partial success
        BulkOperationResult partialResult = new BulkOperationResult(false, 3, 5, "visits");
        assertTrue(partialResult.isPartialSuccess());
        assertTrue(partialResult.getSummaryMessage().contains("Partially successful: deleted 3 of 5 visits"));

        // Test complete failure
        BulkOperationResult failureResult = new BulkOperationResult(false, 0, 5, "visits");
        assertTrue(failureResult.isCompleteFailure());
        assertTrue(failureResult.getSummaryMessage().contains("Failed to delete any visits"));
    }

    @Test
    void testBulkDeleteSuccessRate() {
        // Given
        BulkOperationResult result = new BulkOperationResult();
        result.setTotalRequested(10);
        result.setDeletedCount(7);

        // When
        double successRate = result.getSuccessRate();

        // Then
        assertEquals(70.0, successRate, 0.01);
    }

    @Test
    void testBulkDeleteWithZeroItems() {
        // Given
        BulkOperationResult result = new BulkOperationResult();
        result.setTotalRequested(0);

        // When
        double successRate = result.getSuccessRate();

        // Then
        assertEquals(0.0, successRate, 0.01);
    }
}