-- Add state and zip_code columns to owners table
-- Migration: V6__Add_State_And_ZipCode_To_Owners.sql

ALTER TABLE owners ADD COLUMN state VARCHAR(50);
ALTER TABLE owners ADD COLUMN zip_code VARCHAR(10);

-- Add indexes for better query performance
CREATE INDEX idx_owners_state ON owners(state);
CREATE INDEX idx_owners_zip_code ON owners(zip_code);

-- Update existing records with sample data (optional)
UPDATE owners SET 
    state = CASE 
        WHEN city = 'New York' THEN 'NY'
        WHEN city = 'Los Angeles' THEN 'CA'
        WHEN city = 'Chicago' THEN 'IL'
        WHEN city = 'Houston' THEN 'TX'
        WHEN city = 'Phoenix' THEN 'AZ'
        WHEN city = 'Philadelphia' THEN 'PA'
        WHEN city = 'San Antonio' THEN 'TX'
        WHEN city = 'San Diego' THEN 'CA'
        WHEN city = 'Dallas' THEN 'TX'
        WHEN city = 'San Jose' THEN 'CA'
        ELSE 'CA'
    END,
    zip_code = CASE 
        WHEN city = 'New York' THEN '10001'
        WHEN city = 'Los Angeles' THEN '90210'
        WHEN city = 'Chicago' THEN '60601'
        WHEN city = 'Houston' THEN '77001'
        WHEN city = 'Phoenix' THEN '85001'
        WHEN city = 'Philadelphia' THEN '19101'
        WHEN city = 'San Antonio' THEN '78201'
        WHEN city = 'San Diego' THEN '92101'
        WHEN city = 'Dallas' THEN '75201'
        WHEN city = 'San Jose' THEN '95101'
        ELSE '90210'
    END
WHERE state IS NULL OR zip_code IS NULL;