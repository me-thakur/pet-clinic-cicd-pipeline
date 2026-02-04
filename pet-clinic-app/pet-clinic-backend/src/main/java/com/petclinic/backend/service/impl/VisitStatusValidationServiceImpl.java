package com.petclinic.backend.service.impl;

import com.petclinic.backend.exception.EntityNotFoundException;
import com.petclinic.backend.exception.ValidationException;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.repository.VisitRepository;
import com.petclinic.backend.service.VisitStatusValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of VisitStatusValidationService for validating visit completion status consistency
 * Validates: Requirements 5.1, 5.2
 */
@Service
@Transactional(readOnly = true)
public class VisitStatusValidationServiceImpl implements VisitStatusValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(VisitStatusValidationServiceImpl.class);
    
    @Autowired
    private VisitRepository visitRepository;
    
    @Override
    public ValidationResult validateAllVisitStatuses() {
        logger.info("Starting validation of all visit completion statuses");
        
        List<Visit> allVisits = visitRepository.findAll();
        return performValidation(allVisits, "all visits");
    }
    
    @Override
    public ValidationResult validateVisitStatusesByDateRange(LocalDate startDate, LocalDate endDate) {
        logger.info("Starting validation of visit completion statuses for date range: {} to {}", startDate, endDate);
        
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date are required");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new ValidationException("Start date must be before or equal to end date");
        }
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        return performValidation(visits, String.format("visits between %s and %s", startDate, endDate));
    }
    
    @Override
    public ValidationResult validateVisitStatus(Long visitId) {
        logger.info("Starting validation of visit completion status for visit ID: {}", visitId);
        
        if (visitId == null) {
            throw new ValidationException("Visit ID is required");
        }
        
        Optional<Visit> visitOpt = visitRepository.findById(visitId);
        if (!visitOpt.isPresent()) {
            throw new EntityNotFoundException("Visit", visitId);
        }
        
        List<Visit> visits = List.of(visitOpt.get());
        return performValidation(visits, String.format("visit ID %d", visitId));
    }
    
    @Override
    public ValidationResult validateVisitStatusesByPet(Long petId) {
        logger.info("Starting validation of visit completion statuses for pet ID: {}", petId);
        
        if (petId == null) {
            throw new ValidationException("Pet ID is required");
        }
        
        List<Visit> visits = visitRepository.findByPetId(petId);
        return performValidation(visits, String.format("pet ID %d", petId));
    }
    
    @Override
    public boolean isVisitStatusConsistent(Visit visit) {
        if (visit == null) {
            return false;
        }
        
        boolean expectedStatus = getExpectedCompletionStatus(visit);
        boolean actualStatus = visit.isCompleted();
        
        return expectedStatus == actualStatus;
    }
    
    @Override
    public boolean getExpectedCompletionStatus(Visit visit) {
        if (visit == null) {
            return false;
        }
        
        // Use the same logic as Visit.isCompleted() method
        return visit.getDiagnosis() != null && !visit.getDiagnosis().trim().isEmpty() &&
               visit.getTreatment() != null && !visit.getTreatment().trim().isEmpty();
    }
    
    /**
     * Perform validation on a list of visits
     * @param visits List of visits to validate
     * @param context Context description for logging
     * @return ValidationResult with any inconsistencies found
     */
    private ValidationResult performValidation(List<Visit> visits, String context) {
        logger.debug("Validating {} visits for {}", visits.size(), context);
        
        List<InconsistencyReport> inconsistencies = new ArrayList<>();
        int totalVisitsChecked = visits.size();
        
        for (Visit visit : visits) {
            try {
                if (!isVisitStatusConsistent(visit)) {
                    InconsistencyReport report = createInconsistencyReport(visit);
                    inconsistencies.add(report);
                    
                    // Log the inconsistency as required by the task
                    logger.warn("Visit status inconsistency found - Visit ID: {}, Pet: {}, Owner: {}, " +
                               "Expected Status: {}, Actual Status: {}, Diagnosis: '{}', Treatment: '{}'",
                               visit.getId(),
                               visit.getPet() != null ? visit.getPet().getName() : "Unknown",
                               visit.getPet() != null && visit.getPet().getOwner() != null ? 
                                   visit.getPet().getOwner().getLastName() : "Unknown",
                               getExpectedCompletionStatus(visit),
                               visit.isCompleted(),
                               sanitizeForLogging(visit.getDiagnosis()),
                               sanitizeForLogging(visit.getTreatment()));
                }
            } catch (Exception e) {
                logger.error("Error validating visit ID {}: {}", visit.getId(), e.getMessage(), e);
                
                // Create an error report for visits that couldn't be validated
                InconsistencyReport errorReport = new InconsistencyReport(
                    visit.getId(),
                    visit.getPet() != null ? visit.getPet().getName() : "Unknown",
                    visit.getPet() != null && visit.getPet().getOwner() != null ? 
                        visit.getPet().getOwner().getLastName() : "Unknown",
                    visit.getDiagnosis(),
                    visit.getTreatment(),
                    false,
                    "Error during validation: " + e.getMessage()
                );
                inconsistencies.add(errorReport);
            }
        }
        
        boolean hasInconsistencies = !inconsistencies.isEmpty();
        int inconsistentCount = inconsistencies.size();
        
        if (hasInconsistencies) {
            logger.warn("Validation completed for {} - Found {} inconsistencies out of {} visits checked",
                       context, inconsistentCount, totalVisitsChecked);
        } else {
            logger.info("Validation completed for {} - No inconsistencies found in {} visits checked",
                       context, totalVisitsChecked);
        }
        
        return new ValidationResult(hasInconsistencies, inconsistencies, totalVisitsChecked, inconsistentCount);
    }
    
    /**
     * Create an inconsistency report for a visit
     * @param visit Visit with inconsistent status
     * @return InconsistencyReport describing the inconsistency
     */
    private InconsistencyReport createInconsistencyReport(Visit visit) {
        boolean expectedStatus = getExpectedCompletionStatus(visit);
        boolean actualStatus = visit.isCompleted();
        
        String description;
        if (expectedStatus && !actualStatus) {
            description = "Visit should be marked as completed (has both diagnosis and treatment) but shows as pending";
        } else if (!expectedStatus && actualStatus) {
            description = "Visit shows as completed but is missing diagnosis or treatment";
        } else {
            description = "Visit status calculation inconsistency";
        }
        
        return new InconsistencyReport(
            visit.getId(),
            visit.getPet() != null ? visit.getPet().getName() : "Unknown",
            visit.getPet() != null && visit.getPet().getOwner() != null ? 
                visit.getPet().getOwner().getLastName() : "Unknown",
            visit.getDiagnosis(),
            visit.getTreatment(),
            expectedStatus,
            description
        );
    }
    
    /**
     * Sanitize text for logging to prevent log injection and handle null values
     * @param text Text to sanitize
     * @return Sanitized text safe for logging
     */
    private String sanitizeForLogging(String text) {
        if (text == null) {
            return "null";
        }
        
        if (text.trim().isEmpty()) {
            return "empty";
        }
        
        // Remove newlines and limit length for logging
        String sanitized = text.replaceAll("[\r\n\t]", " ").trim();
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 97) + "...";
        }
        
        return sanitized;
    }
    
    @Override
    @Transactional
    public RecalculationResult recalculateAllVisitStatuses() {
        logger.info("Starting batch recalculation of all visit completion statuses");
        long startTime = System.currentTimeMillis();
        
        List<Visit> allVisits = visitRepository.findAll();
        return performRecalculation(allVisits, "all visits", startTime);
    }
    
    @Override
    @Transactional
    public RecalculationResult recalculateVisitStatusesByDateRange(LocalDate startDate, LocalDate endDate) {
        logger.info("Starting batch recalculation of visit completion statuses for date range: {} to {}", startDate, endDate);
        long startTime = System.currentTimeMillis();
        
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date are required");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new ValidationException("Start date must be before or equal to end date");
        }
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Visit> visits = visitRepository.findByVisitDateBetween(startDateTime, endDateTime);
        return performRecalculation(visits, String.format("visits between %s and %s", startDate, endDate), startTime);
    }
    
    @Override
    @Transactional
    public RecalculationResult recalculateVisitStatusesByPet(Long petId) {
        logger.info("Starting batch recalculation of visit completion statuses for pet ID: {}", petId);
        long startTime = System.currentTimeMillis();
        
        if (petId == null) {
            throw new ValidationException("Pet ID is required");
        }
        
        List<Visit> visits = visitRepository.findByPetId(petId);
        return performRecalculation(visits, String.format("pet ID %d", petId), startTime);
    }
    
    /**
     * Perform batch recalculation on a list of visits
     * Since completion status is computed, this validates data integrity and logs any issues
     * @param visits List of visits to recalculate
     * @param context Context description for logging
     * @param startTime Start time for performance measurement
     * @return RecalculationResult with operation details
     */
    private RecalculationResult performRecalculation(List<Visit> visits, String context, long startTime) {
        logger.debug("Recalculating completion status for {} visits for {}", visits.size(), context);
        
        List<Long> updatedVisitIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int totalProcessed = visits.size();
        int visitsUpdated = 0;
        
        for (Visit visit : visits) {
            try {
                // Since completion status is computed, we validate data integrity
                // and ensure the visit can be properly processed
                boolean currentStatus = visit.isCompleted();
                boolean expectedStatus = getExpectedCompletionStatus(visit);
                
                // The statuses should always match since they use the same logic
                // If they don't match, there's a data integrity issue
                if (currentStatus != expectedStatus) {
                    String error = String.format("Data integrity issue for visit ID %d: computed status mismatch", visit.getId());
                    errors.add(error);
                    logger.error(error);
                } else {
                    // Log the recalculation for audit purposes
                    logger.debug("Recalculated visit ID {}: status = {}, diagnosis = '{}', treatment = '{}'",
                               visit.getId(),
                               currentStatus ? "Completed" : "Pending",
                               sanitizeForLogging(visit.getDiagnosis()),
                               sanitizeForLogging(visit.getTreatment()));
                    
                    // Force a save to ensure any lazy-loaded data is properly initialized
                    // and to trigger any database constraints or validations
                    visitRepository.save(visit);
                    
                    updatedVisitIds.add(visit.getId());
                    visitsUpdated++;
                }
                
            } catch (Exception e) {
                String error = String.format("Error recalculating visit ID %d: %s", visit.getId(), e.getMessage());
                errors.add(error);
                logger.error("Error during recalculation for visit ID {}: {}", visit.getId(), e.getMessage(), e);
            }
        }
        
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;
        
        if (errors.isEmpty()) {
            logger.info("Batch recalculation completed successfully for {} - Processed {} visits in {} ms",
                       context, totalProcessed, processingTime);
        } else {
            logger.warn("Batch recalculation completed with {} errors for {} - Processed {} visits, {} successful in {} ms",
                       errors.size(), context, totalProcessed, visitsUpdated, processingTime);
        }
        
        return new RecalculationResult(totalProcessed, visitsUpdated, updatedVisitIds, errors, processingTime);
    }
}