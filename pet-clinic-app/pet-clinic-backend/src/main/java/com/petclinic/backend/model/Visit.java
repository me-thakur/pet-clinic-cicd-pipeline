package com.petclinic.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.petclinic.backend.config.EncryptionConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Visit entity representing a pet visit to the clinic
 * Validates: Requirements 2.1, 2.5
 */
@Entity
@Table(name = "visits")
public class Visit extends BaseEntity {
    
    @NotNull(message = "Visit date is required")
    @Column(name = "visit_date", nullable = false)
    private LocalDateTime visitDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "visit_type")
    private VisitType visitType;
    
    @Column(name = "duration_minutes")
    private Integer duration;
    
    @Size(max = 500, message = "Diagnosis must not exceed 500 characters")
    @Column(name = "diagnosis", length = 1000) // Increased length for encrypted data
    @Convert(converter = EncryptionConverter.class)
    private String diagnosis;
    
    @Size(max = 500, message = "Treatment must not exceed 500 characters")
    @Column(name = "treatment", length = 1000) // Increased length for encrypted data
    @Convert(converter = EncryptionConverter.class)
    private String treatment;
    
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    @Column(name = "notes", length = 2000) // Increased length for encrypted data
    @Convert(converter = EncryptionConverter.class)
    private String notes;
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Cost must be greater than or equal to 0")
    @Column(name = "cost", precision = 10, scale = 2)
    private BigDecimal cost;
    
    @NotNull(message = "Pet is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pet_id", nullable = false)
    @JsonIgnoreProperties({"visits", "owner"})
    private Pet pet;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "veterinarian_id")
    @JsonIgnoreProperties({"visits", "specialties"})
    private Veterinarian veterinarian;
    
    // Constructors
    public Visit() {
        super();
    }
    
    public Visit(LocalDateTime visitDate, Pet pet) {
        super();
        this.visitDate = visitDate;
        this.pet = pet;
        // Set default duration based on visit type or use default
        this.duration = 30;
    }
    
    public Visit(LocalDateTime visitDate, VisitType visitType, Pet pet) {
        super();
        this.visitDate = visitDate;
        this.visitType = visitType;
        this.pet = pet;
        // Set duration based on visit type
        this.duration = visitType != null ? visitType.getDefaultDurationMinutes() : 30;
    }
    
    public Visit(LocalDateTime visitDate, String diagnosis, String treatment, BigDecimal cost, Pet pet, Veterinarian veterinarian) {
        super();
        this.visitDate = visitDate;
        this.diagnosis = diagnosis;
        this.treatment = treatment;
        this.cost = cost;
        this.pet = pet;
        this.veterinarian = veterinarian;
        this.duration = 30; // Default duration
    }
    
    public Visit(LocalDateTime visitDate, VisitType visitType, String diagnosis, String treatment, String notes, BigDecimal cost, Pet pet, Veterinarian veterinarian) {
        super();
        this.visitDate = visitDate;
        this.visitType = visitType;
        this.diagnosis = diagnosis;
        this.treatment = treatment;
        this.notes = notes;
        this.cost = cost;
        this.pet = pet;
        this.veterinarian = veterinarian;
        this.duration = visitType != null ? visitType.getDefaultDurationMinutes() : 30;
    }
    
    // Getters and Setters
    public LocalDateTime getVisitDate() {
        return visitDate;
    }
    
    public void setVisitDate(LocalDateTime visitDate) {
        this.visitDate = visitDate;
    }
    
    public String getDiagnosis() {
        return diagnosis;
    }
    
    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }
    
    public String getTreatment() {
        return treatment;
    }
    
    public void setTreatment(String treatment) {
        this.treatment = treatment;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public BigDecimal getCost() {
        return cost;
    }
    
    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }
    
    public Pet getPet() {
        return pet;
    }
    
    public void setPet(Pet pet) {
        this.pet = pet;
    }
    
    public Veterinarian getVeterinarian() {
        return veterinarian;
    }
    
    public void setVeterinarian(Veterinarian veterinarian) {
        this.veterinarian = veterinarian;
    }
    
    public VisitType getVisitType() {
        return visitType;
    }
    
    public void setVisitType(VisitType visitType) {
        this.visitType = visitType;
        // Update duration based on visit type
        if (visitType != null && this.duration == null) {
            this.duration = visitType.getDefaultDurationMinutes();
        }
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    // Business Methods
    public boolean isEmergencyVisit() {
        return visitType != null && visitType.isEmergency();
    }
    
    @JsonProperty("completed")
    public boolean isCompleted() {
        try {
            // Handle null/missing diagnosis and treatment fields gracefully
            // Ensure default "Pending" status for corrupted data
            // Requirements: 6.1, 6.3
            
            if (diagnosis == null || treatment == null) {
                return false; // Default to "Pending" for null fields
            }
            
            // Trim and check for empty/whitespace-only content
            String trimmedDiagnosis = diagnosis.trim();
            String trimmedTreatment = treatment.trim();
            
            if (trimmedDiagnosis.isEmpty() || trimmedTreatment.isEmpty()) {
                return false; // Default to "Pending" for empty fields
            }
            
            // Both fields have non-empty, non-whitespace content
            return true;
            
        } catch (Exception e) {
            // Log the error and default to "Pending" status for any unexpected issues
            // This ensures the system remains stable even with corrupted data
            System.err.println("Error calculating visit completion status for visit ID " + 
                             (getId() != null ? getId() : "unknown") + ": " + e.getMessage());
            return false; // Default to "Pending" on any error
        }
    }
    
    public boolean hasCost() {
        return cost != null && cost.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public String getVisitSummary() {
        StringBuilder summary = new StringBuilder();
        if (visitDate != null) {
            summary.append("Visit on ").append(visitDate.toLocalDate());
        } else {
            summary.append("Visit (date TBD)");
        }
        if (veterinarian != null) {
            summary.append(" with Dr. ").append(veterinarian.getLastName());
        }
        if (visitType != null) {
            summary.append(" (").append(visitType.getDisplayName()).append(")");
        }
        if (diagnosis != null && !diagnosis.trim().isEmpty()) {
            summary.append(": ").append(diagnosis);
        }
        return summary.toString();
    }
    
    /**
     * Get the end time of the visit based on start time and duration
     * @return End time of the visit
     */
    public LocalDateTime getEndTime() {
        if (visitDate == null || duration == null) {
            return visitDate;
        }
        return visitDate.plusMinutes(duration);
    }
    
    /**
     * Check if this visit requires a specialist
     * @return true if visit type requires specialist, false otherwise
     */
    public boolean requiresSpecialist() {
        return visitType != null && visitType.requiresSpecialist();
    }
    
    /**
     * Check if this visit is preventive care
     * @return true if preventive care visit, false otherwise
     */
    public boolean isPreventiveCare() {
        return visitType != null && visitType.isPreventiveCare();
    }
    
    /**
     * Check if this visit requires anesthesia
     * @return true if requires anesthesia, false otherwise
     */
    public boolean requiresAnesthesia() {
        return visitType != null && visitType.requiresAnesthesia();
    }
    
    /**
     * Get the recommended specialty for this visit
     * @return Recommended specialty, or null if none specified
     */
    public Specialty getRecommendedSpecialty() {
        return visitType != null ? visitType.getRecommendedSpecialty() : null;
    }
    
    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Visit visit = (Visit) o;
        return Objects.equals(getId(), visit.getId()) &&
               Objects.equals(visitDate, visit.visitDate) &&
               Objects.equals(pet, visit.pet);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getId(), visitDate, pet);
    }
    
    @Override
    public String toString() {
        return "Visit{" +
                "id=" + getId() +
                ", visitDate=" + visitDate +
                ", visitType=" + visitType +
                ", diagnosis='" + diagnosis + '\'' +
                ", cost=" + cost +
                ", completed=" + isCompleted() +
                '}';
    }
}