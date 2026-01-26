'use client';

import { AlertTriangle, Ban, Clock, Activity, CheckCircle } from 'lucide-react';

function MetricCard({ title, value, icon: Icon, color, trend }) {
  const colorClasses = {
    red: 'bg-slate-800 border-slate-700',
    orange: 'bg-slate-800 border-slate-700',
    blue: 'bg-slate-800 border-slate-700',
    green: 'bg-slate-800 border-slate-700',
  };

  const iconBgClasses = {
    red: 'bg-red-900',
    orange: 'bg-yellow-900',
    blue: 'bg-blue-900',
    green: 'bg-green-900',
  };

  const iconColorClasses = {
    red: 'text-red-400',
    orange: 'text-yellow-400',
    blue: 'text-blue-400',
    green: 'text-green-400',
  };

  return (
    <div className={`${colorClasses[color]} border rounded-lg p-6 shadow-sm hover:shadow-lg transition-shadow`}>
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <p className="text-sm font-medium text-slate-400 mb-1">{title}</p>
          <p className="text-3xl font-bold text-white">{value}</p>
          {trend && (
            <p className="text-xs text-slate-500 mt-1">{trend}</p>
          )}
        </div>
        <div className={`${iconBgClasses[color]} ${iconColorClasses[color]} w-10 h-10 rounded-lg flex items-center justify-center`}>
          <Icon size={20} />
        </div>
      </div>
    </div>
  );
}

export default function MetricsGrid({ metrics, loading, error }) {
  if (loading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4 mb-8">
        {[...Array(5)].map((_, i) => (
          <div key={i} className="bg-slate-800 border border-slate-700 rounded-lg p-6 animate-pulse">
            <div className="h-4 bg-slate-700 rounded w-3/4 mb-3"></div>
            <div className="h-8 bg-slate-700 rounded w-1/2"></div>
          </div>
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-900/30 border border-red-700 rounded-lg p-4 mb-8">
        <p className="text-red-300 text-sm">⚠️ Failed to load metrics: {error}</p>
      </div>
    );
  }

  if (!metrics) return null;

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4 mb-8">
      <MetricCard
        title="Violations Today"
        value={metrics.violationsToday || 0}
        icon={AlertTriangle}
        color="red"
        trend="Rate limit breaches"
      />
      <MetricCard
        title="Blocked Requests"
        value={metrics.blockedRequests || 0}
        icon={Ban}
        color="orange"
        trend="Unauthorized access"
      />
      <MetricCard
        title="Avg Latency"
        value={`${metrics.averageLatencyMs?.toFixed(1) || 0}ms`}
        icon={Clock}
        color="blue"
        trend="Last 24 hours"
      />
      <MetricCard
        title="Total Requests"
        value={metrics.totalRequests || 0}
        icon={Activity}
        color="green"
        trend="Last 24 hours"
      />
      <MetricCard
        title="Success Rate"
        value={`${metrics.successRate?.toFixed(1) || 0}%`}
        icon={CheckCircle}
        color="green"
        trend="Request success"
      />
    </div>
  );
}
