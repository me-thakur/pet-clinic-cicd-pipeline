package com.petclinic.backend.service.impl;

import com.petclinic.backend.config.CacheConfig;
import com.petclinic.backend.exception.BusinessRuleException;
import com.petclinic.backend.exception.ConflictException;
import com.petclinic.backend.exception.EntityNotFoundException;
import com.petclinic.backend.exception.ValidationException;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.model.VisitType;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.repository.VeterinarianRepository;
import com.petclinic.backend.repository.VisitRepository;
import com.petclinic.backend.service.AuditService;
import com.petclinic.backend.service.VisitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of VisitService providing comprehensive visit management and scheduling functionality
 * Validates: Requirements 2.2, 2.3, 2.4
 */
@Service
@Transactional
public class VisitServiceImpl implements VisitService {
    
    private static final Logger logger = LoggerFactory.getLogger(VisitServiceImpl.class);
    
    @Autowired
    private VisitRepository visitRepository;
    
    @Autowired
    private PetRepository petRepository;
    
    @Autowired
    private VeterinarianRepository veterinarianRepository;
    
    @Autowired
    private AuditService auditService;
    
    // BaseService implementation
    
    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.VISITS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.STATISTICS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.DASHBOARD_METRICS_CACHE, allEntries = true)
    })
    public Visit create(Visit visit) {
        logger.debug("Creating new visit: {}", visit);
        
        validateVisitForCreation(visit);
        
        Visit savedVisit = visitRepository.save(visit);
        logger.info("Created visit with ID: {}", savedVisit.getId());
        
        // Log visit creation
        auditService.logDataAccess("system", "CREATE", "Visit", savedVisit.getId(), 
                                  "New visit scheduled for pet ID: " + 
                                  (savedVisit.getPet() != null ? savedVisit.getPet().getId() : "unknown") +
                                  " with veterinarian ID: " + 
                                  (savedVisit.getVeterinarian() != null ? savedVisit.getVeterinarian().getId() : "unknown"));
        
        return savedVisit;
    }
    
    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.VISITS_CACHE, key = "#id"),
        @CacheEvict(value = CacheConfig.VISITS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.STATISTICS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.DASHBOARD_METRICS_CACHE, allEntries = true)
    })
    public Visit update(Long id, Visit visit) {
        logger.debug("Updating visit with ID: {}", id);
        
        Visit existingVisit = visitRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Visit", id));
        
        validateVisitForUpdate(visit, existingVisit);
        
        // Update fields
        updateVisitFields(existingVisit, visit);
        
        Visit updatedVisit = visitRepository.save(existingVisit);
        logger.info("Updated visit with ID: {}", updatedVisit.getId());
        
        // Log visit update
        auditService.logDataAccess("system", "UPDATE", "Visit", updatedVisit.getId(), 
                                  "Visit updated for pet ID: " + 
                                  (updatedVisit.getPet() != null ? updatedVisit.getPet().getId() : "unknown"));
        
        return updatedVisit;
    }
    
    @Override
    @Cacheable(value = CacheConfig.VISITS_CACHE, key = "#id")
    public Optional<Visit> findById(Long id) {
        logger.debug("Finding visit by ID: {}", id);
        Optional<Visit> visit = visitRepository.findById(id);
        
        if (visit.isPresent()) {
            // Log sensitive visit data access
            auditService.logDataAccess("system", "READ", "Visit", id, 
                                      "Visit data accessed for pet ID: " + 
                                      (visit.get().getPet() != null ? visit.get().getPet().getId() : "unknown"));
        }
        
        return visit;
    }
    
    @Override
    public List<Visit> findAll() {
        logger.debug("Finding all visits");
        return visitRepository.findAll();
    }
    
    @Override
    public void deleteById(Long id) {
        logger.debug("Deleting visit with ID: {}", id);
        
        if (!visitRepository.existsById(id)) {
            throw new EntityNotFoundException("Visit", id);
        }
        
        // Log visit deletion before actual deletion
        auditService.logDataAccess("system", "DELETE", "Visit", id, 
                                  "Visit deleted");
        
        visitRepository.deleteById(id);
        logger.info("Deleted visit with ID: {}", id);
    }
    
    @Override
    public boolean existsById(Long id) {
        return visitRepository.existsById(id);
    }
    
    @Override
    public long count() {
        return visitRepository.count();
    }
    
    // VisitService specific methods
    
    @Override
    public Visit scheduleVisit(Visit visit) {
        logger.debug("Scheduling visit: {}", visit);
        
        validateVisitForScheduling(visit);
        
        // Check for scheduling conflicts
        if (visit.getVeterinarian() != null && visit.getVisitDate() != null) {
            int duration = visit.getDuration() != null ? visit.getDuration() : 30;
            if (hasSchedulingConflict(visit.getVeterinarian().getId(), visit.getVisitDate(), duration)) {
                // Allow emergency visits to override conflicts
                if (visit.isEmergencyVisit()) {
                    logger.warn("Emergency visit overriding scheduling conflict for veterinarian {} at {}", 
                        visit.getVeterinarian().getId(), visit.getVisitDate());
                } else {
                    throw new ConflictException("Veterinarian scheduling conflict", 
                        String.format("Dr. %s %s is not available at %s", 
                            visit.getVeterinarian().getFirstName(),
                            visit.getVeterinarian().getLastName(),
                            visit.getVisitDate()));
                }
            }
        }
        
        // Set default duration if not specified
        if (visit.getDuration() == null && visit.getVisitType() != null) {
            visit.setDuration(visit.getVisitType().getDefaultDurationMinutes());
        } else if (visit.getDuration() == null) {
            visit.setDuration(30); // Default duration
        }
        
        Visit scheduledVisit = visitRepository.save(visit);
        logger.info("Scheduled visit with ID: {} for pet {} with veterinarian {} at {}", 
            scheduledVisit.getId(), 
            scheduledVisit.getPet().getName(),
            scheduledVisit.getVeterinarian() != null ? scheduledVisit.getVeterinarian().getLastName() : "TBD",
            scheduledVisit.getVisitDate());
        
        return scheduledVisit;
    }
    
    @Override
    public Visit completeVisit(Long id, String diagnosis, String treatment, String notes) {
        logger.debug("Completing visit with ID: {}", id);
        
        Visit visit = visitRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Visit", id));
        
        validateVisitCompletion(diagnosis, treatment);
        
        // Cannot complete visits that are in the future
        if (visit.getVisitDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot complete future visits", 
                "Visit is scheduled for " + visit.getVisitDate());
        }
        
        // Cannot complete already completed visits unless updating
        if (visit.isCompleted() && !StringUtils.hasText(diagnosis) && !StringUtils.hasText(treatment)) {
            throw new BusinessRuleException("Visit is already completed", 
                "Use update operation to modify completed visit details");
        }
        
        visit.setDiagnosis(diagnosis);
        visit.setTreatment(treatment);
        visit.setNotes(notes);
        
        Visit completedVisit = visitRepository.save(visit);
        logger.info("Completed visit with ID: {} - Diagnosis: {}, Treatment: {}", 
            completedVisit.getId(), diagnosis, treatment);
        
        return completedVisit;
    }
    
    @Override
    @Cacheable(value = CacheConfig.VISITS_CACHE, key = "'pet-' + #petId")
    public List<Visit> findByPet(Long petId) {
        logger.debug("Finding visits for pet ID: {}", petId);
        
        if (!petRepository.existsById(petId)) {
            throw new EntityNotFoundException("Pet", petId);
        }
        
        return visitRepository.findByPetId(petId);
    }
    
    @Override
    @Cacheable(value = CacheConfig.VISITS_CACHE, key = "'vet-' + #vetId")
    public List<Visit> findByVeterinarian(Long vetId) {
        logger.debug("Finding visits for veterinarian ID: {}", vetId);
        
        if (!veterinarianRepository.existsById(vetId)) {
            throw new EntityNotFoundException("Veterinarian", vetId);
        }
        
        return visitRepository.findByVeterinarianId(vetId);
    }
    
    @Override
    public List<Visit> findByDateRange(LocalDate startDate, LocalDate endDate) {
        logger.debug("Finding visits between {} and {}", startDate, endDate);
        
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date are required");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new ValidationException("Start date must be before or equal to end date");
        }
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        return visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
    }
    
    @Override
    public List<Visit> findByDate(LocalDate date) {
        logger.debug("Finding visits for date: {}", date);
        
        if (date == null) {
            throw new ValidationException("Date is required");
        }
        
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        return visitRepository.findByVisitDateBetween(startOfDay, endOfDay);
    }
    
    @Override
    public boolean hasSchedulingConflict(Long vetId, LocalDateTime dateTime, int duration) {
        logger.debug("Checking scheduling conflict for veterinarian {} at {} for {} minutes", 
            vetId, dateTime, duration);
        
        if (vetId == null || dateTime == null) {
            return false;
        }
        
        // Check for overlapping appointments
        LocalDateTime endTime = dateTime.plusMinutes(duration);
        
        // Find visits that might conflict (simple approach - check visits in the time window)
        List<Visit> conflictingVisits = visitRepository.findConflictingVisits(
            vetId, dateTime.minusMinutes(120), endTime.plusMinutes(120));
        
        for (Visit existingVisit : conflictingVisits) {
            LocalDateTime existingStart = existingVisit.getVisitDate();
            LocalDateTime existingEnd = existingVisit.getEndTime();
            
            // Check for overlap: new visit starts before existing ends AND new visit ends after existing starts
            if (dateTime.isBefore(existingEnd) && endTime.isAfter(existingStart)) {
                logger.debug("Scheduling conflict found with visit ID: {} at {}", 
                    existingVisit.getId(), existingVisit.getVisitDate());
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public List<Visit> getUpcomingVisits(Long vetId, int days) {
        logger.debug("Finding upcoming visits for veterinarian {} for next {} days", vetId, days);
        
        if (vetId != null && !veterinarianRepository.existsById(vetId)) {
            throw new EntityNotFoundException("Veterinarian", vetId);
        }
        
        LocalDateTime fromDate = LocalDateTime.now();
        LocalDateTime toDate = fromDate.plusDays(days);
        
        if (vetId != null) {
            return visitRepository.findByVeterinarianIdAndVisitDateBetween(vetId, fromDate, toDate);
        } else {
            return visitRepository.findByVisitDateBetween(fromDate, toDate);
        }
    }
    
    @Override
    public List<Visit> findCompletedVisits() {
        logger.debug("Finding completed visits");
        return visitRepository.findCompletedVisits();
    }
    
    @Override
    public List<Visit> findByCostRange(BigDecimal minCost, BigDecimal maxCost) {
        logger.debug("Finding visits with cost between {} and {}", minCost, maxCost);
        
        if (minCost == null || maxCost == null) {
            throw new ValidationException("Min cost and max cost are required");
        }
        
        if (minCost.compareTo(maxCost) > 0) {
            throw new ValidationException("Min cost must be less than or equal to max cost");
        }
        
        return visitRepository.findByCostBetween(minCost, maxCost);
    }
    
    @Override
    public VisitStatistics getVisitStatistics(LocalDate startDate, LocalDate endDate) {
        logger.debug("Calculating visit statistics between {} and {}", startDate, endDate);
        
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date are required");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new ValidationException("Start date must be before or equal to end date");
        }
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        
        long totalVisits = visits.size();
        long completedVisits = visits.stream()
            .mapToLong(v -> v.isCompleted() ? 1 : 0)
            .sum();
        long scheduledVisits = totalVisits - completedVisits;
        
        BigDecimal totalRevenue = visits.stream()
            .filter(v -> v.getCost() != null)
            .map(Visit::getCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        double averageCost = visits.stream()
            .filter(v -> v.getCost() != null)
            .mapToDouble(v -> v.getCost().doubleValue())
            .average()
            .orElse(0.0);
        
        return new VisitStatistics(totalVisits, completedVisits, scheduledVisits, totalRevenue, averageCost);
    }
    
    // Additional helper methods for extended functionality
    
    /**
     * Reschedule an existing visit
     */
    public Visit rescheduleVisit(Long visitId, LocalDateTime newDateTime) {
        logger.debug("Rescheduling visit {} to {}", visitId, newDateTime);
        
        Visit visit = visitRepository.findById(visitId)
            .orElseThrow(() -> new EntityNotFoundException("Visit", visitId));
        
        if (visit.isCompleted()) {
            throw new BusinessRuleException("Cannot reschedule completed visits");
        }
        
        if (newDateTime.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Cannot reschedule visit to past date");
        }
        
        // Check for conflicts at new time
        if (visit.getVeterinarian() != null) {
            int duration = visit.getDuration() != null ? visit.getDuration() : 30;
            if (hasSchedulingConflict(visit.getVeterinarian().getId(), newDateTime, duration)) {
                throw new ConflictException("Veterinarian scheduling conflict at new time");
            }
        }
        
        visit.setVisitDate(newDateTime);
        Visit rescheduledVisit = visitRepository.save(visit);
        
        logger.info("Rescheduled visit {} to {}", visitId, newDateTime);
        return rescheduledVisit;
    }
    
    /**
     * Cancel a visit
     */
    public Visit cancelVisit(Long visitId, String reason) {
        logger.debug("Cancelling visit {} with reason: {}", visitId, reason);
        
        Visit visit = visitRepository.findById(visitId)
            .orElseThrow(() -> new EntityNotFoundException("Visit", visitId));
        
        if (visit.isCompleted()) {
            throw new BusinessRuleException("Cannot cancel completed visits");
        }
        
        // Add cancellation note
        String cancellationNote = "CANCELLED: " + (reason != null ? reason : "No reason provided");
        String existingNotes = visit.getNotes();
        if (StringUtils.hasText(existingNotes)) {
            visit.setNotes(existingNotes + "\n" + cancellationNote);
        } else {
            visit.setNotes(cancellationNote);
        }
        
        Visit cancelledVisit = visitRepository.save(visit);
        logger.info("Cancelled visit {} with reason: {}", visitId, reason);
        
        return cancelledVisit;
    }
    
    /**
     * Find visits by pet ID
     */
    public List<Visit> findVisitsByPet(Long petId) {
        return findByPet(petId);
    }
    
    /**
     * Find visits by veterinarian ID
     */
    public List<Visit> findVisitsByVeterinarian(Long veterinarianId) {
        return findByVeterinarian(veterinarianId);
    }
    
    /**
     * Find visits by date range
     */
    public List<Visit> findVisitsByDateRange(LocalDateTime start, LocalDateTime end) {
        logger.debug("Finding visits between {} and {}", start, end);
        
        if (start == null || end == null) {
            throw new ValidationException("Start and end date times are required");
        }
        
        if (start.isAfter(end)) {
            throw new ValidationException("Start date must be before or equal to end date");
        }
        
        return visitRepository.findByVisitDateBetween(start, end);
    }
    
    /**
     * Find upcoming visits
     */
    public List<Visit> findUpcomingVisits() {
        logger.debug("Finding all upcoming visits");
        return visitRepository.findUpcomingVisits(LocalDateTime.now());
    }
    
    /**
     * Find today's visits
     */
    public List<Visit> findTodaysVisits() {
        logger.debug("Finding today's visits");
        return visitRepository.findTodaysVisits();
    }
    
    /**
     * Check for scheduling conflicts
     */
    public boolean checkSchedulingConflicts(Long veterinarianId, LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Checking scheduling conflicts for veterinarian {} between {} and {}", 
            veterinarianId, startTime, endTime);
        
        if (veterinarianId == null || startTime == null || endTime == null) {
            return false;
        }
        
        List<Visit> conflictingVisits = visitRepository.findConflictingVisits(
            veterinarianId, startTime, endTime);
        
        return !conflictingVisits.isEmpty();
    }
    
    /**
     * Get visit statistics
     */
    public VisitStatistics getVisitStatistics() {
        logger.debug("Getting overall visit statistics");
        
        // Get all visits for overall statistics
        List<Visit> allVisits = visitRepository.findAll();
        
        long totalVisits = allVisits.size();
        long completedVisits = allVisits.stream()
            .mapToLong(v -> v.isCompleted() ? 1 : 0)
            .sum();
        long scheduledVisits = totalVisits - completedVisits;
        
        BigDecimal totalRevenue = allVisits.stream()
            .filter(v -> v.getCost() != null)
            .map(Visit::getCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        double averageCost = allVisits.stream()
            .filter(v -> v.getCost() != null)
            .mapToDouble(v -> v.getCost().doubleValue())
            .average()
            .orElse(0.0);
        
        return new VisitStatistics(totalVisits, completedVisits, scheduledVisits, totalRevenue, averageCost);
    }
    
    /**
     * Find visits by owner
     */
    public List<Visit> findVisitsByOwner(Long ownerId) {
        logger.debug("Finding visits for owner ID: {}", ownerId);
        
        if (ownerId == null) {
            throw new ValidationException("Owner ID is required");
        }
        
        return visitRepository.findByOwnerId(ownerId);
    }
    
    // Private validation and helper methods
    
    private void validateVisitForCreation(Visit visit) {
        if (visit == null) {
            throw new ValidationException("Visit cannot be null");
        }
        
        if (visit.getPet() == null) {
            throw new ValidationException("Pet is required for visit");
        }
        
        if (visit.getVisitDate() == null) {
            throw new ValidationException("Visit date is required");
        }
        
        // Validate pet exists
        if (!petRepository.existsById(visit.getPet().getId())) {
            throw new EntityNotFoundException("Pet", visit.getPet().getId());
        }
        
        // Validate veterinarian exists if specified
        if (visit.getVeterinarian() != null && 
            !veterinarianRepository.existsById(visit.getVeterinarian().getId())) {
            throw new EntityNotFoundException("Veterinarian", visit.getVeterinarian().getId());
        }
    }
    
    private void validateVisitForScheduling(Visit visit) {
        validateVisitForCreation(visit);
        
        // Cannot schedule visits in the past (except for emergency visits)
        if (visit.getVisitDate().isBefore(LocalDateTime.now()) && !visit.isEmergencyVisit()) {
            throw new ValidationException("Cannot schedule visits in the past");
        }
        
        // Validate veterinarian is required for scheduling
        if (visit.getVeterinarian() == null) {
            throw new ValidationException("Veterinarian is required for scheduling");
        }
        
        // Validate visit type if specified
        if (visit.getVisitType() != null && visit.requiresSpecialist()) {
            Veterinarian vet = visit.getVeterinarian();
            if (vet.getSpecialtySet() == null || 
                !vet.getSpecialtySet().contains(visit.getRecommendedSpecialty())) {
                throw new BusinessRuleException("Veterinarian does not have required specialty", 
                    String.format("Visit type %s requires specialty %s", 
                        visit.getVisitType().toString(), 
                        visit.getRecommendedSpecialty() != null ? visit.getRecommendedSpecialty().toString() : "Unknown"));
            }
        }
    }
    
    private void validateVisitForUpdate(Visit visit, Visit existingVisit) {
        if (visit == null) {
            throw new ValidationException("Visit cannot be null");
        }
        
        // Cannot change pet for existing visit
        if (visit.getPet() != null && 
            !visit.getPet().getId().equals(existingVisit.getPet().getId())) {
            throw new BusinessRuleException("Cannot change pet for existing visit");
        }
        
        // Validate veterinarian exists if being changed
        if (visit.getVeterinarian() != null && 
            !veterinarianRepository.existsById(visit.getVeterinarian().getId())) {
            throw new EntityNotFoundException("Veterinarian", visit.getVeterinarian().getId());
        }
    }
    
    private void validateVisitCompletion(String diagnosis, String treatment) {
        if (!StringUtils.hasText(diagnosis)) {
            throw new ValidationException("Diagnosis is required to complete visit");
        }
        
        if (!StringUtils.hasText(treatment)) {
            throw new ValidationException("Treatment is required to complete visit");
        }
        
        if (diagnosis.length() > 500) {
            throw new ValidationException("Diagnosis must not exceed 500 characters");
        }
        
        if (treatment.length() > 500) {
            throw new ValidationException("Treatment must not exceed 500 characters");
        }
    }
    
    private void updateVisitFields(Visit existingVisit, Visit updatedVisit) {
        if (updatedVisit.getVisitDate() != null) {
            existingVisit.setVisitDate(updatedVisit.getVisitDate());
        }
        
        if (updatedVisit.getVisitType() != null) {
            existingVisit.setVisitType(updatedVisit.getVisitType());
        }
        
        if (updatedVisit.getDuration() != null) {
            existingVisit.setDuration(updatedVisit.getDuration());
        }
        
        if (updatedVisit.getDiagnosis() != null) {
            existingVisit.setDiagnosis(updatedVisit.getDiagnosis());
        }
        
        if (updatedVisit.getTreatment() != null) {
            existingVisit.setTreatment(updatedVisit.getTreatment());
        }
        
        if (updatedVisit.getNotes() != null) {
            existingVisit.setNotes(updatedVisit.getNotes());
        }
        
        if (updatedVisit.getCost() != null) {
            existingVisit.setCost(updatedVisit.getCost());
        }
        
        if (updatedVisit.getVeterinarian() != null) {
            existingVisit.setVeterinarian(updatedVisit.getVeterinarian());
        }
    }
}