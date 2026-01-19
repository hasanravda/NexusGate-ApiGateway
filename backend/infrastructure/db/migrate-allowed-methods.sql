-- ============================================
-- Migration Script: Update allowed_methods to array type
-- ============================================

-- Step 1: Delete the public-data-service route
DELETE FROM service_routes WHERE service_name = 'public-data-service';

-- Step 2: Add a temporary column for the new array type
ALTER TABLE service_routes ADD COLUMN IF NOT EXISTS allowed_methods_new TEXT[];

-- Step 3: Migrate data from VARCHAR (comma-separated) to TEXT[] (array)
UPDATE service_routes
SET allowed_methods_new = string_to_array(allowed_methods, ',')
WHERE allowed_methods_new IS NULL;

-- Step 4: Drop the old column
ALTER TABLE service_routes DROP COLUMN allowed_methods;

-- Step 5: Rename the new column to the original name
ALTER TABLE service_routes RENAME COLUMN allowed_methods_new TO allowed_methods;

-- Step 6: Set NOT NULL constraint and default value
ALTER TABLE service_routes 
    ALTER COLUMN allowed_methods SET NOT NULL,
    ALTER COLUMN allowed_methods SET DEFAULT ARRAY['GET', 'POST', 'PUT', 'DELETE'];

-- Verification query (optional - uncomment to check results)
-- SELECT id, service_name, public_path, allowed_methods FROM service_routes;
