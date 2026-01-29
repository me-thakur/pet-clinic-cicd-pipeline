package com.petclinic.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petclinic.backend.model.Specialty;
import com.petclinic.backend.model.Veterinarian;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for VeterinarianController
 * Tests the complete flow with real database and services
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("VeterinarianController Integration Tests")
class VeterinarianControllerIntegrationTest {
    
    @Autowired
    private WebApplicationContext context;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private MockMvc mockMvc;
    
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should get all veterinarians")
    void shouldGetAllVeterinarians() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        
        mockMvc.perform(get("/api/veterinarians"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
    
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should create veterinarian")
    void shouldCreateVeterinarian() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        
        Veterinarian veterinarian = new Veterinarian();
        veterinarian.setFirstName("John");
        veterinarian.setLastName("Doe");
        veterinarian.setLicenseNumber("VET123456");
        veterinarian.setSpecialtySet(Set.of(Specialty.GENERAL_PRACTICE));
        
        mockMvc.perform(post("/api/veterinarians")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(veterinarian)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.licenseNumber").value("VET123456"));
    }
    
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should get veterinarian count")
    void shouldGetVeterinarianCount() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        
        mockMvc.perform(get("/api/veterinarians/count"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.count").isNumber());
    }
    
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should check health endpoint")
    void shouldCheckHealthEndpoint() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        
        mockMvc.perform(get("/api/veterinarians/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("VeterinarianController"));
    }
}