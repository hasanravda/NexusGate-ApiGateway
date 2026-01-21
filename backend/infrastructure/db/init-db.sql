-- ============================================
-- NexusGate Database Schema
-- ============================================

-- ============================================
-- Table: users
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(is_active);

-- ============================================
-- Table: service_routes (NEW!)
-- ============================================
CREATE TABLE IF NOT EXISTS service_routes (
                                              id BIGSERIAL PRIMARY KEY,

    -- Service info
                                              service_name VARCHAR(100) NOT NULL,
    service_description VARCHAR(500),

    -- Routing config
    public_path VARCHAR(200) NOT NULL UNIQUE,
    target_url VARCHAR(500) NOT NULL,
    allowed_methods TEXT[] NOT NULL DEFAULT ARRAY['GET', 'POST', 'PUT', 'DELETE'],

    -- Auth config
    auth_required BOOLEAN NOT NULL DEFAULT TRUE,
    auth_type VARCHAR(20) NOT NULL DEFAULT 'API_KEY',
    requires_api_key BOOLEAN NOT NULL DEFAULT TRUE,

    -- Rate limit config
    rate_limit_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    rate_limit_per_minute INTEGER DEFAULT 100,
    rate_limit_per_hour INTEGER DEFAULT 5000,

    -- Other config
    timeout_ms INTEGER DEFAULT 30000,
    custom_headers TEXT,

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id BIGINT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(500),

    CONSTRAINT fk_service_route_created_by_user
    FOREIGN KEY (created_by_user_id)
    REFERENCES users(id)
    ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_service_routes_public_path ON service_routes(public_path);
CREATE INDEX IF NOT EXISTS idx_service_routes_active ON service_routes(is_active);
CREATE INDEX IF NOT EXISTS idx_service_routes_created_by ON service_routes(created_by_user_id);

-- ============================================
-- Table: api_keys
-- ============================================
CREATE TABLE IF NOT EXISTS api_keys (
                                        id BIGSERIAL PRIMARY KEY,
                                        key_value VARCHAR(64) NOT NULL UNIQUE,
    key_name VARCHAR(255) NOT NULL,

    client_name VARCHAR(255),
    client_email VARCHAR(255),
    client_company VARCHAR(255),

    created_by_user_id BIGINT NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,

    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_created_by_user
    FOREIGN KEY (created_by_user_id)
    REFERENCES users(id)
    ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_api_keys_key_value ON api_keys(key_value);
CREATE INDEX IF NOT EXISTS idx_api_keys_created_by_user ON api_keys(created_by_user_id);
CREATE INDEX IF NOT EXISTS idx_api_keys_active ON api_keys(is_active);
CREATE INDEX IF NOT EXISTS idx_api_keys_client_name ON api_keys(client_name);
CREATE INDEX IF NOT EXISTS idx_api_keys_expires_at ON api_keys(expires_at) WHERE expires_at IS NOT NULL;

-- ============================================
-- Table: rate_limits (UPDATED!)
-- ============================================
CREATE TABLE IF NOT EXISTS rate_limits (
       id BIGSERIAL PRIMARY KEY,

-- CHANGED: Made nullable to support default limits
       api_key_id BIGINT,

-- NEW: Link to service route
       service_route_id BIGINT,

       requests_per_minute INTEGER NOT NULL,
       requests_per_hour INTEGER,
       requests_per_day INTEGER,

       is_active BOOLEAN NOT NULL DEFAULT TRUE,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       notes VARCHAR(500),

        CONSTRAINT fk_rate_limit_api_key
        FOREIGN KEY (api_key_id)
        REFERENCES api_keys(id)
        ON DELETE CASCADE,

        CONSTRAINT fk_rate_limit_service_route
        FOREIGN KEY (service_route_id)
        REFERENCES service_routes(id)
        ON DELETE CASCADE,

        CONSTRAINT check_rate_limits_positive CHECK (
            requests_per_minute > 0 AND
            (requests_per_hour IS NULL OR requests_per_hour >= requests_per_minute) AND
            (requests_per_day IS NULL OR requests_per_day >= requests_per_hour)
        ),

    -- NEW: Ensure unique combination of api_key + service_route
    CONSTRAINT uk_rate_limit_api_key_service UNIQUE (api_key_id, service_route_id)
);

CREATE INDEX IF NOT EXISTS idx_rate_limits_api_key ON rate_limits(api_key_id);
CREATE INDEX IF NOT EXISTS idx_rate_limits_service_route ON rate_limits(service_route_id);
CREATE INDEX IF NOT EXISTS idx_rate_limits_api_key_service ON rate_limits(api_key_id, service_route_id);

-- ============================================
-- Trigger function for updated_at
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'update_users_updated_at'
    ) THEN
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'update_api_keys_updated_at'
    ) THEN
CREATE TRIGGER update_api_keys_updated_at
    BEFORE UPDATE ON api_keys
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'update_rate_limits_updated_at'
    ) THEN
