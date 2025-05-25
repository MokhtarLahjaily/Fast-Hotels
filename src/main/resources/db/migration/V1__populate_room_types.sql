-- Migration to populate the type field based on room names
-- This will extract the room type from the name field and populate the type field

-- Update Standard Rooms
UPDATE rooms
SET type = 'Standard'
WHERE name LIKE '%Standard%' AND (type IS NULL OR type = '');

-- Update Deluxe Rooms
UPDATE rooms
SET type = 'Deluxe'
WHERE name LIKE '%Deluxe%' AND (type IS NULL OR type = '');

-- Update Executive Suites
UPDATE rooms
SET type = 'Executive'
WHERE name LIKE '%Executive%' AND (type IS NULL OR type = '');

-- Update Family Rooms
UPDATE rooms
SET type = 'Family'
WHERE name LIKE '%Family%' AND (type IS NULL OR type = '');

-- Update Suite rooms (general suites not covered by Executive)
UPDATE rooms
SET type = 'Suite'
WHERE name LIKE '%Suite%' AND name NOT LIKE '%Executive%' AND (type IS NULL OR type = '');

-- Update Presidential Suites
UPDATE rooms
SET type = 'Presidential'
WHERE name LIKE '%Presidential%' AND (type IS NULL OR type = '');

-- Update Penthouse rooms
UPDATE rooms
SET type = 'Penthouse'
WHERE name LIKE '%Penthouse%' AND (type IS NULL OR type = '');

-- Set default type for any remaining NULL values
UPDATE rooms
SET type = 'Standard'
WHERE type IS NULL OR type = '';

-- Add a comment to track this migration
COMMENT ON COLUMN rooms.type IS 'Room type populated from room names via migration V1';
