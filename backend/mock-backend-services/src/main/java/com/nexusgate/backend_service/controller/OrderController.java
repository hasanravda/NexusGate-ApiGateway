package com.nexusgate.backend_service.controller;

import com.nexusgate.backend_service.dto.CreateOrderRequest;
import com.nexusgate.backend_service.dto.Order;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock Order Service Controller
 * 
 * Purpose: Simulate order processing for testing gateway behavior.
 * 
 * Features:
 * - In-memory order storage
 * - Prometheus metrics for order creation
 * - Simulated processing delay (100-300ms)
 */
@Slf4j
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final Map<Long, Order> orderStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Counter ordersCreatedCounter;

    public OrderController(MeterRegistry meterRegistry) {
        // Custom metric: Track total orders created
        this.ordersCreatedCounter = Counter.builder("mock.orders.created.total")
                .description("Total number of orders created")
                .tag("service", "order-service")
                .register(meterRegistry);

        // Initialize with some mock data
        initializeMockData();
    }

    private void initializeMockData() {
        createMockOrder("user-001", "Laptop", 1, new BigDecimal("1299.99"));
        createMockOrder("user-002", "Mouse", 2, new BigDecimal("29.99"));
        createMockOrder("user-001", "Keyboard", 1, new BigDecimal("89.99"));
        log.info("Initialized order-service with {} mock orders", orderStore.size());
    }

    private void createMockOrder(String userId, String productName, Integer quantity, BigDecimal totalAmount) {
        Order order = Order.builder()
                .id(idGenerator.getAndIncrement())
                .userId(userId)
                .productName(productName)
                .quantity(quantity)
                .totalAmount(totalAmount)
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .build();
        orderStore.put(order.getId(), order);
    }

    /**
     * GET /orders - List all orders
     */
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        simulateDelay(100, 200);
        log.info("Fetching all orders, count: {}", orderStore.size());
        return ResponseEntity.ok(new ArrayList<>(orderStore.values()));
    }

    /**
     * GET /orders/{id} - Get order by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        simulateDelay(100, 200);
        Order order = orderStore.get(id);
        
        if (order == null) {
            log.warn("Order not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        log.info("Fetched order: {} for user: {}", order.getId(), order.getUserId());
        return ResponseEntity.ok(order);
    }

    /**
     * POST /orders - Create new order
     * Increments custom Prometheus metric
     * Simulates processing delay (100-300ms)
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        // Simulate order processing delay
        simulateDelay(100, 300);
        
        Order order = Order.builder()
                .id(idGenerator.getAndIncrement())
                .userId(request.getUserId())
                .productName(request.getProductName())
                .quantity(request.getQuantity())
                .totalAmount(request.getTotalAmount())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        
        orderStore.put(order.getId(), order);
        
        // Increment Prometheus metric
        ordersCreatedCounter.increment();
        
        log.info("Created order: {} for user: {} - Product: {} (total orders: {})", 
                order.getId(), order.getUserId(), order.getProductName(), orderStore.size());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * GET /orders/user/{userId} - Get orders by user ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUserId(@PathVariable String userId) {
        simulateDelay(120, 250);
        
        List<Order> userOrders = orderStore.values().stream()
                .filter(order -> order.getUserId().equals(userId))
                .toList();
        
        log.info("Fetched {} orders for user: {}", userOrders.size(), userId);
        return ResponseEntity.ok(userOrders);
    }

    /**
     * DELETE /orders/{id} - Cancel order
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        simulateDelay(100, 200);
        Order removed = orderStore.remove(id);
        
        if (removed == null) {
            log.warn("Cannot cancel - order not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        log.info("Cancelled order: {}", removed.getId());
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
