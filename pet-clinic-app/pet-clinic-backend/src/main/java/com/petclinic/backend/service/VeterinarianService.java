package com.petclinic.backend.service;

import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.model.VisitType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service interface for Veterinarian management operations
 * Extends BaseService with Veterinarian-specific functionality
 */
public interface VeterinarianService extends BaseService<Veterinarian, Long> {
    
    /**
     * Find all veterinarians with server-side pagination and sorting
     * @param pageable Pagination and sorting parameters
     * @return Page of veterinarians with server-side sorting applied
     */
    Page<Veterinarian> findAllWithPagination(Pageable pageable);
    
    /**
     * Find veterinarians by specialty
     * @param specialty Specialty to search for
     * @return List of veterinarians with the specified specialty
     */
    List<Veterinarian> findBySpecialty(String specialty);
    
    /**
     * Find available veterinarians at a specific date and time
     * @param dateTime Date and time to check availability
     * @return List of available veterinarians
     */
    List<Veterinarian> findAvailable(LocalDateTime dateTime);
    
    /**
     * Check if a veterinarian is available at a specific time
     * @param vetId Veterinarian ID
     * @param dateTime Date and time to check
     * @return true if available, false otherwise
     */
    boolean isVeterinarianAvailable(Long vetId, LocalDateTime dateTime);
    
    /**
     * Check if a veterinarian can be deleted (no scheduled or completed visits)
     * @param id Veterinarian ID
     * @return true if veterinarian can be deleted, false otherwise
     */
    boolean canDeleteVeterinarian(Long id);
    
    /**
     * Find veterinarians by license number
     * @param licenseNumber License number to search for
     * @return Veterinarian with the specified license number, if found
     */
    Veterinarian findByLicenseNumber(String licenseNumber);
    
    /**
     * Get veterinarians with the most visits
     * @param limit Maximum number of results
     * @return List of veterinarians ordered by visit count (descending)
     */
    List<Veterinarian> findMostActiveVeterinarians(int limit);
    
    /**
     * Search veterinarians by name
     * @param searchTerm Search term to match against first or last name
     * @return List of matching veterinarians
     */
    List<Veterinarian> searchByName(String searchTerm);
    
    /**
     * Search veterinarians by first name
     * @param firstName First name to search for
     * @return List of matching veterinarians
     */
    List<Veterinarian> searchByFirstName(String firstName);
    
    /**
     * Find veterinarians available for a specific visit type
     * @param visitType Visit type requiring specific specialties
     * @param dateTime Desired appointment time
     * @return List of available veterinarians with appropriate specialties
     */
    List<Veterinarian> findAvailableForVisitType(VisitType visitType, LocalDateTime dateTime);
    
    /**
     * Find veterinarians who can handle emergency cases
     * @param dateTime Emergency time
     * @return List of emergency-capable veterinarians
     */
    List<Veterinarian> findEmergencyVeterinarians(LocalDateTime dateTime);
    
    /**
     * Find veterinarians who can perform surgery
     * @param dateTime Surgery time
     * @return List of surgical veterinarians
     */
    List<Veterinarian> findSurgicalVeterinarians(LocalDateTime dateTime);
    
    /**
     * Get veterinarian workload statistics
     * @param vetId Veterinarian ID
     * @return Map containing workload statistics
     */
    Map<String, Object> getVeterinarianWorkload(Long vetId);
    
    /**
     * Find veterinarians with light workload (available for more appointments)
     * @param maxVisits Maximum number of visits to consider as light workload
     * @return List of veterinarians with light workload
     */
    List<Veterinarian> findVeterinariansWithLightWorkload(int maxVisits);
    
    /**
     * Get specialty distribution among veterinarians
     * @return Map of specialty to count
     */
    Map<String, Long> getSpecialtyDistribution();
}