'use client';

import { useState, useEffect } from 'react';
import { X } from 'lucide-react';

export default function ServiceModal({ isOpen, onClose, onSubmit, service }) {
  const [formData, setFormData] = useState({
    serviceName: '',
    serviceDescription: '',
    publicPath: '',
    targetUrl: '',
    allowedMethods: [],
    rateLimitPerMinute: '',
    rateLimitPerHour: '',
    notes: '',
    createdByUserId: 1,
  });

  const httpMethods = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'];

  useEffect(() => {
    if (service) {
      setFormData({
        serviceName: service.serviceName || '',
        serviceDescription: service.serviceDescription || '',
        publicPath: service.publicPath || '',
        targetUrl: service.targetUrl || '',
        allowedMethods: service.allowedMethods || [],
        rateLimitPerMinute: service.rateLimitPerMinute || '',
        rateLimitPerHour: service.rateLimitPerHour || '',
        notes: service.notes || '',
        createdByUserId: service.createdByUserId || 1,
      });
    } else {
      setFormData({
        serviceName: '',
        serviceDescription: '',
        publicPath: '',
        targetUrl: '',
        allowedMethods: [],
        rateLimitPerMinute: '',
        rateLimitPerHour: '',
        notes: '',
        createdByUserId: 1,
      });
    }
  }, [service, isOpen]);

  const handleMethodToggle = (method) => {
    setFormData((prev) => ({
      ...prev,
      allowedMethods: prev.allowedMethods.includes(method)
        ? prev.allowedMethods.filter((m) => m !== method)
        : [...prev.allowedMethods, method],
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit(formData);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-slate-800 rounded-lg w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="p-6 border-b border-slate-700 flex items-center justify-between">
          <h2 className="text-xl font-semibold text-white">
            {service ? 'Edit Service Route' : 'Create New Service Route'}
          </h2>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-white"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Service Name
            </label>
            <input
              type="text"
              required
              value={formData.serviceName}
              onChange={(e) =>
                setFormData({ ...formData, serviceName: e.target.value })
              }
              className="nexus-input w-full"
              placeholder="e.g., user-service"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Description
            </label>
            <textarea
              required
              value={formData.serviceDescription}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  serviceDescription: e.target.value,
                })
              }
              className="nexus-input w-full"
              rows="3"
              placeholder="Describe what this service does"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Public Path
            </label>
            <input
              type="text"
              required
              value={formData.publicPath}
              onChange={(e) =>
                setFormData({ ...formData, publicPath: e.target.value })
              }
              className="nexus-input w-full"
              placeholder="e.g., /api/users/**"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Target URL
            </label>
            <input
              type="text"
              required
              value={formData.targetUrl}
              onChange={(e) =>
                setFormData({ ...formData, targetUrl: e.target.value })
              }
              className="nexus-input w-full"
              placeholder="e.g., http://localhost:8082/users"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Allowed HTTP Methods
            </label>
            <div className="flex flex-wrap gap-2">
              {httpMethods.map((method) => (
                <button
                  key={method}
                  type="button"
                  onClick={() => handleMethodToggle(method)}
                  className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                    formData.allowedMethods.includes(method)
                      ? 'bg-blue-600 text-white'
                      : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                  }`}
                >
                  {method}
                </button>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                Rate Limit per Minute
              </label>
              <input
                type="number"
                required
                value={formData.rateLimitPerMinute}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    rateLimitPerMinute: e.target.value,
                  })
                }
                className="nexus-input w-full"
                placeholder="e.g., 100"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                Rate Limit per Hour
              </label>
              <input
                type="number"
                required
                value={formData.rateLimitPerHour}
                onChange={(e) =>
                  setFormData({ ...formData, rateLimitPerHour: e.target.value })
                }
                className="nexus-input w-full"
                placeholder="e.g., 5000"
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
              rows="2"
              placeholder="Additional notes"
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
              {service ? 'Update Service' : 'Create Service'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
