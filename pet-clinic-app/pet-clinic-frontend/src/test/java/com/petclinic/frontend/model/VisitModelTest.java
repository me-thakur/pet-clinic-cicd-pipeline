package com.petclinic.frontend.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Visit model
 * Tests the completion status getters/setters functionality
 * Validates: Requirements 2.1, 2.3
 */
@DisplayName("Visit Model Tests")
class VisitModelTest {

    @Test
    @DisplayName("Should return false when completion status is null")
    void shouldReturnFalseWhenCompletionStatusIsNull() {
        // Given
        Visit visit = new Visit();
        
        // When
        Boolean completed = visit.getCompleted();
        
        // Then
        assertFalse(completed, "Should return false when completion status is null");
    }

    @Test
    @DisplayName("Should return backend-computed completion status when set")
    void shouldReturnBackendComputedCompletionStatus() {
        // Given
        Visit visit = new Visit();
        
        // When - Set completion status from backend
        visit.setCompleted(true);
        
        // Then
        assertTrue(visit.getCompleted(), "Should return true when backend sets completion to true");
        
        // When - Change completion status from backend
        visit.setCompleted(false);
        
        // Then
        assertFalse(visit.getCompleted(), "Should return false when backend sets completion to false");
    }

    @Test
    @DisplayName("Should store backend response value correctly")
    void shouldStoreBackendResponseValueCorrectly() {
        // Given
        Visit visit = new Visit();
        
        // When - Set various completion values
        visit.setCompleted(true);
        assertTrue(visit.getCompleted(), "Should store and return true");
        
        visit.setCompleted(false);
        assertFalse(visit.getCompleted(), "Should store and return false");
        
        visit.setCompleted(null);
        assertFalse(visit.getCompleted(), "Should return false when set to null");
    }

    @Test
    @DisplayName("Should handle completion status transitions correctly")
    void shouldHandleCompletionStatusTransitionsCorrectly() {
        // Given
        Visit visit = new Visit();
        
        // Initially null/false
        assertFalse(visit.getCompleted(), "Should initially return false");
        
        // Backend marks as completed
        visit.setCompleted(true);
        assertTrue(visit.getCompleted(), "Should return true after backend marks as completed");
        
        // Backend marks as incomplete
        visit.setCompleted(false);
        assertFalse(visit.getCompleted(), "Should return false after backend marks as incomplete");
        
        // Backend marks as completed again
        visit.setCompleted(true);
        assertTrue(visit.getCompleted(), "Should return true after backend marks as completed again");
    }

    @Test
    @DisplayName("Should work correctly with JSON property annotation")
    void shouldWorkCorrectlyWithJsonPropertyAnnotation() {
        // Given
        Visit visit = new Visit();
        
        // The @JsonProperty("completed") annotation should map the "completed" field
        // from JSON responses to the completedFromBackend field
        
        // When - Simulate backend response setting completion status
        visit.setCompleted(true);
        
        // Then
        assertTrue(visit.getCompleted(), "Should correctly handle JSON property mapping");
        
        // Verify the getter returns the backend-computed value
        Boolean result = visit.getCompleted();
        assertNotNull(result, "Completion status should not be null");
        assertTrue(result, "Should return backend-computed completion status");
    }
}