package com.nexusgate.backend_service.controller;

import com.nexusgate.backend_service.dto.CreateUserRequest;
import com.nexusgate.backend_service.dto.User;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock User Service Controller
 * 
 * Purpose: Simulate a real user management service for testing gateway routing,
 * rate limiting, and monitoring.
 * 
 * Features:
 * - In-memory user storage (no database)
 * - Prometheus metrics for user creation
 * - Predictable delays (50-200ms) for realistic behavior
 */
@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final Map<Long, User> userStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Counter usersCreatedCounter;

    public UserController(MeterRegistry meterRegistry) {
        // Custom metric: Track total users created
        this.usersCreatedCounter = Counter.builder("mock.users.created.total")
                .description("Total number of users created")
                .tag("service", "user-service")
                .register(meterRegistry);

        // Initialize with some mock data
        initializeMockData();
    }

    private void initializeMockData() {
        createMockUser("admin@nexusgate.com", "Admin User", "ADMIN");
        createMockUser("john.doe@example.com", "John Doe", "USER");
        createMockUser("jane.smith@example.com", "Jane Smith", "USER");
        log.info("Initialized user-service with {} mock users", userStore.size());
    }

    private void createMockUser(String email, String fullName, String role) {
        User user = User.builder()
                .id(idGenerator.getAndIncrement())
                .email(email)
                .fullName(fullName)
                .role(role)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        userStore.put(user.getId(), user);
    }

    /**
     * GET /users - List all users
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        simulateDelay(50, 150);
        log.info("Fetching all users, count: {}", userStore.size());
        return ResponseEntity.ok(new ArrayList<>(userStore.values()));
    }

    /**
     * GET /users/{id} - Get user by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        simulateDelay(50, 100);
        User user = userStore.get(id);
        
        if (user == null) {
            log.warn("User not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        log.info("Fetched user: {}", user.getEmail());
        return ResponseEntity.ok(user);
    }

    /**
     * POST /users - Create new user
     * Increments custom Prometheus metric
     */
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
        simulateDelay(100, 200);
        
        User user = User.builder()
                .id(idGenerator.getAndIncrement())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(request.getRole() != null ? request.getRole() : "USER")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        
        userStore.put(user.getId(), user);
        
        // Increment Prometheus metric
        usersCreatedCounter.increment();
        
        log.info("Created user: {} (total users: {})", user.getEmail(), userStore.size());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * PUT /users/{id} - Update user
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody CreateUserRequest request) {
        simulateDelay(80, 150);
        User existingUser = userStore.get(id);
        
        if (existingUser == null) {
            log.warn("Cannot update - user not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        existingUser.setEmail(request.getEmail());
        existingUser.setFullName(request.getFullName());
        existingUser.setRole(request.getRole());
        
        log.info("Updated user: {}", existingUser.getEmail());
        return ResponseEntity.ok(existingUser);
    }

    /**
     * DELETE /users/{id} - Delete user
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        simulateDelay(60, 120);
        User removed = userStore.remove(id);
        
        if (removed == null) {
            log.warn("Cannot delete - user not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        log.info("Deleted user: {}", removed.getEmail());
        return ResponseEntity.noContent().build();
    }

    /**
     * Simulate realistic processing delay
     */
    private void simulateDelay(int minMs, int maxMs) {
        try {
            int delay = minMs + (int) (Math.random() * (maxMs - minMs));
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
