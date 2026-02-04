-- Test data for export integration tests
-- This script creates sample data for testing the export functionality

-- Create database schema
CREATE DATABASE IF NOT EXISTS petclinic;
USE petclinic;

-- Create tables
CREATE TABLE IF NOT EXISTS veterinarians (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    specialization VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS owners (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    address VARCHAR(200),
    city VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    species VARCHAR(30) NOT NULL,
    breed VARCHAR(50),
    age INT,
    weight DECIMAL(5,2),
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES owners(id)
);

CREATE TABLE IF NOT EXISTS visits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    veterinarian_id BIGINT NOT NULL,
    visit_date DATE NOT NULL,
    visit_time TIME,
    reason VARCHAR(200),
    diagnosis VARCHAR(500),
    treatment VARCHAR(500),
    cost DECIMAL(10,2),
    status VARCHAR(20) DEFAULT 'COMPLETED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (pet_id) REFERENCES pets(id),
    FOREIGN KEY (veterinarian_id) REFERENCES veterinarians(id)
);

CREATE TABLE IF NOT EXISTS appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    veterinarian_id BIGINT NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    reason VARCHAR(200),
    status VARCHAR(20) DEFAULT 'SCHEDULED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (pet_id) REFERENCES pets(id),
    FOREIGN KEY (veterinarian_id) REFERENCES veterinarians(id)
);

-- Insert test veterinarians
INSERT INTO veterinarians (first_name, last_name, email, phone, specialization) VALUES
('Dr. Sarah', 'Johnson', 'sarah.johnson@petclinic.com', '555-0101', 'Small Animal Medicine'),
('Dr. Michael', 'Chen', 'michael.chen@petclinic.com', '555-0102', 'Surgery'),
('Dr. Emily', 'Rodriguez', 'emily.rodriguez@petclinic.com', '555-0103', 'Emergency Medicine'),
('Dr. David', 'Wilson', 'david.wilson@petclinic.com', '555-0104', 'Dermatology'),
('Dr. Lisa', 'Anderson', 'lisa.anderson@petclinic.com', '555-0105', 'Cardiology');

-- Insert test owners
INSERT INTO owners (first_name, last_name, email, phone, address, city) VALUES
('John', 'Smith', 'john.smith@email.com', '555-1001', '123 Main St', 'Springfield'),
('Jane', 'Doe', 'jane.doe@email.com', '555-1002', '456 Oak Ave', 'Springfield'),
('Bob', 'Johnson', 'bob.johnson@email.com', '555-1003', '789 Pine Rd', 'Springfield'),
('Alice', 'Brown', 'alice.brown@email.com', '555-1004', '321 Elm St', 'Springfield'),
('Charlie', 'Davis', 'charlie.davis@email.com', '555-1005', '654 Maple Dr', 'Springfield'),
('Diana', 'Miller', 'diana.miller@email.com', '555-1006', '987 Cedar Ln', 'Springfield'),
('Frank', 'Wilson', 'frank.wilson@email.com', '555-1007', '147 Birch Ave', 'Springfield'),
('Grace', 'Taylor', 'grace.taylor@email.com', '555-1008', '258 Spruce St', 'Springfield');

-- Insert test pets
INSERT INTO pets (name, species, breed, age, weight, owner_id) VALUES
('Buddy', 'dog', 'Golden Retriever', 3, 65.50, 1),
('Whiskers', 'cat', 'Persian', 2, 8.25, 1),
('Max', 'dog', 'German Shepherd', 5, 75.00, 2),
('Luna', 'cat', 'Siamese', 1, 6.75, 2),
('Charlie', 'dog', 'Labrador', 4, 70.25, 3),
('Bella', 'cat', 'Maine Coon', 3, 12.50, 3),
('Rocky', 'dog', 'Bulldog', 2, 45.00, 4),
('Mittens', 'cat', 'Tabby', 4, 9.00, 4),
('Duke', 'dog', 'Boxer', 6, 68.75, 5),
('Shadow', 'cat', 'Black Cat', 2, 7.50, 5),
('Daisy', 'dog', 'Beagle', 3, 25.00, 6),
('Smokey', 'cat', 'Russian Blue', 5, 10.25, 6),
('Rex', 'dog', 'Rottweiler', 4, 85.00, 7),
('Princess', 'cat', 'Ragdoll', 1, 8.75, 7),
('Scout', 'dog', 'Border Collie', 2, 40.50, 8);

