package com.petclinic.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for user permissions and role information
 * Provides frontend with user permission data for UI customization
 */
@RestController
@RequestMapping("/api/user")
public class UserPermissionsController {
    
    /**
     * Get current user's permissions and roles
     * GET /api/user/permissions
     */
    @GetMapping("/permissions")
    public ResponseEntity<Map<String, Object>> getUserPermissions(Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(createGuestPermissions());
        }
        
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                .collect(Collectors.toList());
        
        Map<String, Object> permissions = new HashMap<>();
        permissions.put("username", authentication.getName());
        permissions.put("roles", roles);
        permissions.put("isAdmin", roles.contains("ADMIN"));
        permissions.put("canExportReports", roles.contains("ADMIN"));
        permissions.put("canManageUsers", roles.contains("ADMIN"));
        permissions.put("canViewReports", roles.contains("ADMIN") || roles.contains("USER"));
        
        return ResponseEntity.ok(permissions);
    }
    
    private Map<String, Object> createGuestPermissions() {
        Map<String, Object> permissions = new HashMap<>();
        permissions.put("username", "guest");
        permissions.put("roles", List.of());
        permissions.put("isAdmin", false);
        permissions.put("canExportReports", false);
        permissions.put("canManageUsers", false);
        permissions.put("canViewReports", false);
        
        return permissions;
    }
}