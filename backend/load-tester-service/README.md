# Load Testing Service

## Overview
A production-ready Spring Boot 3.x microservice designed to simulate high traffic and validate rate limiting behavior in API gateway systems. This service enables comprehensive load testing with configurable concurrency, request patterns, and real-time metrics collection.

**Port:** 8083

## Architecture

### Key Components

#### 1. **DTOs (Data Transfer Objects)**
- `LoadTestRequest` - Test configuration and parameters
- `LoadTestResult` - Aggregated test results and metrics
- `LoadTestStatus` - Test lifecycle status (RUNNING, COMPLETED, STOPPED, FAILED)

#### 2. **Model**
- `TestExecution` - Represents an active test instance with metrics and concurrent task tracking

#### 3. **Services**

**LoadTestService** (Main Orchestrator)
- Manages test lifecycle
- Spawns concurrent clients using CompletableFuture
- Coordinates test execution and termination
- Handles in-memory test storage

**HttpClientService**
- Non-blocking HTTP execution using WebClient
- Measures request latency
- Handles multiple HTTP methods (GET, POST, PUT, DELETE)
- Implements timeout and error handling

**MetricsCollector**
- Thread-safe metrics tracking
- Uses AtomicLong and ConcurrentHashMap
- Calculates average and P95 latency
- Tracks success/failure/rate-limit distributions

**ReportGenerator**
- Generates comprehensive test reports
- Calculates derived metrics (throughput, success rates)
- Supports both final and partial (real-time) reports

#### 4. **Controller**
- `LoadTestController` - REST API for test management

## API Endpoints

### 1. Start Load Test
```http
POST /load-test/start
Content-Type: application/json

{
  "targetKey": "nx_lendingkart_prod_abc123",
  "targetEndpoint": "http://localhost:8080/api/users",
  "requestRate": 100,
  "durationSeconds": 30,
  "concurrencyLevel": 10,
  "requestPattern": "CONSTANT_RATE",
  "httpMethod": "GET"
}
```

**Response:**
```json
{
  "testId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RUNNING",
  "message": "Load test started successfully"
}
```

**Request Parameters:**
- `targetKey` - API key to test
- `targetEndpoint` - Target URL
- `requestRate` - Total requests per second
- `durationSeconds` - Test duration
- `concurrencyLevel` - Number of concurrent clients
- `requestPattern` - Traffic pattern (CONSTANT_RATE, BURST, RAMP_UP)
- `httpMethod` - HTTP method (GET, POST, PUT, DELETE)

### 2. Get Test Status
```http
GET /load-test/status/{testId}
```

**Response (Running Test):**
```json
{
  "testId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RUNNING",
  "totalRequests": 1500,
  "successfulRequests": 1200,
  "rateLimitedRequests": 300,
  "errorRequests": 0,
  "averageLatencyMs": 45.3,
  "p95LatencyMs": 120,
  "requestsPerSecond": 100.5
}
```

### 3. Get Test Results
```http
GET /load-test/results/{testId}
```

**Response (Completed Test):**
```json
{
  "testId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "startTime": "2026-01-16T10:30:00",
  "endTime": "2026-01-16T10:30:30",
  "durationMs": 30000,
  "totalRequests": 3000,
  "successfulRequests": 2400,
  "rateLimitedRequests": 600,
  "errorRequests": 0,
  "averageLatencyMs": 45.3,
  "p95LatencyMs": 120,
  "minLatencyMs": 20,
  "maxLatencyMs": 350,
  "requestsPerSecond": 100.0,
  "successRate": 80.0,
  "rateLimitRate": 20.0,
  "targetEndpoint": "http://localhost:8080/api/users",
  "configuredRequestRate": 100,
  "concurrencyLevel": 10
}
```

### 4. Stop Test
```http
DELETE /load-test/stop/{testId}
```

**Response:**
```json
{
  "testId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Test stop requested"
}
```

### 5. Health Check
```http
GET /load-test/health
```

## Request Patterns

### CONSTANT_RATE
Evenly distributes requests over time for steady load testing.
- Delay between requests: 1000ms / requestRate
- Best for: Testing sustained load and rate limit thresholds

### BURST
Sends requests as fast as possible with minimal delay.
- Delay between requests: 0ms
- Best for: Testing burst handling and rate limit responsiveness

### RAMP_UP
Gradually increases request rate over time.
- Starts at 50% of target rate
- Best for: Testing system warm-up and gradual load increase

## Concurrency Model

### Per-Client Request Distribution
```
Total Request Rate: 100 req/s
Concurrency Level: 10 clients
Per-Client Rate: 100 / 10 = 10 req/s per client
```

Each client runs independently using CompletableFuture for parallel execution.

## Metrics Tracking

### Thread Safety
All metrics use thread-safe data structures:
- `LongAdder` for counters (better than AtomicLong for high contention)
- `AtomicLong` for min/max values with compare-and-swap
- `ConcurrentHashMap` for status code distribution
- `Collections.synchronizedList` for latency tracking

### Calculated Metrics
- **Average Latency**: Total latency / Total requests
- **P95 Latency**: 95th percentile from sorted latency list
- **Success Rate**: (2xx responses / Total requests) × 100
- **Rate Limit Rate**: (429 responses / Total requests) × 100
- **Throughput**: Total requests / Duration (seconds)

