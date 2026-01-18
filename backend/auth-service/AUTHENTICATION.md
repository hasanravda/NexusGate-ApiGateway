# Authentication API Documentation

## 🔐 New Authentication Endpoints

### 1. Login

**POST** `/auth/login`

Authenticate user and receive JWT token.

**Request:**
```json
{
  "email": "admin@demo.com",
  "password": "admin123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "expiresAt": 1705149278
}
```

**Error Response (401 Unauthorized):**
- Invalid credentials
- User is inactive

---

### 2. Get Current User

**GET** `/auth/me`

Get current authenticated user information from JWT token.

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Response (200 OK):**
```json
{
  "email": "admin@demo.com",
  "role": "ADMIN"
}
```

**Error Response (401 Unauthorized):**
- Missing or invalid token

---

## 📝 Postman Testing

### Login Test

**POST** `http://localhost:8084/auth/login`

Headers:
```
Content-Type: application/json
```

Body (raw JSON):
```json
{
  "email": "admin@demo.com",
  "password": "admin123"
}
```

**Copy the `token` from response for next request!**

---

### Get User Info Test

**GET** `http://localhost:8084/auth/me`

Headers:
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...YOUR_TOKEN_HERE
```

---

## 🚀 Testing with cURL

```bash
# 1. Login and get token
TOKEN=$(curl -s -X POST http://localhost:8084/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@demo.com","password":"admin123"}' \
  | jq -r '.token')

echo "Token: $TOKEN"

# 2. Get user info
curl -X GET http://localhost:8084/auth/me \
  -H "Authorization: Bearer $TOKEN" | jq

# 3. Test existing endpoints still work
curl -X POST http://localhost:8084/auth/validate \
  -H "Content-Type: application/json" \
  -d "{\"token\":\"$TOKEN\"}" | jq
```

---

## 🗄️ Database Setup

### Default User
- **Email:** admin@demo.com
- **Password:** admin123
- **Role:** ADMIN
- **Status:** Active

The default user is automatically created on application startup.

### PostgreSQL Setup (if needed)

```bash
# Start PostgreSQL
docker run -d \
  --name postgres-auth \
  -e POSTGRES_DB=authdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15

# Or use existing PostgreSQL and create database
psql -U postgres -c "CREATE DATABASE authdb;"
```

---

## 🔒 Security Configuration

### Public Endpoints (No Authentication)
- `POST /auth/login`
- `POST /auth/validate`
- `POST /auth/refresh`
- `POST /auth/introspect`
- `GET /actuator/health`

### Protected Endpoints (Requires JWT)
- `GET /auth/me`

### Features
- ✅ Stateless authentication (no sessions)
- ✅ BCrypt password encryption
- ✅ JWT-based authorization
- ✅ Role-based access control ready
- ✅ Inactive user rejection

---

## 📊 Database Schema

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true
);
```

---

## 🎯 Complete Testing Flow

1. **Start PostgreSQL** (if not already running)
2. **Start Auth Service** (default user is auto-created)
3. **Test Login:**
   ```bash
   curl -X POST http://localhost:8084/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@demo.com","password":"admin123"}'
   ```
4. **Copy the token from response**
5. **Test /auth/me:**
   ```bash
   curl -X GET http://localhost:8084/auth/me \
     -H "Authorization: Bearer YOUR_TOKEN_HERE"
   ```
6. **Verify existing endpoints still work:**
   ```bash
   curl -X POST http://localhost:8084/auth/validate \
     -H "Content-Type: application/json" \
     -d '{"token":"YOUR_TOKEN_HERE"}'
   ```

---

## ⚠️ Important Notes

- Password is stored as BCrypt hash
- Default password: `admin123` (change in production!)
- JWT secret must be at least 256 bits for HS512
- Inactive users cannot login
- All existing token validation endpoints remain unchanged

---

## 🏗️ Project Structure

```
src/main/java/com/nexusgate/auth/
├── config/
│   ├── DataLoader.java           # Auto-creates default user
│   ├── JwtConfig.java            # JWT configuration
│   └── SecurityConfig.java       # Security + BCrypt config
├── controller/
│   └── AuthController.java       # login + me endpoints
├── dto/
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── UserInfoResponse.java
│   └── ... (existing DTOs)
├── model/
│   └── User.java                 # User entity
├── repository/
│   └── UserRepository.java
├── security/
│   ├── JwtAuthenticationFilter.java  # JWT filter for /auth/me
│   └── JwtTokenProvider.java        # JWT utilities
└── service/
    ├── AuthService.java          # Authentication logic
    └── JwtService.java           # Existing JWT service
```
