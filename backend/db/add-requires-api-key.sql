-- ============================================
-- Migration: Add requires_api_key column
-- Purpose: Enable route-level API key requirement control
-- Date: 2026-01-21
-- ============================================

-- Add requires_api_key column with default value TRUE for backward compatibility
ALTER TABLE service_routes
ADD COLUMN IF NOT EXISTS requires_api_key BOOLEAN NOT NULL DEFAULT TRUE;

-- Add comment for documentation
COMMENT ON COLUMN service_routes.requires_api_key IS 
'Controls whether API key validation is required for this route. TRUE = API key required (default), FALSE = public route (no API key needed)';

-- Create index for faster filtering by requires_api_key
CREATE INDEX IF NOT EXISTS idx_service_routes_requires_api_key ON service_routes(requires_api_key);

-- Display migration status
DO $$
BEGIN
    RAISE NOTICE 'Migration completed: requires_api_key column added to service_routes table';
    RAISE NOTICE 'All existing routes have been set to requires_api_key = TRUE (backward compatible)';
END $$;
