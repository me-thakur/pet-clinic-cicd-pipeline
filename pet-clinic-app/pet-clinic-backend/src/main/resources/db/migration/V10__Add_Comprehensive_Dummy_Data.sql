-- Migration V10: Add Comprehensive Dummy Data
-- This migration adds extensive dummy data for development and testing

-- Additional Specialties
INSERT INTO specialties (name) VALUES
('Behavioral Medicine'),
('Exotic Animal Medicine'),
('Radiology'),
('Anesthesiology'),
('Pathology'),
('Nutrition'),
('Rehabilitation'),
('Acupuncture'),
('Holistic Medicine'),
('Preventive Medicine');

-- Additional Veterinarians (20 total)
INSERT INTO veterinarians (first_name, last_name, email, phone) VALUES
('Amanda', 'Parker', 'amanda.parker@petclinic.com', '555-0107'),
('Christopher', 'Lee', 'christopher.lee@petclinic.com', '555-0108'),
('Rachel', 'Martinez', 'rachel.martinez@petclinic.com', '555-0109'),
('Kevin', 'Garcia', 'kevin.garcia@petclinic.com', '555-0110'),
('Nicole', 'White', 'nicole.white@petclinic.com', '555-0111'),
('Daniel', 'Clark', 'daniel.clark@petclinic.com', '555-0112'),
('Stephanie', 'Lewis', 'stephanie.lewis@petclinic.com', '555-0113'),
('Matthew', 'Walker', 'matthew.walker@petclinic.com', '555-0114'),
('Jessica', 'Hall', 'jessica.hall@petclinic.com', '555-0115'),
('Andrew', 'Allen', 'andrew.allen@petclinic.com', '555-0116'),
('Michelle', 'Young', 'michelle.young@petclinic.com', '555-0117'),
('Ryan', 'King', 'ryan.king@petclinic.com', '555-0118'),
('Laura', 'Wright', 'laura.wright@petclinic.com', '555-0119'),
('Brandon', 'Lopez', 'brandon.lopez@petclinic.com', '555-0120');

-- Assign additional specialties to veterinarians
INSERT INTO vet_specialties (vet_id, specialty_id) VALUES
-- Dr. Amanda Parker - Behavioral Medicine and General Practice
(7, 11), (7, 1),
-- Dr. Christopher Lee - Exotic Animal Medicine and Surgery
(8, 12), (8, 2),
-- Dr. Rachel Martinez - Radiology and Internal Medicine
(9, 13), (9, 10),
-- Dr. Kevin Garcia - Anesthesiology and Surgery
(10, 14), (10, 2),
-- Dr. Nicole White - Pathology and Oncology
(11, 15), (11, 6),
-- Dr. Daniel Clark - Nutrition and General Practice
(12, 16), (12, 1),
-- Dr. Stephanie Lewis - Rehabilitation and Orthopedics
(13, 17), (13, 5),
-- Dr. Matthew Walker - Acupuncture and Holistic Medicine
(14, 18), (14, 19),
-- Dr. Jessica Hall - Preventive Medicine and General Practice
(15, 20), (15, 1),
-- Dr. Andrew Allen - Emergency Medicine and Surgery
(16, 9), (16, 2),
-- Dr. Michelle Young - Dermatology and Ophthalmology
(17, 4), (17, 7),
-- Dr. Ryan King - Cardiology and Internal Medicine
(18, 3), (18, 10),
-- Dr. Laura Wright - Dentistry and General Practice
(19, 8), (19, 1),
-- Dr. Brandon Lopez - Orthopedics and Rehabilitation
(20, 5), (20, 17);

