package com.petclinic.frontend.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Pet Model for Frontend
 * 
 * Represents a pet in the frontend application.
 * This model is used for form binding and data transfer
 * between the frontend and backend services.
 * 
 * Validates: Requirements 8.1, 8.2
 */
public class Pet {
    
    private Long id;
    
    @NotBlank(message = "Pet name is required")
    @Size(max = 50, message = "Pet name must not exceed 50 characters")
    private String name;
    
    @NotBlank(message = "Species is required")
    @Size(max = 30, message = "Species must not exceed 30 characters")
    private String species;
    
    @Size(max = 50, message = "Breed must not exceed 50 characters")
    private String breed;
    
    @NotNull(message = "Birth date is required")
    @Past(message = "Birth date must be in the past")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
    
    @Size(max = 1000, message = "Medical history must not exceed 1000 characters")
    private String medicalHistory;
    
    private LocalDate createdAt;
    private LocalDate updatedAt;
    
    private Owner owner;
    private List<Visit> visits = new ArrayList<>();
    
    // Transient field for form binding
    private Long ownerId;

    // Constructors
    public Pet() {}

    public Pet(String name, String species, LocalDate birthDate) {
        this.name = name;
        this.species = species;
        this.birthDate = birthDate;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public List<Visit> getVisits() {
        return visits;
    }

    public void setVisits(List<Visit> visits) {
        this.visits = visits;
    }
    
    public Long getOwnerId() {
        return owner != null ? owner.getId() : ownerId;
    }
    
    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
        if (ownerId != null && (owner == null || !ownerId.equals(owner.getId()))) {
            // Create a temporary owner object with just the ID
            Owner tempOwner = new Owner();
            tempOwner.setId(ownerId);
            this.owner = tempOwner;
        }
    }

    // Utility methods
    public int getAge() {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public String getAgeString() {
        int age = getAge();
        return age + (age == 1 ? " year" : " years") + " old";
    }

    public int getVisitCount() {
        return visits != null ? visits.size() : 0;
    }

    public boolean isSeniorPet() {
        return getAge() >= 7;
    }

    public boolean isYoungPet() {
        return getAge() < 2;
    }

    @Override
    public String toString() {
        return "Pet{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", species='" + species + '\'' +
                ", breed='" + breed + '\'' +
                ", age=" + getAge() +
                '}';
    }
}