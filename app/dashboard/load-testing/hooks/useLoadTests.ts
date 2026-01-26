// Custom hook for managing load tests

import { useState, useEffect, useCallback, useRef } from 'react';
import { TestListItem } from '../types/loadTester.types';
import { listTests, checkHealth } from '../api/loadTester';

export function useLoadTests(autoRefresh: boolean = true, refreshInterval: number = 5000) {
  const [tests, setTests] = useState<TestListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isHealthy, setIsHealthy] = useState(false);
  const isFetching = useRef(false);

  const fetchTests = useCallback(async (skipCheck = false) => {
    if (isFetching.current) return;
    
    isFetching.current = true;
    setLoading(true);
    
    try {
      setError(null);
      
      // Check service health first
      if (!skipCheck) {
        const health = await checkHealth();
        const healthy = health.status?.toUpperCase() === 'UP';
        setIsHealthy(healthy);
        
        if (!healthy) {
          setError('Service is offline');
          setTests([]);
          return;
        }
      }
      
      const data = await listTests();
      setTests(data);
      setIsHealthy(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch tests');
      setIsHealthy(false);
      setTests([]);
    } finally {
      setLoading(false);
      isFetching.current = false;
    }
  }, []);

  const checkServiceHealth = useCallback(async () => {
    try {
      const health = await checkHealth();
      const healthy = health.status?.toUpperCase() === 'UP';
      setIsHealthy(healthy);
      return healthy;
    } catch (err) {
      setIsHealthy(false);
      return false;
    }
  }, []);

  useEffect(() => {
    let mounted = true;
    
    const init = async () => {
      if (!mounted) return;
      await checkServiceHealth();
      if (mounted) await fetchTests();
    };
    
    init();

    if (autoRefresh) {
      const interval = setInterval(async () => {
        if (!mounted) return;
        const healthy = await checkServiceHealth();
        if (healthy && mounted) await fetchTests();
      }, refreshInterval);

      return () => {
        mounted = false;
        clearInterval(interval);
      };
    }

    return () => {
      mounted = false;
    };
  }, [autoRefresh, refreshInterval, fetchTests, checkServiceHealth]);

  return {
    tests,
    loading,
    error,
    isHealthy,
    refresh: fetchTests,
  };
}
