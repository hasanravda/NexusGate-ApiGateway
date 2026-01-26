# 🔧 CORS Configuration Fix

## Problem
Your Load Tester Service is running, but the browser blocks requests due to CORS (Cross-Origin Resource Sharing) policy.

## Solution: Add CORS to Your Load Tester Service

### For Spring Boot (Java)

Add this configuration class to your Load Tester Service:

```java
package com.example.loadtester.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:3001")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

**OR** add this to your `application.properties` / `application.yml`:

```yaml
# application.yml
spring:
  web:
    cors:
      allowed-origins: "http://localhost:3000"
      allowed-methods: "*"
      allowed-headers: "*"
      allow-credentials: true
```

**OR** add `@CrossOrigin` annotation to your controller:

```java
@RestController
@RequestMapping("/load-test")
@CrossOrigin(origins = "http://localhost:3000")
public class LoadTestController {
    // Your existing code
}
```

### For Node.js/Express

```javascript
const express = require('express');
const cors = require('cors');

const app = express();

// Enable CORS
app.use(cors({
  origin: 'http://localhost:3000',
  credentials: true
}));

// Your existing routes
```

### For .NET Core

```csharp
// In Program.cs or Startup.cs
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowFrontend",
        policy =>
        {
            policy.WithOrigins("http://localhost:3000")
                  .AllowAnyHeader()
                  .AllowAnyMethod()
                  .AllowCredentials();
        });
});

app.UseCors("AllowFrontend");
```

## After Adding CORS:

1. **Restart your Load Tester Service**
2. **Refresh the browser** at `http://localhost:3000/dashboard/load-testing`
3. **Click "Refresh Status"**

You should now see "Service Online" ✅

## Quick Test

After adding CORS, test with:

```bash
# In browser console (F12 → Console), paste:
fetch('http://localhost:8083/load-test/health')
  .then(r => r.json())
  .then(data => console.log('Success:', data))
  .catch(err => console.error('Error:', err));
```

If you see `Success: {status: "UP"}` - it's working! 🎉
