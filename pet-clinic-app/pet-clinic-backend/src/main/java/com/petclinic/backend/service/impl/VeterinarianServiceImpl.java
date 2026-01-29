package com.petclinic.backend.service.impl;

import com.petclinic.backend.config.CacheConfig;
import com.petclinic.backend.exception.BusinessRuleException;
import com.petclinic.backend.exception.EntityNotFoundException;
import com.petclinic.backend.exception.ValidationException;
import com.petclinic.backend.model.Specialty;
import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.model.VisitType;
import com.petclinic.backend.repository.VeterinarianRepository;
import com.petclinic.backend.service.VeterinarianService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of VeterinarianService providing comprehensive veterinarian management functionality
 * with availability tracking, specialty-based filtering, and working hours management.
 * Validates: Requirements 3.3, 3.4, 3.5
 */
@Service
@Transactional
public class VeterinarianServiceImpl implements VeterinarianService {
    
    private static final Logger logger = LoggerFactory.getLogger(VeterinarianServiceImpl.class);
    
    // Working hours configuration (can be externalized to properties)
    private static final LocalTime STANDARD_START_TIME = LocalTime.of(8, 0);  // 8:00 AM
    private static final LocalTime STANDARD_END_TIME = LocalTime.of(18, 0);   // 6:00 PM
    private static final LocalTime EMERGENCY_START_TIME = LocalTime.of(0, 0); // 24/7 for emergency
    private static final LocalTime EMERGENCY_END_TIME = LocalTime.of(23, 59);
    private static final Set<DayOfWeek> STANDARD_WORKING_DAYS = EnumSet.of(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    );
    private static final Set<DayOfWeek> EMERGENCY_WORKING_DAYS = EnumSet.allOf(DayOfWeek.class);
    
    @Autowired
    private VeterinarianRepository veterinarianRepository;
    
