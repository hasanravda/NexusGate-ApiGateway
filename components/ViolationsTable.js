'use client';

import { ChevronLeft, ChevronRight, Download, RefreshCw } from 'lucide-react';

function truncateApiKey(apiKey) {
  if (!apiKey || apiKey.length <= 16) return apiKey;
  return `${apiKey.substring(0, 10)}...${apiKey.substring(apiKey.length - 6)}`;
}

function formatTimestamp(timestamp) {
  const date = new Date(timestamp);
  return date.toLocaleString('en-US', {
    month: 'short',
    day: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

function MethodBadge({ method }) {
  const colorClasses = {
    GET: 'bg-green-100 text-green-800 border-green-300',
    POST: 'bg-blue-100 text-blue-800 border-blue-300',
    PUT: 'bg-yellow-100 text-yellow-800 border-yellow-300',
    DELETE: 'bg-red-100 text-red-800 border-red-300',
    PATCH: 'bg-purple-100 text-purple-800 border-purple-300',
  };

  return (
    <span className={`px-2 py-1 text-xs font-semibold rounded border ${colorClasses[method] || 'bg-gray-100 text-gray-800 border-gray-300'}`}>
      {method}
    </span>
  );
}

export default function ViolationsTable({
  violations,
  page,
  limit,
  totalPages,
  totalElements,
  loading,
  error,
  onPageChange,
  onLimitChange,
  onRefresh,
}) {
  const exportToCSV = () => {
    if (!violations || violations.length === 0) return;

    const headers = ['Timestamp', 'API Key', 'Service Name', 'Endpoint', 'Method', 'Limit Value', 'Actual Value', 'Client IP'];
    const rows = violations.map(v => [
      v.timestamp,
      v.apiKey,
      v.serviceName,
      v.endpoint,
      v.httpMethod,
      v.limitValue,
      v.actualValue,
      v.clientIp,
    ]);

    const csvContent = [
      headers.join(','),
      ...rows.map(row => row.map(cell => `"${cell}"`).join(',')),
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `violations-${new Date().toISOString()}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  };

  return (
    <div className="bg-slate-800 rounded-lg shadow-sm border border-slate-700">
      {/* Header */}
      <div className="px-6 py-4 border-b border-slate-700 flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-white">Recent Rate Limit Violations</h2>
          <p className="text-sm text-slate-400 mt-1">
            {totalElements > 0 ? `Showing ${violations.length} of ${totalElements} violations` : 'No violations found'}
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={onRefresh}
            disabled={loading}
            className="px-3 py-2 text-sm font-medium text-slate-300 bg-slate-700 border border-slate-600 rounded-md hover:bg-slate-600 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
          >
            <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
            Refresh
          </button>
          <button
            onClick={exportToCSV}
            disabled={!violations || violations.length === 0}
            className="px-3 py-2 text-sm font-medium text-slate-300 bg-slate-700 border border-slate-600 rounded-md hover:bg-slate-600 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
          >
            <Download size={16} />
            Export CSV
          </button>
          <select
            value={limit}
            onChange={(e) => onLimitChange(Number(e.target.value))}
            className="px-3 py-2 text-sm bg-slate-700 border border-slate-600 text-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value={10}>10 per page</option>
            <option value={25}>25 per page</option>
            <option value={50}>50 per page</option>
            <option value={100}>100 per page</option>
          </select>
        </div>
      </div>

      {/* Error State */}
      {error && (
        <div className="px-6 py-4 bg-red-900/30 border-b border-red-700">
          <p className="text-red-300 text-sm">⚠️ {error}</p>
        </div>
      )}

      {/* Loading State */}
      {loading && (
        <div className="px-6 py-12 text-center">
          <div className="inline-block h-8 w-8 animate-spin rounded-full border-4 border-solid border-blue-500 border-r-transparent"></div>
          <p className="mt-2 text-sm text-slate-400">Loading violations...</p>
        </div>
      )}

      {/* Table */}
      {!loading && violations && violations.length > 0 && (
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-900 border-b border-slate-700">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">
                  Timestamp
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">
                  API Key
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">
                  Service
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">
                  Endpoint
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">
                  Method
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">
                  Limit
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">
                  Actual
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">
                  Client IP
                </th>
              </tr>
            </thead>
            <tbody className="bg-slate-800 divide-y divide-slate-700">
              {violations.map((violation) => (
                <tr key={violation.id} className="hover:bg-slate-700 transition-colors">
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-300">
                    {formatTimestamp(violation.timestamp)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-300 font-mono">
                    {truncateApiKey(violation.apiKey)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-300">
                    {violation.serviceName}
                  </td>
                  <td className="px-6 py-4 text-sm text-blue-400 max-w-xs truncate">
                    {violation.endpoint}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <MethodBadge method={violation.httpMethod} />
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-300">
                    {violation.limitValue}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-bold text-red-400">
                    {violation.actualValue}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-300 font-mono">
                    {violation.clientIp}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Empty State */}
      {!loading && (!violations || violations.length === 0) && !error && (
        <div className="px-6 py-12 text-center">
          <AlertTriangle size={48} className="mx-auto text-slate-600 mb-3" />
          <p className="text-slate-400">No rate limit violations found</p>
          <p className="text-sm text-slate-500 mt-1">Violations will appear here when rate limits are exceeded</p>
        </div>
      )}

      {/* Pagination */}
      {!loading && violations && violations.length > 0 && (
        <div className="px-6 py-4 border-t border-slate-700 flex items-center justify-between">
          <div className="text-sm text-slate-400">
            Page <span className="font-medium text-slate-300">{page + 1}</span> of{' '}
            <span className="font-medium text-slate-300">{totalPages || 1}</span>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => onPageChange(page - 1)}
              disabled={page === 0 || loading}
              className="px-3 py-2 text-sm font-medium text-slate-300 bg-slate-700 border border-slate-600 rounded-md hover:bg-slate-600 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
            >
              <ChevronLeft size={16} />
              Previous
            </button>
            <button
              onClick={() => onPageChange(page + 1)}
              disabled={page >= totalPages - 1 || loading}
              className="px-3 py-2 text-sm font-medium text-slate-300 bg-slate-700 border border-slate-600 rounded-md hover:bg-slate-600 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
            >
              Next
              <ChevronRight size={16} />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

// Helper component for empty state icon
function AlertTriangle({ size, className }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
      <line x1="12" y1="9" x2="12" y2="13"></line>
      <line x1="12" y1="17" x2="12.01" y2="17"></line>
    </svg>
  );
}