-- Additional Pet Owners (50 total)
INSERT INTO owners (first_name, last_name, address, city, telephone, email, zip_code, state, mobile_number) VALUES
('Sarah', 'Connor', '1984 Skynet Drive', 'Future City', '555-2029', 'sarah.connor@resistance.com', '12345', 'CA', '555-2030'),
('Bruce', 'Wayne', '1007 Mountain Drive', 'Gotham', '555-BATMAN', 'bruce.wayne@wayneent.com', '53540', 'NJ', '555-DARK'),
('Clark', 'Kent', '344 Clinton Street', 'Metropolis', '555-SUPER', 'clark.kent@dailyplanet.com', '12345', 'KS', '555-HERO'),
('Diana', 'Prince', '1600 Paradise Island', 'Themyscira', '555-WONDER', 'diana.prince@amazon.com', '00001', 'DC', '555-LASSO'),
('Peter', 'Parker', '20 Ingram Street', 'Forest Hills', '555-SPIDER', 'peter.parker@bugle.com', '11375', 'NY', '555-WEB'),
('Tony', 'Stark', '10880 Malibu Point', 'Malibu', '555-IRON', 'tony.stark@stark.com', '90265', 'CA', '555-SUIT'),
('Natasha', 'Romanoff', '1 Shield Way', 'Classified', '555-BLACK', 'natasha.romanoff@shield.gov', '00000', 'XX', '555-WIDOW'),
('Steve', 'Rogers', '569 Leaman Place', 'Brooklyn', '555-SHIELD', 'steve.rogers@avengers.com', '11201', 'NY', '555-CAP'),
('Thor', 'Odinson', '1 Rainbow Bridge', 'Asgard', '555-THUNDER', 'thor@asgard.realm', '00001', 'AS', '555-HAMMER'),
('Bruce', 'Banner', '1 Gamma Lab', 'Culver University', '555-HULK', 'bruce.banner@culver.edu', '22903', 'VA', '555-SMASH'),
('Wanda', 'Maximoff', '1 Westview Drive', 'Westview', '555-SCARLET', 'wanda.maximoff@hex.com', '07001', 'NJ', '555-WITCH'),
('Scott', 'Lang', '1 Pym Particle Way', 'San Francisco', '555-ANT', 'scott.lang@pym.com', '94102', 'CA', '555-SMALL'),
('Carol', 'Danvers', '1 Kree Outpost', 'Space Station', '555-MARVEL', 'carol.danvers@airforce.mil', '00000', 'SP', '555-BINARY'),
('Stephen', 'Strange', '177A Bleecker Street', 'New York', '555-MYSTIC', 'stephen.strange@sanctum.com', '10012', 'NY', '555-MAGIC'),
('T''Challa', 'Udaku', '1 Royal Palace', 'Wakanda', '555-PANTHER', 'tchalla@wakanda.gov', '00001', 'WK', '555-VIBRANIUM'),
('Emma', 'Watson', '123 Hogwarts Lane', 'London', '555-0201', 'emma.watson@magic.uk', 'SW1A 1AA', 'UK', '555-0202'),
('Leonardo', 'DiCaprio', '456 Hollywood Blvd', 'Los Angeles', '555-0203', 'leo.dicaprio@movies.com', '90028', 'CA', '555-0204'),
('Jennifer', 'Lawrence', '789 Hunger Ave', 'Louisville', '555-0205', 'jen.lawrence@mockingjay.com', '40202', 'KY', '555-0206'),
('Ryan', 'Reynolds', '321 Deadpool Street', 'Vancouver', '555-0207', 'ryan.reynolds@maximum.ca', 'V6B 1A1', 'BC', '555-0208'),
('Scarlett', 'Johansson', '654 Avenger Way', 'New York', '555-0209', 'scarlett.j@blackwidow.com', '10001', 'NY', '555-0210'),
('Chris', 'Hemsworth', '987 Thunder Road', 'Byron Bay', '555-0211', 'chris.hemsworth@thor.au', '2481', 'NSW', '555-0212'),
('Mark', 'Ruffalo', '147 Hulk Street', 'Kenosha', '555-0213', 'mark.ruffalo@green.com', '53140', 'WI', '555-0214'),
('Jeremy', 'Renner', '258 Hawkeye Lane', 'Modesto', '555-0215', 'jeremy.renner@arrow.com', '95354', 'CA', '555-0216'),
('Paul', 'Rudd', '369 Ant Hill', 'Kansas City', '555-0217', 'paul.rudd@antman.com', '64108', 'MO', '555-0218'),
('Brie', 'Larson', '741 Captain Drive', 'Sacramento', '555-0219', 'brie.larson@marvel.com', '95814', 'CA', '555-0220'),
('Tom', 'Holland', '852 Spider Lane', 'Kingston', '555-0221', 'tom.holland@spidey.uk', 'KT1 1EU', 'UK', '555-0222'),
('Zendaya', 'Coleman', '963 MJ Street', 'Oakland', '555-0223', 'zendaya@spiderman.com', '94612', 'CA', '555-0224'),
('Benedict', 'Cumberbatch', '159 Strange Way', 'London', '555-0225', 'benedict@sorcerer.uk', 'W1K 4HR', 'UK', '555-0226'),
('Elizabeth', 'Olsen', '357 Wanda Road', 'Sherman Oaks', '555-0227', 'elizabeth.olsen@scarlet.com', '91403', 'CA', '555-0228'),
('Anthony', 'Mackie', '468 Falcon Street', 'New Orleans', '555-0229', 'anthony.mackie@wings.com', '70112', 'LA', '555-0230'),
('Sebastian', 'Stan', '579 Winter Ave', 'Constanta', '555-0231', 'sebastian.stan@soldier.ro', '900001', 'RO', '555-0232'),
('Chadwick', 'Boseman', '680 Wakanda Way', 'Anderson', '555-0233', 'chadwick@panther.com', '29621', 'SC', '555-0234'),
('Michael', 'Jordan', '791 Basketball Court', 'Chicago', '555-0235', 'mj@bulls.com', '60601', 'IL', '555-0236'),
('Serena', 'Williams', '802 Tennis Court', 'Saginaw', '555-0237', 'serena@tennis.com', '48601', 'MI', '555-0238'),
('Oprah', 'Winfrey', '913 Media Blvd', 'Kosciusko', '555-0239', 'oprah@harpo.com', '39090', 'MS', '555-0240'),
('Ellen', 'DeGeneres', '024 Comedy Lane', 'Metairie', '555-0241', 'ellen@show.com', '70001', 'LA', '555-0242'),
('Gordon', 'Ramsay', '135 Kitchen Street', 'Johnstone', '555-0243', 'gordon@hells.kitchen', 'PA5 8DF', 'UK', '555-0244'),
('Jamie', 'Oliver', '246 Naked Chef Ave', 'Clavering', '555-0245', 'jamie@oliver.uk', 'CB11 4QT', 'UK', '555-0246'),
('Martha', 'Stewart', '357 Living Way', 'Nutley', '555-0247', 'martha@stewart.com', '07110', 'NJ', '555-0248'),
('Rachel', 'Ray', '468 30Min Street', 'Glens Falls', '555-0249', 'rachel@ray.com', '12801', 'NY', '555-0250');

