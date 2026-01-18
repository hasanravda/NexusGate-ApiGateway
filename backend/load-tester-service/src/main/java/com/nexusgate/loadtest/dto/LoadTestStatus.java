package com.nexusgate.loadtest.dto;

/**
 * Represents the lifecycle status of a load test execution.
 */
public enum LoadTestStatus {
    RUNNING,    // Test is currently executing
    COMPLETED,  // Test finished normally
    STOPPED,    // Test was manually stopped
    FAILED      // Test failed due to error
}
