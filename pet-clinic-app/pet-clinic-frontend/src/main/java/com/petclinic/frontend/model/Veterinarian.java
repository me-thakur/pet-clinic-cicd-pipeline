package com.petclinic.frontend.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Veterinarian Model for Frontend
 * 
 * Represents a veterinarian in the frontend application.
 * This model is used for form binding and data transfer
 * between the frontend and backend services.
 */
public class Veterinarian {
    
    private Long id;
    
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;
    
    @Size(max = 200, message = "Specialties must not exceed 200 characters")
    private String specialties;
    
    @NotBlank(message = "License number is required")
    @Size(max = 20, message = "License number must not exceed 20 characters")
    private String licenseNumber;
    
    private LocalDate createdAt;
    private LocalDate updatedAt;
    
    private List<Visit> visits = new ArrayList<>();

    // Constructors
    public Veterinarian() {}

    public Veterinarian(String firstName, String lastName, String licenseNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.licenseNumber = licenseNumber;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSpecialties() {
        return specialties;
    }

    public void setSpecialties(String specialties) {
        this.specialties = specialties;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
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

    public List<Visit> getVisits() {
        return visits;
    }

    public void setVisits(List<Visit> visits) {
        this.visits = visits;
    }

    // Utility methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public int getVisitCount() {
        return visits != null ? visits.size() : 0;
    }

    @Override
    public String toString() {
        return "Veterinarian{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", licenseNumber='" + licenseNumber + '\'' +
                '}';
    }
}