-- Diverse Pet Collection (100+ pets with various species, breeds, ages)
INSERT INTO pets (name, birth_date, species, breed, medical_history, owner_id) VALUES
-- Dogs of various breeds and sizes
('Ace', '2020-01-15', 'Dog', 'German Shepherd', 'Hip dysplasia monitoring required', 11),
('Bella', '2019-03-22', 'Dog', 'Labrador Retriever', 'Allergic to chicken, beef diet only', 12),
('Charlie', '2021-06-10', 'Dog', 'French Bulldog', 'Breathing issues, avoid overexertion', 13),
('Daisy', '2018-11-05', 'Dog', 'Border Collie', 'High energy, needs mental stimulation', 14),
('Elvis', '2020-09-18', 'Dog', 'Basset Hound', 'Ear infections, regular cleaning needed', 15),
('Finn', '2021-02-28', 'Dog', 'Australian Shepherd', 'Healthy, no known issues', 16),
('Gus', '2019-07-14', 'Dog', 'Pug', 'Overweight, on diet plan', 17),
('Hazel', '2020-12-03', 'Dog', 'Siberian Husky', 'Escape artist, secure fencing required', 18),
('Ivy', '2021-04-20', 'Dog', 'Cocker Spaniel', 'Prone to ear infections', 19),
('Jack', '2018-08-12', 'Dog', 'Jack Russell Terrier', 'High prey drive, keep away from small animals', 20),

