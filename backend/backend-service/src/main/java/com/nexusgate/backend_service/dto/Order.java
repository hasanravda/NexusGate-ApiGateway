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
public class Order {
    private Long id;
    private String userId;
    private String productName;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;
}
