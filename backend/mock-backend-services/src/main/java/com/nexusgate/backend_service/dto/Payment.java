package com.nexusgate.backend_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    private Long id;
    private String orderId;
    private BigDecimal amount;
    private String status; // SUCCESS, FAILED, PENDING
    private String paymentMethod;
    private String failureReason;
    private LocalDateTime createdAt;
}
