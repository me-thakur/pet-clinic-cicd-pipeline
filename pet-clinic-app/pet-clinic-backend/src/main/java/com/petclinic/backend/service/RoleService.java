package com.petclinic.backend.service;

import com.petclinic.backend.model.Role;
import com.petclinic.backend.model.User;

/**
 * Service interface for role-based permission checking
 * 
 * Provides methods to check user permissions based on roles
 * and enforce access control throughout the application.
 * 
 * Validates: Requirements 9.1
 */
public interface RoleService {

    /**
     * Check if user has permission to manage users
     */
    boolean canManageUsers(User user);

    /**
     * Check if user has permission to access medical records
     */
    boolean canAccessMedicalRecords(User user);

    /**
     * Check if user has permission to schedule appointments
     */
    boolean canScheduleAppointments(User user);

    /**
     * Check if user has permission to modify pet information
     */
    boolean canModifyPets(User user);

    /**
     * Check if user has permission to view reports
     */
    boolean canViewReports(User user);

    /**
     * Check if user has permission to delete records
     */
    boolean canDeleteRecords(User user);

    /**
     * Check if user has permission to manage veterinarians
     */
    boolean canManageVeterinarians(User user);

    /**
     * Check if user has permission to access system settings
     */
    boolean canAccessSystemSettings(User user);

    /**
     * Check if user has permission to export data
     */
    boolean canExportData(User user);

    /**
     * Check if user has permission to view audit logs
     */
    boolean canViewAuditLogs(User user);

    /**
     * Check if user has specific role
     */
    boolean hasRole(User user, Role role);

    /**
     * Check if user has any of the specified roles
     */
    boolean hasAnyRole(User user, Role... roles);

    /**
     * Get user's role display name
     */
    String getRoleDisplayName(User user);

    /**
     * Get user's role description
     */
    String getRoleDescription(User user);

    /**
     * Check if user can access resource owned by another user
     */
    boolean canAccessUserResource(User currentUser, User resourceOwner);

    /**
     * Check if user can modify resource owned by another user
     */
    boolean canModifyUserResource(User currentUser, User resourceOwner);
}