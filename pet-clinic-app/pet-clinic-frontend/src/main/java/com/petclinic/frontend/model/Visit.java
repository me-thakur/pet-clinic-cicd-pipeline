package com.petclinic.frontend.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Visit Model for Frontend
 * 
 * Represents a veterinary visit in the frontend application.
 * This model is used for form binding and data transfer
 * between the frontend and backend services.
 * 
 * Validates: Requirements 8.1
 */
public class Visit {
    
    private Long id;
    
    @NotNull(message = "Visit date and time is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime visitDate;
    
    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    @Size(max = 500, message = "Diagnosis must not exceed 500 characters")
    private String diagnosis;
    
    @Size(max = 500, message = "Treatment must not exceed 500 characters")
    private String treatment;
    
    @Positive(message = "Cost must be positive")
    private BigDecimal cost;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private Pet pet;
    private Veterinarian veterinarian;

    // Constructors
    public Visit() {}

    public Visit(LocalDateTime visitDate, String description) {
        this.visitDate = visitDate;
        this.description = description;
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
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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

    // Utility methods
    public String getFormattedVisitDate() {
        if (visitDate == null) {
            return "";
        }
        return visitDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"));
    }

    public String getFormattedCost() {
        if (cost == null) {
            return "$0.00";
        }
        return String.format("$%.2f", cost);
    }

    @Override
    public String toString() {
        return "Visit{" +
                "id=" + id +
                ", visitDate=" + visitDate +
                ", description='" + description + '\'' +
                ", cost=" + cost +
                '}';
    }
}