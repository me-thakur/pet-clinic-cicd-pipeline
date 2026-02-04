-- Add mobile_number column to owners table
-- This migration adds the mobile_number field for owner data validation feature

ALTER TABLE owners ADD COLUMN mobile_number VARCHAR(200);

-- Create index for mobile number lookups (for uniqueness validation)
CREATE INDEX IF NOT EXISTS idx_owner_mobile ON owners (mobile_number);

-- Add comment for documentation
COMMENT ON COLUMN owners.mobile_number IS 'Mobile phone number in international format (E.164), encrypted for privacy';