-- Cats of various breeds
('Whiskers', '2019-05-16', 'Cat', 'Persian', 'Daily grooming required for long coat', 21),
('Shadow', '2020-10-08', 'Cat', 'Maine Coon', 'Large breed, monitor for heart conditions', 22),
('Luna', '2021-01-25', 'Cat', 'Siamese', 'Vocal, may have anxiety issues', 23),
('Mittens', '2018-12-14', 'Cat', 'Ragdoll', 'Docile temperament, indoor only', 24),
('Smokey', '2020-07-30', 'Cat', 'Russian Blue', 'Shy personality, needs quiet environment', 25),
('Tiger', '2019-09-11', 'Cat', 'Bengal', 'High energy, needs climbing structures', 26),
('Princess', '2021-03-07', 'Cat', 'Scottish Fold', 'Monitor for joint issues', 27),
('Oreo', '2020-06-19', 'Cat', 'Tuxedo', 'Playful, good with children', 28),
('Ginger', '2018-04-02', 'Cat', 'Orange Tabby', 'Friendly, loves attention', 29),
('Patches', '2019-11-28', 'Cat', 'Calico', 'Female, spayed, no health issues', 30),

-- Exotic pets
('Spike', '2020-03-15', 'Reptile', 'Bearded Dragon', 'Requires UVB lighting and calcium supplements', 31),
('Nemo', '2021-07-22', 'Fish', 'Clownfish', 'Marine aquarium, monitor water parameters', 32),
('Polly', '2018-09-05', 'Bird', 'African Grey Parrot', 'Highly intelligent, needs mental stimulation', 33),
('Bugs', '2020-11-12', 'Rabbit', 'Holland Lop', 'Indoor rabbit, litter trained', 34),
('Hammy', '2021-05-18', 'Hamster', 'Syrian Hamster', 'Nocturnal, provide wheel for exercise', 35),
('Shelly', '2019-08-30', 'Reptile', 'Red-Eared Slider', 'Aquatic turtle, needs basking area', 36),
('Tweety', '2020-12-25', 'Bird', 'Canary', 'Singer, keep in quiet area during molting', 37),
('Hopscotch', '2021-02-14', 'Rabbit', 'Mini Rex', 'Small breed, gentle handling required', 38),
('Squeaky', '2020-04-01', 'Guinea Pig', 'American Guinea Pig', 'Social animal, consider companion', 39),
('Slither', '2019-06-08', 'Reptile', 'Ball Python', 'Docile snake, feed pre-killed prey only', 40),

-- More dogs with specific health conditions
('Rex', '2017-01-10', 'Dog', 'Rottweiler', 'Senior dog, arthritis in hips', 41),
('Buddy', '2021-08-15', 'Dog', 'Golden Retriever', 'Puppy, completing vaccination series', 42),
('Maximus', '2016-12-20', 'Dog', 'Great Dane', 'Giant breed, monitor for bloat', 43),
('Tiny', '2020-05-03', 'Dog', 'Chihuahua', 'Luxating patella, may need surgery', 44),
('Storm', '2019-02-17', 'Dog', 'Weimaraner', 'Separation anxiety, crate training', 45),
('Copper', '2018-10-22', 'Dog', 'Bloodhound', 'Excellent scent hound, prone to ear issues', 46),
('Duchess', '2020-09-09', 'Dog', 'Cavalier King Charles Spaniel', 'Heart murmur, regular monitoring', 47),
('Ranger', '2021-06-30', 'Dog', 'Belgian Malinois', 'Working dog, high exercise needs', 48),
('Peanut', '2019-12-12', 'Dog', 'Dachshund', 'Back problems, avoid jumping', 49),
('Zeus', '2018-03-25', 'Dog', 'Mastiff', 'Gentle giant, monitor weight', 50);