-- Insert test visits (spread across different dates for testing date ranges)
INSERT INTO visits (pet_id, veterinarian_id, visit_date, visit_time, reason, diagnosis, treatment, cost, status) VALUES
-- Recent visits (last 7 days)
(1, 1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '09:00:00', 'Annual checkup', 'Healthy', 'Vaccinations updated', 125.00, 'COMPLETED'),
(2, 2, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '10:30:00', 'Skin irritation', 'Allergic dermatitis', 'Antihistamine prescribed', 85.50, 'COMPLETED'),
(3, 1, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '14:00:00', 'Limping', 'Sprained paw', 'Rest and pain medication', 95.00, 'COMPLETED'),
(4, 3, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '11:15:00', 'Not eating', 'Upset stomach', 'Dietary changes', 65.00, 'COMPLETED'),
(5, 2, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '15:30:00', 'Surgery follow-up', 'Healing well', 'Suture removal', 75.00, 'COMPLETED'),

-- Last 30 days
(6, 4, DATE_SUB(CURDATE(), INTERVAL 10 DAY), '09:30:00', 'Ear infection', 'Bacterial infection', 'Antibiotics prescribed', 110.00, 'COMPLETED'),
(7, 1, DATE_SUB(CURDATE(), INTERVAL 12 DAY), '13:00:00', 'Breathing issues', 'Respiratory infection', 'Medication and rest', 150.00, 'COMPLETED'),
(8, 3, DATE_SUB(CURDATE(), INTERVAL 15 DAY), '10:00:00', 'Dental cleaning', 'Tartar buildup', 'Professional cleaning', 200.00, 'COMPLETED'),
(9, 2, DATE_SUB(CURDATE(), INTERVAL 18 DAY), '16:00:00', 'Weight loss', 'Hyperthyroidism', 'Medication started', 180.00, 'COMPLETED'),
(10, 5, DATE_SUB(CURDATE(), INTERVAL 20 DAY), '11:30:00', 'Heart murmur check', 'Mild murmur', 'Monitor condition', 165.00, 'COMPLETED'),
(11, 1, DATE_SUB(CURDATE(), INTERVAL 22 DAY), '14:30:00', 'Vaccination', 'Healthy', 'Annual vaccines', 120.00, 'COMPLETED'),
(12, 4, DATE_SUB(CURDATE(), INTERVAL 25 DAY), '09:15:00', 'Skin condition', 'Fungal infection', 'Antifungal treatment', 95.00, 'COMPLETED'),
(13, 3, DATE_SUB(CURDATE(), INTERVAL 28 DAY), '15:00:00', 'Emergency visit', 'Ingested foreign object', 'Surgery required', 850.00, 'COMPLETED'),

