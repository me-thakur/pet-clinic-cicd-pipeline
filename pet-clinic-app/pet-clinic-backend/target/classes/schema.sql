-- Pet Clinic Database Schema
-- This script creates the database schema for the Pet Clinic application

-- H2 Database compatible schema (no foreign key checks needed for H2)

-- Drop tables if they exist (for clean setup)
-- Order doesn't matter in H2
DROP TABLE IF EXISTS audit_log CASCADE;
DROP TABLE IF EXISTS medical_records CASCADE;
DROP TABLE IF EXISTS vaccinations;
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS visits;
DROP TABLE IF EXISTS vet_specialties;
DROP TABLE IF EXISTS pets;
DROP TABLE IF EXISTS owners;
DROP TABLE IF EXISTS veterinarians;
DROP TABLE IF EXISTS specialties;
DROP TABLE IF EXISTS user_accounts;
DROP TABLE IF EXISTS system_settings;

-- H2 Database compatible schema (no foreign key checks needed for H2)

-- Create specialties table
CREATE TABLE specialties (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create veterinarians table
CREATE TABLE veterinarians (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(30) NOT NULL,
    last_name VARCHAR(30) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_vet_name (last_name, first_name)
);

-- Create vet_specialties junction table (many-to-many relationship)
CREATE TABLE vet_specialties (
    vet_id BIGINT NOT NULL,
    specialty_id BIGINT NOT NULL,
    PRIMARY KEY (vet_id, specialty_id),
    FOREIGN KEY (vet_id) REFERENCES veterinarians(id) ON DELETE CASCADE,
    FOREIGN KEY (specialty_id) REFERENCES specialties(id) ON DELETE CASCADE
);

-- Create owners table
CREATE TABLE owners (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(30) NOT NULL,
    last_name VARCHAR(30) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(80),
    telephone VARCHAR(20),
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner_name (last_name, first_name),
    INDEX idx_owner_city (city)
);

-- Create pets table
CREATE TABLE pets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL,
    birth_date DATE,
    type VARCHAR(30) NOT NULL,
    breed VARCHAR(50),
    color VARCHAR(30),
    weight DECIMAL(5,2),
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES owners(id) ON DELETE CASCADE,
    INDEX idx_pet_owner (owner_id),
    INDEX idx_pet_name (name),
    INDEX idx_pet_type (type)
);

-- Create visits table
CREATE TABLE visits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    vet_id BIGINT,
    visit_date DATE NOT NULL,
    visit_time TIME,
    description TEXT,
    diagnosis TEXT,
    treatment TEXT,
    notes TEXT,
    cost DECIMAL(10,2),
    status VARCHAR(20) DEFAULT 'SCHEDULED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE CASCADE,
    FOREIGN KEY (vet_id) REFERENCES veterinarians(id) ON DELETE SET NULL,
    INDEX idx_visit_pet (pet_id),
    INDEX idx_visit_vet (vet_id),
    INDEX idx_visit_date (visit_date),
    INDEX idx_visit_status (status)
);

-- Create user accounts table for authentication
CREATE TABLE user_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    first_name VARCHAR(30),
    last_name VARCHAR(30),
    role VARCHAR(20) DEFAULT 'USER', -- ADMIN, VET, STAFF, USER
    active BOOLEAN DEFAULT TRUE,
    last_login TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_username (username),
    INDEX idx_user_email (email),
    INDEX idx_user_role (role)
);

-- Create appointments table for scheduling
CREATE TABLE appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    vet_id BIGINT,
    owner_id BIGINT NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    duration_minutes INT DEFAULT 30,
    reason VARCHAR(255),
    status VARCHAR(20) DEFAULT 'SCHEDULED', -- SCHEDULED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED
    notes TEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE CASCADE,
    FOREIGN KEY (vet_id) REFERENCES veterinarians(id) ON DELETE SET NULL,
    FOREIGN KEY (owner_id) REFERENCES owners(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES user_accounts(id) ON DELETE SET NULL,
    INDEX idx_appointment_date (appointment_date),
    INDEX idx_appointment_vet (vet_id, appointment_date),
    INDEX idx_appointment_pet (pet_id),
    INDEX idx_appointment_owner (owner_id),
    INDEX idx_appointment_status (status),
    UNIQUE KEY unique_appointment (vet_id, appointment_date, appointment_time)
);

