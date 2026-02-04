package com.petclinic.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.repository.OwnerRepository;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Visit Search endpoints
 * Tests the complete flow from HTTP request to database query
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(OrderAnnotation.class)
@ActiveProfiles("test")
@Transactional
class VisitSearchIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    private Pet testPet;
    private Visit testVisit1;
    private Visit testVisit2;
    private Visit testVisit3;

    @BeforeEach
    void setUp() {
        // Create test owner
        Owner testOwner = new Owner();
        testOwner.setFirstName("John");
        testOwner.setLastName("Doe");
        testOwner.setEmail("john.doe@example.com");
        testOwner.setTelephone("555-1234");
        testOwner.setAddress("123 Main St");
        testOwner.setCity("Anytown");
        testOwner.setZipCode("12345");
        testOwner = ownerRepository.save(testOwner);

        // Create test pet
        testPet = new Pet();
        testPet.setName("Buddy");
        testPet.setSpecies("Dog");
        testPet.setBreed("Golden Retriever");
        testPet.setBirthDate(LocalDate.of(2021, 1, 1));
        testPet.setOwner(testOwner);
        testPet = petRepository.save(testPet);

        // Create test visits with different treatments, diagnoses, and notes
        testVisit1 = new Visit();
        testVisit1.setVisitDate(LocalDateTime.now().minusDays(1));
        testVisit1.setTreatment("Vaccination");
        testVisit1.setDiagnosis("Healthy");
        testVisit1.setNotes("Annual vaccination completed successfully");
        testVisit1.setPet(testPet);
        testVisit1.setCost(new BigDecimal("50.00"));
        testVisit1 = visitRepository.save(testVisit1);

        testVisit2 = new Visit();
        testVisit2.setVisitDate(LocalDateTime.now().minusDays(2));
        testVisit2.setTreatment("Surgery");
        testVisit2.setDiagnosis("Broken leg");
        testVisit2.setNotes("Emergency surgery performed");
        testVisit2.setPet(testPet);
        testVisit2.setCost(new BigDecimal("500.00"));
        testVisit2 = visitRepository.save(testVisit2);

        testVisit3 = new Visit();
        testVisit3.setVisitDate(LocalDateTime.now().minusDays(3));
        testVisit3.setTreatment("Medication");
        testVisit3.setDiagnosis("Infection");
        testVisit3.setNotes("Prescribed antibiotics for infection");
        testVisit3.setPet(testPet);
        testVisit3.setCost(new BigDecimal("75.00"));
        testVisit3 = visitRepository.save(testVisit3);
    }

    @Test
    void testSearchByTreatment_ValidSearch_ReturnsMatchingVisits() {
        String url = "http://localhost:" + port + "/api/visits/search/by-treatment?text=vaccination&page=0&size=10";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("\"content\""));
        assertTrue(response.getBody().contains("\"page\""));
    }

    @Test
    void testSearchByTreatment_EmptyText_ReturnsBadRequest() {
        String url = "http://localhost:" + port + "/api/visits/search/by-treatment?text=";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testSearchByDiagnosis_ValidSearch_ReturnsOk() {
        String url = "http://localhost:" + port + "/api/visits/search/by-diagnosis?text=infection&page=0&size=10";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("\"content\""));
        assertTrue(response.getBody().contains("\"page\""));
    }

    @Test
    void testSearchByDescription_ValidSearch_ReturnsOk() {
        String url = "http://localhost:" + port + "/api/visits/search/by-description?text=emergency&page=0&size=10";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("\"content\""));
        assertTrue(response.getBody().contains("\"page\""));
    }

    @Test
    void testAllSearchEndpoints_ExistAndRespond() {
        // Test that all three search endpoints exist and respond correctly
        
        // Test by-treatment endpoint
        String treatmentUrl = "http://localhost:" + port + "/api/visits/search/by-treatment?text=test";
        ResponseEntity<String> treatmentResponse = restTemplate.getForEntity(treatmentUrl, String.class);
        assertEquals(HttpStatus.OK, treatmentResponse.getStatusCode());

        // Test by-diagnosis endpoint
        String diagnosisUrl = "http://localhost:" + port + "/api/visits/search/by-diagnosis?text=test";
        ResponseEntity<String> diagnosisResponse = restTemplate.getForEntity(diagnosisUrl, String.class);
        assertEquals(HttpStatus.OK, diagnosisResponse.getStatusCode());

        // Test by-description endpoint
        String descriptionUrl = "http://localhost:" + port + "/api/visits/search/by-description?text=test";
        ResponseEntity<String> descriptionResponse = restTemplate.getForEntity(descriptionUrl, String.class);
        assertEquals(HttpStatus.OK, descriptionResponse.getStatusCode());
    }
}