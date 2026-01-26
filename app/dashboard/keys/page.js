'use client';

import { useState, useEffect } from 'react';
import { Plus, Copy, Edit, Trash2, Eye, EyeOff } from 'lucide-react';
import { apiKeysApi } from '@/lib/api';
import ApiKeyModal from '@/components/ApiKeyModal';

export default function ApiKeysPage() {
  const [apiKeys, setApiKeys] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingKey, setEditingKey] = useState(null);
  const [message, setMessage] = useState(null);
  const [visibleKeys, setVisibleKeys] = useState({});

  useEffect(() => {
    fetchApiKeys();
  }, []);

  const fetchApiKeys = async () => {
    try {
      setLoading(true);
      const data = await apiKeysApi.getAll();
      setApiKeys(data);
    } catch (error) {
      showMessage('Error fetching API keys: ' + error.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const showMessage = (text, type = 'success') => {
    setMessage({ text, type });
    setTimeout(() => setMessage(null), 3000);
  };

  const handleCreate = () => {
    setEditingKey(null);
    setIsModalOpen(true);
  };

  const handleEdit = (key) => {
    setEditingKey(key);
    setIsModalOpen(true);
  };

  const handleSubmit = async (formData) => {
    try {
      if (editingKey) {
        await apiKeysApi.update(editingKey.id, formData);
        showMessage('API key updated successfully');
      } else {
        await apiKeysApi.create(formData);
        showMessage('API key created successfully');
      }
      setIsModalOpen(false);
      fetchApiKeys();
    } catch (error) {
      showMessage('Error: ' + error.message, 'error');
    }
  };

  const handleRevoke = async (id) => {
    if (!confirm('Are you sure you want to revoke this API key?')) return;

    try {
      await apiKeysApi.revoke(id);
      showMessage('API key revoked successfully');
      fetchApiKeys();
    } catch (error) {
      showMessage('Error revoking API key: ' + error.message, 'error');
    }
  };

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
    showMessage('API key copied to clipboard');
  };

  const toggleKeyVisibility = (id) => {
    setVisibleKeys((prev) => ({
      ...prev,
      [id]: !prev[id],
    }));
  };

  const maskKey = (key) => {
    if (key.length <= 8) return '***';
    return key.slice(0, 8) + '***' + key.slice(-4);
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
          <h1 className="text-3xl font-bold text-white mb-2">API Keys</h1>
          <p className="text-slate-400">
            Manage client API keys and access credentials
          </p>
        </div>
        <button
          onClick={handleCreate}
          className="nexus-button-primary flex items-center space-x-2"
        >
          <Plus className="w-4 h-4" />
          <span>New API Key</span>
        </button>
      </div>

      <div className="nexus-card overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-slate-400">Loading...</div>
        ) : apiKeys.length === 0 ? (
          <div className="p-8 text-center text-slate-400">
            No API keys found. Create your first API key to get started.
          </div>
        ) : (
          <div className="overflow-x-auto scrollbar-thin scrollbar-thumb-slate-700 scrollbar-track-slate-800">
          <table className="nexus-table w-full min-w-max">
            <thead>
              <tr>
                <th>Key Name</th>
                <th>Key Value</th>
                <th>Client Name</th>
                <th>Client Email</th>
                <th>Client Company</th>
                <th>Status</th>
                <th>Expires</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {apiKeys.map((key) => (
                <tr key={key.id}>
                  <td className="font-medium text-white">{key.keyName}</td>
                  <td>
                    <div className="flex items-center space-x-2">
                      <code className="font-mono text-xs text-slate-400">
                        {visibleKeys[key.id]
                          ? key.keyValue
                          : maskKey(key.keyValue)}
                      </code>
                      <button
                        onClick={() => toggleKeyVisibility(key.id)}
                        className="p-1 hover:bg-slate-700 rounded"
                      >
                        {visibleKeys[key.id] ? (
                          <EyeOff className="w-4 h-4 text-slate-400" />
                        ) : (
                          <Eye className="w-4 h-4 text-slate-400" />
                        )}
                      </button>
                      <button
                        onClick={() => copyToClipboard(key.keyValue)}
                        className="p-1 hover:bg-slate-700 rounded"
                      >
                        <Copy className="w-4 h-4 text-slate-400" />
                      </button>
                    </div>
                  </td>
                  <td>{key.clientName}</td>
                  <td>{key.clientEmail}</td>
                  <td>{key.clientCompany}</td>
                  <td>
                    <span
                      className={`px-3 py-1 rounded-full text-xs font-medium ${
                        key.isActive
                          ? 'bg-green-900 text-green-300'
                          : 'bg-red-900 text-red-300'
                      }`}
                    >
                      {key.isActive ? 'Active' : 'Revoked'}
                    </span>
                  </td>
                  <td className="text-xs">
                    {key.expiresAt
                      ? new Date(key.expiresAt).toLocaleDateString()
                      : 'Never'}
                  </td>
                  <td>
                    <div className="flex items-center space-x-2">
                      <button
                        onClick={() => handleEdit(key)}
                        className="p-1 hover:bg-slate-700 rounded"
                        title="Edit"
                      >
                        <Edit className="w-4 h-4 text-slate-400" />
                      </button>
                      <button
                        onClick={() => handleRevoke(key.id)}
                        className="p-1 hover:bg-slate-700 rounded"
                        title="Revoke"
                        disabled={!key.isActive}
                      >
                        <Trash2
                          className={`w-4 h-4 ${
                            key.isActive ? 'text-red-400' : 'text-slate-600'
                          }`}
                        />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          </div>
        )}
      </div>

      <ApiKeyModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleSubmit}
        apiKey={editingKey}
      />
    </div>
  );
}
