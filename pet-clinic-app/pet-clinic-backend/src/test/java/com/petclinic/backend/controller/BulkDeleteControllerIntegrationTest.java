package com.petclinic.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petclinic.backend.dto.BulkDeleteRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for bulk delete controller endpoints
 * Validates: Requirements 6.5 - Bulk delete backend endpoint with transaction support
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class BulkDeleteControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @Test
    void testBulkDeleteEndpointExists() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        BulkDeleteRequest request = new BulkDeleteRequest(Arrays.asList(1L, 2L, 3L), "visits");

        mockMvc.perform(delete("/api/v1/visits/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalRequested").value(3))
                .andExpect(jsonPath("$.success").exists());
    }

    @Test
    void testBulkDeleteWithInvalidEntityType() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        BulkDeleteRequest request = new BulkDeleteRequest(Arrays.asList(1L, 2L, 3L), "invalid");

        mockMvc.perform(delete("/api/v1/invalid/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testBulkProgressEndpointExists() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/api/v1/visits/bulk-progress/test-operation-123"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.operationId").value("test-operation-123"))
                .andExpect(jsonPath("$.entityType").value("visits"))
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void testBulkProgressWithInvalidEntityType() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/api/v1/invalid/bulk-progress/test-operation-123"))
                .andExpect(status().isBadRequest());
    }
}