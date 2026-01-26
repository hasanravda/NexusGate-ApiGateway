'use client';

import { useState, useEffect } from 'react';
import { Plus, Edit, Trash2, Eye, Power, Loader2 } from 'lucide-react';
import { serviceRoutesApi } from '@/lib/api';
import ServiceModal from '@/components/ServiceModal';
import { Switch } from '@/components/ui/switch';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';

export default function ServicesPage() {
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingService, setEditingService] = useState(null);
  const [message, setMessage] = useState(null);
  const [securityTogglingIds, setSecurityTogglingIds] = useState(new Set());

  useEffect(() => {
    fetchServices();
  }, []);

  const fetchServices = async () => {
    try {
      setLoading(true);
      const data = await serviceRoutesApi.getAll();
      setServices(data);
    } catch (error) {
      showMessage('Error fetching services: ' + error.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const showMessage = (text, type = 'success') => {
    setMessage({ text, type });
    setTimeout(() => setMessage(null), 3000);
  };

  const handleCreate = () => {
    setEditingService(null);
    setIsModalOpen(true);
  };

  const handleEdit = (service) => {
    setEditingService(service);
    setIsModalOpen(true);
  };

  const handleSubmit = async (formData) => {
    try {
      if (editingService) {
        await serviceRoutesApi.update(editingService.id, formData);
        showMessage('Service updated successfully');
      } else {
        await serviceRoutesApi.create(formData);
        showMessage('Service created successfully');
      }
      setIsModalOpen(false);
      fetchServices();
    } catch (error) {
      showMessage('Error: ' + error.message, 'error');
    }
  };

  const handleToggle = async (service) => {
    try {
      await serviceRoutesApi.toggle(service.id);
      showMessage(
        `Service ${service.isActive ? 'disabled' : 'enabled'} successfully`
      );
      fetchServices();
    } catch (error) {
      showMessage('Error toggling service: ' + error.message, 'error');
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('Are you sure you want to delete this service?')) return;

    try {
      await serviceRoutesApi.delete(id);
      showMessage('Service deleted successfully');
      fetchServices();
    } catch (error) {
      showMessage('Error deleting service: ' + error.message, 'error');
    }
  };

  const handleSecurityToggle = async (service) => {
    const newRequiresApiKey = !(service.requiresApiKey ?? false);
    const serviceId = service.id;

    // Add to toggling set
    setSecurityTogglingIds(prev => new Set([...prev, serviceId]));

    // Optimistic update
    setServices(prevServices =>
      prevServices.map(s =>
        s.id === serviceId ? { ...s, requiresApiKey: newRequiresApiKey } : s
      )
    );

    try {
      await serviceRoutesApi.updateSecurity(serviceId, newRequiresApiKey);
      showMessage(
        `API key protection ${newRequiresApiKey ? 'enabled' : 'disabled'} successfully`
      );
      // Refresh to ensure consistency
      await fetchServices();
    } catch (error) {
      // Rollback on error
      setServices(prevServices =>
        prevServices.map(s =>
          s.id === serviceId ? { ...s, requiresApiKey: service.requiresApiKey ?? false } : s
        )
      );
      showMessage('Error updating API key protection: ' + error.message, 'error');
    } finally {
      // Remove from toggling set
      setSecurityTogglingIds(prev => {
        const newSet = new Set(prev);
        newSet.delete(serviceId);
        return newSet;
      });
    }
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
          <h1 className="text-3xl font-bold text-white mb-2">Gateway Services</h1>
          <p className="text-slate-400">
            Manage service routes and their configurations
          </p>
        </div>
        <button onClick={handleCreate} className="nexus-button-primary flex items-center space-x-2">
          <Plus className="w-4 h-4" />
          <span>New Gateway Service</span>
        </button>
      </div>

      <div className="nexus-card overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-slate-400">Loading...</div>
        ) : services.length === 0 ? (
          <div className="p-8 text-center text-slate-400">
            No services found. Create your first service to get started.
          </div>
        ) : (
          <div className="overflow-x-auto scrollbar-thin scrollbar-thumb-slate-700 scrollbar-track-slate-800">
          <table className="nexus-table w-full min-w-max">
            <thead>
              <tr>
                <th>Service Name</th>
                <th>Description</th>
                <th>Public Path</th>
                <th>Target URL</th>
                <th>Methods</th>
                <th>Rate Limit/min</th>
                <th>Rate Limit/hr</th>
                <th className="text-center">API Key Protection</th>
                <th className="text-center">Status</th>
                <th className="text-center">Actions</th>
              </tr>
            </thead>
            <tbody>
              {services.map((service) => (
                <tr key={service.id}>
                  <td className="font-medium text-white">{service.serviceName}</td>
                  <td className="max-w-xs truncate">{service.serviceDescription}</td>
                  <td className="font-mono text-blue-400">{service.publicPath}</td>
                  <td className="font-mono text-xs">{service.targetUrl}</td>
                  <td>
                    <div className="flex flex-wrap gap-1">
                      {service.allowedMethods?.map((method) => (
                        <span
                          key={method}
                          className="px-2 py-0.5 bg-slate-700 text-slate-300 rounded text-xs"
                        >
                          {method}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td>{service.rateLimitPerMinute}</td>
                  <td>{service.rateLimitPerHour}</td>
                  <td className="text-center">
                    <div className="flex items-center justify-center">
                      <TooltipProvider>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <button
                              onClick={() => handleSecurityToggle(service)}
                              disabled={securityTogglingIds.has(service.id)}
                              className={`relative inline-flex items-center gap-2 px-4 py-2 rounded-lg font-medium text-xs transition-all duration-200 shadow-sm ${
                                service.requiresApiKey
                                  ? 'bg-gradient-to-r from-blue-600 to-blue-700 text-white hover:from-blue-700 hover:to-blue-800 shadow-blue-900/50'
                                  : 'bg-slate-700/50 text-slate-300 hover:bg-slate-700 border border-slate-600'
                              } disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:from-blue-600 disabled:hover:to-blue-700 disabled:hover:bg-slate-700/50`}
                            >
                              {securityTogglingIds.has(service.id) ? (
                                <>
                                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                                  <span>Updating...</span>
                                </>
                              ) : (
                                <>
                                  <div className="relative">
                                    <Switch
                                      checked={service.requiresApiKey || false}
                                      disabled={securityTogglingIds.has(service.id)}
                                      className={`scale-90 pointer-events-none ${
                                        service.requiresApiKey
                                          ? 'data-[state=checked]:bg-white/20'
                                          : 'data-[state=unchecked]:bg-slate-600'
                                      }`}
                                    />
                                  </div>
                                  <span className="font-semibold">
                                    {service.requiresApiKey ? '🔒 Protected' : '🌐 Public'}
                                  </span>
                                </>
                              )}
                            </button>
                          </TooltipTrigger>
                          <TooltipContent side="top" className="bg-slate-800 border-slate-700">
                            <div className="text-xs max-w-xs">
                              <p className="font-semibold mb-1">
                                {service.requiresApiKey ? '🔒 API Key Required' : '🌐 Public Access'}
                              </p>
                              <p className="text-slate-400">
                                {service.requiresApiKey
                                  ? 'This route requires a valid API key to access'
                                  : 'This route can be accessed without an API key'}
                              </p>
                              <p className="text-slate-500 mt-1 italic">Click to toggle</p>
                            </div>
                          </TooltipContent>
                        </Tooltip>
                      </TooltipProvider>
                    </div>
                  </td>
                  <td className="text-center">
                    <button
                      onClick={() => handleToggle(service)}
                      className={`px-3 py-1 rounded-full text-xs font-medium ${
                        service.isActive
                          ? 'bg-green-900 text-green-300'
                          : 'bg-red-900 text-red-300'
                      }`}
                    >
                      {service.isActive ? 'Active' : 'Disabled'}
                    </button>
                  </td>
                  <td className="text-center">
                    <div className="flex items-center justify-center space-x-2">
                      <button
                        onClick={() => handleEdit(service)}
                        className="p-1 hover:bg-slate-700 rounded"
                        title="Edit"
                      >
                        <Edit className="w-4 h-4 text-slate-400" />
                      </button>
                      <button
                        onClick={() => handleDelete(service.id)}
                        className="p-1 hover:bg-slate-700 rounded"
                        title="Delete"
                      >
                        <Trash2 className="w-4 h-4 text-red-400" />
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

      <ServiceModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleSubmit}
        service={editingService}
      />
    </div>
  );
}
