-- Pet Clinic Sample Data
-- This script populates the database with sample data for development and testing

-- Insert specialties
INSERT INTO specialties (name) VALUES
('General Practice'),
('Surgery'),
('Cardiology'),
('Dermatology'),
('Orthopedics'),
('Oncology'),
('Ophthalmology'),
('Dentistry'),
('Emergency Medicine'),
('Internal Medicine');

-- Insert veterinarians
INSERT INTO veterinarians (first_name, last_name, email, phone) VALUES
('Sarah', 'Johnson', 'sarah.johnson@petclinic.com', '555-0101'),
('Michael', 'Chen', 'michael.chen@petclinic.com', '555-0102'),
('Emily', 'Rodriguez', 'emily.rodriguez@petclinic.com', '555-0103'),
('David', 'Thompson', 'david.thompson@petclinic.com', '555-0104'),
('Lisa', 'Anderson', 'lisa.anderson@petclinic.com', '555-0105'),
('James', 'Wilson', 'james.wilson@petclinic.com', '555-0106');

-- Assign specialties to veterinarians
INSERT INTO vet_specialties (vet_id, specialty_id) VALUES
-- Dr. Sarah Johnson - General Practice and Surgery
(1, 1), (1, 2),
-- Dr. Michael Chen - Cardiology and Internal Medicine
(2, 3), (2, 10),
-- Dr. Emily Rodriguez - Dermatology and General Practice
(3, 4), (3, 1),
-- Dr. David Thompson - Orthopedics and Surgery
(4, 5), (4, 2),
-- Dr. Lisa Anderson - Oncology and Internal Medicine
(5, 6), (5, 10),
-- Dr. James Wilson - Emergency Medicine and General Practice
(6, 9), (6, 1);

-- Insert sample owners
INSERT INTO owners (first_name, last_name, address, city, telephone, email) VALUES
('John', 'Smith', '123 Main Street', 'Springfield', '555-1234', 'john.smith@email.com'),
('Mary', 'Johnson', '456 Oak Avenue', 'Springfield', '555-5678', 'mary.johnson@email.com'),
('Robert', 'Brown', '789 Pine Road', 'Riverside', '555-9012', 'robert.brown@email.com'),
('Jennifer', 'Davis', '321 Elm Street', 'Springfield', '555-3456', 'jennifer.davis@email.com'),
('William', 'Miller', '654 Maple Drive', 'Riverside', '555-7890', 'william.miller@email.com'),
('Susan', 'Wilson', '987 Cedar Lane', 'Springfield', '555-2345', 'susan.wilson@email.com'),
('James', 'Moore', '147 Birch Street', 'Riverside', '555-6789', 'james.moore@email.com'),
('Patricia', 'Taylor', '258 Walnut Avenue', 'Springfield', '555-0123', 'patricia.taylor@email.com'),
('Michael', 'Anderson', '369 Cherry Road', 'Riverside', '555-4567', 'michael.anderson@email.com'),
('Linda', 'Thomas', '741 Spruce Drive', 'Springfield', '555-8901', 'linda.thomas@email.com');

-- Insert sample pets
INSERT INTO pets (name, birth_date, type, breed, color, weight, owner_id) VALUES
-- John Smith's pets
('Buddy', '2020-03-15', 'Dog', 'Golden Retriever', 'Golden', 32.5, 1),
('Whiskers', '2019-07-22', 'Cat', 'Siamese', 'Cream', 4.2, 1),

-- Mary Johnson's pets
('Max', '2021-01-10', 'Dog', 'German Shepherd', 'Black and Tan', 28.7, 2),
('Luna', '2020-11-05', 'Cat', 'Maine Coon', 'Gray', 5.8, 2),

-- Robert Brown's pets
('Charlie', '2018-09-12', 'Dog', 'Labrador', 'Chocolate', 30.2, 3),
('Mittens', '2021-04-18', 'Cat', 'Persian', 'White', 4.5, 3),

-- Jennifer Davis's pets
('Rocky', '2019-12-03', 'Dog', 'Bulldog', 'Brindle', 25.1, 4),
('Shadow', '2020-08-14', 'Cat', 'Black Shorthair', 'Black', 4.8, 4),

-- William Miller's pets
('Daisy', '2021-06-20', 'Dog', 'Beagle', 'Tricolor', 12.3, 5),
('Smokey', '2019-02-28', 'Cat', 'Russian Blue', 'Blue-Gray', 5.1, 5),

