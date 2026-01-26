'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard,
  Network,
  Key,
  Gauge,
  FileText,
  Settings,
  Activity
} from 'lucide-react';

export default function Sidebar() {
  const pathname = usePathname();

  const navItems = [
    // { name: 'Overview', href: '/dashboard', icon: LayoutDashboard },
    { name: 'Gateway Services', href: '/dashboard/services', icon: Network },
    { name: 'API Keys', href: '/dashboard/keys', icon: Key },
    { name: 'Rate Limits', href: '/dashboard/rate-limits', icon: Gauge },
    { name: 'Load Testing', href: '/dashboard/load-testing', icon: Activity },
    { name: 'Logs & Violations', href: '/dashboard/logs', icon: FileText },
    { name: 'Settings', href: '/dashboard/settings', icon: Settings },
  ];

  return (
    <aside className="w-64 bg-slate-800 border-r border-slate-700 flex flex-col h-screen fixed left-0 top-0">
      <div className="p-6 border-b border-slate-700">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 nexus-gradient rounded-lg flex items-center justify-center">
            <Network className="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-white">NexusGate</h1>
            <p className="text-xs text-slate-400">Control Plane</p>
          </div>
        </div>
      </div>

      <nav className="flex-1 p-4 space-y-1">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = pathname === item.href;

          return (
            <Link
              key={item.href}
              href={item.href}
              className={`flex items-center space-x-3 px-4 py-3 rounded-lg transition-colors ${
                isActive
                  ? 'bg-blue-600 text-white'
                  : 'text-slate-300 hover:bg-slate-700'
              }`}
            >
              <Icon className="w-5 h-5" />
              <span className="font-medium">{item.name}</span>
            </Link>
          );
        })}
      </nav>

      <div className="p-4 border-t border-slate-700">
        <div className="text-xs text-slate-500">
          Version 1.0.0
        </div>
      </div>
    </aside>
  );
}
