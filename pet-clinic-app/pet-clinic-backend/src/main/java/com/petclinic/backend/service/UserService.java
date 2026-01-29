package com.petclinic.backend.service;

import com.petclinic.backend.model.Role;
import com.petclinic.backend.model.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for User management operations
 * 
 * Extends Spring Security's UserDetailsService to provide
 * authentication integration along with user management
 * functionality including role-based operations.
 * 
 * Validates: Requirements 9.1
 */
public interface UserService extends UserDetailsService {

    /**
     * Create a new user
     */
    User createUser(User user);

    /**
     * Update an existing user
     */
    User updateUser(User user);

    /**
     * Find user by ID
     */
    Optional<User> findById(Long id);

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Find all users
     */
    List<User> findAll();

    /**
     * Find users by role
     */
    List<User> findByRole(Role role);

    /**
     * Find all enabled users
     */
    List<User> findEnabledUsers();

    /**
     * Find all disabled users
     */
    List<User> findDisabledUsers();

    /**
     * Delete user by ID
     */
    void deleteUser(Long id);

    /**
     * Enable user account
     */
    void enableUser(Long id);

    /**
     * Disable user account
     */
    void disableUser(Long id);

    /**
     * Lock user account
     */
    void lockUser(String username);

    /**
     * Unlock user account
     */
    void unlockUser(String username);

    /**
     * Update user's last login time
     */
    void updateLastLogin(String username, LocalDateTime loginTime);

    /**
     * Handle failed login attempt
     */
    void handleFailedLogin(String username);

    /**
     * Handle successful login
     */
    void handleSuccessfulLogin(String username);

    /**
     * Check if username is available
     */
    boolean isUsernameAvailable(String username);

    /**
     * Check if email is available
     */
    boolean isEmailAvailable(String email);

    /**
     * Change user password
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * Reset user password (admin function)
     */
    void resetPassword(Long userId, String newPassword);

    /**
     * Search users by name
     */
    List<User> searchByName(String name);

    /**
     * Get user statistics
     */
    UserStatistics getUserStatistics();

    /**
     * Find users with excessive failed login attempts
     */
    List<User> findUsersWithFailedLoginAttempts(int threshold);

    /**
     * Find inactive users (not logged in since date)
     */
    List<User> findInactiveUsers(LocalDateTime since);

    /**
     * Validate password complexity
     */
    boolean isPasswordValid(String password);

    /**
     * Get password validation errors
     */
    List<String> getPasswordValidationErrors(String password);

    /**
     * Inner class for user statistics
     */
    class UserStatistics {
        private long totalUsers;
        private long enabledUsers;
        private long disabledUsers;
        private long adminUsers;
        private long vetUsers;
        private long staffUsers;

        // Constructors
        public UserStatistics() {}

        public UserStatistics(long totalUsers, long enabledUsers, long disabledUsers, 
                            long adminUsers, long vetUsers, long staffUsers) {
            this.totalUsers = totalUsers;
            this.enabledUsers = enabledUsers;
            this.disabledUsers = disabledUsers;
            this.adminUsers = adminUsers;
            this.vetUsers = vetUsers;
            this.staffUsers = staffUsers;
        }

        // Getters and setters
        public long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

        public long getEnabledUsers() { return enabledUsers; }
        public void setEnabledUsers(long enabledUsers) { this.enabledUsers = enabledUsers; }

        public long getDisabledUsers() { return disabledUsers; }
        public void setDisabledUsers(long disabledUsers) { this.disabledUsers = disabledUsers; }

        public long getAdminUsers() { return adminUsers; }
        public void setAdminUsers(long adminUsers) { this.adminUsers = adminUsers; }

        public long getVetUsers() { return vetUsers; }
        public void setVetUsers(long vetUsers) { this.vetUsers = vetUsers; }

        public long getStaffUsers() { return staffUsers; }
        public void setStaffUsers(long staffUsers) { this.staffUsers = staffUsers; }
    }
}