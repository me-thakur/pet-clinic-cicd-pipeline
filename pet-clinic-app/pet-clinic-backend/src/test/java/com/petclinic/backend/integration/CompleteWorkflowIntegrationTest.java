package com.petclinic.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petclinic.backend.dto.AuthRequest;
import com.petclinic.backend.dto.AuthResponse;
import com.petclinic.backend.model.*;
import com.petclinic.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Complete Workflow Integration Test
 * 
 * This test validates complete end-to-end user workflows across the entire Pet Clinic system.
 * It tests realistic scenarios that users would perform, ensuring all components work together
 * correctly and data consistency is maintained across modules.
 * 
 * **Validates: Requirements All**
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CompleteWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private VeterinarianRepository veterinarianRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private String adminToken;
    private String vetToken;
    private String staffToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up data before each test
        visitRepository.deleteAll();
        petRepository.deleteAll();
        ownerRepository.deleteAll();
        veterinarianRepository.deleteAll();
        auditLogRepository.deleteAll();

        // Authenticate different user roles
        adminToken = authenticateUser("admin", "admin123");
        vetToken = authenticateUser("vet", "vet123");
        staffToken = authenticateUser("staff", "staff123");
    }

    @Test
    @Order(1)
    @Transactional
    void testCompleteNewPatientWorkflow() throws Exception {
        // **Scenario: A new pet owner calls to register their pet and schedule a checkup**
        
        // 1. Staff creates new owner record
        Owner owner = createTestOwner("Sarah", "Johnson", "456 Oak Ave", "Springfield", "555-9876");
        
        MvcResult ownerResult = mockMvc.perform(post("/api/owners")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(owner)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Sarah"))
                .andExpect(jsonPath("$.lastName").value("Johnson"))
                .andReturn();

        Owner createdOwner = objectMapper.readValue(
                ownerResult.getResponse().getContentAsString(), Owner.class);

        // 2. Staff adds pet information
        Pet pet = createTestPet("Luna", "Cat", "Persian", LocalDate.of(2021, 3, 10), createdOwner);
        
        MvcResult petResult = mockMvc.perform(post("/api/pets")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pet)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Luna"))
                .andExpect(jsonPath("$.species").value("Cat"))
                .andReturn();

        Pet createdPet = objectMapper.readValue(
                petResult.getResponse().getContentAsString(), Pet.class);

        // 3. Staff searches for available veterinarians with appropriate specialty
        mockMvc.perform(get("/api/veterinarians/available")
                .header("Authorization", staffToken)
                .param("specialty", "GENERAL_PRACTICE")
                .param("date", LocalDateTime.now().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 4. Create veterinarian for the test
        Veterinarian vet = createTestVeterinarian("Dr. Emily", "Brown", "VET789", Set.of(Specialty.GENERAL_PRACTICE));
        
        MvcResult vetResult = mockMvc.perform(post("/api/veterinarians")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(vet)))
                .andExpect(status().isCreated())
                .andReturn();

        Veterinarian createdVet = objectMapper.readValue(
                vetResult.getResponse().getContentAsString(), Veterinarian.class);

        // 5. Staff schedules initial checkup visit
        Visit visit = createTestVisit(LocalDateTime.now().plusDays(1), VisitType.WELLNESS_EXAM, createdPet, createdVet);
        
        MvcResult visitResult = mockMvc.perform(post("/api/visits")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(visit)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visitType").value("CHECKUP"))
                .andReturn();

        Visit createdVisit = objectMapper.readValue(
                visitResult.getResponse().getContentAsString(), Visit.class);

        // 6. Verify data consistency across all entities
        // Check that owner has the pet associated
        mockMvc.perform(get("/api/owners/{id}/pets", createdOwner.getId())
                .header("Authorization", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Luna"));

        // Check that pet has the visit associated
        mockMvc.perform(get("/api/pets/{id}/visits", createdPet.getId())
                .header("Authorization", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].visitType").value("CHECKUP"));

        // Check that veterinarian has the visit associated
        mockMvc.perform(get("/api/veterinarians/{id}/visits", createdVet.getId())
                .header("Authorization", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].visitType").value("CHECKUP"));

        // 7. Verify audit trail was created for all operations
        mockMvc.perform(get("/api/admin/audit-logs")
                .header("Authorization", adminToken)
                .param("entityType", "Owner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[?(@.action == 'CREATE')]").exists());
    }

    @Test
    @Order(2)
    @Transactional
    void testVisitCompletionWorkflow() throws Exception {
        // **Scenario: Veterinarian completes a scheduled visit and updates medical records**
        
        // Setup: Create owner, pet, vet, and scheduled visit
        Owner owner = createAndSaveOwner("Mike", "Davis", "789 Pine St", "Springfield", "555-4567");
        Pet pet = createAndSavePet("Rex", "Dog", "German Shepherd", LocalDate.of(2019, 8, 20), owner);
        Veterinarian vet = createAndSaveVeterinarian("Dr. Robert", "Wilson", "VET456", Set.of(Specialty.SURGERY));
        Visit scheduledVisit = createAndSaveVisit(LocalDateTime.now().minusHours(1), VisitType.SURGERY, pet, vet);

        // 1. Veterinarian retrieves their scheduled visits for today
        mockMvc.perform(get("/api/veterinarians/{id}/visits", vet.getId())
                .header("Authorization", vetToken)
                .param("date", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].visitType").value("SURGERY"));

        // 2. Veterinarian completes the visit with diagnosis and treatment
        Visit completedVisit = new Visit();
        completedVisit.setId(scheduledVisit.getId());
        completedVisit.setVisitDate(scheduledVisit.getVisitDate());
        completedVisit.setVisitType(scheduledVisit.getVisitType());
        completedVisit.setPet(scheduledVisit.getPet());
        completedVisit.setVeterinarian(scheduledVisit.getVeterinarian());
        completedVisit.setDiagnosis("Successful ACL repair surgery");
        completedVisit.setTreatment("Surgical repair of anterior cruciate ligament");
        completedVisit.setNotes("Patient responded well to anesthesia. Post-op recovery normal. Prescribed pain medication and restricted activity for 6 weeks.");

        mockMvc.perform(put("/api/visits/{id}", scheduledVisit.getId())
                .header("Authorization", vetToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(completedVisit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Successful ACL repair surgery"))
                .andExpect(jsonPath("$.treatment").value("Surgical repair of anterior cruciate ligament"));

        // 3. Verify pet's medical history is updated
        mockMvc.perform(get("/api/pets/{id}/visits", pet.getId())
                .header("Authorization", vetToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diagnosis").value("Successful ACL repair surgery"));

        // 4. Schedule follow-up visit
        Visit followUpVisit = createTestVisit(LocalDateTime.now().plusWeeks(2), VisitType.WELLNESS_EXAM, pet, vet);
        followUpVisit.setNotes("Follow-up for ACL surgery recovery check");

        mockMvc.perform(post("/api/visits")
                .header("Authorization", vetToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(followUpVisit)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notes").value("Follow-up for ACL surgery recovery check"));

        // 5. Verify visit statistics are updated
        mockMvc.perform(get("/api/reports/visits")
                .header("Authorization", adminToken)
                .param("startDate", LocalDate.now().minusDays(1).toString())
                .param("endDate", LocalDate.now().plusDays(1).toString())
                .param("veterinarianId", vet.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisits").value(2)); // Original + follow-up
    }

    @Test
    @Order(3)
    @Transactional
    void testMultiPetOwnerWorkflow() throws Exception {
        // **Scenario: Owner with multiple pets schedules visits for different pets**
        
        // 1. Create owner with multiple pets
        Owner owner = createAndSaveOwner("Jennifer", "Martinez", "321 Elm St", "Springfield", "555-7890");
        Pet dog = createAndSavePet("Buddy", "Dog", "Labrador", LocalDate.of(2018, 12, 5), owner);
        Pet cat = createAndSavePet("Whiskers", "Cat", "Siamese", LocalDate.of(2020, 6, 15), owner);
        Pet bird = createAndSavePet("Tweety", "Bird", "Canary", LocalDate.of(2021, 9, 3), owner);

        // 2. Create veterinarians with different specialties
        Veterinarian generalVet = createAndSaveVeterinarian("Dr. Lisa", "Anderson", "VET111", Set.of(Specialty.GENERAL_PRACTICE));
        Veterinarian exoticVet = createAndSaveVeterinarian("Dr. Mark", "Thompson", "VET222", Set.of(Specialty.EXOTIC_ANIMALS));

        // 3. Schedule different types of visits for each pet
        Visit dogCheckup = createTestVisit(LocalDateTime.now().plusDays(1), VisitType.WELLNESS_EXAM, dog, generalVet);
        Visit catVaccination = createTestVisit(LocalDateTime.now().plusDays(2), VisitType.VACCINATION, cat, generalVet);
        Visit birdExam = createTestVisit(LocalDateTime.now().plusDays(3), VisitType.WELLNESS_EXAM, bird, exoticVet);

        // Schedule all visits
        mockMvc.perform(post("/api/visits")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dogCheckup)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/visits")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(catVaccination)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/visits")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(birdExam)))
                .andExpect(status().isCreated());

        // 4. Verify owner can see all their pets and their visits
        mockMvc.perform(get("/api/owners/{id}/pets", owner.getId())
                .header("Authorization", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));

        // 5. Test global search finds all pets for this owner
        mockMvc.perform(get("/api/search/global")
                .header("Authorization", staffToken)
                .param("query", "Jennifer Martinez"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owners[0].firstName").value("Jennifer"))
                .andExpect(jsonPath("$.pets").isArray())
                .andExpect(jsonPath("$.pets.length()").value(3));

        // 6. Test filtering visits by owner
        mockMvc.perform(get("/api/filters/visits")
                .header("Authorization", staffToken)
                .param("ownerId", owner.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(3));

        // 7. Generate owner-specific report
        mockMvc.perform(get("/api/reports/owner-summary")
                .header("Authorization", staffToken)
                .param("ownerId", owner.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerName").value("Jennifer Martinez"))
                .andExpect(jsonPath("$.totalPets").value(3))
                .andExpect(jsonPath("$.upcomingVisits").value(3));
    }

    @Test
    @Order(4)
    @Transactional
    void testVeterinarianScheduleManagementWorkflow() throws Exception {
        // **Scenario: Veterinarian manages their daily schedule and handles conflicts**
        
        // Setup veterinarian and pets
        Veterinarian vet = createAndSaveVeterinarian("Dr. Sarah", "Johnson", "VET333", Set.of(Specialty.GENERAL_PRACTICE, Specialty.SURGERY));
        Owner owner1 = createAndSaveOwner("Tom", "Wilson", "111 First St", "Springfield", "555-1111");
        Owner owner2 = createAndSaveOwner("Amy", "Brown", "222 Second St", "Springfield", "555-2222");
        Pet pet1 = createAndSavePet("Max", "Dog", "Beagle", LocalDate.of(2019, 4, 12), owner1);
        Pet pet2 = createAndSavePet("Bella", "Cat", "Maine Coon", LocalDate.of(2020, 11, 8), owner2);

        LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

        // 1. Schedule first appointment
        Visit visit1 = createTestVisit(appointmentTime, VisitType.WELLNESS_EXAM, pet1, vet);
        
        mockMvc.perform(post("/api/visits")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(visit1)))
                .andExpect(status().isCreated());

        // 2. Try to schedule conflicting appointment (should fail)
        Visit conflictingVisit = createTestVisit(appointmentTime, VisitType.SURGERY, pet2, vet);
        
        mockMvc.perform(post("/api/visits")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(conflictingVisit)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SCHEDULING_CONFLICT"));

        // 3. Check veterinarian availability
        mockMvc.perform(get("/api/veterinarians/{id}/availability", vet.getId())
                .header("Authorization", staffToken)
                .param("date", appointmentTime.toLocalDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSlots").isArray());

        // 4. Schedule second appointment at different time
        Visit visit2 = createTestVisit(appointmentTime.plusHours(2), VisitType.SURGERY, pet2, vet);
        
        mockMvc.perform(post("/api/visits")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(visit2)))
                .andExpect(status().isCreated());

        // 5. Veterinarian views their daily schedule
        mockMvc.perform(get("/api/veterinarians/{id}/schedule", vet.getId())
                .header("Authorization", vetToken)
                .param("date", appointmentTime.toLocalDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visits").isArray())
                .andExpect(jsonPath("$.visits.length()").value(2));

        // 6. Test rescheduling a visit
        visit1.setVisitDate(appointmentTime.plusHours(4));
        
        mockMvc.perform(put("/api/visits/{id}/reschedule", visit1.getId())
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(visit1)))
                .andExpect(status().isOk());

        // 7. Verify schedule is updated
        mockMvc.perform(get("/api/veterinarians/{id}/schedule", vet.getId())
                .header("Authorization", vetToken)
                .param("date", appointmentTime.toLocalDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visits[?(@.visitType == 'CHECKUP')].visitDate").value(appointmentTime.plusHours(4).toString()));
    }

    @Test
    @Order(5)
    @Transactional
    void testReportingAndAnalyticsWorkflow() throws Exception {
        // **Scenario: Clinic manager generates various reports for business analysis**
        
        // Setup test data with multiple visits across different time periods
        setupReportingTestData();

        // 1. Generate daily visit report
        mockMvc.perform(get("/api/reports/visits")
                .header("Authorization", adminToken)
                .param("startDate", LocalDate.now().toString())
                .param("endDate", LocalDate.now().toString())
                .param("groupBy", "veterinarian"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisits").exists())
                .andExpect(jsonPath("$.visitsByVeterinarian").isArray());

        // 2. Generate monthly revenue report
        mockMvc.perform(get("/api/reports/revenue")
                .header("Authorization", adminToken)
                .param("startDate", LocalDate.now().minusMonths(1).toString())
                .param("endDate", LocalDate.now().toString())
                .param("format", "summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").exists())
                .andExpect(jsonPath("$.revenueByService").isArray());

        // 3. Export report to PDF
        mockMvc.perform(post("/api/reports/export")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reportType\":\"visits\",\"format\":\"PDF\",\"startDate\":\"" + LocalDate.now().minusWeeks(1) + "\",\"endDate\":\"" + LocalDate.now() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));

        // 4. Export report to CSV
        mockMvc.perform(post("/api/reports/export")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reportType\":\"revenue\",\"format\":\"CSV\",\"startDate\":\"" + LocalDate.now().minusMonths(1) + "\",\"endDate\":\"" + LocalDate.now() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"));

        // 5. Get dashboard metrics
        mockMvc.perform(get("/api/dashboard/metrics")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPets").exists())
                .andExpect(jsonPath("$.totalOwners").exists())
                .andExpect(jsonPath("$.totalVeterinarians").exists())
                .andExpect(jsonPath("$.todayVisits").exists())
                .andExpect(jsonPath("$.upcomingVisits").exists());

        // 6. Get veterinarian utilization report
        mockMvc.perform(get("/api/reports/veterinarian-utilization")
                .header("Authorization", adminToken)
                .param("period", "month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utilizationByVeterinarian").isArray());

        // 7. Test report filtering and pagination
        mockMvc.perform(get("/api/reports/visits")
                .header("Authorization", adminToken)
                .param("startDate", LocalDate.now().minusWeeks(2).toString())
                .param("endDate", LocalDate.now().toString())
                .param("species", "Dog")
                .param("visitType", "CHECKUP")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").exists());
    }

    @Test
    @Order(6)
    @Transactional
    void testDataConsistencyAndIntegrityWorkflow() throws Exception {
        // **Scenario: Test data consistency across all operations and referential integrity**
        
        // 1. Create complete data set
        Owner owner = createAndSaveOwner("David", "Lee", "555 Oak St", "Springfield", "555-5555");
        Pet pet = createAndSavePet("Charlie", "Dog", "Golden Retriever", LocalDate.of(2020, 1, 15), owner);
        Veterinarian vet = createAndSaveVeterinarian("Dr. Anna", "Garcia", "VET444", Set.of(Specialty.GENERAL_PRACTICE));
        Visit visit = createAndSaveVisit(LocalDateTime.now().plusDays(1), VisitType.WELLNESS_EXAM, pet, vet);

        // 2. Test cascading updates
        owner.setTelephone("555-9999");
        mockMvc.perform(put("/api/owners/{id}", owner.getId())
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(owner)))
                .andExpect(status().isOk());

        // Verify pet still references updated owner
        mockMvc.perform(get("/api/pets/{id}", pet.getId())
                .header("Authorization", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner.telephone").value("555-9999"));

        // 3. Test referential integrity - cannot delete owner with pets
        mockMvc.perform(delete("/api/owners/{id}", owner.getId())
                .header("Authorization", adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REFERENTIAL_INTEGRITY_VIOLATION"));

        // 4. Test referential integrity - cannot delete pet with visits
        mockMvc.perform(delete("/api/pets/{id}", pet.getId())
                .header("Authorization", adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REFERENTIAL_INTEGRITY_VIOLATION"));

        // 5. Test referential integrity - cannot delete veterinarian with visits
        mockMvc.perform(delete("/api/veterinarians/{id}", vet.getId())
                .header("Authorization", adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REFERENTIAL_INTEGRITY_VIOLATION"));

        // 6. Test proper deletion order
        // First delete visit
        mockMvc.perform(delete("/api/visits/{id}", visit.getId())
                .header("Authorization", adminToken))
                .andExpect(status().isNoContent());

        // Then delete pet
        mockMvc.perform(delete("/api/pets/{id}", pet.getId())
                .header("Authorization", adminToken))
                .andExpect(status().isNoContent());

        // Finally delete owner
        mockMvc.perform(delete("/api/owners/{id}", owner.getId())
                .header("Authorization", adminToken))
                .andExpect(status().isNoContent());

        // Verify all related data is cleaned up
        mockMvc.perform(get("/api/owners/{id}", owner.getId())
                .header("Authorization", staffToken))
                .andExpect(status().isNotFound());

        // 7. Verify audit logs captured all operations
        mockMvc.perform(get("/api/admin/audit-logs")
                .header("Authorization", adminToken)
                .param("entityId", owner.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.action == 'CREATE')]").exists())
                .andExpect(jsonPath("$.content[?(@.action == 'UPDATE')]").exists())
                .andExpect(jsonPath("$.content[?(@.action == 'DELETE')]").exists());
    }

    // Helper methods for creating test data
    private String authenticateUser(String username, String password) throws Exception {
        AuthRequest authRequest = new AuthRequest(username, password);
        
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return "Bearer " + authResponse.getToken();
    }

    private Owner createTestOwner(String firstName, String lastName, String address, String city, String telephone) {
        Owner owner = new Owner();
        owner.setFirstName(firstName);
        owner.setLastName(lastName);
        owner.setAddress(address);
        owner.setCity(city);
        owner.setTelephone(telephone);
        return owner;
    }

    private Pet createTestPet(String name, String species, String breed, LocalDate birthDate, Owner owner) {
        Pet pet = new Pet();
        pet.setName(name);
        pet.setSpecies(species);
        pet.setBreed(breed);
        pet.setBirthDate(birthDate);
        pet.setOwner(owner);
        return pet;
    }

    private Veterinarian createTestVeterinarian(String firstName, String lastName, String licenseNumber, Set<Specialty> specialties) {
        Veterinarian vet = new Veterinarian();
        vet.setFirstName(firstName);
        vet.setLastName(lastName);
        vet.setLicenseNumber(licenseNumber);
        vet.setSpecialtySet(specialties);
        return vet;
    }

    private Visit createTestVisit(LocalDateTime visitDate, VisitType visitType, Pet pet, Veterinarian veterinarian) {
        Visit visit = new Visit();
        visit.setVisitDate(visitDate);
        visit.setVisitType(visitType);
        visit.setPet(pet);
        visit.setVeterinarian(veterinarian);
        return visit;
    }

    private Owner createAndSaveOwner(String firstName, String lastName, String address, String city, String telephone) {
        Owner owner = createTestOwner(firstName, lastName, address, city, telephone);
        return ownerRepository.save(owner);
    }

    private Pet createAndSavePet(String name, String species, String breed, LocalDate birthDate, Owner owner) {
        Pet pet = createTestPet(name, species, breed, birthDate, owner);
        return petRepository.save(pet);
    }

    private Veterinarian createAndSaveVeterinarian(String firstName, String lastName, String licenseNumber, Set<Specialty> specialties) {
        Veterinarian vet = createTestVeterinarian(firstName, lastName, licenseNumber, specialties);
        return veterinarianRepository.save(vet);
    }

    private Visit createAndSaveVisit(LocalDateTime visitDate, VisitType visitType, Pet pet, Veterinarian veterinarian) {
        Visit visit = createTestVisit(visitDate, visitType, pet, veterinarian);
        return visitRepository.save(visit);
    }

    private void setupReportingTestData() {
        // Create test data for reporting scenarios
        Owner owner1 = createAndSaveOwner("Report", "Owner1", "123 Test St", "Springfield", "555-0001");
        Owner owner2 = createAndSaveOwner("Report", "Owner2", "456 Test Ave", "Springfield", "555-0002");
        
        Pet pet1 = createAndSavePet("ReportPet1", "Dog", "Labrador", LocalDate.of(2020, 1, 1), owner1);
        Pet pet2 = createAndSavePet("ReportPet2", "Cat", "Persian", LocalDate.of(2021, 1, 1), owner2);
        
        Veterinarian vet1 = createAndSaveVeterinarian("Dr. Report", "Vet1", "VETRPT1", Set.of(Specialty.GENERAL_PRACTICE));
        Veterinarian vet2 = createAndSaveVeterinarian("Dr. Report", "Vet2", "VETRPT2", Set.of(Specialty.SURGERY));
        
        // Create visits across different time periods
        createAndSaveVisit(LocalDateTime.now().minusDays(7), VisitType.WELLNESS_EXAM, pet1, vet1);
        createAndSaveVisit(LocalDateTime.now().minusDays(5), VisitType.VACCINATION, pet2, vet1);
        createAndSaveVisit(LocalDateTime.now().minusDays(3), VisitType.SURGERY, pet1, vet2);
        createAndSaveVisit(LocalDateTime.now().minusDays(1), VisitType.WELLNESS_EXAM, pet2, vet2);
    }
}