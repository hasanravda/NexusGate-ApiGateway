# Load Testing Service - Architecture Overview

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     Load Testing Service (Port 8083)                     │
└─────────────────────────────────────────────────────────────────────────┘

                                    │
                         ┌──────────▼──────────┐
                         │  LoadTestController │
                         │  (REST Endpoints)   │
                         └──────────┬──────────┘
                                    │
                         ┌──────────▼──────────┐
                         │  LoadTestService    │
                         │  (Orchestrator)     │
                         └──────────┬──────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    │               │               │
         ┌──────────▼──────┐  ┌────▼────┐  ┌──────▼──────┐
         │ HttpClientService│  │ Metrics │  │   Report    │
         │   (WebClient)    │  │Collector│  │  Generator  │
         └──────────┬────── ┘  └────┬────┘  └──────┬──────┘
                    │               │               │
                    │               │               │
              ┌─────▼─────┐   ┌────▼─────┐   ┌────▼─────┐
              │ CompletableFuture         │   │  Thread  │
              │   Pool (N clients)        │   │   Safe   │
              └─────┬─────┘               │   │ Counters │
                    │                     │   └──────────┘
        ┌───────────┼───────────┐         │
        │           │           │         │
    ┌───▼───┐   ┌──▼───┐   ┌───▼───┐     │
    │Client1│   │Client2│...│ClientN│     │
    └───┬───┘   └──┬───┘   └───┬───┘     │
        │          │           │          │
        └──────────┼───────────┘          │
                   │                      │
        ┌──────────▼──────────┐           │
        │   Target API        │◄──────────┘
        │   (Rate Limited)    │   Records Metrics
        └─────────────────────┘
```

## Component Flow

### 1. Test Initiation Flow
```
User Request
    │
    ├─► POST /load-test/start
    │       │
    │       ├─► Validate Request (Jakarta Validation)
    │       │
    │       ├─► Generate Test ID (UUID)
    │       │
    │       ├─► Create TestExecution Instance
    │       │       ├─► Initialize MetricsCollector
    │       │       ├─► Set Status = RUNNING
    │       │       └─► Store in ConcurrentHashMap
    │       │
    │       ├─► Calculate Per-Client Rate
    │       │       └─► rate = totalRate / concurrency
    │       │
    │       ├─► Spawn N Concurrent Clients (CompletableFuture)
    │       │       ├─► Client 1 → runClient()
    │       │       ├─► Client 2 → runClient()
    │       │       └─► Client N → runClient()
    │       │
    │       └─► Return 202 Accepted + testId
    │
    └─► Async Execution Continues in Background
```

### 2. Client Execution Flow
```
runClient(clientId, requestsPerSecond)
    │
    ├─► Start Timer
    │
    ├─► Loop (while !stopped && duration not exceeded)
    │       │
    │       ├─► Execute HTTP Request
    │       │       │
    │       │       ├─► WebClient.method(endpoint)
    │       │       │       .header("X-API-Key", apiKey)
    │       │       │       .retrieve()
    │       │       │
    │       │       ├─► Measure Latency (startTime - endTime)
    │       │       │
    │       │       └─► Extract Status Code
    │       │
    │       ├─► Record Metrics (Thread-Safe)
    │       │       ├─► totalRequests.increment()
    │       │       ├─► Categorize by Status (200/429/5xx)
    │       │       ├─► Record Latency
    │       │       └─► Update Min/Max (CAS)
    │       │
    │       └─► Rate Control (Thread.sleep)
    │               └─► delay = calculateDelay(pattern, rate)
    │
    └─► Client Completes
```

### 3. Metrics Collection Flow
```
MetricsCollector (Thread-Safe)
    │
    ├─► Counters (LongAdder)
    │       ├─► totalRequests
    │       ├─► successfulRequests (2xx)
    │       ├─► rateLimitedRequests (429)
    │       └─► errorRequests (5xx, timeout)
    │
    ├─► Latency Tracking
    │       ├─► List<Long> latencies (synchronized)
    │       ├─► totalLatencyMs (AtomicLong)
    │       ├─► minLatencyMs (AtomicLong + CAS)
    │       └─► maxLatencyMs (AtomicLong + CAS)
    │
    └─► Status Distribution
            └─► ConcurrentHashMap<statusCode, count>
```

### 4. Report Generation Flow
```
ReportGenerator
    │
    ├─► Calculate Average Latency
    │       └─► totalLatency / totalRequests
    │
    ├─► Calculate P95 Latency
    │       ├─► Sort latency list
    │       └─► Get value at 95th percentile
    │
    ├─► Calculate Success Rate
    │       └─► (successfulRequests / totalRequests) × 100
    │
    ├─► Calculate Throughput
    │       └─► totalRequests / durationSeconds
    │
    └─► Build LoadTestResult DTO
```

## Concurrency Model

### Thread Pool Architecture
```
ExecutorService (CachedThreadPool)
    │
    ├─► Core Threads: 0 (creates on demand)
    ├─► Max Threads: Integer.MAX_VALUE
    ├─► Keep-Alive: 60 seconds
    │
    └─► Task Queue: SynchronousQueue
            (Direct handoff between producer and consumer)
```

### Client Distribution
```
Example: 100 req/s, 10 concurrent clients

Load Test Service
    │
    ├─► Client 1: 10 req/s  ─┐
    ├─► Client 2: 10 req/s   │
    ├─► Client 3: 10 req/s   │
    ├─► Client 4: 10 req/s   ├─► All clients run in parallel
    ├─► Client 5: 10 req/s   │
    ├─► Client 6: 10 req/s   │
    ├─► Client 7: 10 req/s   │
    ├─► Client 8: 10 req/s   │
    ├─► Client 9: 10 req/s   │
    └─► Client 10: 10 req/s ─┘
            │
            └─► Aggregate: 100 req/s to Target API
