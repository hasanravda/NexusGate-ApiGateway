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
-- Table: rate_limits
-- ============================================
CREATE TABLE IF NOT EXISTS rate_limits (
                                           id BIGSERIAL PRIMARY KEY,
                                           api_key_id BIGINT NOT NULL UNIQUE,

                                           requests_per_minute INTEGER NOT NULL,
                                           requests_per_hour INTEGER,
                                           requests_per_day INTEGER,

                                           is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                           CONSTRAINT fk_api_key
                                           FOREIGN KEY (api_key_id)
    REFERENCES api_keys(id)
    ON DELETE CASCADE,

    CONSTRAINT check_rate_limits_positive CHECK (
                                                    requests_per_minute > 0 AND
(requests_per_hour IS NULL OR requests_per_hour >= requests_per_minute) AND
(requests_per_day IS NULL OR requests_per_day >= requests_per_day)
    )
    );

CREATE INDEX IF NOT EXISTS idx_rate_limits_api_key ON rate_limits(api_key_id);

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

INSERT INTO users (email, password, full_name, role, is_active)
VALUES
    ('admin@demo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Admin User', 'ADMIN', true),
    ('manager@demo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Manager User', 'MANAGER', true),
    ('viewer@demo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Viewer User', 'VIEWER', true)
    ON CONFLICT (email) DO NOTHING;

INSERT INTO api_keys (
    key_value, key_name, client_name, client_email,
    client_company, created_by_user_id, is_active
)
VALUES
    ('nx_lendingkart_prod_abc123', 'LendingKart Production Key', 'LendingKart', 'dev@lendingkart.com', 'LendingKart Technologies', 1, true),
    ('nx_paytm_prod_xyz789', 'PaytmLend Production Key', 'PaytmLend', 'api@paytm.com', 'Paytm Financial Services', 1, true),
    ('nx_mobikwik_test_def456', 'MobiKwik Test Key', 'MobiKwik', 'tech@mobikwik.com', 'MobiKwik Systems', 2, true),
    ('nx_test_key_12345', 'Test Key for Development', 'Test Client', 'test@example.com', 'Test Company', 1, true)
    ON CONFLICT (key_value) DO NOTHING;

-- Insert rate limits using subquery to ensure correct api_key_id mapping
INSERT INTO rate_limits (api_key_id, requests_per_minute, requests_per_hour, requests_per_day, is_active)
SELECT
    ak.id,
    CASE
        WHEN ak.key_value = 'nx_lendingkart_prod_abc123' THEN 1000
        WHEN ak.key_value = 'nx_paytm_prod_xyz789' THEN 500
        WHEN ak.key_value = 'nx_mobikwik_test_def456' THEN 100
        WHEN ak.key_value = 'nx_test_key_12345' THEN 50
        END,
    CASE
        WHEN ak.key_value = 'nx_lendingkart_prod_abc123' THEN 60000
        WHEN ak.key_value = 'nx_paytm_prod_xyz789' THEN 30000
        WHEN ak.key_value = 'nx_mobikwik_test_def456' THEN 6000
        WHEN ak.key_value = 'nx_test_key_12345' THEN 3000
        END,
    CASE
        WHEN ak.key_value = 'nx_lendingkart_prod_abc123' THEN 1000000
        WHEN ak.key_value = 'nx_paytm_prod_xyz789' THEN 500000
        WHEN ak.key_value = 'nx_mobikwik_test_def456' THEN 100000
        WHEN ak.key_value = 'nx_test_key_12345' THEN 50000
        END,
    true
FROM api_keys ak
WHERE ak.key_value IN (
                       'nx_lendingkart_prod_abc123',
                       'nx_paytm_prod_xyz789',
                       'nx_mobikwik_test_def456',
                       'nx_test_key_12345'
    )
    ON CONFLICT (api_key_id) DO NOTHING;