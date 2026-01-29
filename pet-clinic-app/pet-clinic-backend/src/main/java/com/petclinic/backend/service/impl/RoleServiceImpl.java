package com.petclinic.backend.service.impl;

import com.petclinic.backend.model.Role;
import com.petclinic.backend.model.User;
import com.petclinic.backend.service.RoleService;
import org.springframework.stereotype.Service;

/**
 * Implementation of RoleService for role-based permission checking
 * 
 * Provides concrete implementations for checking user permissions
 * based on their assigned roles and enforcing access control.
 * 
 * Validates: Requirements 9.1
 */
@Service
public class RoleServiceImpl implements RoleService {

    @Override
    public boolean canManageUsers(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return user.getRole().canManageUsers();
    }

    @Override
    public boolean canAccessMedicalRecords(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return user.getRole().canAccessMedicalRecords();
    }

    @Override
    public boolean canScheduleAppointments(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return user.getRole().canScheduleAppointments();
    }

    @Override
    public boolean canModifyPets(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return user.getRole().canModifyPets();
    }

    @Override
    public boolean canViewReports(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return user.getRole().canViewReports();
    }

    @Override
    public boolean canDeleteRecords(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        // Only admins can delete records
        return user.getRole() == Role.ADMIN;
    }

    @Override
    public boolean canManageVeterinarians(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        // Only admins can manage veterinarian accounts
        return user.getRole() == Role.ADMIN;
    }

    @Override
    public boolean canAccessSystemSettings(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        // Only admins can access system settings
        return user.getRole() == Role.ADMIN;
    }

    @Override
    public boolean canExportData(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        // Admins and vets can export data
        return user.getRole() == Role.ADMIN || user.getRole() == Role.VET;
    }

    @Override
    public boolean canViewAuditLogs(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        // Only admins can view audit logs
        return user.getRole() == Role.ADMIN;
    }

    @Override
    public boolean hasRole(User user, Role role) {
        if (user == null || user.getRole() == null || role == null) {
            return false;
        }
        return user.getRole() == role;
    }

    @Override
    public boolean hasAnyRole(User user, Role... roles) {
        if (user == null || user.getRole() == null || roles == null || roles.length == 0) {
            return false;
        }
        
        for (Role role : roles) {
            if (user.getRole() == role) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getRoleDisplayName(User user) {
        if (user == null || user.getRole() == null) {
            return "Unknown";
        }
        return user.getRole().getDisplayName();
    }

    @Override
    public String getRoleDescription(User user) {
        if (user == null || user.getRole() == null) {
            return "No role assigned";
        }
        return user.getRole().getDescription();
    }

    @Override
    public boolean canAccessUserResource(User currentUser, User resourceOwner) {
        if (currentUser == null || resourceOwner == null) {
            return false;
        }
        
        // Users can always access their own resources
        if (currentUser.getId().equals(resourceOwner.getId())) {
            return true;
        }
        
        // Admins can access any user's resources
        if (currentUser.getRole() == Role.ADMIN) {
            return true;
        }
        
        // Vets can access staff resources but not other vets or admins
        if (currentUser.getRole() == Role.VET && resourceOwner.getRole() == Role.STAFF) {
            return true;
        }
        
        return false;
    }

    @Override
    public boolean canModifyUserResource(User currentUser, User resourceOwner) {
        if (currentUser == null || resourceOwner == null) {
            return false;
        }
        
        // Users can modify their own resources
        if (currentUser.getId().equals(resourceOwner.getId())) {
            return true;
        }
        
        // Only admins can modify other users' resources
        return currentUser.getRole() == Role.ADMIN;
    }
}