```

## Request Pattern Implementation

### CONSTANT_RATE
```
Timeline (per client):
0ms     100ms   200ms   300ms   400ms   500ms
 │       │       │       │       │       │
 ▼       ▼       ▼       ▼       ▼       ▼
Req1    Req2    Req3    Req4    Req5    Req6

delay = 1000ms / requestsPerSecond
```

### BURST
```
Timeline (per client):
0ms     1ms     2ms     3ms     4ms     5ms
 │       │       │       │       │       │
 ▼       ▼       ▼       ▼       ▼       ▼
Req1    Req2    Req3    Req4    Req5    Req6

delay = 0ms (send as fast as possible)
```

### RAMP_UP
```
Timeline (per client):
0ms     200ms   400ms   600ms   800ms   1000ms
 │       │       │       │       │       │
 ▼       ▼       ▼       ▼       ▼       ▼
Req1    Req2    Req3    Req4    Req5    Req6

delay starts at 2000ms / rate, gradually decreases
```

## Thread Safety Mechanisms

### Atomic Operations
```
LongAdder (Counter)
    │
    ├─► Internal: Array of Cell objects
    ├─► Each thread updates different cell
    └─► Sum on read (reduces contention)

AtomicLong (Min/Max)
    │
    ├─► Compare-And-Swap (CAS) loop
    │       do {
    │           current = get()
    │           if (newValue >= current) return
    │       } while (!compareAndSet(current, newValue))
    │
    └─► Lock-free, thread-safe
```

### Concurrent Data Structures
```
ConcurrentHashMap<StatusCode, LongAdder>
    │
    ├─► Segments for fine-grained locking
    ├─► computeIfAbsent() is atomic
    └─► Multiple threads can read/write simultaneously

Collections.synchronizedList(ArrayList<Long>)
    │
    ├─► Synchronized wrapper around ArrayList
    └─── All operations are synchronized
```

## WebClient Architecture

### Non-Blocking HTTP
```
WebClient (Reactive)
    │
    ├─► Netty Event Loop
    │       ├─► Non-blocking I/O
    │       ├─► Event-driven
    │       └─► Single thread handles multiple connections
    │
    ├─► Connection Pool
    │       ├─► Max Connections: 500
    │       ├─► Acquire Timeout: 45s
    │       └─► Connection Reuse
    │
    └─► Reactive Streams (Mono/Flux)
            ├─► Backpressure support
            └─► Asynchronous processing
```

## Data Flow Diagram

```
┌──────────────┐
│ Test Request │
└──────┬───────┘
       │
       ▼
┌──────────────────┐
│ Test Orchestrator│  Creates N Clients
└──────┬───────────┘
       │
       ├─────────────────┬─────────────────┬─────────────┐
       │                 │                 │             │
       ▼                 ▼                 ▼             ▼
  ┌────────┐       ┌────────┐       ┌────────┐   ┌────────┐
  │Client 1│       │Client 2│  ...  │Client N│   │Metrics │
  └────┬───┘       └────┬───┘       └────┬───┘   │Collector│
       │                │                │        └────┬───┘
       │                │                │             │
       ▼                ▼                ▼             │
  ┌─────────────────────────────────────────┐         │
  │         Target API Gateway              │         │
  │         (Rate Limited)                  │         │
  └──────────────────┬──────────────────────┘         │
                     │                                 │
                     │ HTTP Status + Latency           │
                     └─────────────────────────────────┘
                                   │
                                   ▼
                        ┌──────────────────┐
                        │  Test Results    │
                        │  (Aggregated)    │
                        └──────────────────┘
```

## State Management

### Test Lifecycle
```
                    startLoadTest()
                          │
                          ▼
┌─────────────────────────────────────────┐
│            RUNNING                       │
│  ├─ Clients executing                   │
│  ├─ Metrics being collected             │
│  └─ Status queryable in real-time       │
└─────────┬────────────┬──────────────────┘
          │            │
    stopTest()    Duration Exceeded
          │            │
          ▼            ▼
    ┌──────────┐  ┌──────────┐
    │ STOPPED  │  │COMPLETED │
    └──────────┘  └──────────┘
          │            │
          └────┬───────┘
               │
               ▼
       ┌──────────────┐
       │ Final Report │
       │   Available  │
       └──────────────┘
```

## Key Design Patterns

### 1. Builder Pattern
```java
WebClient.builder()
    .baseUrl(...)
    .defaultHeader(...)
    .build()
```

### 2. Strategy Pattern
```java
interface RequestPattern {
    long calculateDelay(int rate);
}

CONSTANT_RATE → 1000 / rate
BURST → 0
RAMP_UP → gradual decrease
```

### 3. Observer Pattern
```java
MetricsCollector observes each request result
Records metrics in thread-safe manner
```

### 4. Command Pattern
```java
CompletableFuture encapsulates each client as executable command
ExecutorService executes commands asynchronously
```

## Performance Characteristics

### Time Complexity
- Record Metric: O(1) - Atomic increment
- Calculate Average: O(1) - Simple division
- Calculate P95: O(n log n) - Requires sorting
- Get Status: O(1) - HashMap lookup

### Space Complexity
- Per Test: O(n) where n = total requests
- Latency List: O(n) - Stores all latencies
- Status Map: O(k) where k = unique status codes
- Client Futures: O(c) where c = concurrency level

---
**Architecture Version:** 1.0  
**Last Updated:** January 16, 2026  
**Status:** Production Ready
