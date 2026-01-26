// Custom hook for real-time test status updates

import { useState, useEffect, useCallback, useRef } from 'react';
import { LoadTestStatus, LoadTestResult } from '../types/loadTester.types';
import { getTestStatus, getTestResult } from '../api/loadTester';

export function useTestStatus(testId: string | null, autoRefresh: boolean = true) {
  const [status, setStatus] = useState<LoadTestStatus | LoadTestResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const intervalRef = useRef<NodeJS.Timeout | null>(null);

  const fetchStatus = useCallback(async () => {
    if (!testId) return;

    try {
      setError(null);
      setLoading(true);
      
      // Try to get status first (for running tests)
      const statusData = await getTestStatus(testId);
      setStatus(statusData);
      
      // If test is completed, get full results
      if (statusData.status === 'COMPLETED' || statusData.status === 'FAILED' || statusData.status === 'STOPPED') {
        try {
          const resultData = await getTestResult(testId);
          setStatus(resultData);
        } catch (err) {
          // If result endpoint fails, keep the status data
          console.error('Failed to fetch full results:', err);
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch status');
    } finally {
      setLoading(false);
    }
  }, [testId]);

  useEffect(() => {
    if (!testId) {
      setStatus(null);
      return;
    }

    fetchStatus();

    if (autoRefresh && status?.status === 'RUNNING') {
      intervalRef.current = setInterval(fetchStatus, 3000); // Update every 3 seconds for running tests

      return () => {
        if (intervalRef.current) {
          clearInterval(intervalRef.current);
        }
      };
    }
  }, [testId, autoRefresh, status?.status, fetchStatus]);

  const stopPolling = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  }, []);

  return {
    status,
    loading,
    error,
    refresh: fetchStatus,
    stopPolling,
  };
}
