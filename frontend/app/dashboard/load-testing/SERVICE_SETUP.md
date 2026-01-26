# 🚨 Load Tester Service Setup

## Current Issue

The dashboard is showing **"Failed to fetch"** errors because the Load Tester Service is not running.

---

## ✅ Solution: Start the Load Tester Service

The Load Testing Dashboard requires the **Load Tester Service** to be running on **port 8083**.

### Step 1: Locate Your Load Tester Service

Find your Load Tester Service project (usually a Spring Boot or similar backend service).

### Step 2: Start the Service

```bash
# Navigate to your Load Tester Service directory
cd path/to/load-tester-service

# Start the service (adjust command based on your setup)
# For Spring Boot:
./mvnw spring-boot:run
# OR
java -jar target/load-tester-service.jar

# For npm/node:
npm start

# For gradle:
./gradlew bootRun
```

### Step 3: Verify the Service is Running

Open a new terminal and test the health endpoint:

```bash
curl http://localhost:8083/load-test/health
```

**Expected Response:**
```json
{
  "status": "UP"
}
```

### Step 4: Refresh the Dashboard

Once the service is running:
1. Go back to your browser at `http://localhost:3000/dashboard/load-testing`
2. Click the **"Refresh Status"** button
3. The health indicator should turn **green** and show "Service Online"

---

## 📊 Service Requirements

The Load Tester Service must expose these endpoints:

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/load-test/start` | Start new load test |
| GET | `/load-test/status/{testId}` | Get real-time status |
| GET | `/load-test/result/{testId}` | Get final results |
| DELETE | `/load-test/stop/{testId}` | Stop running test |
| GET | `/load-test/list` | List all tests |
| GET | `/load-test/health` | Health check |

---

## 🐛 Troubleshooting

### Issue: "Connection Refused"
**Cause:** Service not running  
**Fix:** Start the Load Tester Service (see Step 2)

### Issue: "Service Offline" indicator
**Cause:** Service not responding on port 8083  
**Fix:** 
1. Check if port 8083 is in use by another service
2. Verify service configuration matches port 8083
3. Check firewall settings

### Issue: CORS Errors
**Cause:** Service not allowing requests from localhost:3000  
**Fix:** Add CORS configuration to your Load Tester Service:

```java
// Spring Boot example
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowedOrigins("http://localhost:3000")
                    .allowedMethods("GET", "POST", "PUT", "DELETE");
            }
        };
    }
}
```

### Issue: Port Already in Use
**Cause:** Another application is using port 8083  
**Fix:**
1. Find and stop the other application
2. OR change the service port in both:
   - Load Tester Service configuration
   - Dashboard API client (`app/dashboard/load-testing/api/loadTester.ts`)

---

## ✅ What I Fixed in the Dashboard

To improve your experience, I made these changes:

1. **Disabled Auto-Refresh** - Stops flickering when service is offline
2. **Better Error Messages** - Clear instructions when service is unavailable  
3. **Service Health Check** - Only refreshes when service is healthy
4. **Disabled Buttons** - "New Load Test" button disabled when service is offline
5. **Clear Status Indicator** - Shows "Service Offline" with alert icon
6. **Helpful Error Screen** - Displays setup instructions when service is unavailable

---

## 🎯 Quick Start Checklist

- [ ] Load Tester Service is running on port 8083
- [ ] Health endpoint responds: `curl http://localhost:8083/load-test/health`
- [ ] Dashboard shows "Service Online" (green indicator)
- [ ] "New Load Test" button is enabled
- [ ] Test History loads without errors

---

## 📞 Still Having Issues?

Make sure:
1. ✅ Load Tester Service is compiled and built
2. ✅ All service dependencies are installed
3. ✅ Service is configured for port 8083
4. ✅ No firewall blocking localhost connections
5. ✅ CORS is configured to allow localhost:3000

Once the service is running, everything will work perfectly! 🚀
