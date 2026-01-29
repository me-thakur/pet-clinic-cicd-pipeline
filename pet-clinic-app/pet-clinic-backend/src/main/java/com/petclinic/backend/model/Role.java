package com.petclinic.backend.model;

/**
 * Role enumeration for user access control
 * 
 * Defines the three main roles in the Pet Clinic system:
 * - ADMIN: Full system access, user management, system configuration
 * - VET: Veterinarian access, can manage pets, visits, medical records
 * - STAFF: Staff access, can manage appointments, basic pet information
 * 
 * Validates: Requirements 9.1
 */
public enum Role {
    /**
     * Administrator role with full system access
     * Can manage users, system settings, and all data
     */
    ADMIN("Administrator", "Full system access and user management"),
    
    /**
     * Veterinarian role with medical access
     * Can manage pets, visits, medical records, and diagnoses
     */
    VET("Veterinarian", "Medical access to pets, visits, and records"),
    
    /**
     * Staff role with basic operational access
     * Can manage appointments, basic pet information, and scheduling
     */
    STAFF("Staff", "Basic operational access for appointments and scheduling");

    private final String displayName;
    private final String description;

    Role(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this role has administrative privileges
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /**
     * Check if this role has veterinary privileges
     */
    public boolean isVet() {
        return this == VET;
    }

    /**
     * Check if this role has staff privileges
     */
    public boolean isStaff() {
        return this == STAFF;
    }

    /**
     * Check if this role can manage users
     */
    public boolean canManageUsers() {
        return this == ADMIN;
    }

    /**
     * Check if this role can access medical records
     */
    public boolean canAccessMedicalRecords() {
        return this == ADMIN || this == VET;
    }

    /**
     * Check if this role can schedule appointments
     */
    public boolean canScheduleAppointments() {
        return this == ADMIN || this == VET || this == STAFF;
    }

    /**
     * Check if this role can modify pet information
     */
    public boolean canModifyPets() {
        return this == ADMIN || this == VET;
    }

    /**
     * Check if this role can view reports
     */
    public boolean canViewReports() {
        return this == ADMIN || this == VET;
    }

    @Override
    public String toString() {
        return displayName;
    }
}