-- Recent and upcoming visits with diverse scenarios
INSERT INTO visits (pet_id, vet_id, visit_date, visit_time, description, diagnosis, treatment, cost, status) VALUES
-- Emergency visits
(21, 16, CURDATE(), '08:00:00', 'Emergency - not eating for 2 days', 'Gastrointestinal upset', 'IV fluids, anti-nausea medication', 285.00, 'COMPLETED'),
(22, 6, CURDATE(), '09:30:00', 'Hit by car, limping', 'Fractured left hind leg', 'X-rays, pain medication, surgery scheduled', 450.00, 'IN_PROGRESS'),

-- Routine checkups
(23, 1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '10:00:00', 'Annual wellness exam', 'Healthy adult cat', 'Vaccinations updated, dental cleaning recommended', 125.00, 'COMPLETED'),
(24, 3, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '14:30:00', 'Skin condition follow-up', 'Dermatitis improving', 'Continue medicated shampoo, recheck in 2 weeks', 75.00, 'COMPLETED'),
(25, 7, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '11:15:00', 'Behavioral consultation', 'Anxiety-related aggression', 'Prescribed anti-anxiety medication, behavior modification plan', 150.00, 'COMPLETED'),

-- Surgical procedures
(26, 2, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '13:00:00', 'Spay surgery', 'Routine ovariohysterectomy', 'Surgery completed successfully, pain management', 320.00, 'COMPLETED'),
(27, 4, DATE_SUB(CURDATE(), INTERVAL 7 DAY), '09:00:00', 'ACL repair surgery', 'Torn anterior cruciate ligament', 'TPLO surgery performed, 8-week recovery', 2500.00, 'COMPLETED'),

-- Dental procedures
(28, 19, DATE_SUB(CURDATE(), INTERVAL 10 DAY), '08:30:00', 'Dental cleaning and extractions', 'Severe periodontal disease', 'Cleaned teeth, extracted 3 molars', 485.00, 'COMPLETED'),
(29, 8, DATE_SUB(CURDATE(), INTERVAL 14 DAY), '15:00:00', 'Dental examination', 'Mild tartar buildup', 'Dental cleaning scheduled for next month', 65.00, 'COMPLETED'),

-- Exotic animal visits
(31, 8, DATE_SUB(CURDATE(), INTERVAL 21 DAY), '16:00:00', 'Bearded dragon health check', 'Metabolic bone disease', 'Calcium supplements, UVB light adjustment', 95.00, 'COMPLETED'),
(33, 8, DATE_SUB(CURDATE(), INTERVAL 28 DAY), '10:30:00', 'Parrot wellness exam', 'Healthy adult bird', 'Wing and nail trim, diet recommendations', 85.00, 'COMPLETED'),

