'use client';

import { useState, useEffect } from 'react';
import { X } from 'lucide-react';

export default function RateLimitModal({
  isOpen,
  onClose,
  onSubmit,
  rateLimit,
  apiKeys,
  services,
}) {
  const [formData, setFormData] = useState({
    apiKeyId: '',
    serviceRouteId: '',
    requestsPerMinute: '',
    requestsPerHour: '',
    requestsPerDay: '',
    notes: '',
  });

  useEffect(() => {
    if (rateLimit) {
      setFormData({
        apiKeyId: rateLimit.apiKeyId || '',
        serviceRouteId: rateLimit.serviceRouteId || '',
        requestsPerMinute: rateLimit.requestsPerMinute || '',
        requestsPerHour: rateLimit.requestsPerHour || '',
        requestsPerDay: rateLimit.requestsPerDay || '',
        notes: rateLimit.notes || '',
      });
    } else {
      setFormData({
        apiKeyId: '',
        serviceRouteId: '',
        requestsPerMinute: '',
        requestsPerHour: '',
        requestsPerDay: '',
        notes: '',
      });
    }
  }, [rateLimit, isOpen]);

  const handleSubmit = (e) => {
    e.preventDefault();
    const submitData = {
      ...formData,
      apiKeyId: formData.apiKeyId ? parseInt(formData.apiKeyId) : null,
      serviceRouteId: formData.serviceRouteId
        ? parseInt(formData.serviceRouteId)
        : null,
      requestsPerMinute: parseInt(formData.requestsPerMinute),
      requestsPerHour: parseInt(formData.requestsPerHour),
      requestsPerDay: parseInt(formData.requestsPerDay),
    };
    onSubmit(submitData);
  };

  if (!isOpen) return null;

  const getLimitType = () => {
    if (formData.apiKeyId && formData.serviceRouteId) {
      return 'Specific (API Key + Service)';
    } else if (formData.serviceRouteId) {
      return 'Route Default (All API keys for this route)';
    } else if (formData.apiKeyId) {
      return 'Key Global (All routes for this API key)';
    }
    return 'Not configured';
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-slate-800 rounded-lg w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="p-6 border-b border-slate-700 flex items-center justify-between">
          <h2 className="text-xl font-semibold text-white">
            {rateLimit ? 'Edit Rate Limit' : 'Create New Rate Limit'}
          </h2>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-white"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div className="p-4 bg-blue-900/30 border border-blue-700 rounded-lg">
            <p className="text-sm text-blue-300 mb-2">
              <strong>Limit Type:</strong> {getLimitType()}
            </p>
            <p className="text-xs text-slate-400">
              Leave fields empty for broader scope. Priority: Specific &gt;
              Route Default &gt; Key Global
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              API Key (Optional)
            </label>
            <select
              value={formData.apiKeyId}
              onChange={(e) =>
                setFormData({ ...formData, apiKeyId: e.target.value })
              }
              className="nexus-select w-full"
            >
              <option value="">All API Keys</option>
              {apiKeys.map((key) => (
                <option key={key.id} value={key.id}>
                  {key.keyName} ({key.clientName})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Service Route (Optional)
            </label>
            <select
              value={formData.serviceRouteId}
              onChange={(e) =>
                setFormData({ ...formData, serviceRouteId: e.target.value })
              }
              className="nexus-select w-full"
            >
              <option value="">All Service Routes</option>
              {services.map((service) => (
                <option key={service.id} value={service.id}>
                  {service.serviceName} ({service.publicPath})
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                Requests per Minute
              </label>
              <input
                type="number"
                required
                value={formData.requestsPerMinute}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    requestsPerMinute: e.target.value,
                  })
                }
                className="nexus-input w-full"
                placeholder="e.g., 100"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                Requests per Hour
              </label>
              <input
                type="number"
                required
                value={formData.requestsPerHour}
                onChange={(e) =>
                  setFormData({ ...formData, requestsPerHour: e.target.value })
                }
                className="nexus-input w-full"
                placeholder="e.g., 5000"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                Requests per Day
              </label>
              <input
                type="number"
                required
                value={formData.requestsPerDay}
                onChange={(e) =>
                  setFormData({ ...formData, requestsPerDay: e.target.value })
                }
                className="nexus-input w-full"
                placeholder="e.g., 100000"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Notes (Optional)
            </label>
            <textarea
              value={formData.notes}
              onChange={(e) =>
                setFormData({ ...formData, notes: e.target.value })
              }
              className="nexus-input w-full"
              rows="3"
              placeholder="Additional notes about this rate limit"
            />
          </div>

          <div className="flex justify-end space-x-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="nexus-button-secondary"
            >
              Cancel
            </button>
            <button type="submit" className="nexus-button-primary">
              {rateLimit ? 'Update Rate Limit' : 'Create Rate Limit'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
