package com.petclinic.backend.service.impl;

import com.petclinic.backend.exception.PetClinicException;
import com.petclinic.backend.model.Role;
import com.petclinic.backend.model.User;
import com.petclinic.backend.repository.UserRepository;
import com.petclinic.backend.service.AuditService;
import com.petclinic.backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Implementation of UserService for user management operations
 * 
 * Provides comprehensive user management functionality including
 * authentication, role management, password validation, and
 * security features like account locking and login tracking.
 * 
 * Validates: Requirements 9.1, 9.3, 9.4
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    
    // Password validation patterns
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditService auditService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user by username: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        // Log user authentication attempt
        auditService.logDataAccess(username, "AUTHENTICATION", "User", user.getId(), 
                                  "User authentication attempt");
        
        logger.debug("User found: {} with role: {}", username, user.getRole());
        return user;
    }

    @Override
    public User createUser(User user) {
        logger.info("Creating new user: {}", user.getUsername());
        
        // Validate username and email uniqueness
        if (!isUsernameAvailable(user.getUsername())) {
            throw new PetClinicException("Username already exists: " + user.getUsername(), "USERNAME_EXISTS", org.springframework.http.HttpStatus.CONFLICT);
        }
        
        if (!isEmailAvailable(user.getEmail())) {
            throw new PetClinicException("Email already exists: " + user.getEmail(), "EMAIL_EXISTS", org.springframework.http.HttpStatus.CONFLICT);
        }
        
        // Validate password
        if (!isPasswordValid(user.getPassword())) {
            List<String> errors = getPasswordValidationErrors(user.getPassword());
            throw new PetClinicException("Password validation failed: " + String.join(", ", errors), "INVALID_PASSWORD", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        
        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        User savedUser = userRepository.save(user);
        logger.info("User created successfully: {}", savedUser.getUsername());
        
        // Log user creation
        auditService.logDataAccess("system", "CREATE", "User", savedUser.getId(), 
                                  "New user created: " + savedUser.getUsername());
        
        return savedUser;
    }

    @Override
    public User updateUser(User user) {
        logger.info("Updating user: {}", user.getUsername());
        
        Optional<User> existingUser = userRepository.findById(user.getId());
        if (existingUser.isEmpty()) {
            throw new PetClinicException("User not found with ID: " + user.getId(), "USER_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND);
        }
        
        User existing = existingUser.get();
        
        // Check if username or email changed and validate uniqueness
        if (!existing.getUsername().equals(user.getUsername()) && !isUsernameAvailable(user.getUsername())) {
            throw new PetClinicException("Username already exists: " + user.getUsername(), "USERNAME_EXISTS", org.springframework.http.HttpStatus.CONFLICT);
        }
        
        if (!existing.getEmail().equals(user.getEmail()) && !isEmailAvailable(user.getEmail())) {
            throw new PetClinicException("Email already exists: " + user.getEmail(), "EMAIL_EXISTS", org.springframework.http.HttpStatus.CONFLICT);
        }
        
        // Don't update password here - use changePassword method
        user.setPassword(existing.getPassword());
        
        User savedUser = userRepository.save(user);
        logger.info("User updated successfully: {}", savedUser.getUsername());
        
        // Log user update
        auditService.logDataAccess(existing.getUsername(), "UPDATE", "User", savedUser.getId(), 
                                  "User profile updated");
        
        return savedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            // Log sensitive data access
            auditService.logDataAccess("system", "READ", "User", id, 
                                      "User data accessed by ID");
        }
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            // Log sensitive data access
            auditService.logDataAccess(username, "READ", "User", user.get().getId(), 
                                      "User data accessed by username");
        }
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByRole(Role role) {
        return userRepository.findByRole(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findEnabledUsers() {
        return userRepository.findByEnabledTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findDisabledUsers() {
        return userRepository.findByEnabledFalse();
    }

    @Override
    public void deleteUser(Long id) {
        logger.info("Deleting user with ID: {}", id);
        
        if (!userRepository.existsById(id)) {
            throw new PetClinicException("User not found with ID: " + id, "USER_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND);
        }
        
        // Log user deletion before actual deletion
        auditService.logDataAccess("system", "DELETE", "User", id, 
                                  "User account deleted");
        
        userRepository.deleteById(id);
        logger.info("User deleted successfully with ID: {}", id);
    }

    @Override
    public void enableUser(Long id) {
        logger.info("Enabling user with ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new PetClinicException("User not found with ID: " + id, "USER_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND));
        
        user.setEnabled(true);
        userRepository.save(user);
        
        logger.info("User enabled successfully: {}", user.getUsername());
    }

    @Override
    public void disableUser(Long id) {
        logger.info("Disabling user with ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new PetClinicException("User not found with ID: " + id, "USER_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND));
        
        user.setEnabled(false);
        userRepository.save(user);
        
        logger.info("User disabled successfully: {}", user.getUsername());
    }

    @Override
    public void lockUser(String username) {
        logger.warn("Locking user account: {}", username);
        userRepository.lockUserAccount(username);
    }

    @Override
    public void unlockUser(String username) {
        logger.info("Unlocking user account: {}", username);
        userRepository.unlockUserAccount(username);
        userRepository.resetFailedLoginAttempts(username);
    }

    @Override
    public void updateLastLogin(String username, LocalDateTime loginTime) {
        userRepository.updateLastLogin(username, loginTime);
    }

    @Override
    public void handleFailedLogin(String username) {
        logger.warn("Failed login attempt for user: {}", username);
        
        userRepository.incrementFailedLoginAttempts(username);
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getFailedLoginAttempts() + 1 >= MAX_FAILED_LOGIN_ATTEMPTS) {
                logger.warn("Locking user account due to excessive failed login attempts: {}", username);
                lockUser(username);
            }
        }
    }

    @Override
    public void handleSuccessfulLogin(String username) {
        logger.debug("Successful login for user: {}", username);
        
        userRepository.resetFailedLoginAttempts(username);
        updateLastLogin(username, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        logger.info("Changing password for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PetClinicException("User not found with ID: " + userId, "USER_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND));
        
        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new PetClinicException("Current password is incorrect", "INVALID_PASSWORD", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        
        // Validate new password
        if (!isPasswordValid(newPassword)) {
            List<String> errors = getPasswordValidationErrors(newPassword);
            throw new PetClinicException("Password validation failed: " + String.join(", ", errors), "INVALID_PASSWORD", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Log password change
        auditService.logDataAccess(user.getUsername(), "PASSWORD_CHANGE", "User", userId, 
                                  "User password changed");
        
        logger.info("Password changed successfully for user: {}", user.getUsername());
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        logger.info("Resetting password for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PetClinicException("User not found with ID: " + userId, "USER_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND));
        
        // Validate new password
        if (!isPasswordValid(newPassword)) {
            List<String> errors = getPasswordValidationErrors(newPassword);
            throw new PetClinicException("Password validation failed: " + String.join(", ", errors), "INVALID_PASSWORD", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setCredentialsNonExpired(true);
        userRepository.save(user);
        
        // Log password reset
        auditService.logDataAccess("system", "PASSWORD_RESET", "User", userId, 
                                  "User password reset by administrator");
        
        logger.info("Password reset successfully for user: {}", user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> searchByName(String name) {
        return userRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatistics getUserStatistics() {
        long totalUsers = userRepository.count();
        long enabledUsers = userRepository.countByEnabledTrue();
        long disabledUsers = totalUsers - enabledUsers;
        long adminUsers = userRepository.countByRole(Role.ADMIN);
        long vetUsers = userRepository.countByRole(Role.VET);
        long staffUsers = userRepository.countByRole(Role.STAFF);
        
        return new UserStatistics(totalUsers, enabledUsers, disabledUsers, adminUsers, vetUsers, staffUsers);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findUsersWithFailedLoginAttempts(int threshold) {
        return userRepository.findUsersWithFailedLoginAttempts(threshold);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findInactiveUsers(LocalDateTime since) {
        return userRepository.findUsersNotLoggedInSince(since);
    }

    @Override
    public boolean isPasswordValid(String password) {
        return getPasswordValidationErrors(password).isEmpty();
    }

    @Override
    public List<String> getPasswordValidationErrors(String password) {
        List<String> errors = new ArrayList<>();
        
        if (password == null || password.length() < 8) {
            errors.add("Password must be at least 8 characters long");
        }
        
        if (password != null) {
            if (!UPPERCASE_PATTERN.matcher(password).matches()) {
                errors.add("Password must contain at least one uppercase letter");
            }
            
            if (!LOWERCASE_PATTERN.matcher(password).matches()) {
                errors.add("Password must contain at least one lowercase letter");
            }
            
            if (!DIGIT_PATTERN.matcher(password).matches()) {
                errors.add("Password must contain at least one digit");
            }
            
            if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
                errors.add("Password must contain at least one special character");
            }
            
            if (password.length() > 128) {
                errors.add("Password must not exceed 128 characters");
            }
        }
        
        return errors;
    }
}