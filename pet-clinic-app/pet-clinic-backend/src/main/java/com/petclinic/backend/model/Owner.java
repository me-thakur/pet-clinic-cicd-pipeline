package com.petclinic.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.petclinic.backend.config.EncryptionConverter;
import com.petclinic.backend.validation.ValidMobileNumber;
import com.petclinic.backend.validation.UniqueMobileNumber;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Owner entity representing a pet owner in the clinic system
 * Validates: Requirements 8.1, 8.2, 8.3
 */
@Entity
@Table(name = "owners")
public class Owner {
    
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
    
    @Size(max = 200, message = "Address must not exceed 200 characters")
    @Column(name = "address", length = 500) // Increased length for encrypted data
    @Convert(converter = EncryptionConverter.class)
    private String address;
    
    @Size(max = 50, message = "City must not exceed 50 characters")
    @Column(name = "city", length = 50)
    private String city;
    
    @Size(max = 50, message = "State must not exceed 50 characters")
    @Column(name = "state", length = 50)
    private String state;
    
    @Size(max = 10, message = "ZIP code must not exceed 10 characters")
    @Column(name = "zip_code", length = 10)
    private String zipCode;
    
    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)\\.]{7,20}$", message = "Invalid telephone format")
    @Column(name = "telephone", length = 200) // Increased length for encrypted data
    @Convert(converter = EncryptionConverter.class)
    private String telephone;
    
    @ValidMobileNumber(message = "Mobile number must be in valid international format")
    @UniqueMobileNumber(message = "Mobile number already exists in the system")
    @Column(name = "mobile_number", length = 200) // Increased length for encrypted data
    @Convert(converter = EncryptionConverter.class)
    private String mobileNumber;
    
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Column(name = "email", length = 100, unique = true)
    private String email;
    
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("owner-pets")
    private List<Pet> pets = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDate createdAt;
    
    @Column(name = "updated_at")
    private LocalDate updatedAt;
    
    // Constructors
    public Owner() {
        this.createdAt = LocalDate.now();
        this.updatedAt = LocalDate.now();
    }
    
    public Owner(String firstName, String lastName, String address, String city, String state, String zipCode, String telephone, String mobileNumber, String email) {
        this();
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.telephone = telephone;
        this.mobileNumber = mobileNumber;
        this.email = email;
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
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
        this.updatedAt = LocalDate.now();
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
        this.updatedAt = LocalDate.now();
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
        this.updatedAt = LocalDate.now();
    }
    
    public String getZipCode() {
        return zipCode;
    }
    
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
        this.updatedAt = LocalDate.now();
    }
    
    public String getTelephone() {
        return telephone;
    }
    
    public void setTelephone(String telephone) {
        this.telephone = telephone;
        this.updatedAt = LocalDate.now();
    }
    
    public String getMobileNumber() {
        return mobileNumber;
    }
    
    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
        this.updatedAt = LocalDate.now();
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
        this.updatedAt = LocalDate.now();
    }
    
    public List<Pet> getPets() {
        return pets;
    }
    
    public void setPets(List<Pet> pets) {
        this.pets = pets;
    }
    
    public void addPet(Pet pet) {
        pets.add(pet);
        pet.setOwner(this);
    }
    
    public void removePet(Pet pet) {
        pets.remove(pet);
        pet.setOwner(null);
    }
    
    public LocalDate getCreatedAt() {
        return createdAt;
    }
    
    public LocalDate getUpdatedAt() {
        return updatedAt;
    }
    
    // Business Methods
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public int getPetCount() {
        return pets.size();
    }
    
    public boolean hasMultiplePets() {
        return pets.size() > 1;
    }
    
    public List<Pet> getPetsBySpecies(String species) {
        return pets.stream()
                .filter(pet -> pet.getSpecies().equalsIgnoreCase(species))
                .collect(java.util.stream.Collectors.toList());
    }
    
    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Owner owner = (Owner) o;
        return Objects.equals(id, owner.id) &&
               Objects.equals(firstName, owner.firstName) &&
               Objects.equals(lastName, owner.lastName) &&
               Objects.equals(email, owner.email) &&
               Objects.equals(mobileNumber, owner.mobileNumber);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName, email, mobileNumber);
    }
    
    @Override
    public String toString() {
        return "Owner{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", mobileNumber='" + mobileNumber + '\'' +
                ", petCount=" + getPetCount() +
                '}';
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDate.now();
    }
}