CREATE TRIGGER update_rate_limits_updated_at
    BEFORE UPDATE ON rate_limits
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
END IF;
END;
$$;

-- ============================================
-- Seed Data (idempotent)
-- ============================================

-- Insert users
INSERT INTO users (email, password, full_name, role, is_active)
VALUES
    ('admin@demo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Admin User', 'ADMIN', true),
    ('manager@demo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Manager User', 'MANAGER', true),
    ('viewer@demo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Viewer User', 'VIEWER', true)
    ON CONFLICT (email) DO NOTHING;

-- Insert service routes (NEW!)
INSERT INTO service_routes (
    service_name, service_description, public_path, target_url,
    allowed_methods, auth_required, auth_type, rate_limit_enabled,
    rate_limit_per_minute, rate_limit_per_hour,
    created_by_user_id, is_active
)
VALUES
    ('user-service', 'Handles all user management operations', '/api/users/**', 'http://localhost:8082/api/users',
     ARRAY['GET', 'POST', 'PUT', 'DELETE'], true, 'API_KEY', true, 200, 10000, 1, true),

    ('order-service', 'Processes order management', '/api/orders/**', 'http://localhost:8083/orders',
     ARRAY['GET', 'POST', 'PUT', 'DELETE'], true, 'API_KEY', true, 150, 8000, 1, true),

    ('payment-service', 'Handles payment processing', '/api/payments/**', 'http://localhost:8084/payments',
     ARRAY['POST'], true, 'API_KEY', true, 50, 2000, 1, true)
    ON CONFLICT (public_path) DO NOTHING;

-- Insert API keys
INSERT INTO api_keys (
    key_value, key_name, client_name, client_email,
    client_company, created_by_user_id, is_active
)
VALUES
    ('nx_lendingkart_prod_abc123', 'LendingKart Production Key', 'LendingKart', 'dev@lendingkart.com', 'LendingKart Technologies', 1, true),
    ('nx_paytm_prod_xyz789', 'PaytmLend Production Key', 'PaytmLend', 'api@paytm.com', 'Paytm Financial Services', 1, true),
    ('nx_mobikwik_test_def456', 'MobiKwik Test Key', 'MobiKwik', 'tech@mobikwik.com', 'MobiKwik Systems', 1, true),
    ('nx_test_key_12345', 'Test Key for Development', 'Test Client', 'test@example.com', 'Test Company', 1, true)
    ON CONFLICT (key_value) DO NOTHING;

-- Insert rate limits (UPDATED to use new schema)
-- Example 1: Default rate limits for service routes (api_key_id = NULL)
INSERT INTO rate_limits (api_key_id, service_route_id, requests_per_minute, requests_per_hour, requests_per_day, is_active)
SELECT
    NULL,
    sr.id,
    sr.rate_limit_per_minute,
    sr.rate_limit_per_hour,
    sr.rate_limit_per_hour * 24,
    true
FROM service_routes sr
WHERE sr.rate_limit_enabled = true
    ON CONFLICT (api_key_id, service_route_id) DO NOTHING;

-- Example 2: Custom rate limits for specific API keys on specific routes
INSERT INTO rate_limits (api_key_id, service_route_id, requests_per_minute, requests_per_hour, requests_per_day, is_active)
SELECT
    ak.id,
    sr.id,
    CASE
        WHEN ak.key_value = 'nx_lendingkart_prod_abc123' AND sr.service_name = 'user-service' THEN 1000
        WHEN ak.key_value = 'nx_lendingkart_prod_abc123' AND sr.service_name = 'payment-service' THEN 100
        WHEN ak.key_value = 'nx_paytm_prod_xyz789' AND sr.service_name = 'user-service' THEN 500
        END,
    CASE
        WHEN ak.key_value = 'nx_lendingkart_prod_abc123' AND sr.service_name = 'user-service' THEN 60000
        WHEN ak.key_value = 'nx_lendingkart_prod_abc123' AND sr.service_name = 'payment-service' THEN 6000
        WHEN ak.key_value = 'nx_paytm_prod_xyz789' AND sr.service_name = 'user-service' THEN 30000
        END,
    CASE
        WHEN ak.key_value = 'nx_lendingkart_prod_abc123' AND sr.service_name = 'user-service' THEN 1000000
        WHEN ak.key_value = 'nx_lendingkart_prod_abc123' AND sr.service_name = 'payment-service' THEN 100000
        WHEN ak.key_value = 'nx_paytm_prod_xyz789' AND sr.service_name = 'user-service' THEN 500000
        END,
    true
FROM api_keys ak
         CROSS JOIN service_routes sr
WHERE
    (ak.key_value = 'nx_lendingkart_prod_abc123' AND sr.service_name IN ('user-service', 'payment-service'))
   OR (ak.key_value = 'nx_paytm_prod_xyz789' AND sr.service_name = 'user-service')
    ON CONFLICT (api_key_id, service_route_id) DO NOTHING;