# Frontend Integration Prompt: Logs & Violations Dashboard

## 🎯 Objective
Integrate the NexusGate Analytics Service Logs & Violations APIs into the frontend dashboard to provide real-time monitoring and analytics visualization.

---

## 📡 Available Backend APIs

### Base URL
```
http://localhost:8085/analytics
```

### 1. Dashboard Metrics (Overview)
**Endpoint:** `GET /dashboard/metrics`  
**Description:** Get all key metrics in a single API call  
**Response:**
```json
{
  "violationsToday": 2,
  "blockedRequests": 2,
  "averageLatencyMs": 23.5,
  "totalRequests": 4,
  "successRate": 50.0
}
```

**Usage:** Display summary cards at the top of the dashboard

---

### 2. Recent Violations (Paginated)
**Endpoint:** `GET /dashboard/violations/recent?limit=10&page=0`  
**Description:** Get recent rate limit violations with pagination  
**Query Parameters:**
- `limit` (optional, default: 10, max: 100)
- `page` (optional, default: 0)

**Response:**
```json
{
  "content": [
    {
      "id": "19815272-62d8-4716-b54d-a7ebc6d3b77f",
      "apiKey": "nx_test_key",
      "serviceName": "user-service",
      "endpoint": "/api/users",
      "httpMethod": "GET",
      "limitValue": "100/min",
      "actualValue": 150,
      "clientIp": "192.168.1.100",
      "timestamp": "2026-01-24T09:04:00Z"
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "number": 0,
  "size": 10,
  "first": true,
  "last": true
}
```

**Usage:** Display violations table with pagination controls

---

### 3. Violations Count (Today)
**Endpoint:** `GET /dashboard/violations/today/count`  
**Response:**
```json
{
  "count": 2
}
```

**Usage:** Show in a metric card or badge

---

### 4. Blocked Requests Count
**Endpoint:** `GET /dashboard/requests/blocked/count`  
**Response:**
```json
{
  "count": 2
}
```

**Usage:** Display blocked/unauthorized requests metric

---

### 5. Average Latency (24h)
**Endpoint:** `GET /dashboard/latency/average`  
**Response:**
```json
{
  "averageLatencyMs": 23.5,
  "period": "24h"
}
```

**Usage:** Show average response time metric

---

## 🎨 UI Components to Build

### 1. **Dashboard Overview (Top Section)**
Create 5 metric cards displaying:

```tsx
// Example component structure
<MetricsOverview>
  <MetricCard 
    title="Violations Today"
    value={violationsToday}
    icon={<AlertIcon />}
    color="red"
  />
  <MetricCard 
    title="Blocked Requests"
    value={blockedRequests}
    icon={<BlockIcon />}
    color="orange"
  />
  <MetricCard 
    title="Average Latency"
    value={`${averageLatencyMs}ms`}
    icon={<ClockIcon />}
    color="blue"
  />
  <MetricCard 
    title="Total Requests (24h)"
    value={totalRequests}
    icon={<ActivityIcon />}
    color="green"
  />
  <MetricCard 
    title="Success Rate"
    value={`${successRate}%`}
    icon={<CheckIcon />}
    color="green"
  />
</MetricsOverview>
```

**Data Source:** `GET /dashboard/metrics`

---

### 2. **Violations Table (Main Content)**
Create a data table with pagination showing:

**Columns:**
- Timestamp (formatted: "Jan 24, 2026 09:04:00")
- API Key (truncate: "nx_test_...key")
- Service Name
- Endpoint
- HTTP Method (badge style)
- Limit Value
- Actual Value (highlight if > limit)
- Client IP

**Features:**
- Pagination controls (10, 25, 50, 100 per page)
- Auto-refresh every 30 seconds
- Sortable columns (timestamp, service name)
- Search/filter by API key or service name
- Export to CSV button

**Data Source:** `GET /dashboard/violations/recent?limit={limit}&page={page}`

---

### 3. **Real-time Updates**
Implement polling mechanism:
```typescript
// Poll every 30 seconds
useEffect(() => {
  const interval = setInterval(() => {
    fetchDashboardMetrics();
    fetchRecentViolations();
  }, 30000); // 30 seconds
  
  return () => clearInterval(interval);
}, []);
```

