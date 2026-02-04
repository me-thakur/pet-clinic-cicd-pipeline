package com.petclinic.frontend.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Visit model JSON serialization/deserialization
 * Tests that the completion status is properly handled when receiving data from backend
 * Validates: Requirements 2.1, 2.3
 */
@DisplayName("Visit Model Integration Tests")
class VisitModelIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should deserialize completion status from JSON correctly")
    void shouldDeserializeCompletionStatusFromJsonCorrectly() throws Exception {
        // Given - JSON response from backend with completion status
        String completedVisitJson = "{" +
            "\"id\": 1," +
            "\"visitDate\": \"2024-01-15T10:30:00\"," +
            "\"description\": \"Routine checkup\"," +
            "\"diagnosis\": \"Healthy pet\"," +
            "\"treatment\": \"Vaccination administered\"," +
            "\"cost\": 75.00," +
            "\"completed\": true," +
            "\"emergencyVisit\": false" +
            "}";

        String incompleteVisitJson = "{" +
            "\"id\": 2," +
            "\"visitDate\": \"2024-01-16T14:00:00\"," +
            "\"description\": \"Initial examination\"," +
            "\"diagnosis\": \"Examination in progress\"," +
            "\"treatment\": null," +
            "\"cost\": 50.00," +
            "\"completed\": false," +
            "\"emergencyVisit\": false" +
            "}";

        // When - Deserialize JSON to Visit objects
        objectMapper.findAndRegisterModules(); // Register JavaTimeModule for LocalDateTime
        Visit completedVisit = objectMapper.readValue(completedVisitJson, Visit.class);
        Visit incompleteVisit = objectMapper.readValue(incompleteVisitJson, Visit.class);

        // Then - Verify completion status is correctly deserialized
        assertTrue(completedVisit.getCompleted(), "Completed visit should have completion status true");
        assertFalse(incompleteVisit.getCompleted(), "Incomplete visit should have completion status false");

        // Verify other fields are also correctly deserialized
        assertEquals(1L, completedVisit.getId());
        assertEquals("Healthy pet", completedVisit.getDiagnosis());
        assertEquals("Vaccination administered", completedVisit.getTreatment());
        assertEquals(0, BigDecimal.valueOf(75.00).compareTo(completedVisit.getCost()));

        assertEquals(2L, incompleteVisit.getId());
        assertEquals("Examination in progress", incompleteVisit.getDiagnosis());
        assertNull(incompleteVisit.getTreatment());
        assertEquals(0, BigDecimal.valueOf(50.00).compareTo(incompleteVisit.getCost()));
    }

    @Test
    @DisplayName("Should handle missing completion status in JSON")
    void shouldHandleMissingCompletionStatusInJson() throws Exception {
        // Given - JSON response without completion status field
        String visitJsonWithoutCompletion = "{" +
            "\"id\": 3," +
            "\"visitDate\": \"2024-01-17T09:00:00\"," +
            "\"description\": \"Follow-up visit\"," +
            "\"diagnosis\": \"Recovery progressing well\"," +
            "\"treatment\": \"Continue medication\"," +
            "\"cost\": 40.00," +
            "\"emergencyVisit\": false" +
            "}";

        // When - Deserialize JSON to Visit object
        objectMapper.findAndRegisterModules();
        Visit visit = objectMapper.readValue(visitJsonWithoutCompletion, Visit.class);

        // Then - Should default to false when completion status is missing
        assertFalse(visit.getCompleted(), "Should default to false when completion status is missing from JSON");
    }

    @Test
    @DisplayName("Should serialize completion status to JSON correctly")
    void shouldSerializeCompletionStatusToJsonCorrectly() throws Exception {
        // Given - Visit objects with different completion statuses
        Visit completedVisit = new Visit();
        completedVisit.setId(1L);
        completedVisit.setVisitDate(LocalDateTime.of(2024, 1, 15, 10, 30));
        completedVisit.setDescription("Routine checkup");
        completedVisit.setDiagnosis("Healthy pet");
        completedVisit.setTreatment("Vaccination administered");
        completedVisit.setCost(BigDecimal.valueOf(75.00));
        completedVisit.setCompleted(true); // Backend sets this

        Visit incompleteVisit = new Visit();
        incompleteVisit.setId(2L);
        incompleteVisit.setVisitDate(LocalDateTime.of(2024, 1, 16, 14, 0));
        incompleteVisit.setDescription("Initial examination");
        incompleteVisit.setDiagnosis("Examination in progress");
        incompleteVisit.setCost(BigDecimal.valueOf(50.00));
        incompleteVisit.setCompleted(false); // Backend sets this

        // When - Serialize Visit objects to JSON
        objectMapper.findAndRegisterModules();
        String completedJson = objectMapper.writeValueAsString(completedVisit);
        String incompleteJson = objectMapper.writeValueAsString(incompleteVisit);

        // Then - Verify completion status is included in JSON
        assertTrue(completedJson.contains("\"completed\":true"), 
                   "Completed visit JSON should include 'completed':true");
        assertTrue(incompleteJson.contains("\"completed\":false"), 
                   "Incomplete visit JSON should include 'completed':false");
    }

    @Test
    @DisplayName("Should maintain backend-computed status regardless of local fields")
    void shouldMaintainBackendComputedStatusRegardlessOfLocalFields() {
        // Given - Visit with diagnosis and treatment but backend says it's incomplete
        Visit visit = new Visit();
        visit.setDiagnosis("Complete diagnosis");
        visit.setTreatment("Complete treatment");
        visit.setCompleted(false); // Backend says it's incomplete despite having diagnosis and treatment

        // Then - Should respect backend decision
        assertFalse(visit.getCompleted(), 
                   "Should respect backend completion status even when diagnosis and treatment are present");

        // Given - Visit without diagnosis but backend says it's complete
        Visit anotherVisit = new Visit();
        anotherVisit.setDiagnosis(null);
        anotherVisit.setTreatment(null);
        anotherVisit.setCompleted(true); // Backend says it's complete despite missing fields

        // Then - Should respect backend decision
        assertTrue(anotherVisit.getCompleted(), 
                  "Should respect backend completion status even when diagnosis and treatment are missing");
    }
}