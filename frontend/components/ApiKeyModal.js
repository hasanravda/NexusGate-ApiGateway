'use client';

import { useState, useEffect } from 'react';
import { X } from 'lucide-react';

export default function ApiKeyModal({ isOpen, onClose, onSubmit, apiKey }) {
  const [formData, setFormData] = useState({
    keyName: '',
    clientName: '',
    clientEmail: '',
    clientCompany: '',
    expiresAt: '',
    notes: '',
    createdByUserId: 1,
  });

  useEffect(() => {
    if (apiKey) {
      setFormData({
        keyName: apiKey.keyName || '',
        clientName: apiKey.clientName || '',
        clientEmail: apiKey.clientEmail || '',
        clientCompany: apiKey.clientCompany || '',
        expiresAt: apiKey.expiresAt
          ? apiKey.expiresAt.split('T')[0]
          : '',
        notes: apiKey.notes || '',
        createdByUserId: apiKey.createdByUserId || 1,
      });
    } else {
      setFormData({
        keyName: '',
        clientName: '',
        clientEmail: '',
        clientCompany: '',
        expiresAt: '',
        notes: '',
        createdByUserId: 1,
      });
    }
  }, [apiKey, isOpen]);

  const handleSubmit = (e) => {
    e.preventDefault();
    const submitData = {
      ...formData,
      expiresAt: formData.expiresAt
        ? `${formData.expiresAt}T23:59:59`
        : null,
    };
    onSubmit(submitData);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-slate-800 rounded-lg w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="p-6 border-b border-slate-700 flex items-center justify-between">
          <h2 className="text-xl font-semibold text-white">
            {apiKey ? 'Edit API Key' : 'Create New API Key'}
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
              Key Name
            </label>
            <input
              type="text"
              required
              value={formData.keyName}
              onChange={(e) =>
                setFormData({ ...formData, keyName: e.target.value })
              }
              className="nexus-input w-full"
              placeholder="e.g., Production Key - Acme Corp"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Client Name
            </label>
            <input
              type="text"
              required
              value={formData.clientName}
              onChange={(e) =>
                setFormData({ ...formData, clientName: e.target.value })
              }
              className="nexus-input w-full"
              placeholder="e.g., Acme Corporation"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Client Email
            </label>
            <input
              type="email"
              required
              value={formData.clientEmail}
              onChange={(e) =>
                setFormData({ ...formData, clientEmail: e.target.value })
              }
              className="nexus-input w-full"
              placeholder="e.g., api@acme.com"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Client Company
            </label>
            <input
              type="text"
              required
              value={formData.clientCompany}
              onChange={(e) =>
                setFormData({ ...formData, clientCompany: e.target.value })
              }
              className="nexus-input w-full"
              placeholder="e.g., Acme Technologies Ltd"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Expiration Date (Optional)
            </label>
            <input
              type="date"
              value={formData.expiresAt}
              onChange={(e) =>
                setFormData({ ...formData, expiresAt: e.target.value })
              }
              className="nexus-input w-full"
            />
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
              placeholder="Additional notes about this API key"
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
              {apiKey ? 'Update API Key' : 'Create API Key'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
