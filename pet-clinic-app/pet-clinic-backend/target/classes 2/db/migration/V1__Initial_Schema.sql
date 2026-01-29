-- Initial Pet Clinic Database Schema
-- This migration creates the enhanced database schema for the Pet Clinic application

-- Create owners table (enhanced)
CREATE TABLE IF NOT EXISTS owners (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    address VARCHAR(200),
    city VARCHAR(50),
    telephone VARCHAR(15),
    email VARCHAR(100) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Create pets table (enhanced)
CREATE TABLE IF NOT EXISTS pets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    species VARCHAR(30) NOT NULL,
    breed VARCHAR(50),
    birth_date DATE,
    medical_history TEXT,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (owner_id) REFERENCES owners(id) ON DELETE CASCADE
);

-- Create veterinarians table (enhanced)
CREATE TABLE IF NOT EXISTS veterinarians (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    specialties VARCHAR(200),
    license_number VARCHAR(20) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Create veterinarian_specialties table for enum-based specialties
CREATE TABLE IF NOT EXISTS veterinarian_specialties (
    veterinarian_id BIGINT NOT NULL,
    specialty VARCHAR(50) NOT NULL,
    PRIMARY KEY (veterinarian_id, specialty),
    FOREIGN KEY (veterinarian_id) REFERENCES veterinarians(id) ON DELETE CASCADE
);

-- Create visits table (enhanced)
CREATE TABLE IF NOT EXISTS visits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    visit_date TIMESTAMP NOT NULL,
    visit_type VARCHAR(50),
    duration_minutes INTEGER,
    diagnosis VARCHAR(500),
    treatment VARCHAR(500),
    notes VARCHAR(1000),
    cost DECIMAL(10,2),
    pet_id BIGINT NOT NULL,
    veterinarian_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE CASCADE,
    FOREIGN KEY (veterinarian_id) REFERENCES veterinarians(id) ON DELETE SET NULL
);

-- Add constraints for data integrity
ALTER TABLE pets ADD CONSTRAINT chk_pet_birth_date 
    CHECK (birth_date IS NULL OR birth_date <= CURRENT_DATE);

ALTER TABLE visits ADD CONSTRAINT chk_visit_cost 
    CHECK (cost IS NULL OR cost >= 0);

-- Create indexes for performance (H2 compatible)
CREATE INDEX IF NOT EXISTS idx_owner_name ON owners (last_name, first_name);
CREATE INDEX IF NOT EXISTS idx_owner_email ON owners (email);
CREATE INDEX IF NOT EXISTS idx_owner_city ON owners (city);

CREATE INDEX IF NOT EXISTS idx_pet_owner ON pets (owner_id);
CREATE INDEX IF NOT EXISTS idx_pet_name ON pets (name);
CREATE INDEX IF NOT EXISTS idx_pet_species ON pets (species);
CREATE INDEX IF NOT EXISTS idx_pet_birth_date ON pets (birth_date);

CREATE INDEX IF NOT EXISTS idx_vet_name ON veterinarians (last_name, first_name);
CREATE INDEX IF NOT EXISTS idx_vet_license ON veterinarians (license_number);

CREATE INDEX IF NOT EXISTS idx_specialty ON veterinarian_specialties (specialty);

CREATE INDEX IF NOT EXISTS idx_visit_pet ON visits (pet_id);
CREATE INDEX IF NOT EXISTS idx_visit_vet ON visits (veterinarian_id);
CREATE INDEX IF NOT EXISTS idx_visit_date ON visits (visit_date);
CREATE INDEX IF NOT EXISTS idx_visit_type ON visits (visit_type);

-- Additional performance indexes
CREATE INDEX IF NOT EXISTS idx_pets_age ON pets (birth_date);
CREATE INDEX IF NOT EXISTS idx_visits_datetime ON visits (visit_date);
CREATE INDEX IF NOT EXISTS idx_owners_created ON owners (created_at);
CREATE INDEX IF NOT EXISTS idx_pets_created ON pets (created_at);
CREATE INDEX IF NOT EXISTS idx_vets_created ON veterinarians (created_at);
CREATE INDEX IF NOT EXISTS idx_visits_created ON visits (created_at);