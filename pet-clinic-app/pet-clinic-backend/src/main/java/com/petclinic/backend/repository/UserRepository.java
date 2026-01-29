package com.petclinic.backend.repository;

import com.petclinic.backend.model.Role;
import com.petclinic.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity operations
 * 
 * Provides data access methods for user management including
 * authentication, role-based queries, and security operations.
 * 
 * Validates: Requirements 9.1
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username for authentication
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email address
     */
    Optional<User> findByEmail(String email);

    /**
     * Find all users with a specific role
     */
    List<User> findByRole(Role role);

    /**
     * Find all enabled users
     */
    List<User> findByEnabledTrue();

    /**
     * Find all disabled users
     */
    List<User> findByEnabledFalse();

    /**
     * Find users by role and enabled status
     */
    List<User> findByRoleAndEnabled(Role role, boolean enabled);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find users with failed login attempts above threshold
     */
    @Query("SELECT u FROM User u WHERE u.failedLoginAttempts >= :threshold")
    List<User> findUsersWithFailedLoginAttempts(@Param("threshold") int threshold);

    /**
     * Find users who haven't logged in since a specific date
     */
    @Query("SELECT u FROM User u WHERE u.lastLogin < :date OR u.lastLogin IS NULL")
    List<User> findUsersNotLoggedInSince(@Param("date") LocalDateTime date);

    /**
     * Update last login time for a user
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :loginTime WHERE u.username = :username")
    void updateLastLogin(@Param("username") String username, @Param("loginTime") LocalDateTime loginTime);

    /**
     * Increment failed login attempts
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.username = :username")
    void incrementFailedLoginAttempts(@Param("username") String username);

    /**
     * Reset failed login attempts
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0 WHERE u.username = :username")
    void resetFailedLoginAttempts(@Param("username") String username);

    /**
     * Lock user account
     */
    @Modifying
    @Query("UPDATE User u SET u.accountNonLocked = false WHERE u.username = :username")
    void lockUserAccount(@Param("username") String username);

    /**
     * Unlock user account
     */
    @Modifying
    @Query("UPDATE User u SET u.accountNonLocked = true WHERE u.username = :username")
    void unlockUserAccount(@Param("username") String username);

    /**
     * Find users by first name or last name (case insensitive)
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Count users by role
     */
    long countByRole(Role role);

    /**
     * Count enabled users
     */
    long countByEnabledTrue();
}