-- Upcoming appointments
(30, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00', 'Vaccination booster', NULL, NULL, NULL, 'SCHEDULED'),
(32, 8, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '14:00:00', 'Fish tank consultation', NULL, NULL, NULL, 'SCHEDULED'),
(34, 12, DATE_ADD(CURDATE(), INTERVAL 3 DAY), '11:30:00', 'Rabbit nutrition consultation', NULL, NULL, NULL, 'SCHEDULED'),
(35, 15, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '15:30:00', 'Hamster wellness check', NULL, NULL, NULL, 'SCHEDULED'),
(36, 8, DATE_ADD(CURDATE(), INTERVAL 7 DAY), '10:00:00', 'Turtle shell examination', NULL, NULL, NULL, 'SCHEDULED');

-- Medical records for comprehensive history
INSERT INTO medical_records (pet_id, vet_id, visit_id, record_date, record_type, title, description, findings, recommendations) VALUES
(21, 16, (SELECT MAX(id) FROM visits WHERE pet_id = 21), CURDATE(), 'EMERGENCY', 'Gastrointestinal Emergency',
 'Patient presented with 2-day history of anorexia and lethargy. Owner reports no vomiting or diarrhea.',
 'Dehydration (8%), elevated BUN, normal CBC. Abdominal palpation revealed mild discomfort.',
 'Continue supportive care, bland diet when appetite returns, recheck in 48 hours if not improving.'),

(23, 1, (SELECT MAX(id) FROM visits WHERE pet_id = 23), DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'EXAMINATION', 'Annual Wellness Examination',
 'Comprehensive physical examination of 4-year-old spayed female Siamese cat.',
 'Weight: 8.2 lbs (ideal), Temperature: 101.5°F, Heart rate: 180 bpm. All systems normal.',
 'Continue current diet and exercise routine. Schedule dental cleaning within 6 months.'),

(27, 4, (SELECT MAX(id) FROM visits WHERE pet_id = 27), DATE_SUB(CURDATE(), INTERVAL 7 DAY), 'SURGERY', 'TPLO Surgery Report',
 'Tibial Plateau Leveling Osteotomy performed for complete ACL rupture in left stifle.',
 'Surgery completed without complications. Bone plate and screws placed successfully.',
 'Strict exercise restriction for 8 weeks. Physical therapy to begin at 2 weeks post-op.');

-- Vaccination records
INSERT INTO vaccinations (pet_id, vet_id, vaccine_name, vaccine_type, vaccination_date, expiration_date, batch_number, manufacturer, next_due_date) VALUES
-- Dog vaccinations
(21, 1, 'DHPP', 'CORE', DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 335 DAY), 'DH2023-1001', 'VetVaccines Inc.', DATE_ADD(CURDATE(), INTERVAL 335 DAY)),
(21, 1, 'Rabies', 'CORE', DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 1065 DAY), 'RB2023-2001', 'VetVaccines Inc.', DATE_ADD(CURDATE(), INTERVAL 1065 DAY)),
(22, 1, 'Bordetella', 'NON_CORE', DATE_SUB(CURDATE(), INTERVAL 60 DAY), DATE_ADD(CURDATE(), INTERVAL 305 DAY), 'BD2023-3001', 'PetHealth Labs', DATE_ADD(CURDATE(), INTERVAL 305 DAY)),
(24, 3, 'Lyme Disease', 'NON_CORE', DATE_SUB(CURDATE(), INTERVAL 90 DAY), DATE_ADD(CURDATE(), INTERVAL 275 DAY), 'LY2023-4001', 'AnimalCare Biologics', DATE_ADD(CURDATE(), INTERVAL 275 DAY)),

-- Cat vaccinations
(23, 1, 'FVRCP', 'CORE', DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 364 DAY), 'FC2023-5001', 'FelineHealth Corp.', DATE_ADD(CURDATE(), INTERVAL 364 DAY)),
(23, 1, 'Rabies', 'CORE', DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 1094 DAY), 'RB2023-6001', 'VetVaccines Inc.', DATE_ADD(CURDATE(), INTERVAL 1094 DAY)),
(25, 7, 'FeLV', 'NON_CORE', DATE_SUB(CURDATE(), INTERVAL 45 DAY), DATE_ADD(CURDATE(), INTERVAL 320 DAY), 'FL2023-7001', 'FelineHealth Corp.', DATE_ADD(CURDATE(), INTERVAL 320 DAY));

