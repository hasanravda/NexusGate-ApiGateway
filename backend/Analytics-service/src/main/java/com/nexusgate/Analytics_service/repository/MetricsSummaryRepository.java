package com.nexusgate.Analytics_service.repository;

import com.nexusgate.Analytics_service.model.MetricsSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MetricsSummaryRepository extends JpaRepository<MetricsSummary, Long> {

    /**
     * Find summary by date and service route
     */
    Optional<MetricsSummary> findByDateAndServiceRouteId(LocalDate date, Long serviceRouteId);

    /**
     * Find summaries for a specific date
     */
    List<MetricsSummary> findByDate(LocalDate date);

    /**
     * Find summaries within a date range
     */
    List<MetricsSummary> findByDateBetween(LocalDate startDate, LocalDate endDate);
}
