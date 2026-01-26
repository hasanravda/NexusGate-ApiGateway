'use client';

import { Activity, Clock, AlertCircle, Zap, Shield } from 'lucide-react';

export default function DashboardPage() {
  const metrics = [
    {
      title: 'Requests per second',
      value: '1,247',
      icon: Activity,
      color: 'blue',
      change: '+12%',
    },
    {
      title: 'P95 Latency',
      value: '42ms',
      icon: Clock,
      color: 'green',
      change: '-8%',
    },
    {
      title: 'Error Rate',
      value: '0.03%',
      icon: AlertCircle,
      color: 'yellow',
      change: '-15%',
    },
    {
      title: 'Rate Violations/min',
      value: '3',
      icon: Zap,
      color: 'red',
      change: '+2',
    },
    {
      title: 'Circuit Breaker',
      value: 'Closed',
      icon: Shield,
      color: 'green',
      change: 'Healthy',
    },
  ];

  return (
    <div className="p-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-white mb-2">Overview</h1>
        <p className="text-slate-400">
          Real-time monitoring of your API Gateway infrastructure
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6 mb-8">
        {metrics.map((metric) => {
          const Icon = metric.icon;
          return (
            <div key={metric.title} className="nexus-card p-6">
              <div className="flex items-center justify-between mb-4">
                <div
                  className={`w-10 h-10 rounded-lg flex items-center justify-center ${
                    metric.color === 'blue'
                      ? 'bg-blue-900'
                      : metric.color === 'green'
                      ? 'bg-green-900'
                      : metric.color === 'yellow'
                      ? 'bg-yellow-900'
                      : 'bg-red-900'
                  }`}
                >
                  <Icon
                    className={`w-5 h-5 ${
                      metric.color === 'blue'
                        ? 'text-blue-400'
                        : metric.color === 'green'
                        ? 'text-green-400'
                        : metric.color === 'yellow'
                        ? 'text-yellow-400'
                        : 'text-red-400'
                    }`}
                  />
                </div>
                <span className="text-xs text-slate-400">{metric.change}</span>
              </div>
              <div>
                <p className="text-2xl font-bold text-white mb-1">
                  {metric.value}
                </p>
                <p className="text-sm text-slate-400">{metric.title}</p>
              </div>
            </div>
          );
        })}
      </div>

      <div className="nexus-card p-6 mb-8">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-semibold text-white">Traffic Overview</h2>
          <div className="flex items-center space-x-4">
            <div className="flex space-x-2">
              <button className="px-3 py-1 bg-blue-600 text-white rounded-md text-sm">
                Global
              </button>
              <button className="px-3 py-1 bg-slate-700 text-slate-300 rounded-md text-sm hover:bg-slate-600">
                Per API
              </button>
              <button className="px-3 py-1 bg-slate-700 text-slate-300 rounded-md text-sm hover:bg-slate-600">
                Per Key
              </button>
            </div>
            <div className="flex space-x-2">
              <button className="px-3 py-1 bg-slate-700 text-slate-300 rounded-md text-sm hover:bg-slate-600">
                1m
              </button>
              <button className="px-3 py-1 bg-blue-600 text-white rounded-md text-sm">
                5m
              </button>
              <button className="px-3 py-1 bg-slate-700 text-slate-300 rounded-md text-sm hover:bg-slate-600">
                15m
              </button>
            </div>
          </div>
        </div>
        <div className="h-64 flex items-center justify-center border border-slate-700 rounded-lg bg-slate-800/50">
          <p className="text-slate-500">Traffic chart visualization</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="nexus-card p-6">
          <h3 className="text-lg font-semibold text-white mb-4">
            Top Services by Traffic
          </h3>
          <div className="space-y-3">
            {[
              { name: 'user-service', requests: '45.2K', percentage: 85 },
              { name: 'order-service', requests: '32.1K', percentage: 65 },
              { name: 'payment-service', requests: '28.4K', percentage: 55 },
              { name: 'notification-service', requests: '12.8K', percentage: 25 },
            ].map((service) => (
              <div key={service.name}>
                <div className="flex items-center justify-between mb-1">
                  <span className="text-sm text-slate-300">{service.name}</span>
                  <span className="text-sm text-slate-400">
                    {service.requests}
                  </span>
                </div>
                <div className="w-full bg-slate-700 rounded-full h-2">
                  <div
                    className="bg-blue-600 h-2 rounded-full"
                    style={{ width: `${service.percentage}%` }}
                  ></div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="nexus-card p-6">
          <h3 className="text-lg font-semibold text-white mb-4">
            Recent Rate Limit Violations
          </h3>
          <div className="space-y-3">
            {[
              {
                key: 'nx_abc123',
                service: 'user-service',
                time: '2 min ago',
              },
              {
                key: 'nx_def456',
                service: 'order-service',
                time: '5 min ago',
              },
              {
                key: 'nx_ghi789',
                service: 'payment-service',
                time: '8 min ago',
              },
            ].map((violation, idx) => (
              <div
                key={idx}
                className="flex items-center justify-between p-3 bg-slate-800 rounded-lg"
              >
                <div>
                  <p className="text-sm font-medium text-white">
                    {violation.key}
                  </p>
                  <p className="text-xs text-slate-400">{violation.service}</p>
                </div>
                <span className="text-xs text-slate-500">{violation.time}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
