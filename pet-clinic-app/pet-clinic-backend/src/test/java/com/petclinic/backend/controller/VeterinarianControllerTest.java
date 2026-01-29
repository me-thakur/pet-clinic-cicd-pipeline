package com.petclinic.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.service.VeterinarianService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for VeterinarianController
 * Tests basic CRUD operations for veterinarian management
 */
@WebMvcTest(VeterinarianController.class)
@DisplayName("VeterinarianController Tests")
class VeterinarianControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private VeterinarianService veterinarianService;
    
    private Veterinarian testVeterinarian;
    
    @BeforeEach
    void setUp() {
        testVeterinarian = new Veterinarian();
        testVeterinarian.setId(1L);
        testVeterinarian.setFirstName("John");
        testVeterinarian.setLastName("Smith");
        testVeterinarian.setLicenseNumber("VET123456");
        testVeterinarian.setSpecialties("General Practice");
    }
    
    @Test
    @DisplayName("Should get all veterinarians")
    void shouldGetAllVeterinarians() throws Exception {
        // Given
        List<Veterinarian> veterinarians = Arrays.asList(testVeterinarian);
        when(veterinarianService.findAll()).thenReturn(veterinarians);
        
        // When & Then
        mockMvc.perform(get("/api/veterinarians"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].firstName").value("John"));
        
        verify(veterinarianService).findAll();
    }
    
    @Test
    @DisplayName("Should get veterinarian by ID")
    void shouldGetVeterinarianById() throws Exception {
        // Given
        when(veterinarianService.findById(1L)).thenReturn(Optional.of(testVeterinarian));
        
        // When & Then
        mockMvc.perform(get("/api/veterinarians/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.licenseNumber").value("VET123456"));
        
        verify(veterinarianService).findById(1L);
    }
    
    @Test
    @DisplayName("Should create veterinarian")
    void shouldCreateVeterinarian() throws Exception {
        // Given
        Veterinarian newVeterinarian = new Veterinarian();
        newVeterinarian.setFirstName("Alice");
        newVeterinarian.setLastName("Johnson");
        newVeterinarian.setLicenseNumber("VET555666");
        newVeterinarian.setSpecialties("Cardiology");
        
        Veterinarian savedVeterinarian = new Veterinarian();
        savedVeterinarian.setId(3L);
        savedVeterinarian.setFirstName("Alice");
        savedVeterinarian.setLastName("Johnson");
        savedVeterinarian.setLicenseNumber("VET555666");
        savedVeterinarian.setSpecialties("Cardiology");
        
        when(veterinarianService.create(any(Veterinarian.class))).thenReturn(savedVeterinarian);
        
        // When & Then
        mockMvc.perform(post("/api/veterinarians")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newVeterinarian)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.firstName").value("Alice"));
        
        verify(veterinarianService).create(any(Veterinarian.class));
    }
}