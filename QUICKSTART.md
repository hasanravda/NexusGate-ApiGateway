# 🚀 NexusGate Quick Start Guide

Get NexusGate up and running in 5 minutes!

## 📋 Prerequisites

Ensure you have the following installed:
- **Docker Desktop** - [Download here](https://www.docker.com/products/docker-desktop)
- **Java 17+** - [Download OpenJDK](https://adoptium.net/)
- **Node.js 18+** - [Download here](https://nodejs.org/)
- **Maven** (optional - included via mvnw)

## 🎯 Step-by-Step Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd NexusGate
```

### 2. Start Infrastructure Services

Start PostgreSQL, Redis, Prometheus, and Grafana:

```bash
cd backend
docker-compose up -d
```

**Verify services are running:**
```bash
docker-compose ps
```

You should see:
- ✅ nexusgate-postgres (Port 5432)
- ✅ nexusgate-redis (Port 6379)
- ✅ prometheus (Port 9090)
- ✅ grafana (Port 3001)

### 3. Initialize Database

The database schema is automatically initialized via `db/init-db.sql`. To verify:

```bash
docker exec -it nexusgate-postgres psql -U nexusgate -d nexusgate -c "\dt"
```

### 4. Start Backend Services

Open **5 separate terminals** from the project root and run:

**Terminal 1 - Gateway Service (Required):**
```bash
cd backend/nexusgate-gateway
./mvnw spring-boot:run
```
✅ Gateway will start on: http://localhost:8081

**Terminal 2 - Config Service (Required):**
```bash
cd backend/config-service
./mvnw spring-boot:run
```
✅ Config service on: http://localhost:8082

**Terminal 3 - Analytics Service (Required):**
```bash
cd backend/Analytics-service
./mvnw spring-boot:run
```
✅ Analytics on: http://localhost:8084

**Terminal 4 - Mock Backend Service (Required):**
```bash
cd backend/mock-backend-services
./mvnw spring-boot:run
```
✅ Mock services on: http://localhost:8091

**Terminal 5 - Load Tester (Optional):**
```bash
cd backend/load-tester-service
./mvnw spring-boot:run
```
✅ Load tester on: http://localhost:8086

> **⚠️ Important**: 
> - Mock Backend Service is **required** - the gateway routes API calls (users, orders, payments) to it
> - Load Tester is **optional** - only needed for performance testing
> - All commands assume you're starting from the NexusGate root directory

### 5. Start Frontend Dashboard

```bash
cd frontend
npm install
npm run dev
```
Dashboard will be available at: http://localhost:3000

## ✅ Verify Installation

### Test Gateway Health
```bash
curl http://localhost:8081/actuator/health
```

### Test with API Key
```bash
curl -X GET http://localhost:8081/api/users \
  -H "X-API-Key: nx_test_key_12345"
```

### Access Monitoring
- **Grafana Dashboard**: http://localhost:3001 (login: admin/admin)
- **Prometheus Metrics**: http://localhost:9090
- **Frontend Dashboard**: http://localhost:3000

## 🎮 Try It Out

### 1. View Available API Keys
Navigate to the dashboard at http://localhost:3000 and check the "API Keys" section.

### 2. Make Sample Requests
```bash
# Get users
curl -X GET http://localhost:8081/api/users \
  -H "X-API-Key: nx_test_key_12345"

# Get orders
curl -X GET http://localhost:8081/api/orders \
  -H "X-API-Key: nx_test_key_12345"
```

### 3. Check Analytics
- Visit the dashboard to see request metrics
- Check Grafana for detailed monitoring

## 🛑 Stopping Services

### Stop backend services
Press `Ctrl+C` in each terminal

### Stop infrastructure
```bash
cd backend
docker-compose down
```

### Stop with data cleanup
```bash
docker-compose down -v  # Removes volumes (database data)
```

## 🐛 Troubleshooting

### Port Already in Use
```bash
# Check what's using port 8081
netstat -ano | findstr :8081  # Windows
lsof -i :8081                 # Mac/Linux

# Kill the process or change ports in application.yml
```

### Database Connection Issues
```bash
# Restart PostgreSQL
docker-compose restart nexusgate-postgres

# Check logs
docker logs nexusgate-postgres
```

### Redis Connection Issues
```bash
# Restart Redis
docker-compose restart nexusgate-redis

# Test connection
docker exec -it nexusgate-redis redis-cli ping
# Should return: PONG
```

## 📚 Next Steps

- Read [ARCHITECTURE.md](ARCHITECTURE.md) to understand the system design
- Check [API_GUIDE.md](API_GUIDE.md) for detailed API documentation
- Explore [backend/PROJECT_DOCUMENTATION.md](backend/PROJECT_DOCUMENTATION.md) for in-depth technical details
- Review [backend/ENDPOINTS.md](backend/ENDPOINTS.md) for all available endpoints

## 🎓 Learning Resources

- [Spring Cloud Gateway Docs](https://spring.io/projects/spring-cloud-gateway)
- [Next.js Documentation](https://nextjs.org/docs)
- [Docker Compose Guide](https://docs.docker.com/compose/)

---

Need help? Check the main [README.md](README.md) or create an issue!
