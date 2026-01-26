// Load Testing TypeScript Types and DTOs

export type RequestPattern = "CONSTANT_RATE" | "BURST" | "RAMP_UP";
export type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";
export type TestStatus = "RUNNING" | "COMPLETED" | "STOPPED" | "FAILED";

export interface LoadTestRequest {
  targetKey: string;              // API key to test
  targetEndpoint: string;         // Target URL (e.g., "http://localhost:8081/api/users")
  requestRate: number;            // Requests per second (1-10000)
  durationSeconds: number;        // Test duration in seconds (1-3600)
  concurrencyLevel: number;       // Number of concurrent clients (1-500)
  requestPattern: RequestPattern; // Traffic pattern
  httpMethod: HttpMethod;         // HTTP method
}

export interface StartTestResponse {
  testId: string;
  status: TestStatus;
  message: string;
}

export interface LoadTestStatus {
  testId: string;
  status: TestStatus;
  totalRequests: number | null;
  successfulRequests: number | null;
  rateLimitedRequests: number | null;
  errorRequests: number | null;
  averageLatencyMs: number | null;
  p95LatencyMs: number | null;
  minLatencyMs: number | null;
  maxLatencyMs: number | null;
  requestsPerSecond: number | null;
  successRate: number | null;
  rateLimitRate: number | null;
  targetEndpoint: string;
  configuredRequestRate: number | null;
  concurrencyLevel: number | null;
}

export interface LoadTestResult {
  testId: string;
  status: TestStatus;
  totalRequests: number | null;
  successfulRequests: number | null;
  rateLimitedRequests: number | null;
  errorRequests: number | null;
  averageLatencyMs: number | null;
  p95LatencyMs: number | null;
  minLatencyMs: number | null;
  maxLatencyMs: number | null;
  requestsPerSecond: number | null;
  successRate: number | null;
  rateLimitRate: number | null;
  errorRate: number | null;
  testDurationSeconds: number | null;
  targetEndpoint: string;
  targetKey: string;
  configuredRequestRate: number | null;
  concurrencyLevel: number | null;
  requestPattern: RequestPattern;
  httpMethod: HttpMethod;
  statusCodeDistribution: Record<number, number> | null; // e.g., { 200: 1500, 429: 300, 500: 10 }
  startTime: string | null; // ISO 8601 datetime
  endTime: string | null;   // ISO 8601 datetime
}

export interface TestListItem {
  testId: string;
  status: TestStatus;
  targetEndpoint: string;
  requestRate: number;
  startTime: string;
}

export interface HealthStatus {
  status: string;
}
