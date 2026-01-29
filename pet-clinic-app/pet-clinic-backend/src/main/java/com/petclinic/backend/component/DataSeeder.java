package com.petclinic.backend.component;

import com.petclinic.backend.service.DataSeedingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Data seeding component for development and testing environments
 * Delegates to DataSeedingService for actual seeding operations
 * 
 * Validates: Requirements 8.3, 8.4
 */
@Component
@Profile({"dev", "test"})
public class DataSeeder implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);
    
    @Autowired
    private DataSeedingService dataSeedingService;
    
    @Override
    public void run(String... args) throws Exception {
        try {
            logger.info("Data seeder starting...");
            
            if (!dataSeedingService.isSeedingNeeded()) {
                logger.info("Data seeding not needed");
                return;
            }
            
            dataSeedingService.seedAllData();
            
            // Log final statistics
            DataSeedingService.DataSeedingStatistics stats = dataSeedingService.getStatistics();
            logger.info("Data seeding completed: {}", stats);
            
        } catch (Exception e) {
            logger.error("Data seeding failed: {}", e.getMessage(), e);
            // Don't rethrow - allow application to start even if seeding fails
        }
    }
}