-- Appointments for the next few weeks
INSERT INTO appointments (pet_id, vet_id, owner_id, appointment_date, appointment_time, duration_minutes, reason, status) VALUES
-- Today's appointments
(41, 1, 41, CURDATE(), '09:00:00', 30, 'Senior wellness exam', 'CONFIRMED'),
(42, 15, 42, CURDATE(), '10:30:00', 45, 'Puppy vaccination series', 'CONFIRMED'),
(43, 2, 43, CURDATE(), '14:00:00', 60, 'Pre-surgical consultation', 'CONFIRMED'),

-- Tomorrow's appointments
(44, 4, 44, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '08:30:00', 30, 'Lameness evaluation', 'CONFIRMED'),
(45, 7, 45, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '11:00:00', 60, 'Behavioral consultation', 'CONFIRMED'),
(46, 3, 46, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '15:30:00', 30, 'Ear infection follow-up', 'CONFIRMED'),

-- Next week appointments
(47, 5, 47, DATE_ADD(CURDATE(), INTERVAL 7 DAY), '09:30:00', 45, 'Cardiology consultation', 'SCHEDULED'),
(48, 6, 48, DATE_ADD(CURDATE(), INTERVAL 8 DAY), '13:00:00', 30, 'Emergency follow-up', 'SCHEDULED'),
(49, 4, 49, DATE_ADD(CURDATE(), INTERVAL 9 DAY), '10:00:00', 30, 'Back pain management', 'SCHEDULED'),
(50, 1, 50, DATE_ADD(CURDATE(), INTERVAL 10 DAY), '14:30:00', 30, 'Weight management consultation', 'SCHEDULED');

-- System settings for the dummy data
INSERT INTO system_settings (setting_key, setting_value, setting_type, description, category, is_public) VALUES
('dummy_data_version', '1.0', 'STRING', 'Version of dummy data installed', 'SYSTEM', FALSE),
('dummy_data_installed', NOW(), 'DATETIME', 'Timestamp when dummy data was installed', 'SYSTEM', FALSE),
('total_dummy_owners', '50', 'INTEGER', 'Number of dummy owners created', 'STATISTICS', FALSE),
('total_dummy_pets', '70', 'INTEGER', 'Number of dummy pets created', 'STATISTICS', FALSE),
('total_dummy_visits', '25', 'INTEGER', 'Number of dummy visits created', 'STATISTICS', FALSE),
('clinic_hours_weekday', '8:00 AM - 6:00 PM', 'STRING', 'Clinic operating hours on weekdays', 'GENERAL', TRUE),
('clinic_hours_weekend', '9:00 AM - 4:00 PM', 'STRING', 'Clinic operating hours on weekends', 'GENERAL', TRUE),
('emergency_contact', '555-EMERGENCY', 'STRING', 'After-hours emergency contact number', 'GENERAL', TRUE);

-- Audit log entries for the dummy data creation
INSERT INTO audit_logs (username, timestamp, action, resource, resource_id, details, ip_address, user_agent) VALUES
('system', NOW(), 'BULK_INSERT', 'owners', NULL, 'Created 40 dummy owner records', '127.0.0.1', 'Migration Script V10'),
('system', NOW(), 'BULK_INSERT', 'pets', NULL, 'Created 50 dummy pet records', '127.0.0.1', 'Migration Script V10'),
('system', NOW(), 'BULK_INSERT', 'visits', NULL, 'Created 15 dummy visit records', '127.0.0.1', 'Migration Script V10'),
('system', NOW(), 'BULK_INSERT', 'appointments', NULL, 'Created 10 dummy appointment records', '127.0.0.1', 'Migration Script V10'),
('system', NOW(), 'BULK_INSERT', 'vaccinations', NULL, 'Created 7 dummy vaccination records', '127.0.0.1', 'Migration Script V10'),
('system', NOW(), 'SYSTEM_UPDATE', 'database', NULL, 'Comprehensive dummy data migration completed', '127.0.0.1', 'Migration Script V10');