package com.petclinic.backend.controller;

import com.petclinic.backend.model.Owner;
import com.petclinic.backend.repository.OwnerRepository;
import com.petclinic.backend.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for OwnerController REST API
 * Tests HTTP endpoints and JSON serialization/deserialization
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4
 */
@WebMvcTest(OwnerController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class OwnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OwnerRepository ownerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void getOwnerById_ShouldReturnOwner_WhenOwnerExists() throws Exception {
        // Arrange
        Owner owner = new Owner();
        owner.setId(1L);
        owner.setFirstName("John");
        owner.setLastName("Doe");
        owner.setEmail("john.doe@example.com");
        owner.setTelephone("555-123-4567");

        when(ownerRepository.findById(1L)).thenReturn(Optional.of(owner));

        // Act & Assert
        mockMvc.perform(get("/api/owners/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.telephone").value("555-123-4567"));
    }

    @Test
    @WithMockUser
    void getOwnerById_ShouldReturnNotFound_WhenOwnerDoesNotExist() throws Exception {
        // Arrange
        when(ownerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/owners/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void createOwner_ShouldReturnCreatedOwner_WhenValidOwner() throws Exception {
        // Arrange
        Owner inputOwner = new Owner();
        inputOwner.setFirstName("Jane");
        inputOwner.setLastName("Smith");
        inputOwner.setEmail("jane.smith@example.com");
        inputOwner.setTelephone("555-987-6543");

        Owner savedOwner = new Owner();
        savedOwner.setId(2L);
        savedOwner.setFirstName("Jane");
        savedOwner.setLastName("Smith");
        savedOwner.setEmail("jane.smith@example.com");
        savedOwner.setTelephone("555-987-6543");

        when(ownerRepository.save(any(Owner.class))).thenReturn(savedOwner);

        // Act & Assert
        mockMvc.perform(post("/api/owners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputOwner)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.email").value("jane.smith@example.com"))
                .andExpect(jsonPath("$.telephone").value("555-987-6543"));
    }

    @Test
    @WithMockUser
    void updateOwner_ShouldReturnUpdatedOwner_WhenOwnerExists() throws Exception {
        // Arrange
        Owner existingOwner = new Owner();
        existingOwner.setId(1L);
        existingOwner.setFirstName("John");
        existingOwner.setLastName("Doe");
        existingOwner.setEmail("john.doe@example.com");

        Owner updateData = new Owner();
        updateData.setFirstName("John");
        updateData.setLastName("Smith");
        updateData.setEmail("john.smith@example.com");
        updateData.setTelephone("555-111-2222");

        Owner updatedOwner = new Owner();
        updatedOwner.setId(1L);
        updatedOwner.setFirstName("John");
        updatedOwner.setLastName("Smith");
        updatedOwner.setEmail("john.smith@example.com");
        updatedOwner.setTelephone("555-111-2222");

        when(ownerRepository.findById(1L)).thenReturn(Optional.of(existingOwner));
        when(ownerRepository.save(any(Owner.class))).thenReturn(updatedOwner);

        // Act & Assert
        mockMvc.perform(put("/api/owners/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.email").value("john.smith@example.com"))
                .andExpect(jsonPath("$.telephone").value("555-111-2222"));
    }

    @Test
    @WithMockUser
    void updateOwner_ShouldReturnNotFound_WhenOwnerDoesNotExist() throws Exception {
        // Arrange
        Owner updateData = new Owner();
        updateData.setFirstName("John");
        updateData.setLastName("Smith");

        when(ownerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(put("/api/owners/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deleteOwner_ShouldReturnNoContent_WhenOwnerExists() throws Exception {
        // Arrange
        when(ownerRepository.existsById(1L)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/owners/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void deleteOwner_ShouldReturnNotFound_WhenOwnerDoesNotExist() throws Exception {
        // Arrange
        when(ownerRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/api/owners/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void searchByFirstName_ShouldReturnOwners_WhenOwnersExist() throws Exception {
        // Arrange
        Owner owner1 = new Owner();
        owner1.setId(1L);
        owner1.setFirstName("John");
        owner1.setLastName("Doe");

        Owner owner2 = new Owner();
        owner2.setId(2L);
        owner2.setFirstName("Johnny");
        owner2.setLastName("Smith");

        when(ownerRepository.findByFirstNameContainingIgnoreCase("john"))
                .thenReturn(Arrays.asList(owner1, owner2));

        // Act & Assert
        mockMvc.perform(get("/api/owners/search/by-first-name")
                .param("name", "john"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[1].firstName").value("Johnny"));
    }

    @Test
    @WithMockUser
    void searchByEmail_ShouldReturnOwner_WhenOwnerExists() throws Exception {
        // Arrange
        Owner owner = new Owner();
        owner.setId(1L);
        owner.setFirstName("John");
        owner.setLastName("Doe");
        owner.setEmail("john.doe@example.com");

        when(ownerRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(owner));

        // Act & Assert
        mockMvc.perform(get("/api/owners/search/by-email")
                .param("email", "john.doe@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    @WithMockUser
    void searchByEmail_ShouldReturnNotFound_WhenOwnerDoesNotExist() throws Exception {
        // Arrange
        when(ownerRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/owners/search/by-email")
                .param("email", "nonexistent@example.com"))
                .andExpect(status().isNotFound());
    }
}