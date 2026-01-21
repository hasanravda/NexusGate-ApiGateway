package com.nexusgate.Analytics_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * MetricsSummary Entity
 * Stores aggregated metrics for dashboard and reporting
 */
@Entity
@Table(name = "metrics_summary", uniqueConstraints = {
        @UniqueConstraint(name = "uk_date_route", columnNames = {"date", "serviceRouteId"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Long serviceRouteId;

    @Column(nullable = false)
    private Long totalRequests;

    @Column(nullable = false)
    private Long errorCount;

    @Column(nullable = false)
    private Double avgLatencyMs;

    @Column(nullable = false)
    private Long p95LatencyMs;
}
