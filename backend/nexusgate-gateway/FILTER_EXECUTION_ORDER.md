# Gateway Filter Execution Order

This document defines the strict execution order of filters in NexusGate Gateway to ensure predictable and consistent request processing.

## Filter Chain Order

The filters are executed in the following order (from first to last):

### 1. GlobalRequestFilter (Order: -100)
**Purpose:** Route resolution and initial API key validation

**Responsibilities:**
- Match incoming request path to a ServiceRoute using wildcard pattern matching
- Validate if route requires API key (`requiresApiKey`)
- If API key is required, validate API key with config-service
- Store route and API key information in exchange attributes
- Return 404 if no matching route found
- Return 401 if API key validation fails

**Attributes Set:**
- `serviceRoute`: ServiceRouteResponse object
- `publicPath`: Matched public path pattern
- `apiKeyId`: Validated API key ID (if applicable)
- `apiKeyValue`: API key value (if applicable)
- `startTime`: Request start timestamp

---

### 2. MethodValidationFilter (Order: -95)
**Purpose:** HTTP method validation

**Responsibilities:**
- Validate that the incoming HTTP method is allowed for the matched route
- Check against `ServiceRoute.allowedMethods` list
- Return 405 (Method Not Allowed) if method is not in the allowed list
- Skip validation if `allowedMethods` is null or empty

**Example:**
```
Route allows: ["GET", "POST"]
Request: PUT /api/users
Response: 405 Method Not Allowed
```

---

### 3. AuthenticationFilter (Order: -90)
**Purpose:** Route-level authentication enforcement

**Responsibilities:**
- Check if route requires authentication (`authRequired`)
- Enforce authentication based on `authType`:
  - `API_KEY`: Validate X-API-Key header (may be redundant if already validated in GlobalRequestFilter)
  - `JWT`: Validate Authorization Bearer token
  - `BOTH`: Require both API key and JWT
- Return 401 if authentication fails
- Skip authentication if `authRequired` is false

**Note:** This filter provides additional authentication layer beyond the initial API key validation in GlobalRequestFilter, supporting multiple auth types.

---

### 4. RateLimitFilter (Order: -80)
**Purpose:** Distributed rate limiting using Redis

**Responsibilities:**
- Check if rate limiting is enabled for the route (`rateLimitEnabled`)
- Call `/rate-limits/check` endpoint to fetch rate limit configuration
- Use Redis to track and enforce rate limits using deterministic keys
- Key format: `rate:{apiKeyId}:{serviceRouteId}:{minute|hour}`
- Enforce per-minute and per-hour limits with TTL-based counters
- Return 429 (Too Many Requests) if limit exceeded
- Skip if rate limiting not enabled or no API key present

**Redis Keys:**
- Minute window: `rate:123:456:minute` (TTL: 60 seconds)
- Hour window: `rate:123:456:hour` (TTL: 3600 seconds)

---

### 5. ServiceRoutingFilter (Order: 0)
**Purpose:** Request forwarding to backend services

**Responsibilities:**
- Forward request to the target URL specified in ServiceRoute
- Inject internal tracing headers:
  - `X-NexusGate-ApiKey-Id`: API key identifier for tracing
  - `X-NexusGate-ServiceRoute-Id`: Route identifier for analytics
- Remove sensitive headers (X-API-Key, Authorization, etc.)
- Add custom headers from route configuration
- Handle timeouts based on route timeout settings
- Forward response back to client
- Log request/response details for observability

**Injected Headers:**
```
X-NexusGate-ApiKey-Id: 123
X-NexusGate-ServiceRoute-Id: 456
```

---

## Filter Execution Flow

```
Client Request
     ↓
GlobalRequestFilter (-100)
  ├─ Match route
  ├─ Validate API key (if required)
  └─ Store route & API key ID
     ↓
MethodValidationFilter (-95)
  ├─ Check HTTP method
  └─ Block if not allowed (405)
     ↓
AuthenticationFilter (-90)
  ├─ Check authRequired
  ├─ Validate API_KEY / JWT / BOTH
  └─ Block if auth fails (401)
     ↓
RateLimitFilter (-80)
  ├─ Check rate limit config
  ├─ Check Redis counters
  └─ Block if limit exceeded (429)
     ↓
ServiceRoutingFilter (0)
  ├─ Inject internal headers
  ├─ Forward to backend
  └─ Return response
     ↓
Client Response
```

## Error Responses

Each filter returns standardized error responses:

| Filter | Status Code | Error Message |
|--------|-------------|---------------|
| GlobalRequestFilter | 404 | "Service route not found" |
| GlobalRequestFilter | 401 | "API key is missing / invalid / inactive / expired" |
| GlobalRequestFilter | 503 | "Authentication service temporarily unavailable" |
| MethodValidationFilter | 400 | "Invalid HTTP method" |
| MethodValidationFilter | 405 | "Method {METHOD} is not allowed. Allowed methods: {LIST}" |
| AuthenticationFilter | 401 | "API key is required / invalid / expired" |
| AuthenticationFilter | 401 | "JWT token is required / invalid / expired" |
| RateLimitFilter | 429 | "Rate limit exceeded" |
| ServiceRoutingFilter | 500 | "Service temporarily unavailable" |
| ServiceRoutingFilter | 504 | "Gateway timeout" |

## Configuration Example

Example ServiceRoute configuration:

```json
{
  "id": 101,
  "publicPath": "/api/users/**",
  "targetUrl": "http://user-service:8080/users",
  "allowedMethods": ["GET", "POST", "PUT"],
  "authRequired": true,
  "authType": "API_KEY",
  "requiresApiKey": true,
  "rateLimitEnabled": true,
  "timeoutMs": 5000,
  "isActive": true
}
```

## Filter Dependencies

Each filter depends on attributes set by previous filters:

| Filter | Required Attributes |
|--------|-------------------|
| GlobalRequestFilter | None (first filter) |
| MethodValidationFilter | `serviceRoute` |
| AuthenticationFilter | `serviceRoute` |
| RateLimitFilter | `serviceRoute`, `apiKeyId` |
| ServiceRoutingFilter | `serviceRoute`, `apiKeyId` (optional) |

## Testing Filter Order

To verify filter execution order, check the logs:

```bash
# Logs should show filters executing in order:
GlobalRequestFilter: Incoming request path...
MethodValidationFilter: Method GET is allowed...
AuthenticationFilter: API key validated successfully...
RateLimitFilter: Rate limit check passed...
ServiceRoutingFilter: Forwarding request...
```

## Notes

1. **Order numbers are negative** to ensure execution before default Spring Gateway filters (Order 0)
2. **Each filter is independent** and can return early with an error response
3. **Filters are stateless** and use exchange attributes to pass data
4. **All filters are reactive** using Project Reactor's Mono/Flux
5. **Error handling is centralized** using ErrorResponseUtil for consistent responses
