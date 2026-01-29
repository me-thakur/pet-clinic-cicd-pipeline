package com.petclinic.backend.properties;

import com.petclinic.backend.model.AuditLog;
import com.petclinic.backend.service.AuditService;
import net.java.quickcheck.Generator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for audit trail completeness
 * **Validates: Requirements 9.2**
 * 
 * Property 24: Audit Trail Completeness
 * For any access to sensitive data, an audit log entry should be created 
 * with user, timestamp, and resource information
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AuditTrailCompletenessProperties extends PropertyTestBase {

    @Autowired
    private AuditService auditService;

    /**
     * Property 24.1: User data access creates audit log
     * For any user data access, an audit log should be created with complete information
     */
    @Test
    void userDataAccessCreatesAuditLog() {
        runPropertyTest(100, () -> {
            String username = "testuser" + System.currentTimeMillis();
            String action = "READ";
            Long resourceId = Math.abs(random.nextLong());
            
            // Given: Initial audit log count
            LocalDateTime beforeAccess = LocalDateTime.now().minusSeconds(1);
            
            // When: Logging user data access
            auditService.logDataAccess(username, action, "User", resourceId, 
                                      "Test user data access");
            
            // Then: Audit log should be created with complete information
            LocalDateTime afterAccess = LocalDateTime.now().plusSeconds(1);
            List<AuditLog> auditLogs = auditService.getAuditLogsByDateRange(beforeAccess, afterAccess);
            
            // Verify audit log exists
            boolean auditLogExists = auditLogs.stream()
                    .anyMatch(log -> log.getUsername().equals(username) &&
                                    log.getAction().equals(action) &&
                                    log.getResource().equals("User") &&
                                    log.getResourceId().equals(resourceId));
            
            assertTrue(auditLogExists, "Expected audit log not found for user data access");
            
            // Verify audit log completeness
            AuditLog auditLog = auditLogs.stream()
                    .filter(log -> log.getUsername().equals(username) &&
                                  log.getAction().equals(action) &&
                                  log.getResource().equals("User") &&
                                  log.getResourceId().equals(resourceId))
                    .findFirst()
                    .orElseThrow();
            
            // Verify required fields are present
            assertNotNull(auditLog.getUsername(), "Audit log missing username");
            assertFalse(auditLog.getUsername().trim().isEmpty(), "Audit log username is empty");
            assertNotNull(auditLog.getTimestamp(), "Audit log missing timestamp");
            assertNotNull(auditLog.getAction(), "Audit log missing action");
            assertFalse(auditLog.getAction().trim().isEmpty(), "Audit log action is empty");
            assertNotNull(auditLog.getResource(), "Audit log missing resource");
            assertFalse(auditLog.getResource().trim().isEmpty(), "Audit log resource is empty");
            assertNotNull(auditLog.getResourceId(), "Audit log missing resource ID");
            
            // Verify timestamp is reasonable
            assertTrue(auditLog.getTimestamp().isAfter(beforeAccess) && 
                      auditLog.getTimestamp().isBefore(afterAccess), 
                      "Audit log timestamp is not within expected range");
        });
    }

    /**
     * Property 24.2: Pet data access creates audit log
     * For any pet data access, an audit log should be created
     */
    @Test
    void petDataAccessCreatesAuditLog() {
        runPropertyTest(100, () -> {
            String username = "testuser" + System.currentTimeMillis();
            Long petId = Math.abs(random.nextLong());
            
            // Given: Initial state
            LocalDateTime beforeAccess = LocalDateTime.now().minusSeconds(1);
            
            // When: Logging pet data access
            auditService.logDataAccess(username, "READ", "Pet", petId, 
                                      "Pet data accessed for testing");
            
            // Then: Audit log should be created
            LocalDateTime afterAccess = LocalDateTime.now().plusSeconds(1);
            List<AuditLog> auditLogs = auditService.getAuditLogsByResource("Pet", petId);
            
            // Verify audit log exists for this pet
            boolean auditLogExists = auditLogs.stream()
                    .anyMatch(log -> log.getUsername().equals(username) &&
                                    log.getAction().equals("READ") &&
                                    log.getResourceId().equals(petId) &&
                                    log.getTimestamp().isAfter(beforeAccess) &&
                                    log.getTimestamp().isBefore(afterAccess));
            
            assertTrue(auditLogExists, "Expected audit log not found for pet data access");
        });
    }

    /**
     * Property 24.3: Visit data access creates audit log
     * For any visit data access, an audit log should be created
     */
    @Test
    void visitDataAccessCreatesAuditLog() {
        runPropertyTest(100, () -> {
            String username = "testuser" + System.currentTimeMillis();
            Long visitId = Math.abs(random.nextLong());
            
            // Given: Initial state
            LocalDateTime beforeAccess = LocalDateTime.now().minusSeconds(1);
            
            // When: Logging visit data access
            auditService.logDataAccess(username, "READ", "Visit", visitId, 
                                      "Visit data accessed for testing");
            
            // Then: Audit log should be created
            LocalDateTime afterAccess = LocalDateTime.now().plusSeconds(1);
            List<AuditLog> auditLogs = auditService.getAuditLogsByResource("Visit", visitId);
            
            // Verify audit log exists for this visit
            boolean auditLogExists = auditLogs.stream()
                    .anyMatch(log -> log.getUsername().equals(username) &&
                                    log.getAction().equals("READ") &&
                                    log.getResourceId().equals(visitId) &&
                                    log.getTimestamp().isAfter(beforeAccess) &&
                                    log.getTimestamp().isBefore(afterAccess));
            
            assertTrue(auditLogExists, "Expected audit log not found for visit data access");
        });
    }

    /**
     * Property 24.4: Multiple actions create separate audit logs
     * For any sequence of data access actions, each should create a separate audit log
     */
    @Test
    void multipleActionsCreateSeparateAuditLogs() {
        runPropertyTest(100, () -> {
            String username = "testuser" + System.currentTimeMillis();
            Long resourceId = Math.abs(random.nextLong());
            
            // Given: Initial state
            LocalDateTime beforeAccess = LocalDateTime.now().minusSeconds(1);
            
            // When: Performing multiple actions
            auditService.logDataAccess(username, "CREATE", "Pet", resourceId, "Pet created");
            auditService.logDataAccess(username, "READ", "Pet", resourceId, "Pet read");
            auditService.logDataAccess(username, "UPDATE", "Pet", resourceId, "Pet updated");
            
            // Then: Each action should create a separate audit log
            LocalDateTime afterAccess = LocalDateTime.now().plusSeconds(1);
            List<AuditLog> auditLogs = auditService.getAuditLogsByResource("Pet", resourceId);
            
            // Filter logs within our time range
            List<AuditLog> relevantLogs = auditLogs.stream()
                    .filter(log -> log.getUsername().equals(username) &&
                                  log.getTimestamp().isAfter(beforeAccess) &&
                                  log.getTimestamp().isBefore(afterAccess))
                    .collect(java.util.stream.Collectors.toList());
            
            // Verify we have exactly 3 audit logs
            assertEquals(3, relevantLogs.size(), "Expected 3 audit logs, but found " + relevantLogs.size());
            
            // Verify each action is logged
            boolean hasCreate = relevantLogs.stream().anyMatch(log -> "CREATE".equals(log.getAction()));
            boolean hasRead = relevantLogs.stream().anyMatch(log -> "READ".equals(log.getAction()));
            boolean hasUpdate = relevantLogs.stream().anyMatch(log -> "UPDATE".equals(log.getAction()));
            
            assertTrue(hasCreate && hasRead && hasUpdate, "Not all actions were properly logged");
        });
    }

    /**
     * Property 24.5: Audit logs are retrievable by user
     * For any user's actions, audit logs should be retrievable by username
     */
    @Test
    void auditLogsRetrievableByUser() {
        runPropertyTest(100, () -> {
            String username = "testuser" + System.currentTimeMillis();
            Long resourceId = Math.abs(random.nextLong());
            
            // Given: User performs an action
            auditService.logDataAccess(username, "READ", "User", resourceId, "User data accessed");
            
            // When: Retrieving audit logs by user
            List<AuditLog> userAuditLogs = auditService.getAuditLogsByUser(username);
            
            // Then: User's audit logs should be retrievable
            boolean userLogExists = userAuditLogs.stream()
                    .anyMatch(log -> log.getUsername().equals(username) &&
                                    log.getAction().equals("READ") &&
                                    log.getResource().equals("User") &&
                                    log.getResourceId().equals(resourceId));
            
            assertTrue(userLogExists, "User's audit log not retrievable by username");
        });
    }

    /**
     * Property 24.6: Audit logs are retrievable by resource
     * For any resource access, audit logs should be retrievable by resource type and ID
     */
    @Test
    void auditLogsRetrievableByResource() {
        runPropertyTest(100, () -> {
            String username = "testuser" + System.currentTimeMillis();
            String resource = "TestResource";
            Long resourceId = Math.abs(random.nextLong());
            
            // Given: Resource is accessed
            auditService.logDataAccess(username, "READ", resource, resourceId, "Resource accessed");
            
            // When: Retrieving audit logs by resource
            List<AuditLog> resourceAuditLogs = auditService.getAuditLogsByResource(resource, resourceId);
            
            // Then: Resource's audit logs should be retrievable
            boolean resourceLogExists = resourceAuditLogs.stream()
                    .anyMatch(log -> log.getUsername().equals(username) &&
                                    log.getResource().equals(resource) &&
                                    log.getResourceId().equals(resourceId));
            
            assertTrue(resourceLogExists, "Resource's audit log not retrievable by resource type and ID");
        });
    }
}