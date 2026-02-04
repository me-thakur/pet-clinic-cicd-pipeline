package com.petclinic.frontend.controller;

import com.petclinic.frontend.model.Pet;
import com.petclinic.frontend.model.PetWithOwnerInfo;
import com.petclinic.frontend.service.PetService;
import com.petclinic.frontend.service.OwnerService;
import com.petclinic.frontend.service.SeniorPetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for Pet View Modes functionality
 * Tests the implementation of Task 11.3: Update pet page frontend with working view toggles
 * 
 * Validates: Requirements 11.1, 11.2, 11.3, 11.5
 */
class PetViewModesTest {

    @Mock
    private PetService petService;
    
    @Mock
    private OwnerService ownerService;
    
    @Mock
    private SeniorPetService seniorPetService;
    
    @Mock
    private Model model;
    
    private PetController petController;
    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        petController = new PetController(petService, ownerService, seniorPetService);
        mockMvc = MockMvcBuilders.standaloneSetup(petController).build();
    }
    
    @Test
    @DisplayName("Standard view should display basic pet information - Requirement 11.1")
    void testStandardViewDisplaysBasicPetInformation() {
        // Arrange
        List<Pet> pets = Arrays.asList(
            createTestPet(1L, "Buddy", "Dog", "Golden Retriever"),
            createTestPet(2L, "Whiskers", "Cat", "Persian")
        );
        Page<Pet> petPage = new PageImpl<>(pets, PageRequest.of(0, 10), pets.size());
        
        when(petService.getAllPets(any(Pageable.class))).thenReturn(Mono.just(petPage));
        
        // Act
        String viewName = petController.listPets(0, 10, model);
        
        // Assert
        assertEquals("pets/list", viewName);
        verify(model).addAttribute("pets", petPage);
        verify(model).addAttribute("enhancedView", false);
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", petPage.getTotalPages());
        verify(model).addAttribute("totalElements", petPage.getTotalElements());
    }
    
    @Test
    @DisplayName("Enhanced view should display additional owner information - Requirement 11.2")
    void testEnhancedViewDisplaysOwnerInformation() {
        // Arrange
        List<PetWithOwnerInfo> petsWithOwner = Arrays.asList(
            createTestPetWithOwnerInfo(1L, "Buddy", "Dog", "Golden Retriever", 1L, "John", "Doe"),
            createTestPetWithOwnerInfo(2L, "Whiskers", "Cat", "Persian", 2L, "Jane", "Smith")
        );
        Page<PetWithOwnerInfo> petPage = new PageImpl<>(petsWithOwner, PageRequest.of(0, 10), petsWithOwner.size());
        
        when(petService.getAllPetsWithOwnerInfo(any(Pageable.class))).thenReturn(Mono.just(petPage));
        
        // Act
        String viewName = petController.listPetsEnhanced(0, 10, model);
        
        // Assert
        assertEquals("pets/list", viewName);
        verify(model).addAttribute("pets", petPage);
        verify(model).addAttribute("enhancedView", true);
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", petPage.getTotalPages());
        verify(model).addAttribute("totalElements", petPage.getTotalElements());
    }
    
    @Test
    @DisplayName("Enhanced view should fallback to standard view on error - Requirement 11.4")
    void testEnhancedViewFallbackOnError() {
        // Arrange
        List<Pet> pets = Arrays.asList(
            createTestPet(1L, "Buddy", "Dog", "Golden Retriever")
        );
        Page<Pet> petPage = new PageImpl<>(pets, PageRequest.of(0, 10), pets.size());
        
        when(petService.getAllPetsWithOwnerInfo(any(Pageable.class)))
            .thenReturn(Mono.error(new RuntimeException("Service unavailable")));
        when(petService.getAllPets(any(Pageable.class))).thenReturn(Mono.just(petPage));
        
        // Act
        String viewName = petController.listPetsEnhanced(0, 10, model);
        
        // Assert
        assertEquals("pets/list", viewName);
        verify(model).addAttribute("error", "Error loading enhanced pets view: Service unavailable");
        verify(model).addAttribute("pets", petPage);
        verify(model).addAttribute("enhancedView", false);
        verify(model).addAttribute("fallbackMessage", "Enhanced view temporarily unavailable. Showing standard view.");
    }
    
    @Test
    @DisplayName("Refresh owner info should return success on successful update")
    void testRefreshOwnerInfoSuccess() {
        // Arrange
        PetWithOwnerInfo refreshedPet = createTestPetWithOwnerInfo(1L, "Buddy", "Dog", "Golden Retriever", 1L, "John", "Doe");
        when(petService.refreshPetOwnerInfo(1L)).thenReturn(Mono.just(refreshedPet));
        
        // Act
        String result = petController.refreshPetOwnerInfo(1L);
        
        // Assert
        assertEquals("success", result);
        verify(petService).refreshPetOwnerInfo(1L);
    }
    
    @Test
    @DisplayName("Refresh owner info should return error message on failure")
    void testRefreshOwnerInfoFailure() {
        // Arrange
        when(petService.refreshPetOwnerInfo(1L))
            .thenReturn(Mono.error(new RuntimeException("Service unavailable")));
        
        // Act
        String result = petController.refreshPetOwnerInfo(1L);
        
        // Assert
        assertTrue(result.startsWith("error:"));
        assertTrue(result.contains("Service unavailable"));
        verify(petService).refreshPetOwnerInfo(1L);
    }
    
    @Test
    @DisplayName("Standard view endpoint should be accessible via HTTP GET")
    void testStandardViewEndpointAccessible() throws Exception {
        // Arrange
        List<Pet> pets = Arrays.asList(createTestPet(1L, "Buddy", "Dog", "Golden Retriever"));
        Page<Pet> petPage = new PageImpl<>(pets, PageRequest.of(0, 10), pets.size());
        when(petService.getAllPets(any(Pageable.class))).thenReturn(Mono.just(petPage));
        
        // Act & Assert
        mockMvc.perform(get("/pets"))
                .andExpect(status().isOk())
                .andExpect(view().name("pets/list"))
                .andExpect(model().attribute("enhancedView", false));
    }
    
    @Test
    @DisplayName("Enhanced view endpoint should be accessible via HTTP GET")
    void testEnhancedViewEndpointAccessible() throws Exception {
        // Arrange
        List<PetWithOwnerInfo> pets = Arrays.asList(
            createTestPetWithOwnerInfo(1L, "Buddy", "Dog", "Golden Retriever", 1L, "John", "Doe")
        );
        Page<PetWithOwnerInfo> petPage = new PageImpl<>(pets, PageRequest.of(0, 10), pets.size());
        when(petService.getAllPetsWithOwnerInfo(any(Pageable.class))).thenReturn(Mono.just(petPage));
        
        // Act & Assert
        mockMvc.perform(get("/pets/enhanced"))
                .andExpect(status().isOk())
                .andExpect(view().name("pets/list"))
                .andExpect(model().attribute("enhancedView", true));
    }
    
    @Test
    @DisplayName("Refresh owner info endpoint should be accessible via HTTP POST")
    void testRefreshOwnerInfoEndpointAccessible() throws Exception {
        // Arrange
        PetWithOwnerInfo refreshedPet = createTestPetWithOwnerInfo(1L, "Buddy", "Dog", "Golden Retriever", 1L, "John", "Doe");
        when(petService.refreshPetOwnerInfo(1L)).thenReturn(Mono.just(refreshedPet));
        
        // Act & Assert
        mockMvc.perform(post("/pets/1/refresh-owner-info"))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
    }
    
    @Test
    @DisplayName("View modes should handle pagination parameters correctly - Requirement 11.3")
    void testViewModesHandlePaginationCorrectly() {
        // Arrange
        List<Pet> pets = Arrays.asList(createTestPet(1L, "Buddy", "Dog", "Golden Retriever"));
        Page<Pet> petPage = new PageImpl<>(pets, PageRequest.of(2, 5), 20);
        when(petService.getAllPets(any(Pageable.class))).thenReturn(Mono.just(petPage));
        
        // Act
        String viewName = petController.listPets(2, 5, model);
        
        // Assert
        assertEquals("pets/list", viewName);
        verify(model).addAttribute("currentPage", 2);
        verify(model).addAttribute("totalPages", 4); // 20 total / 5 per page = 4 pages
        verify(model).addAttribute("totalElements", 20L);
        
        // Verify the correct pageable was passed to the service
        verify(petService).getAllPets(argThat(pageable -> 
            pageable.getPageNumber() == 2 && pageable.getPageSize() == 5
        ));
    }
    
    // Helper methods for creating test data
    
    private Pet createTestPet(Long id, String name, String species, String breed) {
        Pet pet = new Pet();
        pet.setId(id);
        pet.setName(name);
        pet.setSpecies(species);
        pet.setBreed(breed);
        return pet;
    }
    
    private PetWithOwnerInfo createTestPetWithOwnerInfo(Long petId, String petName, String species, String breed,
                                                       Long ownerId, String ownerFirstName, String ownerLastName) {
        PetWithOwnerInfo pet = new PetWithOwnerInfo();
        pet.setPetId(petId);
        pet.setPetName(petName);
        pet.setPetSpecies(species);
        pet.setPetBreed(breed);
        pet.setOwnerId(ownerId);
        pet.setOwnerFirstName(ownerFirstName);
        pet.setOwnerLastName(ownerLastName);
        pet.setOwnerEmail("test@example.com");
        pet.setOwnerMobileNumber("555-1234");
        return pet;
    }
}