package com.petclinic.backend.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge Case Tests for Visit Completion Logic
 * 
 * Tests whitespace-only diagnosis and treatment fields and verifies
 * empty field handling and null value processing.
 * 
 * **Validates: Requirements 6.2**
 */
@DisplayName("Visit Completion Logic Edge Cases")
class VisitCompletionLogicEdgeCasesTest {

    private Visit visit;

    @BeforeEach
    void setUp() {
        visit = new Visit();
        visit.setVisitDate(LocalDateTime.now());
        visit.setVisitType(VisitType.WELLNESS_EXAM);
    }

    @Test
    @DisplayName("Visit with both diagnosis and treatment should be completed")
    void testCompletedVisit() {
        // Given
        visit.setDiagnosis("Healthy pet");
        visit.setTreatment("Routine vaccination");

        // When & Then
        assertTrue(visit.isCompleted(), "Visit with both diagnosis and treatment should be completed");
    }

    @Test
    @DisplayName("Visit with null diagnosis should be pending")
    void testNullDiagnosis() {
        // Given
        visit.setDiagnosis(null);
        visit.setTreatment("Some treatment");

        // When & Then
        assertFalse(visit.isCompleted(), "Visit with null diagnosis should be pending");
    }

    @Test
    @DisplayName("Visit with null treatment should be pending")
    void testNullTreatment() {
        // Given
        visit.setDiagnosis("Some diagnosis");
        visit.setTreatment(null);

        // When & Then
        assertFalse(visit.isCompleted(), "Visit with null treatment should be pending");
    }

