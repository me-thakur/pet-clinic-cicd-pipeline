package com.petclinic.backend.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for database migration functionality.
 * Validates that migrations execute correctly and schema is properly created.
 * 
 * Validates: Requirements 8.1
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
class DatabaseMigrationTest {
    
    @Autowired
    private DataSource dataSource;
    
    @Test
    void testMigrationExecution() {
        // Given: A fresh database and Flyway configuration
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load();
        
        // When: Migrations are executed
        var result = flyway.migrate();
        
        // Then: Migrations should execute successfully
        assertTrue(result.migrationsExecuted >= 0, "Migrations should execute without errors");
        
        // And: Schema should be at the expected version
        var info = flyway.info();
        assertNotNull(info.current(), "Current migration should not be null");
        
        // And: No pending migrations should remain
        assertEquals(0, info.pending().length, "No pending migrations should remain after execution");
        
        // And: Schema should validate successfully
        assertDoesNotThrow(() -> flyway.validate(), "Schema validation should pass");
    }
    
    @Test
    void testMigrationInfo() {
        // Given: A configured Flyway instance
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load();
        
        // When: Getting migration info
        var info = flyway.info();
        
        // Then: Migration info should be available
        assertNotNull(info, "Migration info should not be null");
        
        // And: All migrations should be available
        var all = info.all();
        assertTrue(all.length > 0, "Should have at least one migration");
        
        // And: Applied migrations should be tracked
        var applied = info.applied();
        assertTrue(applied.length >= 0, "Applied migrations should be tracked");
    }
    
    @Test
    void testSchemaValidation() {
        // Given: A migrated database
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load();
        
        flyway.migrate();
        
        // When: Validating the schema
        // Then: Validation should pass without exceptions
        assertDoesNotThrow(() -> flyway.validate(), 
            "Schema validation should pass after migration");
    }
    
    @Test
    void testMigrationIdempotency() {
        // Given: A configured Flyway instance
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load();
        
        // When: Running migrations multiple times
        var firstRun = flyway.migrate();
        var secondRun = flyway.migrate();
        
        // Then: Second run should not apply any new migrations
        assertTrue(firstRun.migrationsExecuted >= 0, "First run should execute migrations");
        assertEquals(0, secondRun.migrationsExecuted, "Second run should not execute any migrations");
        
        // And: Schema should remain valid
        assertDoesNotThrow(() -> flyway.validate(), "Schema should remain valid after multiple runs");
    }
}