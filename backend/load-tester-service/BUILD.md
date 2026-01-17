# Build and Deployment Guide - Load Testing Service

## Quick Start

### 1. Build the Project
```bash
cd /Users/krish/Desktop/NexusGate-ApiGateway/backend/load-tester-service
mvn clean install
```

### 2. Run the Service
```bash
mvn spring-boot:run
```

Or run the compiled JAR:
```bash
java -jar target/load-tester-service-0.0.1-SNAPSHOT.jar
```

### 3. Verify Service is Running
```bash
curl http://localhost:8083/load-test/health
```

Expected response:
```json
{
  "status": "UP",
  "service": "load-tester-service"
}
```

## Testing the Service

### Option 1: Use the Automated Test Script
```bash
cd /Users/krish/Desktop/NexusGate-ApiGateway/backend/load-tester-service
./test-load-tester.sh
```

### Option 2: Manual cURL Commands

#### Start a Load Test
```bash
curl -X POST http://localhost:8083/load-test/start \
  -H "Content-Type: application/json" \
  -d '{
    "targetKey": "nx_test_key_123",
    "targetEndpoint": "http://localhost:8080/api/users",
    "requestRate": 50,
    "durationSeconds": 30,
    "concurrencyLevel": 5,
    "requestPattern": "CONSTANT_RATE",
    "httpMethod": "GET"
  }'
```

#### Monitor Test Status
```bash
# Replace TEST_ID with actual ID from start response
curl http://localhost:8083/load-test/status/TEST_ID | jq
```

#### Get Final Results
```bash
curl http://localhost:8083/load-test/results/TEST_ID | jq
```

#### Stop a Running Test
```bash
curl -X DELETE http://localhost:8083/load-test/stop/TEST_ID
```

## Project Structure

```
load-tester-service/
├── src/main/java/com/nexusgate/
│   ├── load_tester_service/
│   │   └── LoadTesterServiceApplication.java    # Main application
│   └── loadtest/
│       ├── controller/
│       │   └── LoadTestController.java          # REST endpoints
│       ├── dto/
│       │   ├── LoadTestRequest.java             # Request DTO
│       │   ├── LoadTestResult.java              # Result DTO
│       │   └── LoadTestStatus.java              # Status enum
│       ├── model/
│       │   └── TestExecution.java               # Test instance model
│       └── service/
│           ├── LoadTestService.java             # Main orchestrator
│           ├── HttpClientService.java           # HTTP client
│           ├── MetricsCollector.java            # Metrics tracking
│           └── ReportGenerator.java             # Report generation
├── src/main/resources/
│   └── application.properties                   # Configuration
├── test-load-tester.sh                          # Test script
├── test-scenarios.json                          # Example scenarios
├── README.md                                    # Documentation
└── pom.xml                                      # Maven configuration
```

## Key Features Implemented

### 1. **Non-Blocking HTTP Execution**
- Uses Spring WebFlux WebClient
- Reactive programming with Mono/Flux
- No thread blocking during I/O operations

### 2. **Concurrent Load Generation**
- CompletableFuture for async client orchestration
- ExecutorService (CachedThreadPool) for dynamic scaling
- Per-client request rate distribution

### 3. **Thread-Safe Metrics Collection**
- LongAdder for high-contention counters
- AtomicLong with compare-and-swap for min/max
- ConcurrentHashMap for status code tracking
- Synchronized list for latency collection

### 4. **Request Patterns**
- **CONSTANT_RATE**: Evenly distributed requests (1000ms / rate)
- **BURST**: Maximum throughput (0ms delay)
- **RAMP_UP**: Gradual increase (starts at 50% rate)

### 5. **Comprehensive Metrics**
- Total requests, success rate, rate limit rate
- Average latency, P95 latency, min/max latency
- Throughput (requests per second)
- HTTP status code distribution

### 6. **Test Lifecycle Management**
- Asynchronous execution (non-blocking start)
- Status tracking (RUNNING, COMPLETED, STOPPED, FAILED)
- Graceful shutdown with AtomicBoolean
- Manual stop capability

## Configuration

### application.properties
```properties
# Service Configuration
server.port=8083
spring.application.name=load-tester-service

# WebClient Connection Pool
spring.reactor.netty.pool.max-connections=500
spring.reactor.netty.pool.acquire-timeout=45000

# Logging
logging.level.com.nexusgate.loadtest=DEBUG
```