-- Older visits (last 90 days)
(14, 2, DATE_SUB(CURDATE(), INTERVAL 35 DAY), '10:45:00', 'Spay surgery', 'Routine spay', 'Surgery completed', 350.00, 'COMPLETED'),
(15, 1, DATE_SUB(CURDATE(), INTERVAL 40 DAY), '13:30:00', 'Wellness exam', 'Healthy', 'Routine checkup', 100.00, 'COMPLETED'),
(1, 3, DATE_SUB(CURDATE(), INTERVAL 45 DAY), '11:00:00', 'Injury', 'Cut on paw', 'Wound cleaning and bandage', 75.00, 'COMPLETED'),
(2, 4, DATE_SUB(CURDATE(), INTERVAL 50 DAY), '14:15:00', 'Eye discharge', 'Conjunctivitis', 'Eye drops prescribed', 60.00, 'COMPLETED'),
(3, 5, DATE_SUB(CURDATE(), INTERVAL 55 DAY), '09:45:00', 'Cardiac exam', 'Normal heart function', 'Routine cardiac check', 140.00, 'COMPLETED'),
(4, 1, DATE_SUB(CURDATE(), INTERVAL 60 DAY), '16:30:00', 'Digestive issues', 'Gastritis', 'Dietary modification', 85.00, 'COMPLETED'),
(5, 2, DATE_SUB(CURDATE(), INTERVAL 65 DAY), '12:00:00', 'Behavioral consult', 'Anxiety', 'Behavior modification plan', 120.00, 'COMPLETED'),
(6, 3, DATE_SUB(CURDATE(), INTERVAL 70 DAY), '10:15:00', 'Parasite check', 'Intestinal worms', 'Deworming treatment', 45.00, 'COMPLETED'),
(7, 4, DATE_SUB(CURDATE(), INTERVAL 75 DAY), '15:45:00', 'Allergy testing', 'Environmental allergies', 'Allergy management plan', 220.00, 'COMPLETED'),
(8, 5, DATE_SUB(CURDATE(), INTERVAL 80 DAY), '11:30:00', 'Orthopedic exam', 'Hip dysplasia', 'Joint supplements recommended', 175.00, 'COMPLETED'),
(9, 1, DATE_SUB(CURDATE(), INTERVAL 85 DAY), '13:15:00', 'Senior wellness', 'Age-related changes', 'Senior care plan', 130.00, 'COMPLETED');

-- Insert upcoming appointments
INSERT INTO appointments (pet_id, veterinarian_id, appointment_date, appointment_time, reason, status) VALUES
(10, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00', 'Follow-up exam', 'SCHEDULED'),
(11, 2, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '10:30:00', 'Vaccination', 'SCHEDULED'),
(12, 3, DATE_ADD(CURDATE(), INTERVAL 3 DAY), '14:00:00', 'Dental cleaning', 'SCHEDULED'),
(13, 4, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '11:15:00', 'Skin check', 'SCHEDULED'),
(14, 5, DATE_ADD(CURDATE(), INTERVAL 7 DAY), '15:30:00', 'Cardiac follow-up', 'SCHEDULED'),
(15, 1, DATE_ADD(CURDATE(), INTERVAL 10 DAY), '09:30:00', 'Wellness exam', 'SCHEDULED'),
(1, 2, DATE_ADD(CURDATE(), INTERVAL 14 DAY), '13:00:00', 'Surgery consultation', 'SCHEDULED');

-- Create indexes for better query performance
CREATE INDEX idx_visits_date ON visits(visit_date);
CREATE INDEX idx_visits_vet ON visits(veterinarian_id);
CREATE INDEX idx_visits_pet ON visits(pet_id);
CREATE INDEX idx_pets_species ON pets(species);
CREATE INDEX idx_appointments_date ON appointments(appointment_date);

-- Create a view for dashboard metrics
CREATE VIEW dashboard_metrics AS
SELECT 
    COUNT(DISTINCT v.id) as total_visits,
    COUNT(DISTINCT p.id) as total_pets,
    COUNT(DISTINCT o.id) as total_owners,
    COUNT(DISTINCT vet.id) as total_veterinarians,
    SUM(v.cost) as total_revenue,
    AVG(v.cost) as average_visit_cost
FROM visits v
JOIN pets p ON v.pet_id = p.id
JOIN owners o ON p.owner_id = o.id
JOIN veterinarians vet ON v.veterinarian_id = vet.id
WHERE v.status = 'COMPLETED';

-- Grant permissions
GRANT ALL PRIVILEGES ON petclinic.* TO 'petclinic'@'%';
FLUSH PRIVILEGES;