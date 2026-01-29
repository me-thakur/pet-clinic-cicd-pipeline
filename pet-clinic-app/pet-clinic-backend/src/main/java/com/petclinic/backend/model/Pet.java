package com.petclinic.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pet entity representing a pet in the clinic system
 * Validates: Requirements 8.1, 8.2, 8.3
 */
@Entity
@Table(name = "pets")
public class Pet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Pet name is required")
    @Size(min = 1, max = 50, message = "Pet name must be between 1 and 50 characters")
    @Column(name = "name", nullable = false, length = 50)
    private String name;
    
    @NotBlank(message = "Species is required")
    @Size(min = 1, max = 30, message = "Species must be between 1 and 30 characters")
    @Column(name = "species", nullable = false, length = 30)
    private String species;
    
    @Size(max = 50, message = "Breed must not exceed 50 characters")
    @Column(name = "breed", length = 50)
    private String breed;
    
    @Past(message = "Birth date must be in the past")
    @Column(name = "birth_date")
    private LocalDate birthDate;
    
    @Size(max = 1000, message = "Medical history must not exceed 1000 characters")
    @Column(name = "medical_history", length = 1000)
    private String medicalHistory;
    
    @NotNull(message = "Owner is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonBackReference("owner-pets")
    private Owner owner;
    
    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("pet-visits")
    private List<Visit> visits = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDate createdAt;
    
    @Column(name = "updated_at")
    private LocalDate updatedAt;
    
    // Constructors
    public Pet() {
        this.createdAt = LocalDate.now();
        this.updatedAt = LocalDate.now();
    }
    
    public Pet(String name, String species, String breed, LocalDate birthDate, Owner owner) {
        this();
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.birthDate = birthDate;
        this.owner = owner;
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
        this.updatedAt = LocalDate.now();
    }
    
    public String getSpecies() {
        return species;
    }
    
    public void setSpecies(String species) {
        this.species = species;
        this.updatedAt = LocalDate.now();
    }
    
    public String getBreed() {
        return breed;
    }
    
    public void setBreed(String breed) {
        this.breed = breed;
        this.updatedAt = LocalDate.now();
    }
    
    public LocalDate getBirthDate() {
        return birthDate;
    }
    
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
        this.updatedAt = LocalDate.now();
    }
    
    public String getMedicalHistory() {
        return medicalHistory;
    }
    
    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
        this.updatedAt = LocalDate.now();
    }
    
    public Owner getOwner() {
        return owner;
    }
    
    public void setOwner(Owner owner) {
        this.owner = owner;
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
        visit.setPet(this);
    }
    
    public void removeVisit(Visit visit) {
        visits.remove(visit);
        visit.setPet(null);
    }
    
    public LocalDate getCreatedAt() {
        return createdAt;
    }
    
    public LocalDate getUpdatedAt() {
        return updatedAt;
    }
    
    // Business Methods
    public int getAge() {
        if (birthDate == null) {
            return 0;
        }
        return LocalDate.now().getYear() - birthDate.getYear();
    }
    
    public boolean isYoungPet() {
        return getAge() < 2;
    }
    
    public boolean isSeniorPet() {
        return getAge() > 7;
    }
    
    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pet pet = (Pet) o;
        return Objects.equals(id, pet.id) &&
               Objects.equals(name, pet.name) &&
               Objects.equals(species, pet.species) &&
               Objects.equals(owner, pet.owner);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, name, species, owner);
    }
    
    @Override
    public String toString() {
        return "Pet{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", species='" + species + '\'' +
                ", breed='" + breed + '\'' +
                ", birthDate=" + birthDate +
                ", age=" + getAge() +
                '}';
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDate.now();
    }
}