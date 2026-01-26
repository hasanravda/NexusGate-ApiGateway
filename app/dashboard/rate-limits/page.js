'use client';

import { useState, useEffect } from 'react';
import { Plus, Edit, Trash2, Power } from 'lucide-react';
import { rateLimitsApi, apiKeysApi, serviceRoutesApi } from '@/lib/api';
import RateLimitModal from '@/components/RateLimitModal';

export default function RateLimitsPage() {
  const [rateLimits, setRateLimits] = useState([]);
  const [apiKeys, setApiKeys] = useState([]);
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingLimit, setEditingLimit] = useState(null);
  const [message, setMessage] = useState(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [limitsData, keysData, servicesData] = await Promise.all([
        rateLimitsApi.getAll(),
        apiKeysApi.getAll(),
        serviceRoutesApi.getAll(),
      ]);
      setRateLimits(limitsData);
      setApiKeys(keysData);
      setServices(servicesData);
    } catch (error) {
      showMessage('Error fetching data: ' + error.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const showMessage = (text, type = 'success') => {
    setMessage({ text, type });
    setTimeout(() => setMessage(null), 3000);
  };

  const handleCreate = () => {
    setEditingLimit(null);
    setIsModalOpen(true);
  };

  const handleEdit = (limit) => {
    setEditingLimit(limit);
    setIsModalOpen(true);
  };

  const handleSubmit = async (formData) => {
    try {
      if (editingLimit) {
        await rateLimitsApi.update(editingLimit.id, formData);
        showMessage('Rate limit updated successfully');
      } else {
        await rateLimitsApi.create(formData);
        showMessage('Rate limit created successfully');
      }
      setIsModalOpen(false);
      fetchData();
    } catch (error) {
      showMessage('Error: ' + error.message, 'error');
    }
  };

  const handleToggle = async (limit) => {
    try {
      await rateLimitsApi.toggle(limit.id);
      showMessage(
        `Rate limit ${limit.isActive ? 'disabled' : 'enabled'} successfully`
      );
      fetchData();
    } catch (error) {
      showMessage('Error toggling rate limit: ' + error.message, 'error');
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('Are you sure you want to delete this rate limit?')) return;

    try {
      await rateLimitsApi.delete(id);
      showMessage('Rate limit deleted successfully');
      fetchData();
    } catch (error) {
      showMessage('Error deleting rate limit: ' + error.message, 'error');
    }
  };

  const getApiKeyName = (apiKeyId) => {
    if (!apiKeyId) return 'All API Keys';
    const key = apiKeys.find((k) => k.id === apiKeyId);
    return key ? key.keyName : 'Unknown';
  };

  const getServiceName = (serviceRouteId) => {
    if (!serviceRouteId) return 'All Services';
    const service = services.find((s) => s.id === serviceRouteId);
    return service ? service.serviceName : 'Unknown';
  };

  const getLimitType = (limit) => {
    if (limit.apiKeyId && limit.serviceRouteId) {
      return { type: 'Specific', color: 'bg-blue-900 text-blue-300' };
    } else if (limit.serviceRouteId) {
      return { type: 'Route Default', color: 'bg-purple-900 text-purple-300' };
    } else if (limit.apiKeyId) {
      return { type: 'Key Global', color: 'bg-green-900 text-green-300' };
    }
    return { type: 'Unknown', color: 'bg-gray-900 text-gray-300' };
  };

  return (
    <div className="p-8">
      {message && (
        <div
          className={`mb-4 p-4 rounded-lg ${
            message.type === 'error'
              ? 'bg-red-900 text-red-300'
              : 'bg-green-900 text-green-300'
          }`}
        >
          {message.text}
        </div>
      )}

      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold text-white mb-2">Rate Limits</h1>
          <p className="text-slate-400">
            Configure rate limiting rules for API keys and service routes
          </p>
        </div>
        <button
          onClick={handleCreate}
          className="nexus-button-primary flex items-center space-x-2"
        >
          <Plus className="w-4 h-4" />
          <span>New Rate Limit</span>
        </button>
      </div>

      <div className="mb-6 p-4 bg-slate-800 border border-slate-700 rounded-lg">
        <h3 className="text-sm font-semibold text-white mb-2">
          Rate Limit Priority
        </h3>
        <p className="text-xs text-slate-400">
          When multiple limits apply, the most specific one is used:
        </p>
        <div className="flex items-center space-x-4 mt-2">
          <span className="px-2 py-1 bg-blue-900 text-blue-300 rounded text-xs">
            1. Specific
          </span>
          <span className="text-slate-500">&gt;</span>
          <span className="px-2 py-1 bg-purple-900 text-purple-300 rounded text-xs">
            2. Route Default
          </span>
          <span className="text-slate-500">&gt;</span>
          <span className="px-2 py-1 bg-green-900 text-green-300 rounded text-xs">
            3. Key Global
          </span>
          <span className="text-slate-500">&gt;</span>
          <span className="px-2 py-1 bg-gray-900 text-gray-300 rounded text-xs">
            4. System Default
          </span>
        </div>
      </div>

      <div className="nexus-card overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-slate-400">Loading...</div>
        ) : rateLimits.length === 0 ? (
          <div className="p-8 text-center text-slate-400">
            No rate limits found. Create your first rate limit to get started.
          </div>
        ) : (
          <table className="nexus-table">
            <thead>
              <tr>
                <th>Type</th>
                <th>API Key</th>
                <th>Service Route</th>
                <th>Req/min</th>
                <th>Req/hr</th>
                <th>Req/day</th>
                <th>Status</th>
                <th>Notes</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {rateLimits.map((limit) => {
                const limitType = getLimitType(limit);
                return (
                  <tr key={limit.id}>
                    <td>
                      <span
                        className={`px-2 py-1 rounded-full text-xs font-medium ${limitType.color}`}
                      >
                        {limitType.type}
                      </span>
                    </td>
                    <td className="max-w-xs truncate">
                      {getApiKeyName(limit.apiKeyId)}
                    </td>
                    <td className="max-w-xs truncate">
                      {getServiceName(limit.serviceRouteId)}
                    </td>
                    <td className="font-mono">{limit.requestsPerMinute}</td>
                    <td className="font-mono">{limit.requestsPerHour}</td>
                    <td className="font-mono">{limit.requestsPerDay}</td>
                    <td>
                      <button
                        onClick={() => handleToggle(limit)}
                        className={`px-3 py-1 rounded-full text-xs font-medium ${
                          limit.isActive
                            ? 'bg-green-900 text-green-300'
                            : 'bg-red-900 text-red-300'
                        }`}
                      >
                        {limit.isActive ? 'Active' : 'Disabled'}
                      </button>
                    </td>
                    <td className="max-w-xs truncate text-xs">
                      {limit.notes || '-'}
                    </td>
                    <td>
                      <div className="flex items-center space-x-2">
                        <button
                          onClick={() => handleEdit(limit)}
                          className="p-1 hover:bg-slate-700 rounded"
                          title="Edit"
                        >
                          <Edit className="w-4 h-4 text-slate-400" />
                        </button>
                        <button
                          onClick={() => handleDelete(limit.id)}
                          className="p-1 hover:bg-slate-700 rounded"
                          title="Delete"
                        >
                          <Trash2 className="w-4 h-4 text-red-400" />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

      <RateLimitModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleSubmit}
        rateLimit={editingLimit}
        apiKeys={apiKeys}
        services={services}
      />
    </div>
  );
}
