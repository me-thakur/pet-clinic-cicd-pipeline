package com.petclinic.backend.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Flyway configuration for database migrations.
 * Ensures proper migration execution and error handling.
 * 
 * Validates: Requirements 8.1
 */
@Configuration
public class FlywayConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(FlywayConfig.class);
    
    @Autowired
    private DataSource dataSource;
    
    /**
     * Custom Flyway migration strategy for production environments.
     * Provides enhanced error handling and logging.
     */
    @Bean
    @Profile({"staging", "prod"})
    @ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            logger.info("Starting database migration with Flyway");
            
            try {
                // Validate current schema state
                flyway.validate();
                logger.info("Schema validation successful");
                
                // Execute migrations
                int migrationsApplied = flyway.migrate().migrationsExecuted;
                logger.info("Applied {} database migrations", migrationsApplied);
                
                // Log current schema version
                String currentVersion = flyway.info().current() != null ? 
                    flyway.info().current().getVersion().toString() : "empty";
                logger.info("Current database schema version: {}", currentVersion);
                
            } catch (Exception e) {
                logger.error("Database migration failed: {}", e.getMessage(), e);
                throw new RuntimeException("Database migration failed", e);
            }
        };
    }
    
    /**
     * Custom Flyway configuration for development environments.
     * Allows for more flexible migration handling during development.
     */
    @Bean
    @Profile("dev")
    @ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true")
    public FlywayMigrationStrategy devFlywayMigrationStrategy() {
        return flyway -> {
            logger.info("Starting development database migration with Flyway");
            
            try {
                // For development, we can be more lenient with validation
                FluentConfiguration config = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .validateOnMigrate(false); // More lenient for dev
                
                Flyway devFlyway = config.load();
                
                int migrationsApplied = devFlyway.migrate().migrationsExecuted;
                logger.info("Applied {} database migrations in development mode", migrationsApplied);
                
            } catch (Exception e) {
                logger.warn("Development database migration encountered issues: {}", e.getMessage());
                // In development, we might want to continue even if migrations have issues
                logger.warn("Continuing with application startup despite migration warnings");
            }
        };
    }
    
    /**
     * Bean to provide migration information and health checks.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true")
    public FlywayMigrationInfo flywayMigrationInfo() {
        return new FlywayMigrationInfo(dataSource);
    }
    
    /**
     * Helper class to provide migration information.
     */
    public static class FlywayMigrationInfo {
        private final DataSource dataSource;
        
        public FlywayMigrationInfo(DataSource dataSource) {
            this.dataSource = dataSource;
        }
        
        /**
         * Get current schema version.
         */
        public String getCurrentVersion() {
            try {
                Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .load();
                
                return flyway.info().current() != null ? 
                    flyway.info().current().getVersion().toString() : "empty";
            } catch (Exception e) {
                logger.error("Failed to get current schema version: {}", e.getMessage());
                return "unknown";
            }
        }
        
        /**
         * Check if migrations are pending.
         */
        public boolean hasPendingMigrations() {
            try {
                Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .load();
                
                return flyway.info().pending().length > 0;
            } catch (Exception e) {
                logger.error("Failed to check pending migrations: {}", e.getMessage());
                return false;
            }
        }
    }
}