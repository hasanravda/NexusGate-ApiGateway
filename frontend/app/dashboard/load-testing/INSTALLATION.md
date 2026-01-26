# Load Testing Dashboard - Installation & Setup

## 📦 Dependencies

All required dependencies are already included in your package.json:

```json
{
  "lucide-react": "^0.446.0",      // Icons
  "recharts": "^2.12.7",           // Charts
  "date-fns": "^3.6.0",            // Date formatting
  "@radix-ui/react-*": "...",      // shadcn/ui components
  "class-variance-authority": "^0.7.0",
  "tailwind-merge": "^2.5.2",
  "tailwindcss-animate": "^1.0.7"
}
```

## ✅ Installation Status

✅ **All dependencies are already installed!**

No additional installations required. The project already has:
- Lucide React icons
- Recharts for charts
- Date-fns for date formatting
- All Radix UI components for shadcn/ui
- Tailwind CSS with animations

## 🚀 Quick Start

1. **Ensure Load Tester Service is Running**
   ```bash
   # The service should be running on http://localhost:8083
   # Check health endpoint:
   curl http://localhost:8083/load-test/health
   ```

2. **Start the Development Server**
   ```bash
   npm run dev
   ```

3. **Access the Dashboard**
   ```
   http://localhost:3000/dashboard/load-testing
   ```

## 🔗 Navigation

The Load Testing page has been added to the sidebar navigation automatically.

Access it from:
- Sidebar → **Load Testing** (with Activity icon)
- Direct URL: `/dashboard/load-testing`

## 🧪 Testing the Dashboard

### 1. Check Service Health
First, verify the Load Tester Service is running:

```bash
curl http://localhost:8083/load-test/health
```

Expected response:
```json
{
  "status": "UP"
}
```

### 2. Create a Test Load Test
Use the dashboard to create a test:
- Target Endpoint: `http://localhost:8081/api/users`
- API Key: `nx_test_key_123`
- Use "Light Test" preset (10 req/s, 30s)

### 3. Monitor Real-time Updates
Watch the active test card update automatically every 3 seconds.

### 4. View Detailed Results
Click "View Details" to see:
- Comprehensive metrics
- Status code distribution chart
- Configuration details

## 🎨 UI Components Available

All shadcn/ui components used in the dashboard:

- ✅ Card
- ✅ Button
- ✅ Dialog
- ✅ Input
- ✅ Label
- ✅ Select
- ✅ Slider
- ✅ Badge
- ✅ Table
- ✅ Tabs
- ✅ Accordion
- ✅ Toast
- ✅ Progress
- ✅ Skeleton
- ✅ Dropdown Menu
- ✅ Separator

## 🔧 Configuration

### Change API Base URL

Edit `app/dashboard/load-testing/api/loadTester.ts`:

```typescript
const API_BASE = "http://localhost:8083/load-test";
// Change to your Load Tester Service URL
```

### Adjust Auto-refresh Intervals

Edit `app/dashboard/load-testing/page.tsx`:

```typescript
// Test list refresh (default: 5 seconds)
const { tests, loading, error, isHealthy, refresh } = useLoadTests(true, 5000);

// Real-time status updates happen in useTestStatus hook (3 seconds)
```

### Customize Preset Values

Edit `app/dashboard/load-testing/components/CreateTestDialog.tsx`:

```typescript
const presets = {
  light: { requestRate: 10, durationSeconds: 30, concurrencyLevel: 2 },
  standard: { requestRate: 50, durationSeconds: 60, concurrencyLevel: 5 },
  stress: { requestRate: 500, durationSeconds: 30, concurrencyLevel: 20 },
};
```

## 📱 Responsive Breakpoints

The dashboard is responsive at these breakpoints:
- Mobile: < 768px (stacked layout)
- Tablet: 768px - 1024px (2-column grid)
- Desktop: > 1024px (3-4 column grid)

## 🎯 Key Features

✅ Create load tests with attractive input fields
✅ Real-time monitoring with auto-updates
✅ Detailed metrics and charts
✅ Test history with search and filtering
✅ Export results as JSON
✅ Stop running tests
✅ Copy test IDs to clipboard
✅ Service health indicator
✅ Toast notifications
✅ Skeleton loading states
✅ Responsive design

## 📊 API Endpoints Used

All endpoints are implemented in `api/loadTester.ts`:

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/load-test/start` | Start new test |
| GET | `/load-test/status/{id}` | Get real-time status |
| GET | `/load-test/result/{id}` | Get final results |
| DELETE | `/load-test/stop/{id}` | Stop running test |
| GET | `/load-test/list` | List all tests |
| GET | `/load-test/health` | Health check |

## 🐛 Troubleshooting

### Port Already in Use
```bash
# If port 3000 is in use, run on different port
npm run dev -- -p 3001
```

### CORS Issues
If you see CORS errors, the Load Tester Service needs to allow requests from `http://localhost:3000`.

### TypeScript Errors
```bash
# Run type check
npm run typecheck
```

### Missing Components
All shadcn/ui components are already installed. If you see import errors, check:
```bash
# Verify components exist
ls components/ui/
```

## 🎉 Success Criteria

Your dashboard is working correctly if you can:

1. ✅ See the service health indicator as "Online"
2. ✅ Create a new load test using the dialog
3. ✅ See the test appear in "Active Tests" section
4. ✅ Watch real-time metrics update automatically
5. ✅ View detailed results in the modal
6. ✅ See test history in the table
7. ✅ Stop a running test
8. ✅ Export test results as JSON
9. ✅ Search and filter tests
10. ✅ See charts with status code distribution

## 📚 Additional Resources

- [Next.js Documentation](https://nextjs.org/docs)
- [shadcn/ui Documentation](https://ui.shadcn.com)
- [Recharts Documentation](https://recharts.org)
- [Lucide Icons](https://lucide.dev)
- [Tailwind CSS](https://tailwindcss.com)

---

**Ready to Test! 🚀**

Navigate to `http://localhost:3000/dashboard/load-testing` and start creating load tests!
