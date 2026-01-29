package com.petclinic.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Veterinarian entity representing a veterinarian in the clinic system
 * Validates: Requirements 8.1, 8.2, 8.3
 */
@Entity
@Table(name = "veterinarians")
public class Veterinarian {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;
    
    @Size(max = 200, message = "Specialties must not exceed 200 characters")
    @Column(name = "specialties", length = 200)
    private String specialties;
    
    @ElementCollection(targetClass = Specialty.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "veterinarian_specialties", joinColumns = @JoinColumn(name = "veterinarian_id"))
    @Column(name = "specialty")
    private Set<Specialty> specialtySet = new HashSet<>();
    
    @NotBlank(message = "License number is required")
    @Pattern(regexp = "^[A-Z0-9]{6,20}$", message = "License number must be 6-20 alphanumeric characters")
    @Column(name = "license_number", nullable = false, unique = true, length = 20)
    private String licenseNumber;
    
    @OneToMany(mappedBy = "veterinarian", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("veterinarian-visits")
    private List<Visit> visits = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDate createdAt;
    
    @Column(name = "updated_at")
    private LocalDate updatedAt;
    
    // Constructors
    public Veterinarian() {
        this.createdAt = LocalDate.now();
        this.updatedAt = LocalDate.now();
    }
    
    public Veterinarian(String firstName, String lastName, String specialties, String licenseNumber) {
        this();
        this.firstName = firstName;
        this.lastName = lastName;
        this.specialties = specialties;
        this.licenseNumber = licenseNumber;
    }
    
    public Veterinarian(String firstName, String lastName, Set<Specialty> specialtySet, String licenseNumber) {
        this();
        this.firstName = firstName;
        this.lastName = lastName;
        this.specialtySet = specialtySet != null ? specialtySet : new HashSet<>();
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
        this.updatedAt = LocalDate.now();
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
        this.updatedAt = LocalDate.now();
    }
    
    public String getSpecialties() {
        return specialties;
    }
    
    public void setSpecialties(String specialties) {
        this.specialties = specialties;
        this.updatedAt = LocalDate.now();
    }
    
    public String getLicenseNumber() {
        return licenseNumber;
    }
    
    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
        this.updatedAt = LocalDate.now();
    }
    
    public List<Visit> getVisits() {
        return visits;
    }
    
    public void setVisits(List<Visit> visits) {
        this.visits = visits;
    }
    
    public void addVisit(Visit visit) {
        visits.add(visit);
        visit.setVeterinarian(this);
    }
    
    public void removeVisit(Visit visit) {
        visits.remove(visit);
        visit.setVeterinarian(null);
    }
    
    public LocalDate getCreatedAt() {
        return createdAt;
    }
    
    public LocalDate getUpdatedAt() {
        return updatedAt;
    }
    
    public Set<Specialty> getSpecialtySet() {
        return specialtySet;
    }
    
    public void setSpecialtySet(Set<Specialty> specialtySet) {
        this.specialtySet = specialtySet != null ? specialtySet : new HashSet<>();
        this.updatedAt = LocalDate.now();
    }
    
    public void addSpecialty(Specialty specialty) {
        if (specialty != null) {
            this.specialtySet.add(specialty);
            this.updatedAt = LocalDate.now();
        }
    }
    
    public void removeSpecialty(Specialty specialty) {
        if (specialty != null) {
            this.specialtySet.remove(specialty);
            this.updatedAt = LocalDate.now();
        }
    }
    
    // Business Methods
    public String getFullName() {
        return "Dr. " + firstName + " " + lastName;
    }
    
    public int getVisitCount() {
        return visits.size();
    }
    
    public boolean hasSpecialty(String specialty) {
        // Check both old string-based specialties and new enum-based specialties
        boolean hasInStringSpecialties = specialties != null && 
               specialties.toLowerCase().contains(specialty.toLowerCase());
        
        boolean hasInSpecialtySet = specialtySet.stream()
                .anyMatch(s -> s.getDisplayName().equalsIgnoreCase(specialty) || 
                              s.getCode().equalsIgnoreCase(specialty));
        
        return hasInStringSpecialties || hasInSpecialtySet;
    }
    
    public boolean hasSpecialty(Specialty specialty) {
        return specialty != null && specialtySet.contains(specialty);
    }
    
    public List<String> getSpecialtyList() {
        List<String> allSpecialties = new ArrayList<>();
        
        // Add string-based specialties (for backward compatibility)
        if (specialties != null && !specialties.trim().isEmpty()) {
            java.util.Arrays.stream(specialties.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(allSpecialties::add);
        }
        
        // Add enum-based specialties
        specialtySet.stream()
                .map(Specialty::getDisplayName)
                .forEach(allSpecialties::add);
        
        return allSpecialties.stream().distinct().collect(java.util.stream.Collectors.toList());
    }
    
    public boolean isSpecialist() {
        boolean hasStringSpecialties = specialties != null && !specialties.trim().isEmpty();
        boolean hasEnumSpecialties = !specialtySet.isEmpty();
        return hasStringSpecialties || hasEnumSpecialties;
    }
    
    /**
     * Check if veterinarian can handle emergency cases
     * @return true if has emergency-capable specialties
     */
    public boolean canHandleEmergencies() {
        return specialtySet.stream().anyMatch(Specialty::isEmergency) ||
               hasSpecialty("Emergency Medicine") || hasSpecialty("Surgery");
    }
    
    /**
     * Check if veterinarian can perform surgeries
     * @return true if has surgical specialties
     */
    public boolean canPerformSurgery() {
        return specialtySet.stream().anyMatch(Specialty::isSurgical) ||
               hasSpecialty("Surgery") || hasSpecialty("Orthopedics");
    }
    
    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Veterinarian that = (Veterinarian) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(firstName, that.firstName) &&
               Objects.equals(lastName, that.lastName) &&
               Objects.equals(licenseNumber, that.licenseNumber);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName, licenseNumber);
    }
    
    @Override
    public String toString() {
        return "Veterinarian{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", specialties='" + specialties + '\'' +
                ", licenseNumber='" + licenseNumber + '\'' +
                ", visitCount=" + getVisitCount() +
                '}';
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDate.now();
    }
}