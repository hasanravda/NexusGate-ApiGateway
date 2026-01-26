const ANALYTICS_BASE_URL = 'http://localhost:8085/analytics';

async function handleResponse(response) {
  if (!response.ok) {
    throw new Error(`API request failed: ${response.statusText}`);
  }
  return response.json();
}

export const analyticsApi = {
  /**
   * Get all dashboard metrics in a single call
   * @returns {Promise<{violationsToday: number, blockedRequests: number, averageLatencyMs: number, totalRequests: number, successRate: number}>}
   */
  getDashboardMetrics: async () => {
    const response = await fetch(`${ANALYTICS_BASE_URL}/dashboard/metrics`, {
      cache: 'no-store',
    });
    return handleResponse(response);
  },

  /**
   * Get recent violations with pagination
   * @param {number} limit - Number of items per page (default: 10, max: 100)
   * @param {number} page - Page number (default: 0)
   * @returns {Promise<{content: Array, totalElements: number, totalPages: number, number: number, size: number, first: boolean, last: boolean}>}
   */
  getRecentViolations: async (limit = 10, page = 0) => {
    const response = await fetch(
      `${ANALYTICS_BASE_URL}/dashboard/violations/recent?limit=${limit}&page=${page}`,
      { cache: 'no-store' }
    );
    return handleResponse(response);
  },

  /**
   * Get violations count for today
   * @returns {Promise<{count: number}>}
   */
  getViolationsCountToday: async () => {
    const response = await fetch(
      `${ANALYTICS_BASE_URL}/dashboard/violations/today/count`,
      { cache: 'no-store' }
    );
    return handleResponse(response);
  },

  /**
   * Get blocked requests count
   * @returns {Promise<{count: number}>}
   */
  getBlockedRequestsCount: async () => {
    const response = await fetch(
      `${ANALYTICS_BASE_URL}/dashboard/requests/blocked/count`,
      { cache: 'no-store' }
    );
    return handleResponse(response);
  },

  /**
   * Get average latency for the last 24 hours
   * @returns {Promise<{averageLatencyMs: number, period: string}>}
   */
  getAverageLatency: async () => {
    const response = await fetch(
      `${ANALYTICS_BASE_URL}/dashboard/latency/average`,
      { cache: 'no-store' }
    );
    return handleResponse(response);
  },
};
