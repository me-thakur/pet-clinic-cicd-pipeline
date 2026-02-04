package com.petclinic.backend.integration;

import com.petclinic.backend.dto.PetWithOwnerInfo;
import com.petclinic.backend.service.EnhancedPetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Pet-Owner relationship display functionality
 * Tests the enhanced pet service with owner information
 * Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PetOwnerRelationshipIntegrationTest {

    @Autowired
    private EnhancedPetService enhancedPetService;

    @Test
    public void testFindPetsWithOwnerInfo() {
        // Test that the service can retrieve pets with owner information
        Page<PetWithOwnerInfo> pets = enhancedPetService.findPetsWithOwnerInfo(PageRequest.of(0, 10));
        
        assertNotNull(pets, "Pets page should not be null");
        assertTrue(pets.getTotalElements() >= 0, "Should return non-negative number of pets");
        
        // If there are pets, verify the structure
        if (!pets.isEmpty()) {
            PetWithOwnerInfo firstPet = pets.getContent().get(0);
            assertNotNull(firstPet.getPetId(), "Pet ID should not be null");
            assertNotNull(firstPet.getPetName(), "Pet name should not be null");
            
            // Test convenience methods
            assertNotNull(firstPet.getOwnerFullName(), "Owner full name method should work");
            assertNotNull(firstPet.getOwnerContactInfo(), "Owner contact info method should work");
            assertNotNull(firstPet.getOwnerFullAddress(), "Owner full address method should work");
        }
    }

    @Test
    public void testGetPetOwnershipStatistics() {
        // Test that ownership statistics can be retrieved
        long[] stats = enhancedPetService.getPetOwnershipStatistics();
        
        assertNotNull(stats, "Statistics should not be null");
        assertEquals(2, stats.length, "Should return array with 2 elements");
        assertTrue(stats[0] >= 0, "Pets with owners should be non-negative");
        assertTrue(stats[1] >= 0, "Pets without owners should be non-negative");
    }

    @Test
    public void testSearchPetsWithOwnerInfo() {
        // Test search functionality with owner information
        Page<PetWithOwnerInfo> searchResults = enhancedPetService.searchPetsWithOwnerInfo(
            "test", PageRequest.of(0, 10));
        
        assertNotNull(searchResults, "Search results should not be null");
        assertTrue(searchResults.getTotalElements() >= 0, "Should return non-negative number of results");
    }

    @Test
    public void testFindOrphanedPets() {
        // Test finding pets without owners
        Page<PetWithOwnerInfo> orphanedPets = enhancedPetService.findOrphanedPets(PageRequest.of(0, 10));
        
        assertNotNull(orphanedPets, "Orphaned pets page should not be null");
        assertTrue(orphanedPets.getTotalElements() >= 0, "Should return non-negative number of orphaned pets");
        
        // Verify that returned pets indeed have no owner
        for (PetWithOwnerInfo pet : orphanedPets.getContent()) {
            assertFalse(pet.hasOwner(), "Orphaned pets should not have owners");
            assertEquals("No Owner Assigned", pet.getOwnerFullName(), "Should show no owner assigned message");
        }
    }

    @Test
    public void testPetWithOwnerInfoConvenienceMethods() {
        // Test the convenience methods in PetWithOwnerInfo
        PetWithOwnerInfo pet = new PetWithOwnerInfo();
        
        // Test with no owner
        assertFalse(pet.hasOwner(), "Pet without owner ID should return false for hasOwner");
        assertEquals("No Owner Assigned", pet.getOwnerFullName(), "Should return no owner message");
        assertEquals("No Contact Info", pet.getOwnerContactInfo(), "Should return no contact info message");
        assertEquals("No Address", pet.getOwnerFullAddress(), "Should return no address message");
        
        // Test with owner information
        pet.setOwnerId(1L);
        pet.setOwnerFirstName("John");
        pet.setOwnerLastName("Doe");
        pet.setOwnerEmail("john.doe@example.com");
        pet.setOwnerMobileNumber("123-456-7890");
        pet.setOwnerAddress("123 Main St");
        pet.setOwnerCity("Anytown");
        pet.setOwnerState("CA");
        pet.setOwnerZipCode("12345");
        
        assertTrue(pet.hasOwner(), "Pet with owner ID should return true for hasOwner");
        assertEquals("John Doe", pet.getOwnerFullName(), "Should return full name");
        assertTrue(pet.getOwnerContactInfo().contains("john.doe@example.com"), "Should include email in contact info");
        assertTrue(pet.getOwnerFullAddress().contains("123 Main St"), "Should include address");
    }
}