-- Susan Wilson's pets
('Bear', '2020-10-07', 'Dog', 'Rottweiler', 'Black and Tan', 45.6, 6),
('Princess', '2021-03-25', 'Cat', 'Ragdoll', 'Seal Point', 4.9, 6),

-- James Moore's pets
('Zeus', '2018-05-16', 'Dog', 'Great Dane', 'Fawn', 68.2, 7),
('Bella', '2020-12-11', 'Cat', 'Bengal', 'Brown Spotted', 4.3, 7),

-- Patricia Taylor's pets
('Milo', '2021-08-02', 'Dog', 'Poodle', 'White', 8.7, 8),
('Coco', '2019-11-30', 'Cat', 'Himalayan', 'Chocolate Point', 5.2, 8),

-- Michael Anderson's pets
('Duke', '2020-04-09', 'Dog', 'Boxer', 'Fawn', 31.8, 9),
('Ginger', '2021-01-17', 'Cat', 'Orange Tabby', 'Orange', 4.6, 9),

-- Linda Thomas's pets
('Scout', '2019-06-24', 'Dog', 'Border Collie', 'Black and White', 22.4, 10),
('Patches', '2020-09-13', 'Cat', 'Calico', 'Calico', 4.7, 10);

-- Insert sample visits
INSERT INTO visits (pet_id, vet_id, visit_date, visit_time, description, diagnosis, treatment, cost, status) VALUES
-- Recent completed visits
(1, 1, '2023-11-15', '09:00:00', 'Annual checkup and vaccinations', 'Healthy, up to date on vaccines', 'Rabies and DHPP vaccines administered', 125.00, 'COMPLETED'),
(2, 3, '2023-11-10', '14:30:00', 'Skin irritation on paws', 'Allergic dermatitis', 'Prescribed antihistamine and medicated shampoo', 85.50, 'COMPLETED'),
(3, 2, '2023-11-08', '11:15:00', 'Heart murmur detected during routine exam', 'Grade 2/6 heart murmur', 'Echocardiogram scheduled, monitoring recommended', 200.00, 'COMPLETED'),
(4, 1, '2023-11-05', '16:00:00', 'Limping on front left paw', 'Mild sprain', 'Rest and anti-inflammatory medication', 95.00, 'COMPLETED'),
(5, 4, '2023-11-03', '10:30:00', 'Hip dysplasia follow-up', 'Mild improvement with treatment', 'Continue current medication, recheck in 3 months', 110.00, 'COMPLETED'),

-- Upcoming scheduled visits
(6, 1, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '09:30:00', 'Routine wellness exam', NULL, NULL, NULL, 'SCHEDULED'),
(7, 6, DATE_ADD(CURDATE(), INTERVAL 3 DAY), '13:45:00', 'Breathing difficulties', NULL, NULL, NULL, 'SCHEDULED'),
(8, 3, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '15:15:00', 'Skin condition follow-up', NULL, NULL, NULL, 'SCHEDULED'),
(9, 1, DATE_ADD(CURDATE(), INTERVAL 7 DAY), '11:00:00', 'Puppy vaccinations', NULL, NULL, NULL, 'SCHEDULED'),
(10, 2, DATE_ADD(CURDATE(), INTERVAL 10 DAY), '14:00:00', 'Cardiac evaluation', NULL, NULL, NULL, 'SCHEDULED');

-- Insert sample appointments
INSERT INTO appointments (pet_id, vet_id, owner_id, appointment_date, appointment_time, duration_minutes, reason, status) VALUES
-- Today's appointments
(11, 1, 6, CURDATE(), '09:00:00', 30, 'Annual wellness exam', 'CONFIRMED'),
(12, 2, 6, CURDATE(), '10:30:00', 45, 'Cardiac consultation', 'CONFIRMED'),
(13, 3, 7, CURDATE(), '14:00:00', 30, 'Skin condition check', 'SCHEDULED'),
(14, 4, 7, CURDATE(), '15:30:00', 60, 'Orthopedic evaluation', 'CONFIRMED'),

-- Tomorrow's appointments
(15, 1, 8, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '08:30:00', 30, 'Vaccination booster', 'SCHEDULED'),
(16, 5, 8, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '11:00:00', 45, 'Oncology consultation', 'CONFIRMED'),
(17, 6, 9, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '13:30:00', 30, 'Emergency follow-up', 'CONFIRMED'),
(18, 1, 9, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '16:00:00', 30, 'Routine checkup', 'SCHEDULED'),

