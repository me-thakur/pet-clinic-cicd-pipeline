package com.petclinic.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * CommandLineRunner that triggers data seeding on application startup
 * Runs after all database migrations are complete
 */
@Component
@Profile({"dev", "staging"})
@ConditionalOnProperty(name = "pet-clinic.data-seeding.enabled", havingValue = "true", matchIfMissing = true)
@Order(100) // Run after all other initialization
public class DataSeedingRunner implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSeedingRunner.class);
    
    @Autowired
    private DataSeedingService dataSeedingService;
    
    @Value("${pet-clinic.data-seeding.enabled:true}")
    private boolean seedingEnabled;
    
    @Override
    public void run(String... args) {
        if (!seedingEnabled) {
            logger.info("Data seeding is disabled");
            return;
        }
        
        try {
            // Add a small delay to ensure all database initialization is complete
            Thread.sleep(2000);
            
            if (dataSeedingService.isSeedingNeeded()) {
                logger.info("Starting automatic data seeding...");
                dataSeedingService.seedAllData();
                
                DataSeedingService.DataSeedingStatistics stats = dataSeedingService.getStatistics();
                logger.info("Data seeding completed. Statistics: {} owners, {} pets, {} vets, {} visits, {} users",
                    stats.getOwnerCount(), stats.getPetCount(), stats.getVeterinarianCount(), 
                    stats.getVisitCount(), stats.getUserCount());
            } else {
                logger.info("Data seeding not needed - data already exists or seeding is disabled");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Data seeding interrupted: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Error during automatic data seeding: {}", e.getMessage(), e);
            // Don't fail application startup due to seeding errors
        }
    }
}