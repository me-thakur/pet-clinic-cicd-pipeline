package com.petclinic.backend.properties;

import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.PrimitiveGenerators;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * Base class for property-based tests
 * Provides common generators and test setup for Pet Clinic entities
 */
public abstract class PropertyTestBase {
    
    protected Random random;
    
    @BeforeEach
    void setUp() {
        random = new Random();
    }
    
    // Common generators for property-based testing
    
    protected Generator<String> validPetNames() {
        return () -> {
            StringBuilder name = new StringBuilder();
            int length = 1 + random.nextInt(49); // 1-50 characters
            for (int i = 0; i < length; i++) {
                if (i == 0 || random.nextBoolean()) {
                    name.append((char) ('A' + random.nextInt(26)));
                } else {
                    name.append((char) ('a' + random.nextInt(26)));
                }
                // Add space occasionally, but not at the beginning or end
                if (random.nextInt(15) == 0 && i > 0 && i < length - 2) {
                    name.append(' ');
                    i++; // Skip next iteration to avoid consecutive spaces
                }
            }
            String result = name.toString().trim();
            // Ensure we never return empty string
            return result.isEmpty() ? "Pet" + random.nextInt(1000) : result;
        };
    }
    
    protected Generator<String> validSpecies() {
        String[] species = {"Dog", "Cat", "Bird", "Rabbit", "Hamster", "Fish", "Reptile"};
        return () -> species[random.nextInt(species.length)];
    }
    
    protected Generator<String> validBreeds() {
        String[] breeds = {"Golden Retriever", "Siamese", "Parakeet", "Holland Lop", "Syrian", "Goldfish", "Bearded Dragon"};
        return () -> breeds[random.nextInt(breeds.length)];
    }
    
    protected Generator<LocalDate> validBirthDates() {
        return () -> {
            LocalDate now = LocalDate.now();
            LocalDate earliest = now.minusYears(20); // 20 years ago (well within 30 year limit)
            LocalDate latest = now.minusDays(1); // At least 1 day old
            long daysBetween = latest.toEpochDay() - earliest.toEpochDay();
            long randomDays = Math.abs(random.nextLong()) % (daysBetween + 1);
            return earliest.plusDays(randomDays);
        };
    }
    
    protected Generator<String> validOwnerNames() {
        return () -> {
            StringBuilder name = new StringBuilder();
            int length = 1 + random.nextInt(49); // 1-50 characters
            for (int i = 0; i < length; i++) {
                if (i == 0) {
                    name.append((char) ('A' + random.nextInt(26)));
                } else {
                    name.append((char) ('a' + random.nextInt(26)));
                }
            }
            return name.toString();
        };
    }
    
    protected Generator<String> validEmails() {
        return () -> {
            String[] domains = {"example.com", "test.org", "sample.net"};
            StringBuilder username = new StringBuilder();
            int length = 3 + random.nextInt(8); // 3-10 characters
            for (int i = 0; i < length; i++) {
                username.append((char) ('a' + random.nextInt(26)));
            }
            String domain = domains[random.nextInt(domains.length)];
            // Add timestamp and random number to ensure uniqueness
            return username.toString() + System.currentTimeMillis() + random.nextInt(1000) + "@" + domain;
        };
    }
    
    protected Generator<String> validPhoneNumbers() {
        return () -> {
            StringBuilder phone = new StringBuilder();
            // Generate a valid phone number that matches the pattern ^[+]?[0-9\\s\\-\\(\\)]{10,15}$
            phone.append("555-");
            for (int i = 0; i < 3; i++) {
                phone.append(random.nextInt(10));
            }
            phone.append("-");
            for (int i = 0; i < 4; i++) {
                phone.append(random.nextInt(10));
            }
            return phone.toString(); // Format: 555-XXX-XXXX
        };
    }
    
    protected Generator<String> validLicenseNumbers() {
        return () -> {
            StringBuilder license = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                if (random.nextBoolean()) {
                    license.append((char) ('A' + random.nextInt(26)));
                } else {
                    license.append(random.nextInt(10));
                }
            }
            return license.toString();
        };
    }
    
    protected Generator<LocalDateTime> validVisitDates() {
        return () -> {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime earliest = now.minusYears(2);
            LocalDateTime latest = now.plusMonths(6);
            
            long secondsBetween = latest.toEpochSecond(java.time.ZoneOffset.UTC) - 
                                 earliest.toEpochSecond(java.time.ZoneOffset.UTC);
            long randomSeconds = Math.abs(random.nextLong()) % secondsBetween;
            
            return earliest.plusSeconds(randomSeconds);
        };
    }
    
    protected Generator<String> validDescriptions() {
        String[] descriptions = {
            "Annual wellness exam",
            "Vaccination appointment", 
            "Skin condition check",
            "Emergency visit",
            "Follow-up examination",
            "Dental cleaning",
            "Surgery consultation"
        };
        return () -> descriptions[random.nextInt(descriptions.length)];
    }
    
    protected Generator<Integer> validDurations() {
        return () -> {
            int[] durations = {15, 30, 45, 60, 90, 120};
            return durations[random.nextInt(durations.length)];
        };
    }
    
    /**
     * Run a property test with specified number of iterations
     * @param iterations Number of test iterations
     * @param property Property to test
     */
    protected void runPropertyTest(int iterations, Runnable property) {
        for (int i = 0; i < iterations; i++) {
            try {
                property.run();
            } catch (Exception e) {
                throw new AssertionError("Property test failed on iteration " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Default number of iterations for property tests
     */
    protected static final int DEFAULT_ITERATIONS = 100;
}