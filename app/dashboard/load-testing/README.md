# Load Testing Dashboard 🚀

A comprehensive, modern load testing dashboard for the NexusGate API Gateway built with Next.js 14, shadcn/ui, and TypeScript.

## ✨ Features

### 🎯 Core Functionality
- **Create Load Tests**: Intuitive dialog with all configuration options
- **Real-time Monitoring**: Auto-updating status for running tests (every 3 seconds)
- **Detailed Metrics**: Comprehensive performance analytics and charts
- **Test History**: Searchable, filterable, and sortable table of all tests
- **Export Results**: Download test results as JSON
- **Stop Running Tests**: Cancel tests in progress

### 🎨 UI/UX Highlights
- **Modern Design**: Clean, professional interface using shadcn/ui components
- **Responsive**: Works perfectly on desktop, tablet, and mobile
- **Real-time Updates**: Live indicators and auto-refresh for active tests
- **Status Visualization**: Color-coded badges and progress bars
- **Interactive Charts**: Status code distribution bar charts
- **Toast Notifications**: User-friendly feedback for all actions

### 📊 Metrics Displayed
- Total Requests
- Success Rate (with percentage)
- Rate Limited Requests (429 responses)
- Error Requests
- Average Latency
- P95 Latency
- Min/Max Latency
- Requests per Second
- Status Code Distribution

## 🗂️ Project Structure

```
app/dashboard/load-testing/
├── page.tsx                          # Main dashboard page
├── api/
│   └── loadTester.ts                 # API client functions
├── components/
│   ├── CreateTestDialog.tsx          # Test creation form
│   ├── TestCard.tsx                  # Individual test display card
│   ├── TestDetailsModal.tsx          # Detailed metrics modal
│   ├── TestHistoryTable.tsx          # History table with filters
│   ├── StatusBadge.tsx               # Status indicator component
│   └── MetricsCard.tsx               # Reusable metrics display
├── hooks/
│   ├── useLoadTests.ts               # Hook for managing test list
│   └── useTestStatus.ts              # Hook for real-time updates
└── types/
    └── loadTester.types.ts           # TypeScript type definitions
```

## 🔌 API Integration

The dashboard connects to the Load Tester Service at `http://localhost:8083/load-test`

### Endpoints Used:
- `POST /load-test/start` - Start a new test
- `GET /load-test/status/{testId}` - Get real-time status
- `GET /load-test/result/{testId}` - Get final results
- `DELETE /load-test/stop/{testId}` - Stop running test
- `GET /load-test/list` - List all tests
- `GET /load-test/health` - Health check

## 🎯 Usage Guide

### Creating a Load Test

1. Click **"New Load Test"** button
2. Fill in the configuration:
   - **Target Endpoint**: Gateway URL to test (e.g., `http://localhost:8081/api/users`)
   - **API Key**: Authentication key (e.g., `nx_test_key_123`)
   - **Request Rate**: 1-10,000 requests/second (use slider or input)
   - **Duration**: 1-3600 seconds
   - **Concurrency Level**: 1-500 parallel clients
   - **Request Pattern**: Choose from:
     - ⚡ **Constant Rate**: Steady load
     - 💥 **Burst**: Maximum throughput
     - 📈 **Ramp Up**: Gradually increasing
   - **HTTP Method**: GET, POST, PUT, or DELETE

3. **Quick Presets** (optional):
   - **Light Test**: 10 req/s for 30s
   - **Standard Test**: 50 req/s for 60s
   - **Stress Test**: 500 req/s for 30s

4. Click **"Start Test"**

### Monitoring Active Tests

- **Active Tests** section shows running tests with:
  - Live status updates every 3 seconds
  - Progress bar
  - Real-time metrics
  - Quick actions (View Details, Stop)

### Viewing Test Details

1. Click **"View Details"** on any test card
2. See comprehensive metrics:
   - Request breakdown (successful, rate-limited, errors)
   - Performance metrics (latency statistics)
   - Throughput metrics
   - Status code distribution chart
   - Full configuration details

### Managing Test History

The **Test History** table provides:
- **Search**: Filter by test ID or endpoint
- **Status Filter**: Show only running/completed/failed tests
- **Sorting**: Click column headers to sort
- **Pagination**: 10, 25, or 50 tests per page
- **Actions Menu**: View, Export, or Delete tests

## 🎨 Design System

### Colors
- **Success**: Green (200 status, completed tests)
- **Warning**: Yellow (429 rate limited)
- **Error**: Red (5xx errors, failed tests)
- **Running**: Amber with pulse animation
- **Neutral**: Gray (stopped tests)

### Components Used
- Card, Button, Dialog, Input, Label
- Select, Slider, Badge, Table, Tabs
- Accordion, Toast, Progress, Skeleton
- Dropdown Menu, Separator

### Animations
- Pulse effect for running tests
- Smooth progress bar transitions
- Fade in/out for modals
- Skeleton loaders for data fetching

## 🔧 Configuration

### API Base URL
To change the Load Tester Service URL, edit:
```typescript
// app/dashboard/load-testing/api/loadTester.ts
const API_BASE = "http://localhost:8083/load-test";
```

### Auto-refresh Intervals
```typescript
// Main page - test list refresh
useLoadTests(true, 5000); // 5 seconds

// Test status - real-time updates
useTestStatus(testId, true); // 3 seconds for running tests
```

## 📱 Responsive Design

- **Mobile**: Stacked layout, single column cards
- **Tablet**: 2-column grid for metrics
- **Desktop**: 3-4 column grid, full table view

## 🚦 Status Indicators

| Status | Color | Icon | Description |
|--------|-------|------|-------------|
| RUNNING | Amber | Spinner | Test in progress |
| COMPLETED | Green | Checkmark | Successfully finished |
| FAILED | Red | X Circle | Test failed |
| STOPPED | Gray | Stop Circle | Manually stopped |

## 💡 Tips & Best Practices

1. **Start Small**: Use "Light Test" preset first to verify configuration
2. **Monitor Health**: Check the service status indicator in the header
3. **Real-time Data**: Running tests update automatically - no need to refresh
4. **Export Results**: Download JSON for further analysis or reporting
5. **Rate Limiting**: Watch the 429 responses to tune your rate limits
6. **Latency Analysis**: P95 latency is key for understanding worst-case performance

## 🐛 Troubleshooting

### Service Offline
- Check if Load Tester Service is running on port 8083
- Verify network connectivity
- Check browser console for CORS errors

### Tests Not Appearing
- Click "Refresh Status" button
- Check browser console for API errors
- Verify Load Tester Service is returning data

### Real-time Updates Not Working
- Running tests auto-update every 3 seconds
- Check if test status is actually "RUNNING"
- Refresh the page if updates stop

## 🎯 Future Enhancements

Potential features to add:
- [ ] Scheduled/recurring tests
- [ ] Test comparison view
- [ ] Advanced filtering and search
- [ ] Test templates/favorites
- [ ] Webhook notifications
- [ ] Multi-endpoint tests
- [ ] Request body configuration
- [ ] Custom headers support
- [ ] Dark mode toggle
- [ ] CSV export option

## 📄 License

Part of the NexusGate API Gateway project.

---

**Built with ❤️ using Next.js, shadcn/ui, TypeScript, and Recharts**
