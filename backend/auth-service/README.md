# Auth Service

Central Identity Provider and JWT validation service for NexusGate API Gateway microservices system.

## Features

- **JWT Token Validation** - Validate JWT tokens for API Gateway
- **Token Refresh** - Generate new JWT tokens with extended expiry
- **Token Introspection** - Return token metadata (active status, subject, role, expiry)
- **Stateless Architecture** - No session management, fully stateless
- **Machine-to-Machine Authentication** - Support for service-to-service authentication

## Tech Stack

- **Java 21** (LTS)
- **Spring Boot 3.2.1**
- **Spring Security** (Stateless configuration)
- **JWT** - io.jsonwebtoken (jjwt) 0.12.3
- **PostgreSQL** (Minimal usage)
- **Maven**

## Project Structure

```
src/main/java/com/nexusgate/auth/
├── AuthServiceApplication.java
├── config/
│   ├── JwtConfig.java              # JWT configuration properties
│   └── SecurityConfig.java         # Spring Security configuration
├── controller/
│   └── AuthController.java         # REST endpoints
├── dto/
│   ├── JwtValidationRequest.java
│   ├── JwtValidationResponse.java
│   ├── RefreshTokenRequest.java
│   ├── RefreshTokenResponse.java
│   └── TokenIntrospectionResponse.java
├── exception/
│   └── GlobalExceptionHandler.java # Global exception handling
├── security/
│   └── JwtTokenProvider.java       # JWT generation and validation
└── service/
    └── JwtService.java              # Business logic
```

## API Endpoints

### 1. Validate Token

**Endpoint:** `POST /auth/validate`

**Request:**
```json
{
  "token": "eyJhbGciOi..."
}
```

**Response:**
```json
{
  "valid": true,
  "subject": "admin@demo.com",
  "role": "ADMIN",
  "expiresAt": 1712345678
}
```

### 2. Refresh Token

**Endpoint:** `POST /auth/refresh`

**Request:**
```json
{
  "token": "eyJhbGciOi..."
}
```

**Response:**
```json
{
  "token": "eyJhbGciOi...[new token]",
  "expiresAt": 1712349278
}
```

### 3. Introspect Token

**Endpoint:** `POST /auth/introspect`

**Request:**
```json
{
  "token": "eyJhbGciOi..."
}
```

**Response:**
```json
{
  "active": true,
  "subject": "admin@demo.com",
  "role": "ADMIN",
  "issuedAt": 1712342078,
  "expiresAt": 1712345678
}
```

## Configuration

Update `application.properties` with your settings:

```properties
# Server Configuration
server.port=8081

# JWT Configuration
jwt.secret=your-256-bit-secret-key-change-this-in-production
jwt.access-token-expiration=3600000      # 1 hour in milliseconds
jwt.refresh-token-expiration=86400000    # 24 hours in milliseconds
jwt.issuer=nexusgate-auth-service

# PostgreSQL Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/authdb
spring.datasource.username=postgres
spring.datasource.password=postgres
```

## Building and Running

### Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 12+ (optional for minimal database usage)

### Build

```bash
mvn clean compile
```

### Run

```bash
mvn spring-boot:run
```

The service will start on port `8081`.

### Package

```bash
mvn clean package
java -jar target/auth-service-0.0.1-SNAPSHOT.jar
```

## Security Considerations

1. **Change the JWT Secret** - Update `jwt.secret` in production with a strong, randomly generated 256-bit key
2. **Use HTTPS** - Always use HTTPS in production
3. **Token Expiry** - Adjust token expiration times based on your security requirements
4. **No User CRUD** - This service does not handle user registration or management (handled by config-service)
5. **Stateless** - No session storage, all authentication is token-based

## Integration with API Gateway

The API Gateway should call `/auth/validate` to verify incoming JWT tokens before routing requests to downstream services.

Example integration flow:
1. Client sends request with JWT token to API Gateway
2. Gateway calls `POST /auth/validate` with the token
3. If valid, Gateway forwards request to target microservice
4. If invalid, Gateway returns 401 Unauthorized

## Testing

```bash
mvn test
```

## Health Check

```
GET /actuator/health
```

## License

© 2026 NexusGate