---

## 💻 Implementation Guide

### Step 1: Create API Service Layer

```typescript
// services/analyticsService.ts

const BASE_URL = 'http://localhost:8085/analytics';

export const analyticsApi = {
  // Get all dashboard metrics
  getDashboardMetrics: async () => {
    const response = await fetch(`${BASE_URL}/dashboard/metrics`);
    if (!response.ok) throw new Error('Failed to fetch metrics');
    return response.json();
  },

  // Get recent violations with pagination
  getRecentViolations: async (limit = 10, page = 0) => {
    const response = await fetch(
      `${BASE_URL}/dashboard/violations/recent?limit=${limit}&page=${page}`
    );
    if (!response.ok) throw new Error('Failed to fetch violations');
    return response.json();
  },

  // Get violations count today
  getViolationsCountToday: async () => {
    const response = await fetch(`${BASE_URL}/dashboard/violations/today/count`);
    if (!response.ok) throw new Error('Failed to fetch violations count');
    return response.json();
  },

  // Get blocked requests count
  getBlockedRequestsCount: async () => {
    const response = await fetch(`${BASE_URL}/dashboard/requests/blocked/count`);
    if (!response.ok) throw new Error('Failed to fetch blocked requests');
    return response.json();
  },

  // Get average latency
  getAverageLatency: async () => {
    const response = await fetch(`${BASE_URL}/dashboard/latency/average`);
    if (!response.ok) throw new Error('Failed to fetch latency');
    return response.json();
  },
};
```

---

### Step 2: Create Dashboard Page Component

```typescript
// pages/LogsAndViolationsDashboard.tsx

import { useEffect, useState } from 'react';
import { analyticsApi } from '@/services/analyticsService';

interface DashboardMetrics {
  violationsToday: number;
  blockedRequests: number;
  averageLatencyMs: number;
  totalRequests: number;
  successRate: number;
}

interface Violation {
  id: string;
  apiKey: string;
  serviceName: string;
  endpoint: string;
  httpMethod: string;
  limitValue: string;
  actualValue: number;
  clientIp: string;
  timestamp: string;
}

export default function LogsAndViolationsDashboard() {
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [violations, setViolations] = useState<Violation[]>([]);
  const [page, setPage] = useState(0);
  const [limit, setLimit] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Fetch dashboard metrics
  const fetchMetrics = async () => {
    try {
      const data = await analyticsApi.getDashboardMetrics();
      setMetrics(data);
      setError(null);
    } catch (err) {
      setError('Failed to load dashboard metrics');
      console.error(err);
    }
  };

  // Fetch violations
  const fetchViolations = async () => {
    try {
      const data = await analyticsApi.getRecentViolations(limit, page);
      setViolations(data.content);
      setTotalPages(data.totalPages);
      setError(null);
    } catch (err) {
      setError('Failed to load violations');
      console.error(err);
    }
  };

  // Initial load
  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      await Promise.all([fetchMetrics(), fetchViolations()]);
      setLoading(false);
    };
    loadData();
  }, [page, limit]);

  // Auto-refresh every 30 seconds
  useEffect(() => {
    const interval = setInterval(() => {
      fetchMetrics();
      fetchViolations();
    }, 30000);

    return () => clearInterval(interval);
  }, [page, limit]);

  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={error} />;

  return (
    <div className="dashboard-container">
      <h1>Logs & Violations Dashboard</h1>
      
      {/* Metrics Overview */}
      <MetricsGrid metrics={metrics} />
      
      {/* Violations Table */}
      <ViolationsTable
        violations={violations}
        page={page}
        limit={limit}
        totalPages={totalPages}
        onPageChange={setPage}
        onLimitChange={setLimit}
      />
    </div>
  );
}
```

---

### Step 3: Create Metric Cards Component

