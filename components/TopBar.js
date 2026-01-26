'use client';

import { CheckCircle } from 'lucide-react';

export default function TopBar() {
  return (
    <header className="h-16 bg-slate-800 border-b border-slate-700 flex items-center justify-between px-8 fixed top-0 left-64 right-0 z-10">
      <div className="flex items-center space-x-4">
        <div className="flex items-center space-x-2">
          <span className="text-sm text-slate-400">Environment:</span>
          <span className="px-3 py-1 bg-slate-700 text-white rounded-md text-sm font-medium">
            Production
          </span>
        </div>
      </div>

      <div className="flex items-center space-x-6">
        <div className="flex items-center space-x-2">
          <CheckCircle className="w-4 h-4 text-green-500" />
          <span className="text-sm text-slate-300">All systems operational</span>
        </div>

        <div className="flex items-center space-x-2">
          <div className="w-2 h-2 bg-green-500 rounded-full"></div>
          <span className="text-sm text-slate-300">Redis: Connected</span>
        </div>
      </div>
    </header>
  );
}
