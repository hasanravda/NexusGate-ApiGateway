import Sidebar from '@/components/Sidebar';
import TopBar from '@/components/TopBar';

export default function DashboardLayout({ children }) {
  return (
    <div className="min-h-screen bg-slate-900">
      <Sidebar />
      <TopBar />
      <main className="ml-64 pt-16">
        {children}
      </main>
    </div>
  );
}
