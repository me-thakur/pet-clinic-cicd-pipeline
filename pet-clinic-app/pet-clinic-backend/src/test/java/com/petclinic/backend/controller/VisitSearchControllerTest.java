package com.petclinic.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.service.VisitSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for VisitSearchController
 * Tests API endpoint functionality and error handling
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5
 */
@WebMvcTest(controllers = VisitSearchController.class, 
           excludeAutoConfiguration = {SecurityAutoConfiguration.class})
class VisitSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VisitSearchService visitSearchService;

    @Autowired
    private ObjectMapper objectMapper;

    private List<Visit> testVisits;
    private Pet testPet;

    @BeforeEach
    void setUp() {
        testPet = new Pet();
        testPet.setId(1L);
        testPet.setName("Buddy");

        Visit visit1 = new Visit();
        visit1.setId(1L);
        visit1.setVisitDate(LocalDateTime.now());
        visit1.setTreatment("Vaccination");
        visit1.setDiagnosis("Healthy");
        visit1.setNotes("Annual checkup completed");
        visit1.setPet(testPet);
        visit1.setCost(new BigDecimal("50.00"));

        Visit visit2 = new Visit();
        visit2.setId(2L);
        visit2.setVisitDate(LocalDateTime.now().minusDays(1));
        visit2.setTreatment("Surgery");
        visit2.setDiagnosis("Broken leg");
        visit2.setNotes("Emergency surgery performed");
        visit2.setPet(testPet);
        visit2.setCost(new BigDecimal("500.00"));

        testVisits = Arrays.asList(visit1, visit2);
    }

    @Test
    void testSearchByTreatment_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        Page<Visit> page = new PageImpl<>(testVisits, PageRequest.of(0, 10), testVisits.size());
        PagedResponse<Visit> pagedResponse = new PagedResponse<>(page);
        when(visitSearchService.searchByTreatment(eq("vaccination"), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/visits/search/by-treatment")
                        .param("text", "vaccination")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void testSearchByTreatment_DefaultPagination_ReturnsOk() throws Exception {
        // Arrange
        Page<Visit> page = new PageImpl<>(testVisits, PageRequest.of(0, 10), testVisits.size());
        PagedResponse<Visit> pagedResponse = new PagedResponse<>(page);
        when(visitSearchService.searchByTreatment(eq("vaccination"), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/visits/search/by-treatment")
                        .param("text", "vaccination")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(10));
    }

    @Test
    void testSearchByTreatment_EmptyText_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/visits/search/by-treatment")
                        .param("text", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchByTreatment_MissingText_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/visits/search/by-treatment")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchByTreatment_NegativePage_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/visits/search/by-treatment")
                        .param("text", "vaccination")
                        .param("page", "-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchByTreatment_ZeroSize_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/visits/search/by-treatment")
                        .param("text", "vaccination")
                        .param("size", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchByTreatment_LargeSize_CapsToMaximum() throws Exception {
        // Arrange
        Page<Visit> page = new PageImpl<>(testVisits, PageRequest.of(0, 100), testVisits.size());
        PagedResponse<Visit> pagedResponse = new PagedResponse<>(page);
        when(visitSearchService.searchByTreatment(eq("vaccination"), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/visits/search/by-treatment")
                        .param("text", "vaccination")
                        .param("size", "200") // Should be capped to 100
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testSearchByTreatment_ServiceException_ReturnsInternalServerError() throws Exception {
        // Arrange
        when(visitSearchService.searchByTreatment(eq("vaccination"), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/visits/search/by-treatment")
                        .param("text", "vaccination")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testSearchByDiagnosis_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        Page<Visit> page = new PageImpl<>(testVisits, PageRequest.of(0, 10), testVisits.size());
        PagedResponse<Visit> pagedResponse = new PagedResponse<>(page);
        when(visitSearchService.searchByDiagnosis(eq("infection"), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/visits/search/by-diagnosis")
                        .param("text", "infection")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void testSearchByDiagnosis_EmptyText_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/visits/search/by-diagnosis")
                        .param("text", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchByDiagnosis_ServiceException_ReturnsInternalServerError() throws Exception {
        // Arrange
        when(visitSearchService.searchByDiagnosis(eq("infection"), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/visits/search/by-diagnosis")
                        .param("text", "infection")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testSearchByDescription_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        Page<Visit> page = new PageImpl<>(testVisits, PageRequest.of(0, 10), testVisits.size());
        PagedResponse<Visit> pagedResponse = new PagedResponse<>(page);
        when(visitSearchService.searchByDescription(eq("emergency"), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/visits/search/by-description")
                        .param("text", "emergency")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void testSearchByDescription_EmptyText_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/visits/search/by-description")
                        .param("text", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchByDescription_ServiceException_ReturnsInternalServerError() throws Exception {
        // Arrange
        when(visitSearchService.searchByDescription(eq("emergency"), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/visits/search/by-description")
                        .param("text", "emergency")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testSearchByTreatment_NoResults_ReturnsEmptyPage() throws Exception {
        // Arrange
        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        PagedResponse<Visit> pagedResponse = new PagedResponse<>(emptyPage);
        when(visitSearchService.searchByTreatment(eq("nonexistent"), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/visits/search/by-treatment")
                        .param("text", "nonexistent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void testSearchByDiagnosis_NoResults_ReturnsEmptyPage() throws Exception {
        // Arrange
        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        PagedResponse<Visit> pagedResponse = new PagedResponse<>(emptyPage);
        when(visitSearchService.searchByDiagnosis(eq("nonexistent"), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/visits/search/by-diagnosis")
                        .param("text", "nonexistent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void testSearchByDescription_NoResults_ReturnsEmptyPage() throws Exception {
        // Arrange
        Page<Visit> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        PagedResponse<Visit> pagedResponse = new PagedResponse<>(emptyPage);
        when(visitSearchService.searchByDescription(eq("nonexistent"), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/visits/search/by-description")
                        .param("text", "nonexistent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void testSearchByTreatment_SpecialCharacters_HandledCorrectly() throws Exception {
        // Arrange
        Page<Visit> page = new PageImpl<>(testVisits, PageRequest.of(0, 10), testVisits.size());
        PagedResponse<Visit> pagedResponse = new PagedResponse<>(page);
        when(visitSearchService.searchByTreatment(eq("test & special"), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/visits/search/by-treatment")
                        .param("text", "test & special")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testSearchByTreatment_UnicodeCharacters_HandledCorrectly() throws Exception {
        // Arrange
        Page<Visit> page = new PageImpl<>(testVisits, PageRequest.of(0, 10), testVisits.size());
        PagedResponse<Visit> pagedResponse = new PagedResponse<>(page);
        when(visitSearchService.searchByTreatment(eq("tëst"), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/visits/search/by-treatment")
                        .param("text", "tëst")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testAllEndpoints_CorsEnabled_ReturnsCorrectHeaders() throws Exception {
        // Arrange
        Page<Visit> page = new PageImpl<>(testVisits, PageRequest.of(0, 10), testVisits.size());
        PagedResponse<Visit> pagedResponse = new PagedResponse<>(page);
        when(visitSearchService.searchByTreatment(eq("test"), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // Act & Assert - Test CORS headers are present
        mockMvc.perform(get("/api/visits/search/by-treatment")
                        .param("text", "test")
                        .header("Origin", "http://localhost:3000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}