-- Next week's appointments
(1, 2, 1, DATE_ADD(CURDATE(), INTERVAL 7 DAY), '09:15:00', 45, 'Cardiology follow-up', 'SCHEDULED'),
(2, 3, 1, DATE_ADD(CURDATE(), INTERVAL 8 DAY), '14:45:00', 30, 'Dermatology recheck', 'SCHEDULED'),
(3, 4, 2, DATE_ADD(CURDATE(), INTERVAL 9 DAY), '10:00:00', 60, 'Surgical consultation', 'SCHEDULED'),
(4, 1, 2, DATE_ADD(CURDATE(), INTERVAL 10 DAY), '15:00:00', 30, 'Post-treatment check', 'SCHEDULED');

-- Insert sample medical records
INSERT INTO medical_records (pet_id, vet_id, visit_id, record_date, record_type, title, description, findings, recommendations) VALUES
(1, 1, 1, '2023-11-15', 'EXAMINATION', 'Annual Wellness Examination', 
 'Comprehensive physical examination including weight, temperature, heart rate, and general health assessment',
 'Patient appears healthy with normal vital signs. Weight: 32.5kg, Temperature: 101.2°F, Heart Rate: 80 bpm. No abnormalities detected.',
 'Continue current diet and exercise routine. Schedule next annual exam in 12 months.'),

(2, 3, 2, '2023-11-10', 'EXAMINATION', 'Dermatological Consultation',
 'Examination of skin irritation on paws with microscopic analysis of skin samples',
 'Mild allergic dermatitis affecting interdigital spaces. No secondary bacterial infection present.',
 'Avoid known allergens, use hypoallergenic shampoo, continue prescribed antihistamine for 10 days.'),

(3, 2, 3, '2023-11-08', 'EXAMINATION', 'Cardiac Evaluation',
 'Cardiac auscultation revealed heart murmur, ECG and chest X-rays performed',
 'Grade 2/6 systolic murmur detected. ECG shows normal rhythm. Chest X-rays reveal mild left atrial enlargement.',
 'Schedule echocardiogram within 2 weeks. Monitor for exercise intolerance or breathing difficulties.'),

(1, 1, NULL, '2023-06-15', 'VACCINATION', 'Annual Vaccinations',
 'Administration of core vaccines including rabies, DHPP, and bordetella',
 'All vaccines administered without adverse reactions. Patient tolerated procedure well.',
 'Next vaccination due in 12 months. Monitor for any delayed reactions over next 48 hours.'),

(5, 4, 5, '2023-11-03', 'EXAMINATION', 'Orthopedic Follow-up',
 'Evaluation of hip dysplasia treatment progress with physical examination and gait analysis',
 'Mild improvement in mobility. Less stiffness observed. Owner reports increased activity level.',
 'Continue current NSAID therapy. Maintain weight management. Recheck in 3 months.');

-- Insert sample vaccinations
INSERT INTO vaccinations (pet_id, vet_id, vaccine_name, vaccine_type, vaccination_date, expiration_date, batch_number, manufacturer, next_due_date) VALUES
-- Buddy's vaccinations
(1, 1, 'Rabies', 'CORE', '2023-11-15', '2024-11-15', 'RB2023-1145', 'VetVaccines Inc.', '2024-11-15'),
(1, 1, 'DHPP', 'CORE', '2023-11-15', '2024-11-15', 'DH2023-0892', 'VetVaccines Inc.', '2024-11-15'),
(1, 1, 'Bordetella', 'NON_CORE', '2023-11-15', '2024-05-15', 'BD2023-0445', 'PetHealth Labs', '2024-05-15'),

-- Whiskers' vaccinations
(2, 1, 'FVRCP', 'CORE', '2023-06-20', '2024-06-20', 'FC2023-0678', 'FelineVax Corp.', '2024-06-20'),
(2, 1, 'Rabies', 'CORE', '2023-06-20', '2026-06-20', 'RB2023-0889', 'VetVaccines Inc.', '2026-06-20'),

-- Max's vaccinations
(3, 1, 'Rabies', 'CORE', '2023-08-10', '2024-08-10', 'RB2023-1001', 'VetVaccines Inc.', '2024-08-10'),
(3, 1, 'DHPP', 'CORE', '2023-08-10', '2024-08-10', 'DH2023-0934', 'VetVaccines Inc.', '2024-08-10'),

-- Luna's vaccinations
(4, 1, 'FVRCP', 'CORE', '2023-07-15', '2024-07-15', 'FC2023-0723', 'FelineVax Corp.', '2024-07-15'),
(4, 1, 'Rabies', 'CORE', '2023-07-15', '2026-07-15', 'RB2023-0945', 'VetVaccines Inc.', '2026-07-15'),

