'use client';

import { useEffect, useState } from 'react';
import { analyticsApi } from '@/lib/analyticsApi';
import MetricsGrid from '@/components/MetricsGrid';
import ViolationsTable from '@/components/ViolationsTable';

export default function LogsPage() {
  const [metrics, setMetrics] = useState(null);
  const [violations, setViolations] = useState([]);
  const [page, setPage] = useState(0);
  const [limit, setLimit] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [metricsLoading, setMetricsLoading] = useState(true);
  const [violationsLoading, setViolationsLoading] = useState(true);
  const [metricsError, setMetricsError] = useState(null);
  const [violationsError, setViolationsError] = useState(null);
  const [lastRefresh, setLastRefresh] = useState(new Date());

  // Fetch dashboard metrics
  const fetchMetrics = async () => {
    try {
      setMetricsLoading(true);
      setMetricsError(null);
      const data = await analyticsApi.getDashboardMetrics();
      setMetrics(data);
      setLastRefresh(new Date());
    } catch (err) {
      console.error('Failed to fetch metrics:', err);
      setMetricsError(err.message || 'Failed to load metrics');
    } finally {
      setMetricsLoading(false);
    }
  };

  // Fetch violations
  const fetchViolations = async () => {
    try {
      setViolationsLoading(true);
      setViolationsError(null);
      const data = await analyticsApi.getRecentViolations(limit, page);
      setViolations(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      console.error('Failed to fetch violations:', err);
      setViolationsError(err.message || 'Failed to load violations');
    } finally {
      setViolationsLoading(false);
    }
  };

  // Initial load
  useEffect(() => {
    fetchMetrics();
    fetchViolations();
  }, [page, limit]);

  // Auto-refresh every 30 seconds
  useEffect(() => {
    const interval = setInterval(() => {
      fetchMetrics();
      fetchViolations();
    }, 30000); // 30 seconds

    return () => clearInterval(interval);
  }, [page, limit]);

  // Handle page change
  const handlePageChange = (newPage) => {
    setPage(newPage);
  };

  // Handle limit change
  const handleLimitChange = (newLimit) => {
    setLimit(newLimit);
    setPage(0); // Reset to first page when changing limit
  };

  // Manual refresh
  const handleRefresh = () => {
    fetchMetrics();
    fetchViolations();
  };

  return (
    <div className="p-8">
      <div className="mb-8">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-white mb-2">
              Logs & Violations Dashboard
            </h1>
            <p className="text-slate-400">
              Real-time monitoring of rate limit violations and API gateway activity
            </p>
          </div>
          <div className="text-sm text-slate-400">
            Last updated: {lastRefresh.toLocaleTimeString()}
          </div>
        </div>
      </div>

      {/* Metrics Overview */}
      <MetricsGrid 
        metrics={metrics} 
        loading={metricsLoading} 
        error={metricsError} 
      />

      {/* Info Banner */}
      {!metricsError && !violationsError && (
        <div className="mb-6 p-4 bg-blue-900/30 border border-blue-700 rounded-lg">
          <p className="text-sm text-blue-300">
            <strong>🔄 Auto-refresh enabled:</strong> Dashboard updates every 30 seconds. 
            Data is fetched from Analytics Service (localhost:8085).
          </p>
        </div>
      )}

      {/* Violations Table */}
      <ViolationsTable
        violations={violations}
        page={page}
        limit={limit}
        totalPages={totalPages}
        totalElements={totalElements}
        loading={violationsLoading}
        error={violationsError}
        onPageChange={handlePageChange}
        onLimitChange={handleLimitChange}
        onRefresh={handleRefresh}
      />
    </div>
  );
}
