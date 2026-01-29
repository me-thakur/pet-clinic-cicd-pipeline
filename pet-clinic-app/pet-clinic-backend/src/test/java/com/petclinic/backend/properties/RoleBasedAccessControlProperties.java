package com.petclinic.backend.properties;

import com.petclinic.backend.model.Role;
import com.petclinic.backend.model.User;
import com.petclinic.backend.service.UserService;
import net.java.quickcheck.Generator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for role-based access control
 * 
 * **Property 23: Role-Based Access Control**
 * *For any* user with a specific role, access should be granted only to 
 * resources and operations appropriate for that role
 * 
 * **Validates: Requirements 9.1**
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RoleBasedAccessControlProperties extends PropertyTestBase {

    @Autowired
    private UserService userService;

    /**
     * Generator for valid usernames
     */
    protected Generator<String> validUsernames() {
        return () -> {
            StringBuilder username = new StringBuilder();
            int length = 3 + random.nextInt(47); // 3-49 characters (leaving room for timestamp)
            for (int i = 0; i < length; i++) {
                if (random.nextBoolean()) {
                    username.append((char) ('a' + random.nextInt(26)));
                } else {
                    username.append(random.nextInt(10));
                }
            }
            // Add small timestamp to ensure uniqueness but keep within 50 char limit
            String baseUsername = username.toString();
            String timestamp = String.valueOf(System.currentTimeMillis() % 1000); // Last 3 digits
            
            // Ensure total length doesn't exceed 50 characters
            if (baseUsername.length() + timestamp.length() > 50) {
                baseUsername = baseUsername.substring(0, 50 - timestamp.length());
            }
            
            return baseUsername + timestamp;
        };
    }

    /**
     * Generator for valid passwords
     */
    protected Generator<String> validPasswords() {
        return () -> {
            // Generate password that meets complexity requirements
            StringBuilder password = new StringBuilder();
            password.append((char) ('A' + random.nextInt(26))); // Uppercase
            password.append((char) ('a' + random.nextInt(26))); // Lowercase
            password.append(random.nextInt(10)); // Digit
            password.append("!@#$%^&*".charAt(random.nextInt(8))); // Special char
            
            // Add more characters to reach minimum length
            for (int i = 4; i < 8 + random.nextInt(8); i++) {
                int type = random.nextInt(4);
                if (type == 0) {
                    password.append((char) ('A' + random.nextInt(26)));
                } else if (type == 1) {
                    password.append((char) ('a' + random.nextInt(26)));
                } else if (type == 2) {
                    password.append(random.nextInt(10));
                } else {
                    password.append("!@#$%^&*".charAt(random.nextInt(8)));
                }
            }
            return password.toString();
        };
    }

    /**
     * Generator for all roles
     */
    protected Generator<Role> allRoles() {
        return () -> {
            Role[] roles = Role.values();
            return roles[random.nextInt(roles.length)];
        };
    }

    @Test
    public void testRoleBasedAccessControlProperty() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate test data
            String username = validUsernames().next();
            String password = validPasswords().next();
            String email = validEmails().next();
            String firstName = validOwnerNames().next();
            String lastName = validOwnerNames().next();
            Role role = allRoles().next();

            // Create user with specific role
            User user = new User(username, password, email, firstName, lastName, role);
            User createdUser = userService.createUser(user);

            // Verify user was created with correct role
            assertNotNull(createdUser);
            assertEquals(role, createdUser.getRole());

            // Test role-specific permissions
            testRolePermissions(createdUser);
        });
    }

    /**
     * Test permissions for each role type
     */
    private void testRolePermissions(User user) {
        Role role = user.getRole();
        
        // Test role-specific capabilities
        switch (role) {
            case ADMIN:
                testAdminPermissions(user);
                break;
            case VET:
                testVetPermissions(user);
                break;
            case STAFF:
                testStaffPermissions(user);
                break;
        }
    }

    /**
     * Test ADMIN role permissions
     */
    private void testAdminPermissions(User user) {
        // ADMIN should have all permissions
        assertTrue(user.getRole().canManageUsers(), 
            "ADMIN should be able to manage users");
        assertTrue(user.getRole().canAccessMedicalRecords(), 
            "ADMIN should be able to access medical records");
        assertTrue(user.getRole().canScheduleAppointments(), 
            "ADMIN should be able to schedule appointments");
        assertTrue(user.getRole().canModifyPets(), 
            "ADMIN should be able to modify pets");
        assertTrue(user.getRole().canViewReports(), 
            "ADMIN should be able to view reports");
        assertTrue(user.isAdmin(), 
            "User with ADMIN role should return true for isAdmin()");
        assertFalse(user.isVet(), 
            "User with ADMIN role should return false for isVet()");
        assertFalse(user.isStaff(), 
            "User with ADMIN role should return false for isStaff()");
    }

    /**
     * Test VET role permissions
     */
    private void testVetPermissions(User user) {
        // VET should have medical access but not user management
        assertFalse(user.getRole().canManageUsers(), 
            "VET should not be able to manage users");
        assertTrue(user.getRole().canAccessMedicalRecords(), 
            "VET should be able to access medical records");
        assertTrue(user.getRole().canScheduleAppointments(), 
            "VET should be able to schedule appointments");
        assertTrue(user.getRole().canModifyPets(), 
            "VET should be able to modify pets");
        assertTrue(user.getRole().canViewReports(), 
            "VET should be able to view reports");
        assertFalse(user.isAdmin(), 
            "User with VET role should return false for isAdmin()");
        assertTrue(user.isVet(), 
            "User with VET role should return true for isVet()");
        assertFalse(user.isStaff(), 
            "User with VET role should return false for isStaff()");
    }

    /**
     * Test STAFF role permissions
     */
    private void testStaffPermissions(User user) {
        // STAFF should have limited access
        assertFalse(user.getRole().canManageUsers(), 
            "STAFF should not be able to manage users");
        assertFalse(user.getRole().canAccessMedicalRecords(), 
            "STAFF should not be able to access medical records");
        assertTrue(user.getRole().canScheduleAppointments(), 
            "STAFF should be able to schedule appointments");
        assertFalse(user.getRole().canModifyPets(), 
            "STAFF should not be able to modify pets");
        assertFalse(user.getRole().canViewReports(), 
            "STAFF should not be able to view reports");
        assertFalse(user.isAdmin(), 
            "User with STAFF role should return false for isAdmin()");
        assertFalse(user.isVet(), 
            "User with STAFF role should return false for isVet()");
        assertTrue(user.isStaff(), 
            "User with STAFF role should return true for isStaff()");
    }

    @Test
    public void testRoleHierarchyProperty() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            Role role = allRoles().next();
            
            // Test role hierarchy - ADMIN has most permissions, STAFF has least
            if (role == Role.ADMIN) {
                assertTrue(role.canManageUsers());
                assertTrue(role.canAccessMedicalRecords());
                assertTrue(role.canScheduleAppointments());
                assertTrue(role.canModifyPets());
                assertTrue(role.canViewReports());
            } else if (role == Role.VET) {
                assertFalse(role.canManageUsers());
                assertTrue(role.canAccessMedicalRecords());
                assertTrue(role.canScheduleAppointments());
                assertTrue(role.canModifyPets());
                assertTrue(role.canViewReports());
            } else if (role == Role.STAFF) {
                assertFalse(role.canManageUsers());
                assertFalse(role.canAccessMedicalRecords());
                assertTrue(role.canScheduleAppointments());
                assertFalse(role.canModifyPets());
                assertFalse(role.canViewReports());
            }
        });
    }

    @Test
    public void testUserRoleConsistencyProperty() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate test data
            String username = validUsernames().next();
            String password = validPasswords().next();
            String email = validEmails().next();
            String firstName = validOwnerNames().next();
            String lastName = validOwnerNames().next();
            Role role = allRoles().next();

            // Create user
            User user = new User(username, password, email, firstName, lastName, role);
            User createdUser = userService.createUser(user);

            // Verify role consistency
            assertEquals(role, createdUser.getRole());
            assertTrue(createdUser.hasRole(role));
            
            // Verify Spring Security authorities are correctly set
            assertNotNull(createdUser.getAuthorities());
            assertEquals(1, createdUser.getAuthorities().size());
            assertTrue(createdUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role.name())));
        });
    }

    @Test
    public void testRoleBasedMethodAccessProperty() {
        runPropertyTest(50, () -> { // Reduced iterations for integration test
            // Generate test data
            String username = validUsernames().next();
            String password = validPasswords().next();
            String email = validEmails().next();
            String firstName = validOwnerNames().next();
            String lastName = validOwnerNames().next();
            Role role = allRoles().next();

            // Create user
            User user = new User(username, password, email, firstName, lastName, role);
            User createdUser = userService.createUser(user);

            // Set up security context
            Authentication auth = new UsernamePasswordAuthenticationToken(
                createdUser, null, createdUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            try {
                // Test that user can access methods appropriate for their role
                testMethodAccessForRole(createdUser, role);
            } finally {
                // Clean up security context
                SecurityContextHolder.clearContext();
            }
        });
    }

    /**
     * Test method access based on role
     */
    private void testMethodAccessForRole(User user, Role role) {
        // Test basic user operations that all roles should be able to perform
        assertDoesNotThrow(() -> {
            userService.findById(user.getId());
        }, "All roles should be able to find users by ID");

        // Test role-specific operations
        switch (role) {
            case ADMIN:
                // ADMIN should be able to perform all operations
                assertDoesNotThrow(() -> {
                    userService.findAll();
                    userService.getUserStatistics();
                }, "ADMIN should be able to access all user operations");
                break;
            case VET:
                // VET should be able to access user information but not manage users
                assertDoesNotThrow(() -> {
                    userService.findByRole(Role.VET);
                }, "VET should be able to find users by role");
                break;
            case STAFF:
                // STAFF should have limited access
                assertDoesNotThrow(() -> {
                    userService.findByUsername(user.getUsername());
                }, "STAFF should be able to find users by username");
                break;
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testAdminRoleAccess() {
        // Test that ADMIN role has full access
        assertDoesNotThrow(() -> {
            userService.findAll();
            userService.getUserStatistics();
        }, "ADMIN should have full access to user operations");
    }

    @Test
    @WithMockUser(roles = "VET")
    public void testVetRoleAccess() {
        // Test that VET role has appropriate access
        assertDoesNotThrow(() -> {
            userService.findByRole(Role.VET);
        }, "VET should be able to access user information");
    }

    @Test
    @WithMockUser(roles = "STAFF")
    public void testStaffRoleAccess() {
        // Test that STAFF role has limited access
        assertDoesNotThrow(() -> {
            userService.findEnabledUsers();
        }, "STAFF should be able to access basic user information");
    }

    @Test
    public void testRoleEnumConsistencyProperty() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            Role role = allRoles().next();
            
            // Test that role enum methods are consistent
            switch (role) {
                case ADMIN:
                    assertTrue(role.isAdmin());
                    assertFalse(role.isVet());
                    assertFalse(role.isStaff());
                    break;
                case VET:
                    assertFalse(role.isAdmin());
                    assertTrue(role.isVet());
                    assertFalse(role.isStaff());
                    break;
                case STAFF:
                    assertFalse(role.isAdmin());
                    assertFalse(role.isVet());
                    assertTrue(role.isStaff());
                    break;
            }
            
            // Test display name and description are not null
            assertNotNull(role.getDisplayName());
            assertNotNull(role.getDescription());
            assertFalse(role.getDisplayName().trim().isEmpty());
            assertFalse(role.getDescription().trim().isEmpty());
        });
    }
}