-- Charlie's vaccinations
(5, 1, 'Rabies', 'CORE', '2023-05-20', '2024-05-20', 'RB2023-0567', 'VetVaccines Inc.', '2024-05-20'),
(5, 1, 'DHPP', 'CORE', '2023-05-20', '2024-05-20', 'DH2023-0612', 'VetVaccines Inc.', '2024-05-20');

-- Insert user accounts for system access
INSERT INTO user_accounts (username, password_hash, email, first_name, last_name, role) VALUES
-- Admin account (password: admin123)
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBaLyifkdwP7Iq', 'admin@petclinic.com', 'System', 'Administrator', 'ADMIN'),

-- Veterinarian accounts (password: admin123 - using same as admin for demo)
('sjohnson', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBaLyifkdwP7Iq', 'sarah.johnson@petclinic.com', 'Sarah', 'Johnson', 'VET'),
('mchen', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBaLyifkdwP7Iq', 'michael.chen@petclinic.com', 'Michael', 'Chen', 'VET'),
('erodriguez', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBaLyifkdwP7Iq', 'emily.rodriguez@petclinic.com', 'Emily', 'Rodriguez', 'VET'),
('vet1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBaLyifkdwP7Iq', 'vet1@petclinic.com', 'Dr. Vet', 'One', 'VET'),
('vet2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBaLyifkdwP7Iq', 'vet2@petclinic.com', 'Dr. Vet', 'Two', 'VET'),

-- Staff accounts (password: staff123)
('receptionist1', '$2a$10$7L2p/g7LjjlpXSvdr6K13.3I3c3G4uS4vNlQIpphEIcNPDXcsO.3d', 'reception@petclinic.com', 'Jane', 'Doe', 'STAFF'),
('nurse1', '$2a$10$7L2p/g7LjjlpXSvdr6K13.3I3c3G4uS4vNlQIpphEIcNPDXcsO.3d', 'nurse@petclinic.com', 'Alice', 'Smith', 'STAFF'),
('staff1', '$2a$10$7L2p/g7LjjlpXSvdr6K13.3I3c3G4uS4vNlQIpphEIcNPDXcsO.3d', 'staff1@petclinic.com', 'Staff', 'One', 'STAFF'),
('staff2', '$2a$10$7L2p/g7LjjlpXSvdr6K13.3I3c3G4uS4vNlQIpphEIcNPDXcsO.3d', 'staff2@petclinic.com', 'Staff', 'Two', 'STAFF'),

-- Additional user accounts 
-- user123 hash: $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.
-- test123 hash: $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi. (using same for demo)
-- password123 hash: $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi. (using same for demo)
('user', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'user@petclinic.com', 'Test', 'User', 'USER'),
('test', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'test@petclinic.com', 'Test', 'Account', 'USER'),
('demo', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'demo@petclinic.com', 'Demo', 'User', 'USER');

-- Insert system settings
INSERT INTO system_settings (setting_key, setting_value, setting_type, description, category, is_public) VALUES
('clinic_name', 'Pet Clinic Management System', 'STRING', 'Name of the veterinary clinic', 'GENERAL', TRUE),
('clinic_address', '123 Veterinary Lane, Pet City, PC 12345', 'STRING', 'Physical address of the clinic', 'GENERAL', TRUE),
('clinic_phone', '555-PET-CARE', 'STRING', 'Main phone number for the clinic', 'GENERAL', TRUE),
('clinic_email', 'info@petclinic.com', 'STRING', 'Main email address for the clinic', 'GENERAL', TRUE),
('business_hours', '{"monday": "8:00-18:00", "tuesday": "8:00-18:00", "wednesday": "8:00-18:00", "thursday": "8:00-18:00", "friday": "8:00-18:00", "saturday": "9:00-15:00", "sunday": "closed"}', 'JSON', 'Business hours for each day of the week', 'GENERAL', TRUE),
('appointment_duration_default', '30', 'INTEGER', 'Default appointment duration in minutes', 'APPOINTMENTS', FALSE),
('appointment_buffer_time', '15', 'INTEGER', 'Buffer time between appointments in minutes', 'APPOINTMENTS', FALSE),
('max_appointments_per_day', '20', 'INTEGER', 'Maximum number of appointments per veterinarian per day', 'APPOINTMENTS', FALSE),
('vaccination_reminder_days', '30', 'INTEGER', 'Days before vaccination due date to send reminders', 'NOTIFICATIONS', FALSE),
('appointment_reminder_hours', '24', 'INTEGER', 'Hours before appointment to send reminders', 'NOTIFICATIONS', FALSE),
('enable_email_notifications', 'true', 'BOOLEAN', 'Enable email notifications for appointments and reminders', 'NOTIFICATIONS', FALSE),
('enable_sms_notifications', 'false', 'BOOLEAN', 'Enable SMS notifications for appointments and reminders', 'NOTIFICATIONS', FALSE),
('currency_symbol', '$', 'STRING', 'Currency symbol for pricing', 'BILLING', TRUE),
('tax_rate', '0.08', 'STRING', 'Tax rate for services (as decimal)', 'BILLING', FALSE),
('backup_retention_days', '90', 'INTEGER', 'Number of days to retain database backups', 'SYSTEM', FALSE),
('session_timeout_minutes', '60', 'INTEGER', 'User session timeout in minutes', 'SECURITY', FALSE);

-- Insert some audit log entries (examples)
INSERT INTO audit_log (table_name, record_id, action, new_values, changed_by, changed_at) VALUES
('owners', 1, 'INSERT', '{"first_name": "John", "last_name": "Smith", "telephone": "555-1234"}', 'admin', '2023-11-01 10:00:00'),
('pets', 1, 'INSERT', '{"name": "Buddy", "type": "Dog", "owner_id": 1}', 'admin', '2023-11-01 10:05:00'),
('appointments', 1, 'INSERT', '{"pet_id": 1, "vet_id": 1, "appointment_date": "2023-11-15"}', 'receptionist1', '2023-11-10 14:30:00'),
('visits', 1, 'UPDATE', '{"status": "COMPLETED", "cost": 125.00}', 'sjohnson', '2023-11-15 09:45:00');

-- Create some test data for edge cases and validation

-- Pet with very recent birth date
INSERT INTO pets (name, birth_date, type, breed, color, weight, owner_id) VALUES
('Newborn', DATE_SUB(CURDATE(), INTERVAL 7 DAY), 'Dog', 'Mixed', 'Brown', 0.5, 1);

-- Very old pet
INSERT INTO pets (name, birth_date, type, breed, color, weight, owner_id) VALUES
('Senior', '2010-01-01', 'Cat', 'Domestic Shorthair', 'Gray', 3.8, 2);

-- Pet with no breed specified
INSERT INTO pets (name, birth_date, type, breed, color, weight, owner_id) VALUES
('Mystery', '2021-05-15', 'Bird', NULL, 'Green', 0.1, 3);

-- Owner with minimal information
INSERT INTO owners (first_name, last_name, telephone) VALUES
('Jane', 'Minimal', '555-0000');

-- Emergency appointment for today
INSERT INTO appointments (pet_id, vet_id, owner_id, appointment_date, appointment_time, duration_minutes, reason, status) VALUES
((SELECT id FROM pets WHERE name = 'Newborn'), 6, 1, CURDATE(), '17:00:00', 45, 'Emergency - not eating', 'CONFIRMED');

-- Vaccination that's overdue
INSERT INTO vaccinations (pet_id, vet_id, vaccine_name, vaccine_type, vaccination_date, expiration_date, next_due_date) VALUES
((SELECT id FROM pets WHERE name = 'Senior'), 1, 'Rabies', 'CORE', '2022-01-15', '2023-01-15', '2023-01-15');

-- Medical record with follow-up needed
INSERT INTO medical_records (pet_id, vet_id, record_date, record_type, title, description, recommendations, follow_up_date) VALUES
((SELECT id FROM pets WHERE name = 'Senior'), 5, '2023-10-15', 'EXAMINATION', 'Geriatric Health Assessment',
 'Comprehensive senior pet examination including blood work and urinalysis',
 'Monitor kidney function closely. Recheck blood work in 3 months. Consider dietary changes.',
 DATE_ADD(CURDATE(), INTERVAL 30 DAY));

-- Add a setting for the last data update
INSERT INTO system_settings (setting_key, setting_value, setting_type, description, category, is_public) VALUES
('last_data_update', NOW(), 'STRING', 'Timestamp of last sample data insertion', 'SYSTEM', FALSE);

-- Sample data insertion completed
SELECT 'Pet Clinic sample data inserted successfully!' as message,
       (SELECT COUNT(*) FROM owners) as total_owners,
       (SELECT COUNT(*) FROM pets) as total_pets,
       (SELECT COUNT(*) FROM veterinarians) as total_vets,
       (SELECT COUNT(*) FROM appointments WHERE appointment_date >= CURDATE()) as upcoming_appointments,
       (SELECT COUNT(*) FROM visits) as total_visits;