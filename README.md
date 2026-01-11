# NexusGate API Gateway

## Description

NexusGate is a lightweight and developer-friendly API traffic management platform built to protect backend services, control API usage, and provide real-time visibility into API consumption.

It acts as an intelligent control layer in front of APIs to identify clients, enforce rate limits and quotas, apply authentication-based rules, and monitor traffic behavior. NexusGate focuses on protection and observability rather than full API routing, making it easy to integrate alongside existing systems.

## Docker Setup

### Prerequisites

- Docker Desktop installed on your system

### Installation Steps

1. **Install Docker Desktop**
   
   Download and install Docker Desktop for your operating system from [https://www.docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop)

2. **Navigate to Infrastructure Folder**
   
   ```bash
   cd backend/infrastructure
   ```

3. **Start Services**
   
   Run the following command to start all required services:
   
   ```bash
   docker-compose up -d
   ```

This will start the following services:
- **PostgreSQL** (Port 5432) - Database for NexusGate
- **Redis** (Port 6379) - Cache and session storage

### Verify Installation

To verify that all services are running correctly:

```bash
docker-compose ps
```

### Stop Services

To stop all running services:

```bash
docker-compose down
```
