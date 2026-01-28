package com.petclinic.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Visit entity representing a pet visit to the clinic
 * Validates: Requirements 8.1, 8.2, 8.3
 */
@Entity
@Table(name = "visits")
public class Visit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Visit date is required")
    @Column(name = "visit_date", nullable = false)
    private LocalDateTime visitDate;
    
    @NotBlank(message = "Visit description is required")
    @Size(min = 1, max = 500, message = "Description must be between 1 and 500 characters")
    @Column(name = "description", nullable = false, length = 500)
    private String description;
    
    @Size(max = 500, message = "Diagnosis must not exceed 500 characters")
    @Column(name = "diagnosis", length = 500)
    private String diagnosis;
    
    @Size(max = 500, message = "Treatment must not exceed 500 characters")
    @Column(name = "treatment", length = 500)
    private String treatment;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Cost must be greater than 0")
    @Column(name = "cost", precision = 10, scale = 2)
    private BigDecimal cost;
    
    @NotNull(message = "Pet is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    @JsonBackReference
    private Pet pet;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinarian_id")
    private Veterinarian veterinarian;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Visit() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Visit(LocalDateTime visitDate, String description, Pet pet) {
        this();
        this.visitDate = visitDate;
        this.description = description;
        this.pet = pet;
    }
    
    public Visit(LocalDateTime visitDate, String description, String diagnosis, String treatment, BigDecimal cost, Pet pet, Veterinarian veterinarian) {
        this();
        this.visitDate = visitDate;
        this.description = description;
        this.diagnosis = diagnosis;
        this.treatment = treatment;
        this.cost = cost;
        this.pet = pet;
        this.veterinarian = veterinarian;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LocalDateTime getVisitDate() {
        return visitDate;
    }
    
    public void setVisitDate(LocalDateTime visitDate) {
        this.visitDate = visitDate;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getDiagnosis() {
        return diagnosis;
    }
    
    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getTreatment() {
        return treatment;
    }
    
    public void setTreatment(String treatment) {
        this.treatment = treatment;
        this.updatedAt = LocalDateTime.now();
    }
    
    public BigDecimal getCost() {
        return cost;
    }
    
    public void setCost(BigDecimal cost) {
        this.cost = cost;
        this.updatedAt = LocalDateTime.now();
    }
    
    public Pet getPet() {
        return pet;
    }
    
    public void setPet(Pet pet) {
        this.pet = pet;
        this.updatedAt = LocalDateTime.now();
    }
    
    public Veterinarian getVeterinarian() {
        return veterinarian;
    }
    
    public void setVeterinarian(Veterinarian veterinarian) {
        this.veterinarian = veterinarian;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    // Business Methods
    public boolean isEmergencyVisit() {
        return description != null && 
               (description.toLowerCase().contains("emergency") || 
                description.toLowerCase().contains("urgent"));
    }
    
    public boolean isCompleted() {
        return diagnosis != null && !diagnosis.trim().isEmpty() &&
               treatment != null && !treatment.trim().isEmpty();
    }
    
    public boolean hasCost() {
        return cost != null && cost.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public String getVisitSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Visit on ").append(visitDate.toLocalDate());
        if (veterinarian != null) {
            summary.append(" with Dr. ").append(veterinarian.getLastName());
        }
        summary.append(": ").append(description);
        return summary.toString();
    }
    
    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Visit visit = (Visit) o;
        return Objects.equals(id, visit.id) &&
               Objects.equals(visitDate, visit.visitDate) &&
               Objects.equals(pet, visit.pet);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, visitDate, pet);
    }
    
    @Override
    public String toString() {
        return "Visit{" +
                "id=" + id +
                ", visitDate=" + visitDate +
                ", description='" + description + '\'' +
                ", diagnosis='" + diagnosis + '\'' +
                ", cost=" + cost +
                ", completed=" + isCompleted() +
                '}';
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}