-- Add contact information columns to veterinarians table
-- Migration: V7__Add_Contact_Info_To_Veterinarians.sql

ALTER TABLE veterinarians ADD COLUMN telephone VARCHAR(20);
ALTER TABLE veterinarians ADD COLUMN email VARCHAR(100);
ALTER TABLE veterinarians ADD COLUMN address VARCHAR(200);

-- Add indexes for better query performance
CREATE INDEX idx_veterinarians_email ON veterinarians(email);
CREATE INDEX idx_veterinarians_telephone ON veterinarians(telephone);

-- Update existing records with sample contact data
UPDATE veterinarians SET 
    telephone = CASE 
        WHEN id = 1 THEN '555-0101'
        WHEN id = 2 THEN '555-0102'
        WHEN id = 3 THEN '555-0103'
        WHEN id = 4 THEN '555-0104'
        WHEN id = 5 THEN '555-0105'
        WHEN id = 6 THEN '555-0106'
        WHEN id = 7 THEN '555-0107'
        WHEN id = 8 THEN '555-0108'
        WHEN id = 9 THEN '555-0109'
        WHEN id = 10 THEN '555-0110'
        ELSE '555-0100'
    END,
    email = LOWER(first_name) || '.' || LOWER(last_name) || '@petclinic.com',
    address = CASE 
        WHEN id % 4 = 1 THEN '123 Veterinary Ave'
        WHEN id % 4 = 2 THEN '456 Animal Care Blvd'
        WHEN id % 4 = 3 THEN '789 Pet Health St'
        ELSE '321 Medical Center Dr'
    END
WHERE telephone IS NULL OR email IS NULL OR address IS NULL;