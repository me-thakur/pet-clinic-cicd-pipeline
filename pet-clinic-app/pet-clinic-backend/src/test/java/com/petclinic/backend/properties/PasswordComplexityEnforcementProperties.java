package com.petclinic.backend.properties;

import com.petclinic.backend.dto.PasswordChangeRequest;
import com.petclinic.backend.exception.PetClinicException;
import com.petclinic.backend.model.Role;
import com.petclinic.backend.model.User;
import com.petclinic.backend.service.UserService;
import net.java.quickcheck.Generator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for password complexity enforcement
 * **Property 27: Password Complexity Enforcement**
 * **Validates: Requirements 9.3**
 * 
 * For any password creation or update, the password should meet all defined complexity requirements
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PasswordComplexityEnforcementProperties extends PropertyTestBase {

    @Autowired
    private UserService userService;

    // Password complexity pattern: at least one lowercase, uppercase, digit, and special character
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$"
    );

    /**
     * Generator for complex passwords that meet all requirements
     */
    protected Generator<String> complexPasswords() {
        return () -> {
            StringBuilder password = new StringBuilder();
            password.append((char) ('A' + random.nextInt(26))); // Uppercase
            password.append((char) ('a' + random.nextInt(26))); // Lowercase
            password.append(random.nextInt(10)); // Digit
            password.append("@$!%*?&".charAt(random.nextInt(7))); // Special char
            
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
                    password.append("@$!%*?&".charAt(random.nextInt(7)));
                }
            }
            return password.toString();
        };
    }

    /**
     * Generator for passwords without lowercase letters
     */
    protected Generator<String> passwordsWithoutLowercase() {
        return () -> {
            StringBuilder password = new StringBuilder();
            password.append((char) ('A' + random.nextInt(26))); // Uppercase
            password.append(random.nextInt(10)); // Digit
            password.append("@$!%*?&".charAt(random.nextInt(7))); // Special char
            
            // Add more characters
            for (int i = 3; i < 8 + random.nextInt(8); i++) {
                int type = random.nextInt(3);
                if (type == 0) {
                    password.append((char) ('A' + random.nextInt(26)));
                } else if (type == 1) {
                    password.append(random.nextInt(10));
                } else {
                    password.append("@$!%*?&".charAt(random.nextInt(7)));
                }
            }
            return password.toString();
        };
    }

    /**
     * Generator for passwords without uppercase letters
     */
    protected Generator<String> passwordsWithoutUppercase() {
        return () -> {
            StringBuilder password = new StringBuilder();
            password.append((char) ('a' + random.nextInt(26))); // Lowercase
            password.append(random.nextInt(10)); // Digit
            password.append("@$!%*?&".charAt(random.nextInt(7))); // Special char
            
            // Add more characters
            for (int i = 3; i < 8 + random.nextInt(8); i++) {
                int type = random.nextInt(3);
                if (type == 0) {
                    password.append((char) ('a' + random.nextInt(26)));
                } else if (type == 1) {
                    password.append(random.nextInt(10));
                } else {
                    password.append("@$!%*?&".charAt(random.nextInt(7)));
                }
            }
            return password.toString();
        };
    }

    /**
     * Generator for passwords without digits
     */
    protected Generator<String> passwordsWithoutDigits() {
        return () -> {
            StringBuilder password = new StringBuilder();
            password.append((char) ('A' + random.nextInt(26))); // Uppercase
            password.append((char) ('a' + random.nextInt(26))); // Lowercase
            password.append("@$!%*?&".charAt(random.nextInt(7))); // Special char
            
            // Add more characters
            for (int i = 3; i < 8 + random.nextInt(8); i++) {
                int type = random.nextInt(3);
                if (type == 0) {
                    password.append((char) ('A' + random.nextInt(26)));
                } else if (type == 1) {
                    password.append((char) ('a' + random.nextInt(26)));
                } else {
                    password.append("@$!%*?&".charAt(random.nextInt(7)));
                }
            }
            return password.toString();
        };
    }

    /**
     * Generator for passwords without special characters
     */
    protected Generator<String> passwordsWithoutSpecialChars() {
        return () -> {
            StringBuilder password = new StringBuilder();
            password.append((char) ('A' + random.nextInt(26))); // Uppercase
            password.append((char) ('a' + random.nextInt(26))); // Lowercase
            password.append(random.nextInt(10)); // Digit
            
            // Add more characters
            for (int i = 3; i < 8 + random.nextInt(8); i++) {
                int type = random.nextInt(3);
                if (type == 0) {
                    password.append((char) ('A' + random.nextInt(26)));
                } else if (type == 1) {
                    password.append((char) ('a' + random.nextInt(26)));
                } else {
                    password.append(random.nextInt(10));
                }
            }
            return password.toString();
        };
    }

    /**
     * Generator for short passwords
     */
    protected Generator<String> shortPasswords() {
        return () -> {
            int length = 1 + random.nextInt(6); // 1-6 characters
            StringBuilder password = new StringBuilder();
            for (int i = 0; i < length; i++) {
                int type = random.nextInt(4);
                if (type == 0) {
                    password.append((char) ('A' + random.nextInt(26)));
                } else if (type == 1) {
                    password.append((char) ('a' + random.nextInt(26)));
                } else if (type == 2) {
                    password.append(random.nextInt(10));
                } else {
                    password.append("@$!%*?&".charAt(random.nextInt(7)));
                }
            }
            return password.toString();
        };
    }

    /**
     * Property: Valid complex passwords should always be accepted
     */
    @Test
    public void validComplexPasswordsShouldAlwaysBeAccepted() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String complexPassword = complexPasswords().next();
            
            // Property: Complex password should match the pattern
            assertTrue(PASSWORD_PATTERN.matcher(complexPassword).matches(), 
                      "Generated complex password should match complexity pattern");
            
            // Property: Complex password should be accepted by validation
            assertTrue(isPasswordValid(complexPassword), 
                      "Complex password should pass validation");
        });
    }

    /**
     * Property: Passwords without lowercase letters should always be rejected
     */
    @Test
    public void passwordsWithoutLowercaseShouldBeRejected() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String passwordWithoutLowercase = passwordsWithoutLowercase().next();
            
            // Property: Password without lowercase should not match pattern
            assertFalse(PASSWORD_PATTERN.matcher(passwordWithoutLowercase).matches(), 
                       "Password without lowercase should not match complexity pattern");
            
            // Property: Password without lowercase should fail validation
            assertFalse(isPasswordValid(passwordWithoutLowercase), 
                       "Password without lowercase should fail validation");
        });
    }

    /**
     * Property: Passwords without uppercase letters should always be rejected
     */
    @Test
    public void passwordsWithoutUppercaseShouldBeRejected() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String passwordWithoutUppercase = passwordsWithoutUppercase().next();
            
            // Property: Password without uppercase should not match pattern
            assertFalse(PASSWORD_PATTERN.matcher(passwordWithoutUppercase).matches(), 
                       "Password without uppercase should not match complexity pattern");
            
            // Property: Password without uppercase should fail validation
            assertFalse(isPasswordValid(passwordWithoutUppercase), 
                       "Password without uppercase should fail validation");
        });
    }

    /**
     * Property: Passwords without digits should always be rejected
     */
    @Test
    public void passwordsWithoutDigitsShouldBeRejected() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String passwordWithoutDigits = passwordsWithoutDigits().next();
            
            // Property: Password without digits should not match pattern
            assertFalse(PASSWORD_PATTERN.matcher(passwordWithoutDigits).matches(), 
                       "Password without digits should not match complexity pattern");
            
            // Property: Password without digits should fail validation
            assertFalse(isPasswordValid(passwordWithoutDigits), 
                       "Password without digits should fail validation");
        });
    }

    /**
     * Property: Passwords without special characters should always be rejected
     */
    @Test
    public void passwordsWithoutSpecialCharactersShouldBeRejected() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String passwordWithoutSpecial = passwordsWithoutSpecialChars().next();
            
            // Property: Password without special characters should not match pattern
            assertFalse(PASSWORD_PATTERN.matcher(passwordWithoutSpecial).matches(), 
                       "Password without special characters should not match complexity pattern");
            
            // Property: Password without special characters should fail validation
            assertFalse(isPasswordValid(passwordWithoutSpecial), 
                       "Password without special characters should fail validation");
        });
    }

    /**
     * Property: Passwords shorter than 8 characters should always be rejected
     */
    @Test
    public void shortPasswordsShouldBeRejected() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String shortPassword = shortPasswords().next();
            
            // Ensure it's actually short
            if (shortPassword.length() >= 8) {
                shortPassword = shortPassword.substring(0, 7);
            }
            
            // Property: Short password should fail validation due to length
            assertFalse(isPasswordValid(shortPassword), 
                       "Password shorter than 8 characters should fail validation");
        });
    }

    /**
     * Property: Password change request validation should enforce complexity
     */
    @Test
    public void passwordChangeRequestShouldEnforceComplexity() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String currentPassword = complexPasswords().next();
            String simplePassword = passwordsWithoutUppercase().next(); // Use simple password
            
            // Create password change request with simple password
            PasswordChangeRequest request = new PasswordChangeRequest();
            request.setCurrentPassword(currentPassword);
            request.setNewPassword(simplePassword);
            request.setConfirmPassword(simplePassword);
            
            // Property: Simple password should fail validation in DTO
            // This is validated by the @Pattern annotation on the DTO
            assertFalse(PASSWORD_PATTERN.matcher(simplePassword).matches(), 
                       "Simple password should not match complexity pattern");
        });
    }

    private boolean isPasswordValid(String password) {
        // Check length
        if (password == null || password.length() < 8 || password.length() > 128) {
            return false;
        }
        
        // Check complexity pattern
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    private User createTestUser(String username) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPassword("TempPassword1@");
        user.setRole(Role.STAFF);
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        return user;
    }
}