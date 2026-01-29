package com.petclinic.backend.service.impl;

import com.petclinic.backend.config.DataSeedingConfig;
import com.petclinic.backend.model.*;
import com.petclinic.backend.repository.*;
import com.petclinic.backend.service.DataSeedingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementation of DataSeedingService
 * Handles all data seeding operations with proper error handling and logging
 * 
 * Validates: Requirements 8.3, 8.4
 */
@Service
public class DataSeedingServiceImpl implements DataSeedingService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSeedingServiceImpl.class);
    
    @Autowired
    private OwnerRepository ownerRepository;
    
    @Autowired
    private PetRepository petRepository;
    
    @Autowired
    private VeterinarianRepository veterinarianRepository;
    
    @Autowired
    private VisitRepository visitRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DataSeedingConfig config;
    
    private final Random random = new Random();
    
    @Override
    @Transactional
    public void seedAllData() throws Exception {
        try {
            logger.info("Starting comprehensive data seeding...");
            
            if (!isSeedingNeeded()) {
                logger.info("Data seeding not needed - data already exists or disabled");
                return;
            }
            
            // Seed data in dependency order
            List<Owner> owners = seedOwners();
            List<Veterinarian> veterinarians = seedVeterinarians();
            List<Pet> pets = seedPets(owners);
            seedVisits(pets, veterinarians);
            
            if (config.isSeedUsers()) {
                seedUsers();
            }
            
            logger.info("Data seeding completed successfully");
            
        } catch (Exception e) {
            logger.error("Critical error during data seeding: {}", e.getMessage(), e);
            throw new RuntimeException("Data seeding failed", e);
        }
    }
    
    @Override
    public boolean isSeedingNeeded() {
        if (!config.isEnabled()) {
            logger.debug("Data seeding is disabled in configuration");
            return false;
        }
        
        if (config.isSkipIfDataExists() && ownerRepository.count() > 0) {
            logger.debug("Data already exists and skip-if-data-exists is enabled");
            return false;
        }
        
        return true;
    }
    
    @Override
    @Transactional
    public void clearAllData() throws Exception {
        try {
            logger.warn("Clearing all seeded data - this operation cannot be undone");
            
            // Delete in reverse dependency order
            visitRepository.deleteAll();
            petRepository.deleteAll();
            veterinarianRepository.deleteAll();
            ownerRepository.deleteAll();
            userRepository.deleteAll();
            
            logger.info("All data cleared successfully");
            
        } catch (Exception e) {
            logger.error("Error clearing data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to clear data", e);
        }
    }
    
    @Override
    public DataSeedingStatistics getStatistics() {
        return new DataSeedingStatistics(
            ownerRepository.count(),
            petRepository.count(),
            veterinarianRepository.count(),
            visitRepository.count(),
            userRepository.count()
        );
    }
    
    /**
     * Seed sample owners with error handling
     */
    private List<Owner> seedOwners() throws Exception {
        try {
            logger.info("Seeding {} owners...", config.getOwnerCount());
            
            List<Owner> owners = new ArrayList<>();
            String[] firstNames = {"John", "Sarah", "Michael", "Emily", "David", "Lisa", "Robert", "Jennifer", 
                                 "William", "Amanda", "James", "Michelle", "Christopher", "Jessica", "Matthew"};
            String[] lastNames = {"Smith", "Johnson", "Brown", "Davis", "Wilson", "Miller", "Garcia", "Martinez", 
                                "Anderson", "Taylor", "Thomas", "Jackson", "White", "Harris", "Martin"};
            String[] cities = {"Springfield", "Riverside", "Hillside", "Oakville", "Maplewood"};
            String[] streets = {"Main St", "Oak Ave", "Pine Rd", "Elm St", "Maple Dr", "Cedar Ln", "Birch Way", 
                              "Spruce St", "Willow Ave", "Poplar Rd", "Ash Dr", "Hickory Ln", "Walnut St", "Cherry Ave"};
            
            for (int i = 0; i < Math.min(config.getOwnerCount(), firstNames.length); i++) {
                String firstName = firstNames[i];
                String lastName = lastNames[i % lastNames.length];
                String address = (100 + i * 10) + " " + streets[i % streets.length];
                String city = cities[i % cities.length];
                String telephone = String.format("555-%04d", 101 + i);
                String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@email.com";
                
                Owner owner = new Owner(firstName, lastName, address, city, telephone, email);
                owners.add(owner);
            }
            
            List<Owner> savedOwners = ownerRepository.saveAll(owners);
            logger.info("Successfully seeded {} owners", savedOwners.size());
            return savedOwners;
            
        } catch (Exception e) {
            logger.error("Error seeding owners: {}", e.getMessage(), e);
            throw new Exception("Failed to seed owners", e);
        }
    }
    
    /**
     * Seed sample veterinarians with specialties and error handling
     */
    private List<Veterinarian> seedVeterinarians() throws Exception {
        try {
            logger.info("Seeding {} veterinarians...", config.getVeterinarianCount());
            
            List<Veterinarian> veterinarians = new ArrayList<>();
            
            // Veterinarian data with specialties
            Object[][] vetData = {
                {"Alice", "Smith", new Specialty[]{Specialty.GENERAL_PRACTICE}, "VET001234"},
                {"Robert", "Johnson", new Specialty[]{Specialty.GENERAL_PRACTICE}, "VET002345"},
                {"Maria", "Brown", new Specialty[]{Specialty.SURGERY, Specialty.EMERGENCY_MEDICINE}, "VET003456"},
                {"Thomas", "Davis", new Specialty[]{Specialty.CARDIOLOGY}, "VET004567"},
                {"Linda", "Wilson", new Specialty[]{Specialty.DERMATOLOGY}, "VET005678"},
                {"Kevin", "Miller", new Specialty[]{Specialty.ORTHOPEDICS, Specialty.SURGERY}, "VET006789"},
                {"Patricia", "Garcia", new Specialty[]{Specialty.DENTISTRY}, "VET007890"},
                {"Daniel", "Martinez", new Specialty[]{Specialty.OPHTHALMOLOGY}, "VET008901"},
                {"Susan", "Anderson", new Specialty[]{Specialty.INTERNAL_MEDICINE}, "VET009012"},
                {"Mark", "Taylor", new Specialty[]{Specialty.EMERGENCY, Specialty.EMERGENCY_MEDICINE}, "VET010123"}
            };
            
            for (int i = 0; i < Math.min(config.getVeterinarianCount(), vetData.length); i++) {
                Object[] data = vetData[i];
                String firstName = (String) data[0];
                String lastName = (String) data[1];
                Specialty[] specialties = (Specialty[]) data[2];
                String licenseNumber = (String) data[3];
                
                Veterinarian vet = new Veterinarian();
                vet.setFirstName(firstName);
                vet.setLastName(lastName);
                vet.setLicenseNumber(licenseNumber);
                
                // Set specialties
                Set<Specialty> specialtySet = new HashSet<>(Arrays.asList(specialties));
                vet.setSpecialtySet(specialtySet);
                
                // Also set the legacy specialties string for backward compatibility
                String specialtyString = Arrays.stream(specialties)
                        .map(Specialty::getDisplayName)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                vet.setSpecialties(specialtyString);
                
                veterinarians.add(vet);
            }
            
            List<Veterinarian> savedVeterinarians = veterinarianRepository.saveAll(veterinarians);
            logger.info("Successfully seeded {} veterinarians", savedVeterinarians.size());
            return savedVeterinarians;
            
        } catch (Exception e) {
            logger.error("Error seeding veterinarians: {}", e.getMessage(), e);
            throw new Exception("Failed to seed veterinarians", e);
        }
    }
    
    /**
     * Seed sample pets for owners with error handling
     */
    private List<Pet> seedPets(List<Owner> owners) throws Exception {
        try {
            logger.info("Seeding pets for {} owners...", owners.size());
            
            List<Pet> pets = new ArrayList<>();
            
            // Pet data with realistic names, species, breeds, and ages
            String[][] petData = {
                {"Buddy", "Dog", "Golden Retriever", "3"},
                {"Whiskers", "Cat", "Persian", "2"},
                {"Max", "Dog", "German Shepherd", "5"},
                {"Luna", "Cat", "Siamese", "1"},
                {"Charlie", "Dog", "Labrador", "4"},
                {"Mittens", "Cat", "Maine Coon", "6"},
                {"Rocky", "Dog", "Bulldog", "2"},
                {"Shadow", "Cat", "Black Shorthair", "3"},
                {"Bella", "Dog", "Poodle", "1"},
                {"Smokey", "Cat", "Russian Blue", "4"},
                {"Duke", "Dog", "Rottweiler", "7"},
                {"Princess", "Cat", "Ragdoll", "2"},
                {"Zeus", "Dog", "Great Dane", "3"},
                {"Cleo", "Cat", "Egyptian Mau", "5"},
                {"Ranger", "Dog", "Border Collie", "2"},
                {"Patches", "Cat", "Calico", "1"},
                {"Thor", "Dog", "Husky", "4"},
                {"Ginger", "Cat", "Orange Tabby", "3"},
                {"Scout", "Dog", "Beagle", "6"},
                {"Snowball", "Cat", "White Persian", "2"},
                {"Rex", "Dog", "Boxer", "5"},
                {"Midnight", "Cat", "Bombay", "1"},
                {"Ace", "Dog", "Australian Shepherd", "3"},
                {"Fluffy", "Cat", "Himalayan", "4"},
                {"Bear", "Dog", "Saint Bernard", "2"},
                {"Oreo", "Cat", "Tuxedo", "3"},
                {"Storm", "Dog", "Weimaraner", "1"},
                {"Pepper", "Cat", "Tortoiseshell", "5"},
                {"Hunter", "Dog", "Pointer", "4"},
                {"Angel", "Cat", "Birman", "2"}
            };
            
            int petIndex = 0;
            for (Owner owner : owners) {
                // Each owner gets 1 to maxPetsPerOwner pets
                int petCount = 1 + random.nextInt(config.getMaxPetsPerOwner());
                
                for (int i = 0; i < petCount && petIndex < petData.length; i++, petIndex++) {
                    String[] data = petData[petIndex];
                    
                    LocalDate birthDate = LocalDate.now().minusYears(Integer.parseInt(data[3]))
                            .minusMonths(random.nextInt(12))
                            .minusDays(random.nextInt(28));
                    
                    Pet pet = new Pet(data[0], data[1], data[2], birthDate, owner);
                    
                    // Add medical history if configured
                    if (config.isGenerateMedicalHistory() && random.nextBoolean()) {
                        pet.setMedicalHistory(generateMedicalHistory(data[1]));
                    }
                    
                    pets.add(pet);
                }
            }
            
            List<Pet> savedPets = petRepository.saveAll(pets);
            logger.info("Successfully seeded {} pets", savedPets.size());
            return savedPets;
            
        } catch (Exception e) {
            logger.error("Error seeding pets: {}", e.getMessage(), e);
            throw new Exception("Failed to seed pets", e);
        }
    }
    
    /**
     * Seed sample visits with error handling
     */
    private void seedVisits(List<Pet> pets, List<Veterinarian> veterinarians) throws Exception {
        try {
            logger.info("Seeding visits for {} pets...", pets.size());
            
            List<Visit> visits = new ArrayList<>();
            VisitType[] visitTypes = VisitType.values();
            
            // Create visits for the configured time range
            LocalDateTime startDate = LocalDateTime.now().minusMonths(config.getPastMonths());
            LocalDateTime endDate = LocalDateTime.now().plusMonths(config.getFutureMonths());
            
            for (Pet pet : pets) {
                // Each pet gets 2 to maxVisitsPerPet visits
                int visitCount = 2 + random.nextInt(config.getMaxVisitsPerPet() - 1);
                
                for (int i = 0; i < visitCount; i++) {
                    try {
                        LocalDateTime visitDate = generateRandomDateTime(startDate, endDate);
                        VisitType visitType = visitTypes[random.nextInt(visitTypes.length)];
                        Veterinarian veterinarian = selectAppropriateVeterinarian(veterinarians, visitType);
                        
                        Visit visit = new Visit(visitDate, visitType, pet);
                        visit.setVeterinarian(veterinarian);
                        visit.setDuration(visitType.getDefaultDurationMinutes());
                        
                        // Add details for past visits
                        if (visitDate.isBefore(LocalDateTime.now())) {
                            visit.setDiagnosis(generateDiagnosis(pet.getSpecies(), visitType));
                            visit.setTreatment(generateTreatment(visitType));
                            visit.setNotes(generateNotes(pet.getName(), visitType));
                            visit.setCost(generateCost(visitType));
                        }
                        
                        visits.add(visit);
                        
                    } catch (Exception e) {
                        logger.warn("Error creating visit for pet {}: {}", pet.getName(), e.getMessage());
                        // Continue with other visits
                    }
                }
            }
            
            visitRepository.saveAll(visits);
            logger.info("Successfully seeded {} visits", visits.size());
            
        } catch (Exception e) {
            logger.error("Error seeding visits: {}", e.getMessage(), e);
            throw new Exception("Failed to seed visits", e);
        }
    }
    
    /**
     * Seed sample users with error handling
     */
    private void seedUsers() throws Exception {
        try {
            logger.info("Seeding users...");
            
            if (userRepository.count() > 0) {
                logger.info("Users already exist, skipping user seeding");
                return;
            }
            
            List<User> users = Arrays.asList(
                new User("admin", "Admin123!", "admin@petclinic.com", "Admin", "User", Role.ADMIN),
                new User("vet1", "Vet123!", "vet1@petclinic.com", "Alice", "Smith", Role.VET),
                new User("vet2", "Vet123!", "vet2@petclinic.com", "Robert", "Johnson", Role.VET),
                new User("staff1", "Staff123!", "staff1@petclinic.com", "John", "Staff", Role.STAFF),
                new User("staff2", "Staff123!", "staff2@petclinic.com", "Jane", "Staff", Role.STAFF)
            );
            
            userRepository.saveAll(users);
            logger.info("Successfully seeded {} users", users.size());
            
        } catch (Exception e) {
            logger.error("Error seeding users: {}", e.getMessage(), e);
            throw new Exception("Failed to seed users", e);
        }
    }
    
    // Helper methods (same as in DataSeeder)
    private String generateMedicalHistory(String species) {
        String[] dogHistory = {
            "Vaccinated against rabies, distemper, and parvovirus",
            "Previous hip dysplasia screening - normal results",
            "Allergic to chicken-based foods",
            "History of ear infections, treated successfully",
            "Spayed/neutered, microchipped"
        };
        
        String[] catHistory = {
            "Indoor cat, vaccinated against FVRCP and rabies",
            "Declawed front paws (previous owner)",
            "History of urinary tract issues, managed with diet",
            "Spayed/neutered, microchipped",
            "Sensitive to certain flea treatments"
        };
        
        String[] history = species.equalsIgnoreCase("Dog") ? dogHistory : catHistory;
        return history[random.nextInt(history.length)];
    }
    
    private Veterinarian selectAppropriateVeterinarian(List<Veterinarian> veterinarians, VisitType visitType) {
        Specialty recommendedSpecialty = visitType.getRecommendedSpecialty();
        
        // Try to find a veterinarian with the recommended specialty
        List<Veterinarian> suitableVets = veterinarians.stream()
                .filter(vet -> vet.hasSpecialty(recommendedSpecialty))
                .collect(java.util.stream.Collectors.toList());
        
        if (!suitableVets.isEmpty()) {
            return suitableVets.get(random.nextInt(suitableVets.size()));
        }
        
        // Fallback to any veterinarian
        return veterinarians.get(random.nextInt(veterinarians.size()));
    }
    
    private LocalDateTime generateRandomDateTime(LocalDateTime start, LocalDateTime end) {
        long startEpoch = start.toEpochSecond(java.time.ZoneOffset.UTC);
        long endEpoch = end.toEpochSecond(java.time.ZoneOffset.UTC);
        long randomEpoch = startEpoch + (long) (random.nextDouble() * (endEpoch - startEpoch));
        
        LocalDateTime randomDateTime = LocalDateTime.ofEpochSecond(randomEpoch, 0, java.time.ZoneOffset.UTC);
        
        // Round to business hours (8 AM to 6 PM, Monday to Friday)
        randomDateTime = randomDateTime.withHour(8 + random.nextInt(10))
                .withMinute(random.nextInt(4) * 15)
                .withSecond(0)
                .withNano(0);
        
        return randomDateTime;
    }
    
    private String generateDiagnosis(String species, VisitType visitType) {
        Map<String, String[]> diagnoses = new HashMap<>();
        
        diagnoses.put("Dog_WELLNESS_EXAM", new String[]{
            "Healthy adult dog, no concerns noted",
            "Mild dental tartar, recommend cleaning",
            "Overweight, recommend diet adjustment",
            "Excellent health, continue current care"
        });
        
        diagnoses.put("Cat_WELLNESS_EXAM", new String[]{
            "Healthy adult cat, no concerns noted",
            "Mild gingivitis, monitor dental health",
            "Indoor cat in excellent condition",
            "Age-appropriate health status"
        });
        
        diagnoses.put("Dog_EMERGENCY", new String[]{
            "Gastric dilatation-volvulus (bloat)",
            "Acute lameness, possible fracture",
            "Severe allergic reaction",
            "Ingestion of foreign object"
        });
        
        diagnoses.put("Cat_EMERGENCY", new String[]{
            "Urinary blockage",
            "Respiratory distress",
            "Trauma from fall",
            "Acute poisoning"
        });
        
        String key = species + "_" + visitType.name();
        String[] options = diagnoses.get(key);
        
        if (options == null) {
            return "Routine " + visitType.getDisplayName().toLowerCase() + " - no abnormalities detected";
        }
        
        return options[random.nextInt(options.length)];
    }
    
    private String generateTreatment(VisitType visitType) {
        Map<VisitType, String[]> treatments = new HashMap<>();
        
        treatments.put(VisitType.WELLNESS_EXAM, new String[]{
            "Annual vaccinations administered",
            "Physical examination completed, no treatment needed",
            "Dental cleaning recommended",
            "Weight management plan discussed"
        });
        
        treatments.put(VisitType.VACCINATION, new String[]{
            "DHPP vaccination administered",
            "Rabies vaccination given",
            "FVRCP vaccination for cat",
            "Bordetella vaccination completed"
        });
        
        treatments.put(VisitType.EMERGENCY, new String[]{
            "Emergency surgery performed",
            "IV fluids and pain management",
            "Stabilization and monitoring",
            "Immediate intervention and referral"
        });
        
        treatments.put(VisitType.SURGERY, new String[]{
            "Surgical procedure completed successfully",
            "Post-operative care instructions provided",
            "Sutures placed, follow-up scheduled",
            "Anesthesia recovery monitored"
        });
        
        String[] options = treatments.get(visitType);
        if (options == null) {
            return "Standard " + visitType.getDisplayName().toLowerCase() + " treatment provided";
        }
        
        return options[random.nextInt(options.length)];
    }
    
    private String generateNotes(String petName, VisitType visitType) {
        String[] notes = {
            petName + " was cooperative during examination",
            "Owner reports good appetite and normal behavior",
            "Recommend follow-up in 6 months",
            "Pet responded well to treatment",
            "Owner educated on home care instructions",
            "No adverse reactions observed",
            "Continue current medications as prescribed"
        };
        
        return notes[random.nextInt(notes.length)];
    }
    
    private BigDecimal generateCost(VisitType visitType) {
        Map<VisitType, BigDecimal[]> costRanges = new HashMap<>();
        
        costRanges.put(VisitType.WELLNESS_EXAM, new BigDecimal[]{new BigDecimal("75"), new BigDecimal("125")});
        costRanges.put(VisitType.VACCINATION, new BigDecimal[]{new BigDecimal("25"), new BigDecimal("50")});
        costRanges.put(VisitType.EMERGENCY, new BigDecimal[]{new BigDecimal("200"), new BigDecimal("800")});
        costRanges.put(VisitType.SURGERY, new BigDecimal[]{new BigDecimal("500"), new BigDecimal("2000")});
        costRanges.put(VisitType.DENTAL_CLEANING, new BigDecimal[]{new BigDecimal("300"), new BigDecimal("600")});
        costRanges.put(VisitType.FOLLOW_UP, new BigDecimal[]{new BigDecimal("50"), new BigDecimal("100")});
        
        BigDecimal[] range = costRanges.get(visitType);
        if (range == null) {
            range = new BigDecimal[]{new BigDecimal("50"), new BigDecimal("150")};
        }
        
        BigDecimal min = range[0];
        BigDecimal max = range[1];
        BigDecimal randomCost = min.add(max.subtract(min).multiply(BigDecimal.valueOf(random.nextDouble())));
        
        // Round to nearest dollar
        return randomCost.setScale(0, java.math.RoundingMode.HALF_UP);
    }
}