    @Test
    @DisplayName("Visit with both null diagnosis and treatment should be pending")
    void testBothNull() {
        // Given
        visit.setDiagnosis(null);
        visit.setTreatment(null);

        // When & Then
        assertFalse(visit.isCompleted(), "Visit with both null fields should be pending");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"", " ", "  ", "\t", "\n", "\r", " \t\n\r "})
    @DisplayName("Visit with empty or whitespace-only diagnosis should be pending")
    void testEmptyOrWhitespaceDiagnosis(String diagnosis) {
        // Given
        visit.setDiagnosis(diagnosis);
        visit.setTreatment("Valid treatment");

        // When & Then
        assertFalse(visit.isCompleted(), 
            "Visit with empty/whitespace diagnosis '" + diagnosis + "' should be pending");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"", " ", "  ", "\t", "\n", "\r", " \t\n\r "})
    @DisplayName("Visit with empty or whitespace-only treatment should be pending")
    void testEmptyOrWhitespaceTreatment(String treatment) {
        // Given
        visit.setDiagnosis("Valid diagnosis");
        visit.setTreatment(treatment);

        // When & Then
        assertFalse(visit.isCompleted(), 
            "Visit with empty/whitespace treatment '" + treatment + "' should be pending");
    }

    @Test
    @DisplayName("Visit with whitespace-padded valid content should be completed")
    void testWhitespacePaddedValidContent() {
        // Given
        visit.setDiagnosis("  Valid diagnosis  ");
        visit.setTreatment("  Valid treatment  ");

        // When & Then
        assertTrue(visit.isCompleted(), 
            "Visit with whitespace-padded valid content should be completed");
    }

    @Test
    @DisplayName("Visit with mixed whitespace and content should handle correctly")
    void testMixedWhitespaceAndContent() {
        // Test cases with various combinations of whitespace and content
        
        // Valid diagnosis with whitespace treatment
        visit.setDiagnosis("Valid diagnosis");
        visit.setTreatment("   ");
        assertFalse(visit.isCompleted(), "Valid diagnosis + whitespace treatment should be pending");

        // Whitespace diagnosis with valid treatment
        visit.setDiagnosis("   ");
        visit.setTreatment("Valid treatment");
        assertFalse(visit.isCompleted(), "Whitespace diagnosis + valid treatment should be pending");

        // Both have content with different whitespace patterns
        visit.setDiagnosis("\tDiagnosis with tab\n");
        visit.setTreatment(" Treatment with spaces ");
        assertTrue(visit.isCompleted(), "Both fields with content and whitespace should be completed");
    }

    @Test
    @DisplayName("Visit with special characters should be handled correctly")
    void testSpecialCharacters() {
        // Test with various special characters that should be considered valid content
        
        visit.setDiagnosis("Diagnosis: Pet has condition #1");
        visit.setTreatment("Treatment: Apply medication @2x daily");
        assertTrue(visit.isCompleted(), "Fields with special characters should be completed");

        visit.setDiagnosis("Diagnosis with émojis 🐕");
        visit.setTreatment("Treatment with symbols: $50 cost");
        assertTrue(visit.isCompleted(), "Fields with unicode and symbols should be completed");
    }

    @Test
    @DisplayName("Visit with very long content should be handled correctly")
    void testVeryLongContent() {
        // Test with very long strings to ensure no length-based issues
        String longDiagnosis = "Very long diagnosis: " + "A".repeat(1000);
        String longTreatment = "Very long treatment: " + "B".repeat(1000);

        visit.setDiagnosis(longDiagnosis);
        visit.setTreatment(longTreatment);
        
        assertTrue(visit.isCompleted(), "Visit with very long content should be completed");
    }

    @Test
    @DisplayName("Visit with single character content should be completed")
    void testSingleCharacterContent() {
        // Test edge case with minimal valid content
        visit.setDiagnosis("A");
        visit.setTreatment("B");
        
        assertTrue(visit.isCompleted(), "Visit with single character content should be completed");
    }

    @Test
    @DisplayName("Visit with numeric content should be completed")
    void testNumericContent() {
        // Test with numeric content
        visit.setDiagnosis("123");
        visit.setTreatment("456");
        
        assertTrue(visit.isCompleted(), "Visit with numeric content should be completed");
    }

    @Test
    @DisplayName("Visit completion logic should be consistent across multiple calls")
    void testConsistencyAcrossMultipleCalls() {
        // Test that the completion logic is consistent when called multiple times
        visit.setDiagnosis("Consistent diagnosis");
        visit.setTreatment("Consistent treatment");
        
        boolean firstCall = visit.isCompleted();
        boolean secondCall = visit.isCompleted();
        boolean thirdCall = visit.isCompleted();
        
        assertTrue(firstCall, "First call should return true");
        assertEquals(firstCall, secondCall, "Second call should be consistent with first");
        assertEquals(secondCall, thirdCall, "Third call should be consistent with second");
    }

    @Test
    @DisplayName("Visit completion logic should handle field modifications correctly")
    void testFieldModifications() {
        // Start with incomplete visit
        visit.setDiagnosis("Valid diagnosis");
        visit.setTreatment(null);
        assertFalse(visit.isCompleted(), "Initially should be pending");

        // Add treatment to make it complete
        visit.setTreatment("Valid treatment");
        assertTrue(visit.isCompleted(), "Should become completed after adding treatment");

        // Remove diagnosis to make it pending again
        visit.setDiagnosis("");
        assertFalse(visit.isCompleted(), "Should become pending after removing diagnosis");

        // Add diagnosis back
        visit.setDiagnosis("New diagnosis");
        assertTrue(visit.isCompleted(), "Should become completed again after adding diagnosis");
    }

    @Test
    @DisplayName("Visit completion logic should handle concurrent access safely")
    void testConcurrentAccess() throws InterruptedException {
        // Set up a completed visit
        visit.setDiagnosis("Thread-safe diagnosis");
        visit.setTreatment("Thread-safe treatment");

        // Test concurrent access to isCompleted method
        final boolean[] results = new boolean[10];
        Thread[] threads = new Thread[10];

        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = visit.isCompleted();
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all results are consistent
        for (int i = 0; i < 10; i++) {
            assertTrue(results[i], "Result from thread " + i + " should be true");
        }
    }
}