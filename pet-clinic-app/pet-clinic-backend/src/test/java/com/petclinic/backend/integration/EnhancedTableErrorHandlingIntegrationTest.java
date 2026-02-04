package com.petclinic.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petclinic.backend.dto.BulkDeleteRequest;
import com.petclinic.backend.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for comprehensive error handling in enhanced table operations
 * Tests end-to-end error handling with user-friendly messages and recovery options
 * Requirements: 2.4, 4.2
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EnhancedTableErrorHandlingIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testInvalidEntityTypeError() throws Exception {
        setUp();
        
        // Test invalid entity type
        MvcResult result = mockMvc.perform(get("/api/v1/invalid-entity")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertFalse(responseBody.isEmpty(), "Response should not be empty for invalid entity type");
    }

    @Test
    void testInvalidSortColumnError() throws Exception {
        setUp();
        
        // Test invalid sort column for visits
        MvcResult result = mockMvc.perform(get("/api/v1/visits")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "invalid_column")
                .param("sortDir", "asc"))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertFalse(responseBody.isEmpty(), "Response should not be empty for invalid sort column");
    }

    @Test
    void testInvalidSortDirectionError() throws Exception {
        setUp();
        
        // Test invalid sort direction
        MvcResult result = mockMvc.perform(get("/api/v1/visits")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "id")
                .param("sortDir", "invalid"))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertFalse(responseBody.isEmpty(), "Response should not be empty for invalid sort direction");
    }

    @Test
    void testInvalidPageParametersError() throws Exception {
        setUp();
        
        // Test negative page number
        mockMvc.perform(get("/api/v1/visits")
                .param("page", "-1")
                .param("size", "10"))
                .andExpect(status().isBadRequest());

        // Test zero page size
        mockMvc.perform(get("/api/v1/visits")
                .param("page", "0")
                .param("size", "0"))
                .andExpect(status().isBadRequest());

        // Test excessive page size
        mockMvc.perform(get("/api/v1/visits")
                .param("page", "0")
                .param("size", "1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMissingRequiredParameterError() throws Exception {
        setUp();
        
        // Test missing entity type in bulk delete
        BulkDeleteRequest request = new BulkDeleteRequest();
        request.setSelectedIds(Arrays.asList(1L, 2L, 3L));
        // Intentionally not setting entity type

        MvcResult result = mockMvc.perform(delete("/api/v1/visits/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);
        
        assertNotNull(errorResponse, "Error response should not be null");
        assertEquals("INVALID_BULK_REQUEST", errorResponse.getErrorCode());
        assertNotNull(errorResponse.getFallbackOptions(), "Should provide fallback options");
    }

    @Test
    void testMalformedJsonError() throws Exception {
        setUp();
        
        // Test malformed JSON in bulk delete request
        String malformedJson = "{ invalid json }";

        MvcResult result = mockMvc.perform(delete("/api/v1/visits/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);
        
        assertNotNull(errorResponse, "Error response should not be null");
        assertEquals("MALFORMED_REQUEST", errorResponse.getErrorCode());
        assertEquals("Please check your request format and try again", errorResponse.getMessage());
        assertNotNull(errorResponse.getFallbackOptions(), "Should provide fallback options");
    }

    @Test
    void testValidationErrorWithFieldErrors() throws Exception {
        setUp();
        
        // Test bulk delete with invalid request structure
        BulkDeleteRequest request = new BulkDeleteRequest();
        request.setEntityType("visits");
        request.setSelectedIds(Arrays.asList()); // Empty list should be invalid
        request.setSelectAll(false);

        MvcResult result = mockMvc.perform(delete("/api/v1/visits/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);
        
        assertNotNull(errorResponse, "Error response should not be null");
        assertEquals("INVALID_BULK_REQUEST", errorResponse.getErrorCode());
        assertNotNull(errorResponse.getFallbackOptions(), "Should provide fallback options");
    }

    @Test
    void testSortValidationEndpoint() throws Exception {
        setUp();
        
        // Test valid sort parameters
        MvcResult validResult = mockMvc.perform(get("/api/v1/visits/validate-sort")
                .param("column", "visitDate")
                .param("direction", "asc"))
                .andExpect(status().isOk())
                .andReturn();

        String validResponseBody = validResult.getResponse().getContentAsString();
        assertFalse(validResponseBody.isEmpty(), "Valid sort validation should return response");
        assertTrue(validResponseBody.contains("\"valid\":true"), "Valid parameters should be marked as valid");

        // Test invalid sort parameters
        MvcResult invalidResult = mockMvc.perform(get("/api/v1/visits/validate-sort")
                .param("column", "invalid_column")
                .param("direction", "invalid_direction"))
                .andExpect(status().isOk())
                .andReturn();

        String invalidResponseBody = invalidResult.getResponse().getContentAsString();
        assertFalse(invalidResponseBody.isEmpty(), "Invalid sort validation should return response");
        assertTrue(invalidResponseBody.contains("\"valid\":false"), "Invalid parameters should be marked as invalid");
    }

    @Test
    void testFilterValuesEndpoint() throws Exception {
        setUp();
        
        // Test valid filter values request
        MvcResult validResult = mockMvc.perform(get("/api/v1/visits/filter-values/visitType"))
                .andExpect(status().isOk())
                .andReturn();

        String validResponseBody = validResult.getResponse().getContentAsString();
        assertFalse(validResponseBody.isEmpty(), "Valid filter values should return response");

        // Test invalid column for filter values
        mockMvc.perform(get("/api/v1/visits/filter-values/invalid_column"))
                .andExpect(status().isBadRequest());

        // Test invalid entity type for filter values
        mockMvc.perform(get("/api/v1/invalid-entity/filter-values/column"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testErrorResponseStructure() throws Exception {
        setUp();
        
        // Generate an error and verify response structure
        MvcResult result = mockMvc.perform(get("/api/v1/invalid-entity"))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        
        if (!responseBody.isEmpty()) {
            ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);
            
            // Verify required fields are present
            assertNotNull(errorResponse.getTimestamp(), "Error response should have timestamp");
            assertNotNull(errorResponse.getPath(), "Error response should have path");
            
            // Verify optional fields are handled correctly
            if (errorResponse.getFallbackOptions() != null) {
                assertTrue(errorResponse.getFallbackOptions() instanceof java.util.List, 
                          "Fallback options should be a list");
            }
            
            if (errorResponse.getFieldErrors() != null) {
                assertTrue(errorResponse.getFieldErrors() instanceof java.util.List, 
                          "Field errors should be a list");
            }
        }
    }

    @Test
    void testConcurrentOperationEndpoints() throws Exception {
        setUp();
        
        // Test concurrent operation statistics
        MvcResult statsResult = mockMvc.perform(get("/api/v1/visits/concurrent/statistics"))
                .andExpect(status().isOk())
                .andReturn();

        String statsResponseBody = statsResult.getResponse().getContentAsString();
        assertFalse(statsResponseBody.isEmpty(), "Concurrent statistics should return response");

        // Test operation status with non-existent key
        mockMvc.perform(get("/api/v1/visits/concurrent/status/non-existent-key"))
                .andExpect(status().isNotFound());

        // Test operation cancellation with non-existent key
        MvcResult cancelResult = mockMvc.perform(post("/api/v1/visits/concurrent/cancel/non-existent-key"))
                .andExpect(status().isOk())
                .andReturn();

        String cancelResponseBody = cancelResult.getResponse().getContentAsString();
        assertFalse(cancelResponseBody.isEmpty(), "Cancel operation should return response");
        assertTrue(cancelResponseBody.contains("\"cancelled\":false"), "Non-existent operation should not be cancelled");
    }

    @Test
    void testCacheManagementEndpoints() throws Exception {
        setUp();
        
        // Test cache statistics
        MvcResult statsResult = mockMvc.perform(get("/api/v1/visits/cache/statistics"))
                .andExpect(status().isOk())
                .andReturn();

        String statsResponseBody = statsResult.getResponse().getContentAsString();
        assertFalse(statsResponseBody.isEmpty(), "Cache statistics should return response");

        // Test cache clearing
        MvcResult clearResult = mockMvc.perform(post("/api/v1/visits/cache/clear"))
                .andExpect(status().isOk())
                .andReturn();

        String clearResponseBody = clearResult.getResponse().getContentAsString();
        assertFalse(clearResponseBody.isEmpty(), "Cache clear should return response");
        assertTrue(clearResponseBody.contains("\"status\":\"success\""), "Cache clear should be successful");

        // Test cache warm-up
        MvcResult warmUpResult = mockMvc.perform(post("/api/v1/visits/cache/warm-up"))
                .andExpect(status().isOk())
                .andReturn();

        String warmUpResponseBody = warmUpResult.getResponse().getContentAsString();
        assertFalse(warmUpResponseBody.isEmpty(), "Cache warm-up should return response");
        assertTrue(warmUpResponseBody.contains("\"status\":\"success\""), "Cache warm-up should be successful");

        // Test cache toggle
        MvcResult toggleResult = mockMvc.perform(post("/api/v1/visits/cache/toggle")
                .param("enabled", "false"))
                .andExpect(status().isOk())
                .andReturn();

        String toggleResponseBody = toggleResult.getResponse().getContentAsString();
        assertFalse(toggleResponseBody.isEmpty(), "Cache toggle should return response");
        assertTrue(toggleResponseBody.contains("\"cachingEnabled\":false"), "Cache should be disabled");
    }

    @Test
    void testErrorResponseSerialization() throws Exception {
        setUp();
        
        // Generate an error and verify JSON serialization
        MvcResult result = mockMvc.perform(get("/api/v1/visits")
                .param("sortBy", "invalid_column")
                .param("sortDir", "asc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        
        if (!responseBody.isEmpty()) {
            // Verify it's valid JSON
            ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);
            assertNotNull(errorResponse, "Should be able to deserialize error response");
            
            // Verify it can be serialized back to JSON
            String serialized = objectMapper.writeValueAsString(errorResponse);
            assertNotNull(serialized, "Should be able to serialize error response");
            assertFalse(serialized.isEmpty(), "Serialized response should not be empty");
            
            // Verify round-trip serialization
            ErrorResponse roundTrip = objectMapper.readValue(serialized, ErrorResponse.class);
            assertEquals(errorResponse.getErrorCode(), roundTrip.getErrorCode(), "Error code should survive round-trip");
            assertEquals(errorResponse.getMessage(), roundTrip.getMessage(), "Message should survive round-trip");
        }
    }

    @Test
    void testInvalidEntityTypeInAllEndpoints() throws Exception {
        setUp();
        
        String invalidEntity = "invalid-entity";
        
        // Test main endpoint
        mockMvc.perform(get("/api/v1/" + invalidEntity))
                .andExpect(status().isBadRequest());
        
        // Test bulk delete endpoint
        BulkDeleteRequest request = new BulkDeleteRequest();
        request.setEntityType(invalidEntity);
        request.setSelectedIds(Arrays.asList(1L));
        
        mockMvc.perform(delete("/api/v1/" + invalidEntity + "/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        
        // Test filter values endpoint
        mockMvc.perform(get("/api/v1/" + invalidEntity + "/filter-values/column"))
                .andExpect(status().isBadRequest());
        
        // Test sort validation endpoint
        mockMvc.perform(get("/api/v1/" + invalidEntity + "/validate-sort")
                .param("column", "id")
                .param("direction", "asc"))
                .andExpect(status().isBadRequest());
        
        // Test cache endpoints
        mockMvc.perform(get("/api/v1/" + invalidEntity + "/cache/statistics"))
                .andExpect(status().isBadRequest());
        
        mockMvc.perform(post("/api/v1/" + invalidEntity + "/cache/clear"))
                .andExpect(status().isBadRequest());
        
        mockMvc.perform(post("/api/v1/" + invalidEntity + "/cache/warm-up"))
                .andExpect(status().isBadRequest());
        
        mockMvc.perform(post("/api/v1/" + invalidEntity + "/cache/toggle")
                .param("enabled", "true"))
                .andExpect(status().isBadRequest());
        
        // Test concurrent operation endpoints
        mockMvc.perform(get("/api/v1/" + invalidEntity + "/concurrent/statistics"))
                .andExpect(status().isBadRequest());
        
        mockMvc.perform(get("/api/v1/" + invalidEntity + "/concurrent/status/key"))
                .andExpect(status().isBadRequest());
        
        mockMvc.perform(post("/api/v1/" + invalidEntity + "/concurrent/cancel/key"))
                .andExpect(status().isBadRequest());
    }
}