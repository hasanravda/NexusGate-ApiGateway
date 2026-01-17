package com.nexusgate.load_tester_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Load Testing Service Application
 * 
 * Main entry point for the Load Testing microservice.
 * This service simulates high traffic to test API gateway rate limiting behavior.
 * 
 * Key Features:
 * - Generate concurrent load with multiple clients
 * - Measure request latency and success rates
 * - Track rate limit enforcement (HTTP 429 responses)
 * - Support various traffic patterns (constant, burst, ramp-up)
 * 
 * Port: 8083
 * 
 * Architecture:
 * - Non-blocking HTTP clients using WebClient
 * - Concurrent execution using CompletableFuture
 * - Thread-safe metrics collection
 * - Asynchronous test orchestration
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.nexusgate.load_tester_service", "com.nexusgate.loadtest"})
public class LoadTesterServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LoadTesterServiceApplication.class, args);
		System.out.println("========================================");
		System.out.println("Load Testing Service Started on Port 8083");
		System.out.println("========================================");
	}

}