-- Create medical records table for detailed pet health history
CREATE TABLE medical_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    vet_id BIGINT,
    visit_id BIGINT,
    record_date DATE NOT NULL,
    record_type VARCHAR(50) NOT NULL, -- EXAMINATION, VACCINATION, SURGERY, MEDICATION, etc.
    title VARCHAR(100) NOT NULL,
    description TEXT,
    findings TEXT,
    recommendations TEXT,
    medications TEXT,
    follow_up_date DATE,
    attachments JSON, -- Store file paths/URLs
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE CASCADE,
    FOREIGN KEY (vet_id) REFERENCES veterinarians(id) ON DELETE SET NULL,
    FOREIGN KEY (visit_id) REFERENCES visits(id) ON DELETE SET NULL,
    INDEX idx_medical_pet (pet_id),
    INDEX idx_medical_date (record_date),
    INDEX idx_medical_type (record_type),
    INDEX idx_medical_vet (vet_id)
);

-- Create vaccinations table for tracking pet vaccinations
CREATE TABLE vaccinations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    vet_id BIGINT,
    vaccine_name VARCHAR(100) NOT NULL,
    vaccine_type VARCHAR(50), -- CORE, NON_CORE, REQUIRED
    vaccination_date DATE NOT NULL,
    expiration_date DATE,
    batch_number VARCHAR(50),
    manufacturer VARCHAR(100),
    notes TEXT,
    next_due_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE CASCADE,
    FOREIGN KEY (vet_id) REFERENCES veterinarians(id) ON DELETE SET NULL,
    INDEX idx_vaccination_pet (pet_id),
    INDEX idx_vaccination_date (vaccination_date),
    INDEX idx_vaccination_due (next_due_date),
    INDEX idx_vaccination_type (vaccine_type)
);

-- Create system settings table for application configuration
CREATE TABLE system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT,
    setting_type VARCHAR(20) DEFAULT 'STRING', -- STRING, INTEGER, BOOLEAN, JSON
    description TEXT,
    category VARCHAR(50) DEFAULT 'GENERAL',
    is_public BOOLEAN DEFAULT FALSE, -- Whether setting can be viewed by non-admin users
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_setting_category (category),
    INDEX idx_setting_public (is_public)
);

-- Create audit log table for tracking changes
CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_name VARCHAR(50) NOT NULL,
    record_id BIGINT NOT NULL,
    action VARCHAR(10) NOT NULL, -- INSERT, UPDATE, DELETE
    old_values JSON,
    new_values JSON,
    changed_by VARCHAR(100),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_table (table_name),
    INDEX idx_audit_record (table_name, record_id),
    INDEX idx_audit_date (changed_at)
);

-- Add some constraints and triggers for data integrity

-- Ensure appointment times don't overlap for the same vet
DELIMITER //
CREATE TRIGGER check_appointment_overlap 
BEFORE INSERT ON appointments
FOR EACH ROW
BEGIN
    DECLARE overlap_count INT DEFAULT 0;
    
    SELECT COUNT(*) INTO overlap_count
    FROM appointments 
    WHERE vet_id = NEW.vet_id 
    AND appointment_date = NEW.appointment_date
    AND status NOT IN ('CANCELLED', 'COMPLETED')
    AND (
        (appointment_time <= NEW.appointment_time AND 
         ADDTIME(appointment_time, SEC_TO_TIME(duration_minutes * 60)) > NEW.appointment_time)
        OR
        (NEW.appointment_time <= appointment_time AND 
         ADDTIME(NEW.appointment_time, SEC_TO_TIME(NEW.duration_minutes * 60)) > appointment_time)
    );
    
    IF overlap_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Appointment time conflicts with existing appointment';
    END IF;
END//
DELIMITER ;

-- Ensure pets have valid birth dates
DELIMITER //
CREATE TRIGGER check_pet_birth_date 
BEFORE INSERT ON pets
FOR EACH ROW
BEGIN
    IF NEW.birth_date > CURDATE() THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Pet birth date cannot be in the future';
    END IF;
    
    IF NEW.birth_date < DATE_SUB(CURDATE(), INTERVAL 50 YEAR) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Pet birth date is too far in the past';
    END IF;
END//
DELIMITER ;

