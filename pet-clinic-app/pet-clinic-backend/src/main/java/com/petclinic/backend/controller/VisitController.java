package com.petclinic.backend.controller;

import com.petclinic.backend.model.Visit;
import com.petclinic.backend.repository.VisitRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * REST Controller for Visit management
 * Provides CRUD operations and search functionality for pet visits
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4
 */
@RestController
@RequestMapping("/api/visits")
@CrossOrigin(origins = "*")
public class VisitController {

    @Autowired
    private VisitRepository visitRepository;

    /**
     * Get all visits with pagination
     * GET /api/visits
     */
    @GetMapping
    public ResponseEntity<Page<Visit>> getAllVisits(Pageable pageable) {
        try {
            Page<Visit> visits = visitRepository.findAll(pageable);
            return ResponseEntity.ok(visits);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get visit by ID
     * GET /api/visits/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Visit> getVisitById(@PathVariable Long id) {
        try {
            Optional<Visit> visit = visitRepository.findById(id);
            return visit.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create new visit
     * POST /api/visits
     */
    @PostMapping
    public ResponseEntity<Visit> createVisit(@Valid @RequestBody Visit visit) {
        try {
            // Ensure ID is null for new entities
            visit.setId(null);
            Visit savedVisit = visitRepository.save(visit);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedVisit);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Update existing visit
     * PUT /api/visits/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Visit> updateVisit(@PathVariable Long id, @Valid @RequestBody Visit visitDetails) {
        try {
            Optional<Visit> optionalVisit = visitRepository.findById(id);
            if (optionalVisit.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Visit visit = optionalVisit.get();
            visit.setVisitDate(visitDetails.getVisitDate());
            visit.setDescription(visitDetails.getDescription());
            visit.setDiagnosis(visitDetails.getDiagnosis());
            visit.setTreatment(visitDetails.getTreatment());
            visit.setCost(visitDetails.getCost());
            visit.setVeterinarian(visitDetails.getVeterinarian());

            Visit updatedVisit = visitRepository.save(visit);
            return ResponseEntity.ok(updatedVisit);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Delete visit
     * DELETE /api/visits/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVisit(@PathVariable Long id) {
        try {
            if (!visitRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            visitRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get visits by pet ID
     * GET /api/visits/pet/{petId}
     */
    @GetMapping("/pet/{petId}")
    public ResponseEntity<List<Visit>> getVisitsByPetId(@PathVariable Long petId) {
        try {
            List<Visit> visits = visitRepository.findByPetIdOrderByVisitDateDesc(petId);
            return ResponseEntity.ok(visits);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get visits by veterinarian ID
     * GET /api/visits/veterinarian/{veterinarianId}
     */
    @GetMapping("/veterinarian/{veterinarianId}")
    public ResponseEntity<List<Visit>> getVisitsByVeterinarianId(@PathVariable Long veterinarianId) {
        try {
            List<Visit> visits = visitRepository.findByVeterinarianIdOrderByVisitDateDesc(veterinarianId);
            return ResponseEntity.ok(visits);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get visits by owner ID
     * GET /api/visits/owner/{ownerId}
     */
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<Visit>> getVisitsByOwnerId(@PathVariable Long ownerId) {
        try {
            List<Visit> visits = visitRepository.findByOwnerId(ownerId);
            return ResponseEntity.ok(visits);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get visits within date range
     * GET /api/visits/date-range?start={start}&end={end}
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<Visit>> getVisitsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        try {
            List<Visit> visits = visitRepository.findByVisitDateBetweenOrderByVisitDateDesc(start, end);
            return ResponseEntity.ok(visits);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search visits by description
     * GET /api/visits/search/by-description?text={text}
     */
    @GetMapping("/search/by-description")
    public ResponseEntity<List<Visit>> searchByDescription(@RequestParam String text) {
        try {
            List<Visit> visits = visitRepository.findByDescriptionContainingIgnoreCase(text);
            return ResponseEntity.ok(visits);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search visits by diagnosis
     * GET /api/visits/search/by-diagnosis?text={text}
     */
    @GetMapping("/search/by-diagnosis")
    public ResponseEntity<List<Visit>> searchByDiagnosis(@RequestParam String text) {
        try {
            List<Visit> visits = visitRepository.findByDiagnosisContainingIgnoreCase(text);
            return ResponseEntity.ok(visits);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search visits by treatment
     * GET /api/visits/search/by-treatment?text={text}
     */
    @GetMapping("/search/by-treatment")
    public ResponseEntity<List<Visit>> searchByTreatment(@RequestParam String text) {
        try {
            List<Visit> visits = visitRepository.findByTreatmentContainingIgnoreCase(text);
            return ResponseEntity.ok(visits);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get visits by cost range
     * GET /api/visits/cost-range?min={min}&max={max}
     */
    @GetMapping("/cost-range")
    public ResponseEntity<List<Visit>> getVisitsByCostRange(
            @RequestParam BigDecimal min, @RequestParam BigDecimal max) {
        try {
            List<Visit> visits = visitRepository.findByCostBetween(min, max);
            return ResponseEntity.ok(visits);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get emergency visits
     * GET /api/visits/emergency
     */
    @GetMapping("/emergency")
    public ResponseEntity<List<Visit>> getEmergencyVisits() {
        try {
            List<Visit> visits = visitRepository.findEmergencyVisits();
            return ResponseEntity.ok(visits);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get completed visits
     * GET /api/visits/completed
     */
    @GetMapping("/completed")
    public ResponseEntity<List<Visit>> getCompletedVisits() {
        try {
            List<Visit> visits = visitRepository.findCompletedVisits();
            return ResponseEntity.ok(visits);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get incomplete visits
     * GET /api/visits/incomplete
     */
    @GetMapping("/incomplete")
    public ResponseEntity<List<Visit>> getIncompleteVisits() {
        try {
            List<Visit> visits = visitRepository.findIncompleteVisits();
            return ResponseEntity.ok(visits);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get visits without cost
     * GET /api/visits/without-cost
     */
    @GetMapping("/without-cost")
    public ResponseEntity<List<Visit>> getVisitsWithoutCost() {
        try {
            List<Visit> visits = visitRepository.findVisitsWithoutCost();
            return ResponseEntity.ok(visits);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get today's visits
     * GET /api/visits/today
     */
    @GetMapping("/today")
    public ResponseEntity<List<Visit>> getTodaysVisits() {
        try {
            List<Visit> visits = visitRepository.findTodaysVisits();
            return ResponseEntity.ok(visits);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get upcoming visits
     * GET /api/visits/upcoming
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<Visit>> getUpcomingVisits() {
        try {
            List<Visit> visits = visitRepository.findUpcomingVisits();
            return ResponseEntity.ok(visits);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get visit statistics
     * GET /api/visits/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Object[]> getVisitStatistics() {
        try {
            Object[] stats = visitRepository.getVisitStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Calculate revenue by date range
     * GET /api/visits/revenue?start={start}&end={end}
     */
    @GetMapping("/revenue")
    public ResponseEntity<BigDecimal> calculateRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        try {
            BigDecimal revenue = visitRepository.calculateRevenueByDateRange(start, end);
            return ResponseEntity.ok(revenue != null ? revenue : BigDecimal.ZERO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get most common diagnoses
     * GET /api/visits/common-diagnoses?limit={limit}
     */
    @GetMapping("/common-diagnoses")
    public ResponseEntity<List<Object[]>> getMostCommonDiagnoses(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<Object[]> diagnoses = visitRepository.findMostCommonDiagnoses(
                org.springframework.data.domain.PageRequest.of(0, limit));
            return ResponseEntity.ok(diagnoses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Advanced search with multiple criteria
     * GET /api/visits/search
     */
    @GetMapping("/search")
    public ResponseEntity<Page<Visit>> searchVisits(
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) Long veterinarianId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String diagnosis,
            Pageable pageable) {
        try {
            Page<Visit> visits = visitRepository.searchVisits(
                petId, veterinarianId, startDate, endDate, description, diagnosis, pageable);
            return ResponseEntity.ok(visits);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}