```typescript
// components/MetricsGrid.tsx

interface MetricsGridProps {
  metrics: DashboardMetrics | null;
}

export function MetricsGrid({ metrics }: MetricsGridProps) {
  if (!metrics) return null;

  return (
    <div className="grid grid-cols-1 md:grid-cols-5 gap-4 mb-8">
      <MetricCard
        title="Violations Today"
        value={metrics.violationsToday}
        color="red"
        icon="⚠️"
      />
      <MetricCard
        title="Blocked Requests"
        value={metrics.blockedRequests}
        color="orange"
        icon="🚫"
      />
      <MetricCard
        title="Avg Latency"
        value={`${metrics.averageLatencyMs.toFixed(1)}ms`}
        color="blue"
        icon="⏱️"
      />
      <MetricCard
        title="Total Requests (24h)"
        value={metrics.totalRequests}
        color="green"
        icon="📊"
      />
      <MetricCard
        title="Success Rate"
        value={`${metrics.successRate.toFixed(1)}%`}
        color="green"
        icon="✅"
      />
    </div>
  );
}
```

---

### Step 4: Create Violations Table Component

```typescript
// components/ViolationsTable.tsx

interface ViolationsTableProps {
  violations: Violation[];
  page: number;
  limit: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  onLimitChange: (limit: number) => void;
}

export function ViolationsTable({
  violations,
  page,
  limit,
  totalPages,
  onPageChange,
  onLimitChange,
}: ViolationsTableProps) {
  return (
    <div className="violations-table-container">
      <div className="table-header">
        <h2>Recent Rate Limit Violations</h2>
        <div className="table-controls">
          <select value={limit} onChange={(e) => onLimitChange(Number(e.target.value))}>
            <option value={10}>10 per page</option>
            <option value={25}>25 per page</option>
            <option value={50}>50 per page</option>
            <option value={100}>100 per page</option>
          </select>
        </div>
      </div>

      <table className="violations-table">
        <thead>
          <tr>
            <th>Timestamp</th>
            <th>API Key</th>
            <th>Service</th>
            <th>Endpoint</th>
            <th>Method</th>
            <th>Limit</th>
            <th>Actual</th>
            <th>Client IP</th>
          </tr>
        </thead>
        <tbody>
          {violations.map((violation) => (
            <tr key={violation.id}>
              <td>{new Date(violation.timestamp).toLocaleString()}</td>
              <td className="truncate">{truncateApiKey(violation.apiKey)}</td>
              <td>{violation.serviceName}</td>
              <td>{violation.endpoint}</td>
              <td>
                <span className={`method-badge ${violation.httpMethod.toLowerCase()}`}>
                  {violation.httpMethod}
                </span>
              </td>
              <td>{violation.limitValue}</td>
              <td className="text-red-600 font-bold">{violation.actualValue}</td>
              <td>{violation.clientIp}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Pagination Controls */}
      <div className="pagination">
        <button
          disabled={page === 0}
          onClick={() => onPageChange(page - 1)}
        >
          Previous
        </button>
        <span>
          Page {page + 1} of {totalPages}
        </span>
        <button
          disabled={page >= totalPages - 1}
          onClick={() => onPageChange(page + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
}

function truncateApiKey(apiKey: string): string {
  if (apiKey.length <= 12) return apiKey;
  return `${apiKey.substring(0, 8)}...${apiKey.substring(apiKey.length - 4)}`;
}
```

---

## 🎨 Styling Recommendations

### Color Scheme
```css
/* Metric card colors */
.metric-red { background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%); }
.metric-orange { background: linear-gradient(135deg, #fed7aa 0%, #fdba74 100%); }
.metric-blue { background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%); }
.metric-green { background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%); }

/* HTTP method badges */
.method-badge.get { background: #10b981; color: white; }
.method-badge.post { background: #3b82f6; color: white; }
.method-badge.put { background: #f59e0b; color: white; }
.method-badge.delete { background: #ef4444; color: white; }
```

---

## 🔄 Data Flow

```
┌─────────────────┐
│   Frontend      │
│   Dashboard     │
└────────┬────────┘
         │
         │ HTTP GET Requests
         │ (every 30 seconds)
         ▼
┌─────────────────────────┐
│  Analytics Service      │
│  Port: 8085             │
│                         │
│  Controllers:           │
│  - DashboardAnalytics   │
│  - LogIngestion         │
└────────┬────────────────┘
         │
         │ JPA Queries
         ▼
┌─────────────────────────┐
│  PostgreSQL Database    │
│                         │
│  Tables:                │
│  - request_logs         │
│  - rate_limit_violations│
└─────────────────────────┘
```