-- Ensure visit dates are not in the future beyond reasonable limits
DELIMITER //
CREATE TRIGGER check_visit_date 
BEFORE INSERT ON visits
FOR EACH ROW
BEGIN
    IF NEW.visit_date > DATE_ADD(CURDATE(), INTERVAL 1 YEAR) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Visit date cannot be more than 1 year in the future';
    END IF;
END//
DELIMITER ;

-- Create views for common queries

-- View for pet details with owner information
CREATE VIEW pet_details AS
SELECT 
    p.id as pet_id,
    p.name as pet_name,
    p.birth_date,
    p.type,
    p.breed,
    p.color,
    p.weight,
    TIMESTAMPDIFF(YEAR, p.birth_date, CURDATE()) as age_years,
    TIMESTAMPDIFF(MONTH, p.birth_date, CURDATE()) % 12 as age_months,
    o.id as owner_id,
    CONCAT(o.first_name, ' ', o.last_name) as owner_name,
    o.telephone as owner_phone,
    o.email as owner_email,
    o.address,
    o.city,
    p.created_at as registered_date
FROM pets p
JOIN owners o ON p.owner_id = o.id;

-- View for upcoming appointments
CREATE VIEW upcoming_appointments AS
SELECT 
    a.id as appointment_id,
    a.appointment_date,
    a.appointment_time,
    a.duration_minutes,
    a.reason,
    a.status,
    p.name as pet_name,
    p.type as pet_type,
    CONCAT(o.first_name, ' ', o.last_name) as owner_name,
    o.telephone as owner_phone,
    CONCAT(v.first_name, ' ', v.last_name) as vet_name,
    v.email as vet_email
FROM appointments a
JOIN pets p ON a.pet_id = p.id
JOIN owners o ON a.owner_id = o.id
LEFT JOIN veterinarians v ON a.vet_id = v.id
WHERE a.appointment_date >= CURDATE()
AND a.status IN ('SCHEDULED', 'CONFIRMED')
ORDER BY a.appointment_date, a.appointment_time;

-- View for veterinarian schedules
CREATE VIEW vet_schedules AS
SELECT 
    v.id as vet_id,
    CONCAT(v.first_name, ' ', v.last_name) as vet_name,
    a.appointment_date,
    a.appointment_time,
    a.duration_minutes,
    ADDTIME(a.appointment_time, SEC_TO_TIME(a.duration_minutes * 60)) as end_time,
    p.name as pet_name,
    CONCAT(o.first_name, ' ', o.last_name) as owner_name,
    a.reason,
    a.status
FROM veterinarians v
LEFT JOIN appointments a ON v.id = a.vet_id
LEFT JOIN pets p ON a.pet_id = p.id
LEFT JOIN owners o ON a.owner_id = o.id
WHERE a.appointment_date >= CURDATE() OR a.appointment_date IS NULL
ORDER BY v.last_name, v.first_name, a.appointment_date, a.appointment_time;

-- Add indexes for better performance on common queries
CREATE INDEX idx_pets_age ON pets (birth_date);
CREATE INDEX idx_appointments_datetime ON appointments (appointment_date, appointment_time);
CREATE INDEX idx_visits_datetime ON visits (visit_date, visit_time);
CREATE INDEX idx_medical_records_date ON medical_records (record_date);

-- Add comments to tables for documentation
ALTER TABLE owners COMMENT = 'Pet owners information and contact details';
ALTER TABLE pets COMMENT = 'Pet information including breed, age, and owner relationship';
ALTER TABLE veterinarians COMMENT = 'Veterinarian staff information and contact details';
ALTER TABLE specialties COMMENT = 'Medical specialties that veterinarians can have';
ALTER TABLE vet_specialties COMMENT = 'Many-to-many relationship between vets and their specialties';
ALTER TABLE visits COMMENT = 'Pet visits to the clinic with diagnosis and treatment information';
ALTER TABLE appointments COMMENT = 'Scheduled appointments for pets with veterinarians';
ALTER TABLE medical_records COMMENT = 'Detailed medical history and records for each pet';
ALTER TABLE vaccinations COMMENT = 'Vaccination records and schedules for pets';
ALTER TABLE user_accounts COMMENT = 'System user accounts for authentication and authorization';
ALTER TABLE system_settings COMMENT = 'Application configuration settings';
ALTER TABLE audit_log COMMENT = 'Audit trail for tracking data changes';

-- Schema creation completed
SELECT 'Pet Clinic database schema created successfully!' as message;