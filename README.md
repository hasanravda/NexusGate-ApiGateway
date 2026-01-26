# 🚀 NexusGate API Gateway

> Enterprise-grade API Gateway with distributed rate limiting, authentication, and real-time analytics

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x%2F4.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Next.js](https://img.shields.io/badge/Next.js-14-black)](https://nextjs.org/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## 📖 Overview

NexusGate is a production-ready API Gateway that provides centralized API management, authentication, rate limiting, and observability for microservices architectures. Built with Spring Cloud Gateway (reactive) and Next.js dashboard.

### Why NexusGate?

In modern microservices architectures, managing API traffic, enforcing security policies, and monitoring usage across multiple services becomes complex. NexusGate solves this by providing a single, intelligent entry point that:

- **Protects** your backend services from abuse and unauthorized access
- **Controls** API usage with fine-grained rate limiting per client
- **Monitors** all traffic in real-time with detailed analytics
- **Scales** horizontally with Redis-backed distributed state
- **Simplifies** client integration with consistent authentication

**Key Features:**
- 🔐 **Multi-Auth Support** - API Keys, JWT tokens, or combined authentication
- ⚡ **Distributed Rate Limiting** - Redis-backed with per-minute, per-hour, per-day limits
- 📊 **Real-Time Analytics** - Track requests, response times, errors, and violations
- 🎯 **Dynamic Routing** - Database-driven routes with wildcard pattern matching
- 🛡️ **Security First** - Request validation, sanitization, and protection
- 🧪 **Built-in Load Testing** - Validate performance and rate limits
- 🎨 **Modern Dashboard** - React-based admin panel with live metrics
- 📈 **Observability** - Prometheus metrics + Grafana dashboards
- 🔄 **Reactive Architecture** - Non-blocking, high-throughput design
- 🐳 **Docker Ready** - Full containerization support

### Use Cases

- **SaaS Platforms**: Control API access per customer with tiered rate limits
- **Microservices**: Single gateway for multiple internal services
- **Partner APIs**: Secure external API access with key management
- **Mobile Backends**: Rate limiting and analytics for mobile apps
- **IoT Platforms**: Manage high-volume device communication

## 🚀 Quick Start

### Prerequisites
- Docker Desktop
- Java 17+ (for backend development)
- Node.js 18+ (for frontend development)

### Start All Services

```bash
# Start infrastructure (PostgreSQL, Redis, Prometheus, Grafana)
cd backend
docker-compose up -d

# Start backend services (in separate terminals - all required)
cd backend/nexusgate-gateway && ./mvnw spring-boot:run       # Gateway (Port 8081)
cd backend/config-service && ./mvnw spring-boot:run          # Config (Port 8082)
cd backend/Analytics-service && ./mvnw spring-boot:run       # Analytics (Port 8084)
cd backend/mock-backend-services && ./mvnw spring-boot:run   # Mock APIs (Port 8091)

# Optional: Load testing service
cd backend/load-tester-service && ./mvnw spring-boot:run     # Load Tester (Port 8086)

# Start frontend
cd frontend
npm install
npm run dev
```

### Access Points
- 🌐 **Gateway**: http://localhost:8081
- 📊 **Dashboard**: http://localhost:3000
- 📈 **Grafana**: http://localhost:3001 (admin/admin)
- 🎯 **Prometheus**: http://localhost:9090
- 🧪 **Mock Backend**: http://localhost:8091
- ⚡ **Load Tester**: http://localhost:8086 (optional)

## 📚 Documentation

- **[ARCHITECTURE.md](ARCHITECTURE.md)** - System design & components
- **[QUICKSTART.md](QUICKSTART.md)** - Step-by-step setup guide
- **[API_GUIDE.md](API_GUIDE.md)** - API reference & examples
- **[Backend Documentation](backend/PROJECT_DOCUMENTATION.md)** - Complete backend docs
- **[API Endpoints](backend/ENDPOINTS.md)** - All available endpoints

## 🏗️ Project Structure

```
NexusGate/
├── backend/                    # Spring Boot microservices
│   ├── nexusgate-gateway/     # Main API Gateway (Port 8081)
│   ├── Analytics-service/     # Analytics & logging (Port 8084)
│   ├── config-service/        # Configuration service (Port 8082)
│   ├── load-tester-service/   # Load testing (Port 8086)
│   ├── mock-backend-services/ # Mock services for testing (Port 8091)
│   ├── docker-compose.yml     # Infrastructure setup
│   ├── db/                    # Database initialization scripts
│   ├── certs/                 # SSL certificates (for HTTPS)
│   └── prometheus.yml         # Prometheus configuration
├── frontend/                   # Next.js dashboard
│   ├── app/                   # Next.js pages and API routes
│   ├── components/            # React components
│   └── lib/                   # Utility functions and API clients
├── ARCHITECTURE.md            # System architecture details
├── QUICKSTART.md              # Step-by-step setup guide
└── API_GUIDE.md               # Complete API documentation
```

## ✨ What's Included

### Backend Services
- **Gateway Service**: Main entry point with routing, auth, and rate limiting
- **Analytics Service**: Centralized logging, metrics, and violation tracking
- **Config Service**: User management and service configuration
- **Load Tester**: Built-in performance testing and validation
- **Mock Services**: Simulated backend for testing (Users, Orders, Payments)

### Infrastructure
- **PostgreSQL**: Persistent storage for routes, keys, users, and logs
- **Redis**: Distributed rate limiting and caching
- **Prometheus**: Metrics collection and time-series data
- **Grafana**: Pre-configured dashboards for visualization

### Frontend
- **Admin Dashboard**: React-based UI for managing API keys, viewing logs, and monitoring
- **Real-time Metrics**: Live charts and statistics
- **Rate Limit Monitoring**: Track violations and usage patterns

## 🛠️ Tech Stack

**Backend:** Spring Boot 3.x, Spring Cloud Gateway, PostgreSQL, Redis, Prometheus  
**Frontend:** Next.js 14, React, TailwindCSS, Recharts  
**DevOps:** Docker, Docker Compose, Grafana

## 🎯 Performance Metrics

- **Throughput**: 10,000+ requests/second (single gateway instance)
- **Latency**: <10ms (p95 with Redis cache hit)
- **Rate Limiting**: Sub-millisecond decision with Redis
- **Scalability**: Horizontal scaling ready (stateless gateway)

## 🔧 Configuration

Key configuration files:
- `backend/nexusgate-gateway/src/main/resources/application.yml` - Gateway config
- `backend/docker-compose.yml` - Infrastructure setup
- `backend/db/init-db.sql` - Database schema and seed data
- `frontend/next.config.js` - Frontend configuration

## 🧪 Testing

```bash
# Run backend tests
cd backend/nexusgate-gateway
./mvnw test

# Run load tests
cd backend/load-tester-service
./test-rate-limiting.sh

# Test specific scenarios
curl -X GET http://localhost:8081/api/users \
  -H "X-API-Key: nx_test_key_12345"
```

## 📊 Monitoring

- **Grafana**: http://localhost:3001 (admin/admin)
  - Pre-configured dashboards for API metrics
  - Rate limit violation tracking
  - Response time percentiles
  
- **Prometheus**: http://localhost:9090
  - Raw metrics and queries
  - PromQL for custom queries
  
- **Frontend Dashboard**: http://localhost:3000
  - User-friendly interface
  - Real-time log viewer
  - API key management

## 🚀 Deployment

NexusGate is designed for easy deployment:

- **Docker Compose**: For local development (included)
- **Kubernetes**: Helm charts coming soon
- **Cloud**: AWS, Azure, GCP compatible
- **CI/CD**: GitHub Actions workflows included

## 🤝 Contributing

Contributions welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## 📄 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

Built with:
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [Next.js](https://nextjs.org/)
- [Redis](https://redis.io/)
- [PostgreSQL](https://www.postgresql.org/)
- [Prometheus](https://prometheus.io/) & [Grafana](https://grafana.com/)