---

## 🧪 Testing Checklist

### Manual Testing
- [ ] Metrics cards display correct values
- [ ] Violations table shows data with proper formatting
- [ ] Pagination works (next, previous, page size)
- [ ] Auto-refresh updates data every 30 seconds
- [ ] Timestamps are displayed in local timezone
- [ ] API keys are truncated for security
- [ ] HTTP method badges have correct colors
- [ ] Table is responsive on mobile devices
- [ ] Error states display properly
- [ ] Loading states show spinner

### API Testing
```bash
# Test all endpoints
curl http://localhost:8085/analytics/dashboard/metrics
curl http://localhost:8085/analytics/dashboard/violations/recent?limit=10&page=0
curl http://localhost:8085/analytics/dashboard/violations/today/count
curl http://localhost:8085/analytics/dashboard/requests/blocked/count
curl http://localhost:8085/analytics/dashboard/latency/average
```

---

## 📊 Sample Dashboard Layout

```
┌─────────────────────────────────────────────────────────────┐
│  Logs & Violations Dashboard                    [Refresh]   │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───┐│
│  │ ⚠️  2    │ │ 🚫  2    │ │ ⏱️ 23.5ms│ │ 📊  4    │ │✅ │ │
│  │Violations│ │ Blocked  │ │Avg Latency│ │Total Req│ │50%│ │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └───┘│
│                                                               │
├─────────────────────────────────────────────────────────────┤
│  Recent Rate Limit Violations              [10 per page ▼] │
├─────────────────────────────────────────────────────────────┤
│ Timestamp      │API Key    │Service │Endpoint│Method│Limit │
│────────────────┼───────────┼────────┼────────┼──────┼──────│
│ Jan 24, 09:04  │nx_test... │user-svc│/users  │ GET  │100/m │
│ Jan 24, 09:03  │nx_prod... │auth-svc│/login  │ POST │50/m  │
│                                                               │
├─────────────────────────────────────────────────────────────┤
│              [Previous]  Page 1 of 1  [Next]                │
└─────────────────────────────────────────────────────────────┘
```

---

## 🚀 Additional Features (Optional)

### 1. Charts & Visualizations
- Line chart: Violations over time (hourly)
- Bar chart: Top violating API keys
- Pie chart: Violations by service

### 2. Filters
- Date range picker (last hour, 24h, 7 days)
- Service name filter
- API key search
- HTTP method filter

### 3. Export
- Export violations to CSV
- Download full report PDF

### 4. Alerts
- Browser notifications for high violation rates
- Email alerts (backend integration)

---

## ✅ Success Criteria

Your integration is complete when:
1. ✅ All 5 metric cards display real data from APIs
2. ✅ Violations table shows paginated data
3. ✅ Auto-refresh works (30-second interval)
4. ✅ Pagination controls work correctly
5. ✅ Error handling displays user-friendly messages
6. ✅ Loading states prevent empty UI flashes
7. ✅ Responsive design works on mobile/tablet
8. ✅ No console errors or warnings

---

## 📚 Reference Documentation

- **API Documentation:** `/backend/Analytics-service/LOGS_VIOLATIONS_API.md`
- **Architecture:** `/backend/Analytics-service/ARCHITECTURE.md`
- **Testing Guide:** Run `curl` commands above to verify endpoints

---

## 🐛 Troubleshooting

### Issue: CORS Errors
**Solution:** Analytics service has `@CrossOrigin(origins = "*")` enabled. If issues persist, check browser console for specific error.

### Issue: Empty Data
**Solution:** 
1. Verify Analytics service is running: `curl http://localhost:8085/actuator/health`
2. Test API endpoints directly with curl
3. Check if data exists in PostgreSQL: `SELECT COUNT(*) FROM rate_limit_violations;`

### Issue: Auto-refresh Not Working
**Solution:** Check browser console for interval cleanup issues. Ensure `useEffect` cleanup function is called properly.

---

## 🎯 Next Steps After Integration

1. Add filtering/search functionality
2. Implement time-series charts
3. Add export to CSV/PDF
4. Create alert notifications
5. Add drill-down details modal for violations

---

**Good luck with the integration! 🚀**