## Technical Implementation

### Non-Blocking I/O
- Uses Spring WebFlux WebClient for async HTTP calls
- Reactive execution with Mono/Flux
- No thread blocking during request execution

### Concurrent Execution
- ExecutorService (CachedThreadPool) for dynamic scaling
- CompletableFuture for async client orchestration
- AtomicBoolean for graceful shutdown coordination

### Rate Control
Implemented using Thread.sleep with calculated delays:
```java
delayMs = 1000 / requestsPerSecond
```

### Error Handling
- WebClient exceptions mapped to error metrics
- Timeout handling (10s per request)
- Connection errors tracked separately
- No test failure on individual request errors

## Configuration

### application.properties
```properties
server.port=8083
spring.application.name=load-tester-service

# WebClient connection pool
spring.reactor.netty.pool.max-connections=500
spring.reactor.netty.pool.acquire-timeout=45000

# Logging
logging.level.com.nexusgate.loadtest=DEBUG
```

## Running the Service

### Prerequisites
- Java 21
- Maven 3.6+
- Spring Boot 3.x

### Build
```bash
cd backend/load-tester-service
mvn clean install
```

### Run
```bash
mvn spring-boot:run
```

Or run the JAR:
```bash
java -jar target/load-tester-service-0.0.1-SNAPSHOT.jar
```

### Verify
```bash
curl http://localhost:8083/load-test/health
```

## Usage Examples

### Example 1: Test Rate Limit Enforcement
```bash
curl -X POST http://localhost:8083/load-test/start \
  -H "Content-Type: application/json" \
  -d '{
    "targetKey": "nx_test_key_123",
    "targetEndpoint": "http://localhost:8080/api/users",
    "requestRate": 50,
    "durationSeconds": 60,
    "concurrencyLevel": 5,
    "requestPattern": "CONSTANT_RATE",
    "httpMethod": "GET"
  }'
```

**Expected Result:** Mix of 200 and 429 responses based on rate limit configuration

### Example 2: Burst Load Test
```bash
curl -X POST http://localhost:8083/load-test/start \
  -H "Content-Type: application/json" \
  -d '{
    "targetKey": "nx_test_key_123",
    "targetEndpoint": "http://localhost:8080/api/products",
    "requestRate": 200,
    "durationSeconds": 10,
    "concurrencyLevel": 20,
    "requestPattern": "BURST",
    "httpMethod": "GET"
  }'
```

**Expected Result:** High rate limit enforcement, tests burst handling

### Example 3: Monitor Running Test
```bash
# Get test ID from start response
TEST_ID="550e8400-e29b-41d4-a716-446655440000"

# Check status every 5 seconds
watch -n 5 "curl -s http://localhost:8083/load-test/status/$TEST_ID | jq"
```

### Example 4: Get Final Results
```bash
curl http://localhost:8083/load-test/results/$TEST_ID | jq
```

## Design Decisions

### 1. In-Memory Storage
Tests stored in ConcurrentHashMap for simplicity and speed. For production with multiple instances, consider Redis or database.

### 2. CompletableFuture over Reactive Streams
CompletableFuture provides simpler concurrency control for client spawning, while WebClient handles reactive HTTP execution.

### 3. Thread-Safe Metrics
LongAdder preferred over AtomicLong for high-contention scenarios (multiple clients updating simultaneously).

### 4. Per-Client Rate Distribution
Dividing total rate by concurrency level ensures accurate aggregate throughput and realistic client behavior.

### 5. No Persistence
Test results are ephemeral. Implement cleanup strategy or add persistence layer for long-term storage.

## Interview Talking Points

### Concurrency
- **CompletableFuture**: Async orchestration of multiple clients
- **ExecutorService**: Dynamic thread pool management
- **AtomicBoolean**: Graceful shutdown coordination across threads

### Thread Safety
- **LongAdder**: Lock-free counters with better contention handling
- **ConcurrentHashMap**: Thread-safe status code tracking
- **Compare-and-Swap**: Atomic min/max updates

### Performance
- **Non-blocking I/O**: WebClient prevents thread blocking
- **Reactive Execution**: Mono/Flux for efficient resource usage
- **Connection Pooling**: Reusable HTTP connections

### Observability
- **Real-time Metrics**: Partial reports for running tests
- **Comprehensive Reporting**: Latency percentiles, success rates
- **Structured Logging**: Test lifecycle tracking

## Future Enhancements

1. **Persistence Layer**: Store test results in database
2. **Distributed Execution**: Multiple load tester instances
3. **Custom Payloads**: Support POST/PUT request bodies
4. **Authentication**: Multiple auth methods (Bearer, Basic, OAuth)
5. **Scheduled Tests**: Cron-based recurring tests
6. **WebSocket Support**: Real-time metric streaming
7. **Test Templates**: Predefined test scenarios
8. **Result Export**: CSV/JSON report downloads

## Troubleshooting

### High Memory Usage
- Reduce concurrency level
- Shorten test duration
- Implement result cleanup (cleanupOldTests method)

### Connection Timeouts
- Increase WebClient timeout settings
- Check target service capacity
- Reduce request rate

### Inaccurate Metrics
- Verify target service is responding
- Check network latency
- Ensure sufficient system resources

## License
MIT

## Author
NexusGate Engineering Team
