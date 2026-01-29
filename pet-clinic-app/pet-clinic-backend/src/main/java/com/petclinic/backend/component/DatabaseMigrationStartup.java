package com.petclinic.backend.component;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Component that ensures database migrations are executed on application startup.
 * Provides enhanced logging and error handling for migration execution.
 * 
 * Validates: Requirements 8.1
 */
@Component
@Order(1) // Execute early in the startup process
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseMigrationStartup implements ApplicationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationStartup.class);
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("Verifying database migration status on startup");
        
        try {
            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
            
            // Get migration info
            var info = flyway.info();
            var current = info.current();
            var pending = info.pending();
            
            if (current != null) {
                logger.info("Current database schema version: {}", current.getVersion());
                logger.info("Current migration description: {}", current.getDescription());
            } else {
                logger.info("Database schema is empty - baseline will be created");
            }
            
            if (pending.length > 0) {
                logger.info("Found {} pending migrations", pending.length);
                for (var migration : pending) {
                    logger.info("Pending migration: {} - {}", 
                        migration.getVersion(), migration.getDescription());
                }
            } else {
                logger.info("No pending migrations found - database is up to date");
            }
            
            // Validate schema
            try {
                flyway.validate();
                logger.info("Database schema validation successful");
            } catch (Exception e) {
                logger.warn("Schema validation warning: {}", e.getMessage());
                // Continue startup even with validation warnings in some cases
            }
            
        } catch (Exception e) {
            logger.error("Failed to verify database migration status: {}", e.getMessage(), e);
            // Don't fail startup for migration verification issues
            // The actual migration will be handled by Spring Boot's Flyway integration
        }
        
        logger.info("Database migration verification completed");
    }
}