    // CRUD Operations
    
    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.VETERINARIANS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.STATISTICS_CACHE, allEntries = true)
    })
    public Veterinarian create(Veterinarian veterinarian) {
        logger.debug("Creating new veterinarian: {}", veterinarian);
        
        validateVeterinarianForCreation(veterinarian);
        
        // Check for duplicate license number
        if (StringUtils.hasText(veterinarian.getLicenseNumber())) {
            boolean exists = veterinarianRepository.existsByLicenseNumber(veterinarian.getLicenseNumber());
            if (exists) {
                throw new ValidationException("License number '" + veterinarian.getLicenseNumber() + 
                    "' already exists");
            }
        }
        
        Veterinarian savedVeterinarian = veterinarianRepository.save(veterinarian);
        logger.info("Created veterinarian with ID: {} and license: {}", 
                   savedVeterinarian.getId(), savedVeterinarian.getLicenseNumber());
        return savedVeterinarian;
    }
    
    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.VETERINARIANS_CACHE, key = "#id"),
        @CacheEvict(value = CacheConfig.VETERINARIANS_CACHE, key = "'all'"),
        @CacheEvict(value = CacheConfig.STATISTICS_CACHE, allEntries = true)
    })
    public Veterinarian update(Long id, Veterinarian veterinarian) {
        logger.debug("Updating veterinarian with ID: {}", id);
        
        Veterinarian existingVeterinarian = veterinarianRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Veterinarian", id));
        
        validateVeterinarianForUpdate(veterinarian, existingVeterinarian);
        
        // Check for duplicate license number if it's being changed
        if (!existingVeterinarian.getLicenseNumber().equals(veterinarian.getLicenseNumber())) {
            boolean exists = veterinarianRepository.existsByLicenseNumber(veterinarian.getLicenseNumber());
            if (exists) {
                throw new ValidationException("License number '" + veterinarian.getLicenseNumber() + 
                    "' already exists");
            }
        }
        
        // Update fields
        updateVeterinarianFields(existingVeterinarian, veterinarian);
        
        Veterinarian updatedVeterinarian = veterinarianRepository.save(existingVeterinarian);
        logger.info("Updated veterinarian with ID: {}", updatedVeterinarian.getId());
        return updatedVeterinarian;
    }
    
    @Override
    @Cacheable(value = CacheConfig.VETERINARIANS_CACHE, key = "#id")
    public Optional<Veterinarian> findById(Long id) {
        logger.debug("Finding veterinarian by ID: {}", id);
        return veterinarianRepository.findById(id);
    }
    
    @Override
    @Cacheable(value = CacheConfig.VETERINARIANS_CACHE, key = "'all'")
    public List<Veterinarian> findAll() {
        logger.debug("Finding all veterinarians");
        return veterinarianRepository.findAll(Sort.by(Sort.Direction.ASC, "lastName", "firstName"));
    }
    
    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.VETERINARIANS_CACHE, key = "#id"),
        @CacheEvict(value = CacheConfig.VETERINARIANS_CACHE, key = "'all'"),
        @CacheEvict(value = CacheConfig.STATISTICS_CACHE, allEntries = true)
    })
    public void deleteById(Long id) {
        logger.debug("Attempting to delete veterinarian with ID: {}", id);
        
        Veterinarian veterinarian = veterinarianRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Veterinarian", id));
        
        // Business rule: Cannot delete veterinarian with existing visits
        if (!canDeleteVeterinarian(id)) {
            throw new BusinessRuleException("Cannot delete veterinarian with existing visits. " +
                "Dr. " + veterinarian.getFullName() + " has " + veterinarian.getVisitCount() + " visit(s) on record.");
        }
        
        veterinarianRepository.deleteById(id);
        logger.info("Deleted veterinarian with ID: {}", id);
    }
    
    @Override
    public boolean existsById(Long id) {
        return veterinarianRepository.existsById(id);
    }
    
    @Override
    public long count() {
        return veterinarianRepository.count();
    }
    
    // Veterinarian-specific methods
    
    @Override
    @Cacheable(value = CacheConfig.VETERINARIANS_CACHE, key = "'specialty-' + #specialty")
    public List<Veterinarian> findBySpecialty(String specialty) {
        logger.debug("Finding veterinarians by specialty: {}", specialty);
        
        if (!StringUtils.hasText(specialty)) {
            throw new ValidationException("Specialty cannot be null or empty");
        }
        
        String trimmedSpecialty = specialty.trim();
        
        // Try to find by enum specialty first
        Specialty specialtyEnum = Specialty.findByDisplayName(trimmedSpecialty);
        if (specialtyEnum != null) {
            List<Veterinarian> enumResults = veterinarianRepository.findBySpecialtyEnum(specialtyEnum);
            if (!enumResults.isEmpty()) {
                return enumResults;
            }
        }
        
        // Fallback to string-based search for backward compatibility
        return veterinarianRepository.findBySpecialty(trimmedSpecialty);
    }
    
    @Override
    public List<Veterinarian> findAvailable(LocalDateTime dateTime) {
        logger.debug("Finding available veterinarians at: {}", dateTime);
        
        if (dateTime == null) {
            throw new ValidationException("DateTime cannot be null");
        }
        
        // Get all veterinarians who are working at this time
        List<Veterinarian> workingVeterinarians = findVeterinariansWorkingAt(dateTime);
        
        // Filter out those who have conflicting appointments
        return workingVeterinarians.stream()
            .filter(vet -> isVeterinarianAvailable(vet.getId(), dateTime))
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean isVeterinarianAvailable(Long vetId, LocalDateTime dateTime) {
        logger.debug("Checking availability for veterinarian {} at {}", vetId, dateTime);
        
        if (vetId == null || dateTime == null) {
            return false;
        }
        
        // Check if veterinarian exists
        Optional<Veterinarian> vetOpt = veterinarianRepository.findById(vetId);
        if (vetOpt.isEmpty()) {
            return false;
        }
        
        Veterinarian veterinarian = vetOpt.get();
        
        // Check working hours
        if (!isWithinWorkingHours(veterinarian, dateTime)) {
            logger.debug("Veterinarian {} not working at {}", vetId, dateTime);
            return false;
        }
        
        // Check for scheduling conflicts (assume 30-minute default duration for checking)
        LocalDateTime endTime = dateTime.plusMinutes(30);
        boolean hasConflict = veterinarianRepository.isVeterinarianAvailable(vetId, dateTime, endTime);
        
        logger.debug("Veterinarian {} availability at {}: {}", vetId, dateTime, hasConflict);
        return hasConflict;
    }
    
    @Override
    public boolean canDeleteVeterinarian(Long id) {
        logger.debug("Checking if veterinarian can be deleted: {}", id);
        
        if (id == null) {
            return false;
        }
        
        Optional<Veterinarian> vetOpt = veterinarianRepository.findById(id);
        if (vetOpt.isEmpty()) {
            return false;
        }
        
        Veterinarian veterinarian = vetOpt.get();
        boolean canDelete = veterinarian.getVisits().isEmpty();
        
        logger.debug("Veterinarian {} can be deleted: {}", id, canDelete);
        return canDelete;
    }
    
    @Override
    public Veterinarian findByLicenseNumber(String licenseNumber) {
        logger.debug("Finding veterinarian by license number: {}", licenseNumber);
        
        if (!StringUtils.hasText(licenseNumber)) {
            throw new ValidationException("License number cannot be null or empty");
        }
        
        return veterinarianRepository.findByLicenseNumber(licenseNumber.trim())
            .orElseThrow(() -> new EntityNotFoundException("Veterinarian with license number: " + licenseNumber));
    }
    
    @Override
    public List<Veterinarian> findMostActiveVeterinarians(int limit) {
        logger.debug("Finding most active veterinarians, limit: {}", limit);
        
        if (limit <= 0) {
            throw new ValidationException("Limit must be greater than 0");
        }
        
        return veterinarianRepository.findVeterinariansByVisitCount(PageRequest.of(0, limit));
    }
    
    @Override
    public List<Veterinarian> searchByName(String searchTerm) {
        logger.debug("Searching veterinarians by name: {}", searchTerm);
        
        if (!StringUtils.hasText(searchTerm)) {
            return findAll();
        }
        
        String trimmedTerm = searchTerm.trim();
        
        // Search by full name first
        List<Veterinarian> fullNameResults = veterinarianRepository.findByFullNameContaining(trimmedTerm);
        if (!fullNameResults.isEmpty()) {
            return fullNameResults;
        }
        
        // Search by first name and last name separately
        List<Veterinarian> firstNameResults = veterinarianRepository.findByFirstNameContainingIgnoreCase(trimmedTerm);
        List<Veterinarian> lastNameResults = veterinarianRepository.findByLastNameContainingIgnoreCase(trimmedTerm);
        
        // Combine results and remove duplicates
        Set<Veterinarian> combinedResults = new HashSet<>(firstNameResults);
        combinedResults.addAll(lastNameResults);
        
        return new ArrayList<>(combinedResults);
    }
    
    // Additional business logic methods
    
    /**
     * Find veterinarians available for a specific visit type
     * @param visitType Visit type requiring specific specialties
     * @param dateTime Desired appointment time
     * @return List of available veterinarians with appropriate specialties
     */
    public List<Veterinarian> findAvailableForVisitType(VisitType visitType, LocalDateTime dateTime) {
        logger.debug("Finding veterinarians available for visit type {} at {}", visitType, dateTime);
        
        if (visitType == null || dateTime == null) {
            throw new ValidationException("Visit type and date time cannot be null");
        }
        
        // Get veterinarians with the required specialty
        Specialty requiredSpecialty = visitType.getRecommendedSpecialty();
        List<Veterinarian> specialistVeterinarians;
        
        if (requiredSpecialty != null && visitType.requiresSpecialist()) {
            specialistVeterinarians = findBySpecialty(requiredSpecialty.getDisplayName());
        } else {
            // For general visits, any veterinarian can handle
            specialistVeterinarians = findAll();
        }
        
        // Filter by availability
        return specialistVeterinarians.stream()
            .filter(vet -> isVeterinarianAvailable(vet.getId(), dateTime))
            .collect(Collectors.toList());
    }
    
    /**
     * Find veterinarians who can handle emergency cases
     * @param dateTime Emergency time
     * @return List of emergency-capable veterinarians
     */
    public List<Veterinarian> findEmergencyVeterinarians(LocalDateTime dateTime) {
        logger.debug("Finding emergency veterinarians at {}", dateTime);
        
        if (dateTime == null) {
            throw new ValidationException("DateTime cannot be null");
        }
        
        // Get emergency specialties
        List<Specialty> emergencySpecialties = Arrays.stream(Specialty.values())
            .filter(Specialty::isEmergency)
            .collect(Collectors.toList());
        
        List<Veterinarian> emergencyVets = veterinarianRepository.findEmergencyVeterinarians(emergencySpecialties);
        
        // Filter by availability (emergency vets work 24/7)
        return emergencyVets.stream()
            .filter(vet -> isVeterinarianAvailable(vet.getId(), dateTime))
            .collect(Collectors.toList());
    }
    
    /**
     * Find veterinarians who can perform surgery
     * @param dateTime Surgery time
     * @return List of surgical veterinarians
     */
    public List<Veterinarian> findSurgicalVeterinarians(LocalDateTime dateTime) {
        logger.debug("Finding surgical veterinarians at {}", dateTime);
        
        if (dateTime == null) {
            throw new ValidationException("DateTime cannot be null");
        }
        
        // Get surgical specialties
        List<Specialty> surgicalSpecialties = Arrays.stream(Specialty.values())
            .filter(Specialty::isSurgical)
            .collect(Collectors.toList());
        
        List<Veterinarian> surgicalVets = veterinarianRepository.findSurgicalVeterinarians(surgicalSpecialties);
        
        // Filter by availability
        return surgicalVets.stream()
            .filter(vet -> isVeterinarianAvailable(vet.getId(), dateTime))
            .collect(Collectors.toList());
    }
    
    /**
     * Get veterinarian workload statistics
     * @param vetId Veterinarian ID
     * @return Map containing workload statistics
     */
    public Map<String, Object> getVeterinarianWorkload(Long vetId) {
        logger.debug("Getting workload for veterinarian {}", vetId);
        
        if (vetId == null) {
            throw new ValidationException("Veterinarian ID cannot be null");
        }
        
        Veterinarian veterinarian = veterinarianRepository.findById(vetId)
            .orElseThrow(() -> new EntityNotFoundException("Veterinarian", vetId));
        
        Map<String, Object> workload = new HashMap<>();
        workload.put("veterinarianId", vetId);
        workload.put("fullName", veterinarian.getFullName());
        workload.put("totalVisits", veterinarian.getVisitCount());
        workload.put("specialties", veterinarian.getSpecialtyList());
        
        // Calculate upcoming visits
        long upcomingVisits = veterinarian.getVisits().stream()
            .filter(visit -> visit.getVisitDate().isAfter(LocalDateTime.now()))
            .count();
        workload.put("upcomingVisits", upcomingVisits);
        
        // Calculate completed visits
        long completedVisits = veterinarian.getVisits().stream()
            .filter(Visit::isCompleted)
            .count();
        workload.put("completedVisits", completedVisits);
        
        return workload;
    }
    
    /**
     * Find veterinarians with light workload (available for more appointments)
     * @param maxVisits Maximum number of visits to consider as light workload
     * @return List of veterinarians with light workload
     */
    public List<Veterinarian> findVeterinariansWithLightWorkload(int maxVisits) {
        logger.debug("Finding veterinarians with light workload (max {} visits)", maxVisits);
        
        if (maxVisits < 0) {
            throw new ValidationException("Max visits must be non-negative");
        }
        
        return veterinarianRepository.findAvailableVeterinarians(maxVisits);
    }
    
    /**
     * Get specialty distribution among veterinarians
     * @return Map of specialty to count
     */
    @Override
    @Cacheable(value = CacheConfig.STATISTICS_CACHE, key = "'specialty-distribution'")
    public Map<String, Long> getSpecialtyDistribution() {
        logger.debug("Getting specialty distribution");
        
        List<Object[]> specialtyEnumCounts = veterinarianRepository.countVeterinariansBySpecialtyEnum();
        Map<String, Long> distribution = new HashMap<>();
        
        for (Object[] result : specialtyEnumCounts) {
            Specialty specialty = (Specialty) result[0];
            Long count = (Long) result[1];
            distribution.put(specialty.getDisplayName(), count);
        }
        
        return distribution;
    }
    
    // Private helper methods
    
    private void validateVeterinarianForCreation(Veterinarian veterinarian) {
        if (veterinarian == null) {
            throw new ValidationException("Veterinarian cannot be null");
        }
        
        if (!StringUtils.hasText(veterinarian.getFirstName())) {
            throw new ValidationException("First name is required");
        }
        
        if (!StringUtils.hasText(veterinarian.getLastName())) {
            throw new ValidationException("Last name is required");
        }
        
        if (!StringUtils.hasText(veterinarian.getLicenseNumber())) {
            throw new ValidationException("License number is required");
        }
        
        // Validate license number format
        if (!veterinarian.getLicenseNumber().matches("^[A-Z0-9]{6,20}$")) {
            throw new ValidationException("License number must be 6-20 alphanumeric characters");
        }
        
        // Validate name lengths
        if (veterinarian.getFirstName().length() > 50) {
            throw new ValidationException("First name cannot exceed 50 characters");
        }
        
        if (veterinarian.getLastName().length() > 50) {
            throw new ValidationException("Last name cannot exceed 50 characters");
        }
        
        // Validate specialties if provided
        if (veterinarian.getSpecialtySet() != null) {
            for (Specialty specialty : veterinarian.getSpecialtySet()) {
                if (specialty == null) {
                    throw new ValidationException("Invalid specialty in specialty set");
                }
            }
        }
    }
    
    private void validateVeterinarianForUpdate(Veterinarian veterinarian, Veterinarian existingVeterinarian) {
        if (veterinarian == null) {
            throw new ValidationException("Veterinarian cannot be null");
        }
        
        // Use existing values if not provided
        if (!StringUtils.hasText(veterinarian.getFirstName())) {
            veterinarian.setFirstName(existingVeterinarian.getFirstName());
        }
        
        if (!StringUtils.hasText(veterinarian.getLastName())) {
            veterinarian.setLastName(existingVeterinarian.getLastName());
        }
        
        if (!StringUtils.hasText(veterinarian.getLicenseNumber())) {
            veterinarian.setLicenseNumber(existingVeterinarian.getLicenseNumber());
        }
        
        // Validate the updated veterinarian
        validateVeterinarianForCreation(veterinarian);
    }
    
    private void updateVeterinarianFields(Veterinarian existingVeterinarian, Veterinarian updatedVeterinarian) {
        if (StringUtils.hasText(updatedVeterinarian.getFirstName())) {
            existingVeterinarian.setFirstName(updatedVeterinarian.getFirstName());
        }
        
        if (StringUtils.hasText(updatedVeterinarian.getLastName())) {
            existingVeterinarian.setLastName(updatedVeterinarian.getLastName());
        }
        
        if (StringUtils.hasText(updatedVeterinarian.getLicenseNumber())) {
            existingVeterinarian.setLicenseNumber(updatedVeterinarian.getLicenseNumber());
        }
        
        if (updatedVeterinarian.getSpecialties() != null) {
            existingVeterinarian.setSpecialties(updatedVeterinarian.getSpecialties());
        }
        
        if (updatedVeterinarian.getSpecialtySet() != null) {
            existingVeterinarian.setSpecialtySet(updatedVeterinarian.getSpecialtySet());
        }
    }
    
    private List<Veterinarian> findVeterinariansWorkingAt(LocalDateTime dateTime) {
        // Get all veterinarians
        List<Veterinarian> allVeterinarians = findAll();
        
        // Filter by working hours
        return allVeterinarians.stream()
            .filter(vet -> isWithinWorkingHours(vet, dateTime))
            .collect(Collectors.toList());
    }
    
    private boolean isWithinWorkingHours(Veterinarian veterinarian, LocalDateTime dateTime) {
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        LocalTime time = dateTime.toLocalTime();
        
        // Check if veterinarian can handle emergencies (24/7 availability)
        if (veterinarian.canHandleEmergencies()) {
            return EMERGENCY_WORKING_DAYS.contains(dayOfWeek) &&
                   !time.isBefore(EMERGENCY_START_TIME) &&
                   !time.isAfter(EMERGENCY_END_TIME);
        }
        
        // Standard working hours
        return STANDARD_WORKING_DAYS.contains(dayOfWeek) &&
               !time.isBefore(STANDARD_START_TIME) &&
               !time.isAfter(STANDARD_END_TIME);
    }
}