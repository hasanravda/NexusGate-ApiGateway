package com.nexusgate.backend_service.controller;

import com.nexusgate.backend_service.dto.Payment;
import com.nexusgate.backend_service.dto.ProcessPaymentRequest;
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
 * Mock Payment Service Controller
 * 
 * Purpose: Simulate payment processing with realistic delays and random failures.
 * 
 * Features:
 * - In-memory payment storage
 * - Prometheus metrics for successful and failed payments
 * - Simulated processing delay (300-700ms)
 * - Random failure simulation (~10%)
 */
@Slf4j
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final Map<Long, Payment> paymentStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Counter successfulPaymentsCounter;
    private final Counter failedPaymentsCounter;

    // Simulate 10% failure rate
    private static final double FAILURE_RATE = 0.10;

    public PaymentController(MeterRegistry meterRegistry) {
        // Custom metric: Track successful payments
        this.successfulPaymentsCounter = Counter.builder("mock.payments.success.total")
                .description("Total number of successful payments")
                .tag("service", "payment-service")
                .tag("status", "success")
                .register(meterRegistry);

        // Custom metric: Track failed payments
        this.failedPaymentsCounter = Counter.builder("mock.payments.failed.total")
                .description("Total number of failed payments")
                .tag("service", "payment-service")
                .tag("status", "failed")
                .register(meterRegistry);

        // Initialize with some mock data
        initializeMockData();
    }

    private void initializeMockData() {
        createMockPayment("order-001", "CREDIT_CARD", true);
        createMockPayment("order-002", "DEBIT_CARD", true);
        createMockPayment("order-003", "UPI", false);
        log.info("Initialized payment-service with {} mock payments", paymentStore.size());
    }

    private void createMockPayment(String orderId, String paymentMethod, boolean success) {
        Payment payment = Payment.builder()
                .id(idGenerator.getAndIncrement())
                .orderId(orderId)
                .amount(java.math.BigDecimal.valueOf(Math.random() * 1000 + 100))
                .status(success ? "SUCCESS" : "FAILED")
                .paymentMethod(paymentMethod)
                .failureReason(success ? null : "Insufficient funds")
                .createdAt(LocalDateTime.now())
                .build();
        paymentStore.put(payment.getId(), payment);
    }

    /**
     * GET /payments - List all payments
     */
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        simulateDelay(300, 500);
        log.info("Fetching all payments, count: {}", paymentStore.size());
        return ResponseEntity.ok(new ArrayList<>(paymentStore.values()));
    }

    /**
     * GET /payments/{id} - Get payment by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        simulateDelay(300, 500);
        Payment payment = paymentStore.get(id);
        
        if (payment == null) {
            log.warn("Payment not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        log.info("Fetched payment: {} - Status: {}", payment.getId(), payment.getStatus());
        return ResponseEntity.ok(payment);
    }

    /**
     * POST /payments - Process payment
     * 
     * Features:
     * - Increments success or failure Prometheus metrics
     * - Simulates processing delay (300-700ms)
     * - Random 10% failure rate
     */
    @PostMapping
    public ResponseEntity<Payment> processPayment(@RequestBody ProcessPaymentRequest request) {
        // Simulate payment processing delay (300-700ms)
        simulateDelay(300, 700);
        
        // Simulate random payment failure (~10%)
        boolean isSuccessful = Math.random() > FAILURE_RATE;
        
        Payment payment = Payment.builder()
                .id(idGenerator.getAndIncrement())
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .status(isSuccessful ? "SUCCESS" : "FAILED")
                .paymentMethod(request.getPaymentMethod())
                .failureReason(isSuccessful ? null : getRandomFailureReason())
                .createdAt(LocalDateTime.now())
                .build();
        
        paymentStore.put(payment.getId(), payment);
        
        // Increment appropriate Prometheus metric
        if (isSuccessful) {
            successfulPaymentsCounter.increment();
            log.info("Payment processed successfully: {} for order: {} - Amount: {}", 
                    payment.getId(), payment.getOrderId(), payment.getAmount());
        } else {
            failedPaymentsCounter.increment();
            log.warn("Payment failed: {} for order: {} - Reason: {}", 
                    payment.getId(), payment.getOrderId(), payment.getFailureReason());
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    /**
     * GET /payments/order/{orderId} - Get payment by order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrderId(@PathVariable String orderId) {
        simulateDelay(300, 600);
        
        Payment payment = paymentStore.values().stream()
                .filter(p -> p.getOrderId().equals(orderId))
                .findFirst()
                .orElse(null);
        
        if (payment == null) {
            log.warn("Payment not found for order: {}", orderId);
            return ResponseEntity.notFound().build();
        }
        
        log.info("Fetched payment for order: {} - Status: {}", orderId, payment.getStatus());
        return ResponseEntity.ok(payment);
    }

    /**
     * Simulate realistic payment processing delay
     */
    private void simulateDelay(int minMs, int maxMs) {
        try {
            int delay = minMs + (int) (Math.random() * (maxMs - minMs));
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Generate random failure reasons for failed payments
     */
    private String getRandomFailureReason() {
        String[] reasons = {
            "Insufficient funds",
            "Card declined",
            "Payment timeout",
            "Invalid card details",
            "Bank server unavailable"
        };
        return reasons[(int) (Math.random() * reasons.length)];
    }
}