## Dependencies (from pom.xml)
- Spring Boot 3.x (4.0.1)
- Spring WebFlux (WebClient)
- Spring Web (REST controller)
- Spring Actuator (health endpoints)
- Jakarta Validation (DTO validation)
- Java 21

## API Documentation

### POST /load-test/start
Starts a new load test execution

**Request Body:**
```json
{
  "targetKey": "string",           // API key to test
  "targetEndpoint": "string",       // Target URL
  "requestRate": 50,                // Requests per second
  "durationSeconds": 30,            // Test duration
  "concurrencyLevel": 5,            // Number of concurrent clients
  "requestPattern": "CONSTANT_RATE",// Pattern: CONSTANT_RATE, BURST, RAMP_UP
  "httpMethod": "GET"               // Method: GET, POST, PUT, DELETE
}
```

**Response:** 202 Accepted
```json
{
  "testId": "uuid",
  "status": "RUNNING",
  "message": "Load test started successfully"
}
```

### GET /load-test/status/{testId}
Get current status of a test

**Response:** 200 OK
```json
{
  "testId": "uuid",
  "status": "RUNNING",
  "totalRequests": 1500,
  "successfulRequests": 1200,
  "rateLimitedRequests": 300,
  "averageLatencyMs": 45.3,
  "requestsPerSecond": 100.5
}
```

### GET /load-test/results/{testId}
Get final results of a completed test

**Response:** 200 OK (see README.md for full schema)

### DELETE /load-test/stop/{testId}
Stop a running test

**Response:** 200 OK
```json
{
  "testId": "uuid",
  "message": "Test stop requested"
}
```

## Troubleshooting

### Build Issues
```bash
# Clean and rebuild
mvn clean install -U

# Skip tests if needed
mvn clean install -DskipTests
```

### Port Already in Use
```bash
# Find process using port 8083
lsof -ti:8083

# Kill process
kill -9 $(lsof -ti:8083)

# Or change port in application.properties
server.port=8084
```

### High Memory Usage
- Reduce concurrency level
- Shorten test duration
- Implement periodic cleanup of old tests

### Connection Timeouts
- Check target service is running
- Increase WebClient timeout in application.properties
- Reduce request rate

## Performance Considerations

### Thread Pool Sizing
Using CachedThreadPool for dynamic scaling:
- Creates new threads as needed
- Reuses idle threads
- Terminates threads after 60s idle

For production, consider:
```java
ExecutorService executor = new ThreadPoolExecutor(
    10,              // core pool size
    100,             // max pool size
    60L,             // keep alive time
    TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(1000)
);
```

### Memory Management
- Tests stored in memory (ConcurrentHashMap)
- Implement cleanup strategy for old tests
- Consider Redis for distributed deployment

### Rate Limiting Accuracy
- Uses Thread.sleep for rate control
- Accuracy depends on system scheduler
- For sub-millisecond accuracy, consider ScheduledExecutorService

## Next Steps

1. **Build and Test**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ./test-load-tester.sh
   ```

2. **Integrate with API Gateway**
   - Ensure target service is running on port 8080
   - Configure valid API keys
   - Run load tests to validate rate limiting

3. **Monitor Results**
   - Check success vs rate-limited responses
   - Verify P95 latency is acceptable
   - Analyze throughput metrics

4. **Iterate**
   - Adjust rate limits based on results
   - Test different concurrency patterns
   - Validate sustained load behavior

## Interview Highlights

When discussing this service in interviews, emphasize:

1. **Concurrency Design**
   - CompletableFuture for async orchestration
   - Thread-safe metrics with atomic operations
   - Per-client rate distribution algorithm

2. **Performance**
   - Non-blocking I/O with WebClient
   - Reactive programming with Reactor
   - Connection pooling and resource management

3. **Observability**
   - Comprehensive metrics (latency percentiles, success rates)
   - Real-time status monitoring
   - Structured logging

4. **Production Readiness**
   - Clean architecture (separation of concerns)
   - Validation and error handling
   - Graceful shutdown
   - Configurable parameters

5. **Testing Strategy**
   - Multiple traffic patterns
   - Configurable concurrency
   - Extensible for future enhancements

## Additional Resources

- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
- [Project Reactor](https://projectreactor.io/docs)
- [CompletableFuture Guide](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html)

---
**Service Port:** 8083  
**Status:** Production Ready  
**Last Updated:** January 16, 2026
