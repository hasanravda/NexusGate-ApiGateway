# Route-Level API Key Requirement Feature

## Overview

This feature adds the ability to configure individual routes to either require or skip API key validation at the route level. This provides flexibility for having both public and protected routes within the same gateway.

## Changes Made

### 1. Database Changes

#### Migration Script: `add-requires-api-key.sql`
- Added `requires_api_key` column to `service_routes` table
- Type: `BOOLEAN NOT NULL DEFAULT TRUE`
- Default value: `TRUE` (backward compatible - all existing routes remain protected)
- Indexed for performance: `idx_service_routes_requires_api_key`

#### Updated: `init-db.sql`
- Added `requires_api_key` column to the `service_routes` table definition

### 2. Entity & DTO Updates

#### ServiceRoute Entity (config-service)
- Added `requiresApiKey` field (Boolean, nullable=false, default=true)
- Column name: `requires_api_key`

#### ServiceRouteDto (config-service)
- Added `requiresApiKey` field to response DTO

#### CreateServiceRouteRequest (config-service)
- Added `requiresApiKey` field to request DTO
- Optional field with default value `true`

#### UpdateSecurityRequest (config-service) - NEW
- New DTO for PATCH /service-routes/{id}/security endpoint
- Contains single field: `requiresApiKey` (Boolean, required)

#### ServiceRouteResponse (gateway-service)
- Added `requiresApiKey` field to match config-service response

### 3. Config-Service API Updates

#### Modified Endpoints:

**POST /service-routes**
- Request body now accepts optional `requiresApiKey` field
- Default value: `true` (if not provided)

**PUT /service-routes/{id}**
- Request body now accepts optional `requiresApiKey` field
- If not provided, existing value is preserved

**GET /service-routes**
**GET /service-routes/{id}**
**GET /service-routes/by-path**
- Response now includes `requiresApiKey` field

#### New Endpoint:

**PATCH /service-routes/{id}/security**

Update API key requirement for a specific route (designed for frontend checkbox toggle).

**Request:**
```json
{
  "requiresApiKey": true
}
```

**Response:**
```json
{
  "id": 1,
  "serviceName": "user-service",
  "publicPath": "/api/users/**",
  "targetUrl": "http://localhost:8083",
  "requiresApiKey": true,
  ...
}
```

**Error Cases:**
- 404: Route not found
- 400: Missing or invalid `requiresApiKey` value

### 4. Gateway-Service Changes

#### GlobalRequestFilter Updates

The filter now checks the `requiresApiKey` flag before enforcing API key validation:

**Flow:**
1. Match route by path pattern
2. Read `requiresApiKey` from matched route
3. **If `requiresApiKey == false`:**
   - Log: "Route does not require API key - Skipping API key validation"
   - Store route info in exchange attributes
   - Skip API key validation
   - Forward request immediately to next filter
4. **If `requiresApiKey == true` (or null for safety):**
   - Log: "Route requires API key - Validating API key"
   - Extract X-API-KEY header
   - Validate API key with config service
   - Check active status and expiration
   - Store apiKeyId in exchange attributes
   - Forward request to next filter

**Logging Enhancements:**
- Route match log now includes `RequiresApiKey` value
- Separate debug logs for public vs protected route flows
- Clear indication when API key validation is skipped

## Behavior

### Public Routes (requiresApiKey = false)
- ✅ No X-API-KEY header required
- ✅ Request forwarded directly to backend
- ✅ No rate limiting based on API key
- ✅ Still respects route-level config (allowed methods, timeouts, etc.)

### Protected Routes (requiresApiKey = true)
- ✅ X-API-KEY header required
- ✅ API key validation enforced
- ✅ Returns 401 if API key missing, invalid, inactive, or expired
- ✅ Rate limiting applies based on API key
- ✅ Full existing authentication flow

### Default Behavior
- New routes default to `requiresApiKey = true` (secure by default)
- If field is null in database, gateway treats it as `true` (fail-safe)

## Testing

### Test Scenarios

#### 1. Create Public Route
```bash
curl -X POST http://localhost:8082/service-routes \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "public-api",
    "publicPath": "/api/public/**",
    "targetUrl": "http://localhost:8083",
    "requiresApiKey": false,
    "createdByUserId": 1
  }'
```

#### 2. Access Public Route (No API Key)
```bash
curl -X GET http://localhost:8081/api/public/data
# Expected: 200 OK (no API key needed)
```

#### 3. Create Protected Route
```bash
curl -X POST http://localhost:8082/service-routes \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "protected-api",
    "publicPath": "/api/protected/**",
    "targetUrl": "http://localhost:8083",
    "requiresApiKey": true,
    "createdByUserId": 1
  }'
```

#### 4. Access Protected Route Without API Key
```bash
curl -X GET http://localhost:8081/api/protected/data
# Expected: 401 Unauthorized - "API key is missing"
```

#### 5. Access Protected Route With Valid API Key
```bash
curl -X GET http://localhost:8081/api/protected/data \
  -H "X-API-KEY: your-valid-api-key"
# Expected: 200 OK (forwarded to backend)
```

#### 6. Toggle Route Security (Frontend Use Case)
```bash
# Make route public
curl -X PATCH http://localhost:8082/service-routes/1/security \
  -H "Content-Type: application/json" \
  -d '{
    "requiresApiKey": false
  }'

# Make route protected again
curl -X PATCH http://localhost:8082/service-routes/1/security \
  -H "Content-Type: application/json" \
  -d '{
    "requiresApiKey": true
  }'
```

## Migration Steps

### For Existing Deployments:

1. **Run Database Migration:**
   ```bash
   psql -U nexusgate -d nexusgate_db -f infrastructure/db/add-requires-api-key.sql
   ```

2. **Deploy Config-Service:**
   - Updated entity, DTOs, service, and controller
   - New PATCH endpoint available

3. **Deploy Gateway-Service:**
   - Updated filter logic to respect `requiresApiKey` flag
   - Enhanced logging

4. **Verify Existing Routes:**
   - All existing routes will have `requiresApiKey = true`
   - Backward compatible - no behavior change for existing routes

### For New Deployments:

1. Use updated `init-db.sql` (already includes `requires_api_key` column)
2. Deploy both services
3. Configure routes as needed

## Security Considerations

1. **Secure by Default:** 
   - New routes default to `requiresApiKey = true`
   - Null safety in gateway: treats null as `true`

2. **No Hardcoded Paths:**
   - No special cases or hardcoded public paths in gateway
   - All configuration is database-driven

3. **Audit Trail:**
   - Changes tracked via `updated_at` timestamp
   - Can be monitored through logs

4. **Public Route Risks:**
   - Public routes (`requiresApiKey = false`) have no rate limiting by API key
   - Consider enabling route-level rate limiting independently
   - Monitor public endpoints for abuse

## Architecture Benefits

1. **Flexibility:** Mix public and protected routes in the same gateway
2. **No Code Changes:** Toggle via API without redeployment
3. **Frontend Friendly:** Simple checkbox toggle using PATCH endpoint
4. **Backward Compatible:** Existing routes maintain current behavior
5. **Database-Driven:** Single source of truth for route configuration
6. **Observable:** Clear logging for debugging and monitoring

## Future Enhancements

Potential improvements for consideration:

1. **Anonymous Rate Limiting:** Rate limit public routes by IP address
2. **Bulk Security Update:** PATCH endpoint to update multiple routes
3. **Security Audit Log:** Track who changed `requiresApiKey` and when
4. **Route Templates:** Preset configurations for common use cases
5. **Validation Rules:** Warn when changing production routes to public
