-- Add notes column to visits table if it doesn't exist
-- This migration ensures the visits table has the notes column

-- Add notes column if it doesn't exist (H2 compatible)
ALTER TABLE visits ADD COLUMN IF NOT EXISTS notes VARCHAR(1000);

-- Remove description column if it exists (H2 compatible)
-- Note: H2 doesn't support DROP COLUMN IF EXISTS, so we skip this for H2