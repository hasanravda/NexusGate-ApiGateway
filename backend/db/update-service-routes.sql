-- Update service routes to point to backend-service (port 8091)
-- All three mock services (user, order, payment) run on the same backend-service

-- Update user-service route
UPDATE service_routes
SET target_url = 'http://localhost:8091/users',
    updated_at = CURRENT_TIMESTAMP
WHERE service_name = 'user-service';

-- Update order-service route
UPDATE service_routes
SET target_url = 'http://localhost:8091/orders',
    updated_at = CURRENT_TIMESTAMP
WHERE service_name = 'order-service';

-- Update payment-service route
UPDATE service_routes
SET target_url = 'http://localhost:8091/payments',
    updated_at = CURRENT_TIMESTAMP
WHERE service_name = 'payment-service';

-- Verify the updates
SELECT id, service_name, public_path, target_url, is_active
FROM service_routes
ORDER BY id;
