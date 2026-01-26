'use client';

import { Settings, Database, Shield, Bell } from 'lucide-react';

export default function SettingsPage() {
  const settingsSections = [
    {
      title: 'General Settings',
      icon: Settings,
      description: 'Configure basic gateway settings and preferences',
    },
    {
      title: 'Database Configuration',
      icon: Database,
      description: 'Manage database connections and caching',
    },
    {
      title: 'Security & Authentication',
      icon: Shield,
      description: 'Configure authentication methods and security policies',
    },
    {
      title: 'Notifications & Alerts',
      icon: Bell,
      description: 'Set up email and webhook notifications',
    },
  ];

  return (
    <div className="p-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-white mb-2">Settings</h1>
        <p className="text-slate-400">
          Configure your NexusGate Control Plane settings
        </p>
      </div>

      <div className="mb-6 p-4 bg-blue-900/30 border border-blue-700 rounded-lg">
        <p className="text-sm text-blue-300">
          <strong>Note:</strong> Settings page is under development. Advanced
          configuration options will be available in future releases.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {settingsSections.map((section) => {
          const Icon = section.icon;
          return (
            <div key={section.title} className="nexus-card p-6">
              <div className="flex items-start space-x-4">
                <div className="w-12 h-12 bg-blue-900 rounded-lg flex items-center justify-center flex-shrink-0">
                  <Icon className="w-6 h-6 text-blue-400" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-white mb-2">
                    {section.title}
                  </h3>
                  <p className="text-sm text-slate-400">{section.description}</p>
                  <button className="mt-4 px-4 py-2 bg-slate-700 text-slate-300 rounded-md text-sm hover:bg-slate-600 transition-colors cursor-not-allowed opacity-50">
                    Coming Soon
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      <div className="mt-8 nexus-card p-6">
        <h2 className="text-xl font-semibold text-white mb-4">
          System Information
        </h2>
        <div className="space-y-3">
          <div className="flex justify-between py-2 border-b border-slate-700">
            <span className="text-slate-400">Version</span>
            <span className="text-white font-mono">1.0.0</span>
          </div>
          <div className="flex justify-between py-2 border-b border-slate-700">
            <span className="text-slate-400">Config Service</span>
            <span className="text-green-400">http://localhost:8082</span>
          </div>
          <div className="flex justify-between py-2 border-b border-slate-700">
            <span className="text-slate-400">Environment</span>
            <span className="text-white">Production</span>
          </div>
          <div className="flex justify-between py-2">
            <span className="text-slate-400">Last Updated</span>
            <span className="text-white">2026-01-20</span>
          </div>
        </div>
      </div>
    </div>
  );
}
