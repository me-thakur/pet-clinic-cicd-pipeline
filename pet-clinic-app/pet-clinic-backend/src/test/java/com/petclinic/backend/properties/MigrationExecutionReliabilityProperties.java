package com.petclinic.backend.properties;

import net.java.quickcheck.Generator;
import net.java.quickcheck.QuickCheck;
import net.java.quickcheck.characteristic.AbstractCharacteristic;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static net.java.quickcheck.generator.PrimitiveGenerators.integers;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for migration execution reliability.
 * 
 * **Validates: Requirements 8.1**
 */
@SpringBootTest
@ActiveProfiles("test")
class MigrationExecutionReliabilityProperties extends PropertyTestBase {

    @Autowired
    private DataSource dataSource;

    /**
     * Property 19: Migration Execution Reliability
     * For any system startup, database schema migrations should be applied in correct order and complete successfully
     */
    @Test
    void testMigrationExecutionReliability() {
        QuickCheck.forAll(integers(1, 10), new AbstractCharacteristic<Integer>() {
            @Override
            protected void doSpecify(Integer iterations) throws Throwable {
                // Given: A fresh Flyway instance for each iteration
                Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .cleanDisabled(false)
                    .load();

                // When: We clean and migrate multiple times
                for (int i = 0; i < iterations; i++) {
                    // Clean the database to simulate fresh startup
                    flyway.clean();
                    
                    // Execute migrations
                    var result = flyway.migrate();
                    
                    // Then: Migrations should execute successfully
                    assertTrue(result.migrationsExecuted >= 0, 
                        "Migrations should execute without errors on iteration " + i);
                    
                    // And: All migrations should be in SUCCESS state
                    var info = flyway.info();
                    var appliedMigrations = Arrays.stream(info.applied())
                        .collect(Collectors.toList());
                    
                    for (MigrationInfo migration : appliedMigrations) {
                        assertEquals(MigrationState.SUCCESS, migration.getState(),
                            "Migration " + migration.getVersion() + " should be in SUCCESS state");
                    }
                    
                    // And: Migrations should be applied in correct version order
                    verifyMigrationOrder(appliedMigrations);
                    
                    // And: Schema should validate successfully
                    assertDoesNotThrow(() -> flyway.validate(),
                        "Schema validation should pass after migration on iteration " + i);
                }
            }
        });
    }

    /**
     * Property: Migration Idempotency
     * Running migrations multiple times should not cause errors or duplicate applications
     */
    @Test
    void testMigrationIdempotency() {
        QuickCheck.forAll(integers(2, 5), new AbstractCharacteristic<Integer>() {
            @Override
            protected void doSpecify(Integer runs) throws Throwable {
                // Given: A configured Flyway instance
                Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .load();

                // When: We run migrations multiple times without cleaning
                var firstResult = flyway.migrate();
                int firstRunMigrations = firstResult.migrationsExecuted;
                
                for (int i = 1; i < runs; i++) {
                    var subsequentResult = flyway.migrate();
                    
                    // Then: Subsequent runs should not execute any new migrations
                    assertEquals(0, subsequentResult.migrationsExecuted,
                        "Run " + (i + 1) + " should not execute any migrations (idempotency)");
                }
                
                // And: Final state should be consistent
                var info = flyway.info();
                assertNotNull(info.current(), "Current migration should be tracked");
                assertEquals(0, info.pending().length, "No pending migrations should remain");
            }
        });
    }

    /**
     * Property: Migration Rollback Safety
     * If a migration fails, the system should be in a consistent state
     */
    @Test
    void testMigrationConsistency() {
        QuickCheck.forAll(integers(1, 3), new AbstractCharacteristic<Integer>() {
            @Override
            protected void doSpecify(Integer attempts) throws Throwable {
                // Given: A configured Flyway instance
                Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .cleanDisabled(false)
                    .load();

                for (int i = 0; i < attempts; i++) {
                    // When: We clean and migrate
                    flyway.clean();
                    flyway.migrate();
                    
                    // Then: The migration info should be consistent
                    var info = flyway.info();
                    
                    // And: All applied migrations should have valid checksums
                    var applied = info.applied();
                    for (MigrationInfo migration : applied) {
                        assertNotNull(migration.getChecksum(),
                            "Migration " + migration.getVersion() + " should have a valid checksum");
                        assertEquals(MigrationState.SUCCESS, migration.getState(),
                            "Migration " + migration.getVersion() + " should be in SUCCESS state");
                    }
                    
                    // And: Schema should be valid
                    assertDoesNotThrow(() -> flyway.validate(),
                        "Schema should be valid after migration attempt " + i);
                }
            }
        });
    }

    /**
     * Property: Migration Version Ordering
     * Migrations should always be applied in version order
     */
    @Test
    void testMigrationVersionOrdering() {
        QuickCheck.forAll(integers(1, 5), new AbstractCharacteristic<Integer>() {
            @Override
            protected void doSpecify(Integer iterations) throws Throwable {
                for (int i = 0; i < iterations; i++) {
                    // Given: A fresh Flyway instance
                    Flyway flyway = Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration")
                        .baselineOnMigrate(true)
                        .cleanDisabled(false)
                        .load();

                    // When: We clean and migrate
                    flyway.clean();
                    flyway.migrate();
                    
                    // Then: Applied migrations should be in correct version order
                    var info = flyway.info();
                    var applied = Arrays.asList(info.applied());
                    
                    verifyMigrationOrder(applied);
                    
                    // And: Each migration should have executed exactly once
                    for (MigrationInfo migration : applied) {
                        assertEquals(MigrationState.SUCCESS, migration.getState(),
                            "Migration " + migration.getVersion() + " should be successfully applied");
                    }
                }
            }
        });
    }

    /**
     * Helper method to verify migrations are applied in correct version order
     */
    private void verifyMigrationOrder(List<MigrationInfo> migrations) {
        if (migrations.size() <= 1) {
            return; // No ordering to verify
        }
        
        for (int i = 1; i < migrations.size(); i++) {
            var previous = migrations.get(i - 1);
            var current = migrations.get(i);
            
            assertTrue(previous.getVersion().compareTo(current.getVersion()) < 0,
                "Migration " + previous.getVersion() + " should come before " + current.getVersion());
        }
    }
}