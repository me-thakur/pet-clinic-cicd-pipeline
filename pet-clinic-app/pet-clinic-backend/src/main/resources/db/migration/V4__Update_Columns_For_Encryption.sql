-- Migration to update column lengths for encrypted data
-- Validates: Requirements 9.5

-- Update owners table for encrypted fields (H2 compatible syntax)
ALTER TABLE owners ALTER COLUMN address VARCHAR(500);
ALTER TABLE owners ALTER COLUMN telephone VARCHAR(200);

-- Update visits table for encrypted fields (H2 compatible syntax)
ALTER TABLE visits ALTER COLUMN diagnosis VARCHAR(1000);
ALTER TABLE visits ALTER COLUMN treatment VARCHAR(1000);
ALTER TABLE visits ALTER COLUMN notes VARCHAR(2000);

-- Add indexes for performance (on non-encrypted fields only)
CREATE INDEX IF NOT EXISTS idx_owners_email ON owners(email);
CREATE INDEX IF NOT EXISTS idx_visits_visit_date ON visits(visit_date);
CREATE INDEX IF NOT EXISTS idx_visits_pet_id ON visits(pet_id);
CREATE INDEX IF NOT EXISTS idx_visits_veterinarian_id ON visits(veterinarian_id);