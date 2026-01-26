// Load Testing Dashboard Page

'use client';

import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import {
  Play,
  List,
  RefreshCw,
  Activity,
  AlertCircle,
  TrendingUp,
  Zap,
} from 'lucide-react';
import { CreateTestDialog } from './components/CreateTestDialog';
import { TestCard } from './components/TestCard';
import { TestDetailsModal } from './components/TestDetailsModal';
import { TestHistoryTable } from './components/TestHistoryTable';
import { useLoadTests } from './hooks/useLoadTests';
import { useTestStatus } from './hooks/useTestStatus';
import { useToast } from '@/hooks/use-toast';
import { stopTest, getTestResult } from './api/loadTester';
import { LoadTestStatus, LoadTestResult } from './types/loadTester.types';

export default function LoadTestingPage() {
  const { toast } = useToast();
  const { tests, loading, error, isHealthy, refresh } = useLoadTests(true, 10000);
  
  const [selectedTestId, setSelectedTestId] = useState<string | null>(null);
  const [detailsModalOpen, setDetailsModalOpen] = useState(false);
  const [selectedTestData, setSelectedTestData] = useState<LoadTestStatus | LoadTestResult | null>(null);
  const [activeTestIds, setActiveTestIds] = useState<string[]>([]);

  // Track running tests and update their status
  useEffect(() => {
    const runningTests = tests.filter(t => t.status === 'RUNNING');
    setActiveTestIds(runningTests.map(t => t.testId));
  }, [tests]);

  const handleTestCreated = (testId: string) => {
    toast({
      title: 'Test Started Successfully!',
      description: 'Your load test is now running',
    });
    
    // Add to active tests
    setActiveTestIds(prev => [...prev, testId]);
    
    // Refresh list
    refresh();
  };

  const handleViewDetails = async (testId: string) => {
    try {
      const result = await getTestResult(testId);
      setSelectedTestData(result);
      setSelectedTestId(testId);
      setDetailsModalOpen(true);
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to Load Details',
        description: error instanceof Error ? error.message : 'An error occurred',
      });
    }
  };

  const handleStopTest = async (testId: string) => {
    try {
      await stopTest(testId);
      toast({
        title: 'Test Stopped',
        description: `Test ${testId.substring(0, 12)}... has been stopped`,
      });
      
      // Remove from active tests
      setActiveTestIds(prev => prev.filter(id => id !== testId));
      
      // Refresh list
      refresh();
      
      // If details modal is open for this test, close it
      if (selectedTestId === testId) {
        setDetailsModalOpen(false);
      }
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to Stop Test',
        description: error instanceof Error ? error.message : 'An error occurred',
      });
    }
  };

  const handleExport = async (testId: string) => {
    try {
      const result = await getTestResult(testId);
      const dataStr = JSON.stringify(result, null, 2);
      const dataUri = `data:application/json;charset=utf-8,${encodeURIComponent(dataStr)}`;
      const exportFileDefaultName = `load-test-${testId}-${Date.now()}.json`;

      const linkElement = document.createElement('a');
      linkElement.setAttribute('href', dataUri);
      linkElement.setAttribute('download', exportFileDefaultName);
      linkElement.click();

      toast({
        title: 'Export Successful',
        description: 'Test results downloaded',
      });
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to Export',
        description: error instanceof Error ? error.message : 'An error occurred',
      });
    }
  };

  const handleDelete = (testId: string) => {
    // In a real implementation, this would call a delete API endpoint
    toast({
      title: 'Delete Not Implemented',
      description: 'Delete functionality would be implemented here',
      variant: 'default',
    });
  };

  // Get active tests (running tests with real-time data)
  const activeTests = tests.filter(t => activeTestIds.includes(t.testId));
  const completedTests = tests.filter(t => !activeTestIds.includes(t.testId));

  // Get statistics
  const totalTests = tests.length;
  const runningCount = tests.filter(t => t.status === 'RUNNING').length;
  const completedCount = tests.filter(t => t.status === 'COMPLETED').length;
  const failedCount = tests.filter(t => t.status === 'FAILED').length;

  return (
    <div className="container mx-auto p-6 space-y-8 max-w-7xl">
      {/* Header Section */}
      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Load Testing Dashboard</h1>
            <p className="text-muted-foreground mt-1">
              Monitor and execute API load tests
            </p>
          </div>
          <div className="flex items-center gap-2">
            {isHealthy ? (
              <Badge className="bg-green-500 hover:bg-green-600 gap-2">
                <span className="relative flex h-2 w-2">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-300 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-2 w-2 bg-white"></span>
                </span>
                Service Online
              </Badge>
            ) : (
              <Badge variant="destructive" className="gap-2">
                <AlertCircle className="h-3 w-3" />
                Service Offline
              </Badge>
            )}
          </div>
        </div>
      </div>

      {/* Quick Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Tests</CardTitle>
            <Activity className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{totalTests}</div>
            <p className="text-xs text-muted-foreground">All time</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Running</CardTitle>
            <Zap className="h-4 w-4 text-amber-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-amber-500">{runningCount}</div>
            <p className="text-xs text-muted-foreground">Active tests</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Completed</CardTitle>
            <TrendingUp className="h-4 w-4 text-green-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-500">{completedCount}</div>
            <p className="text-xs text-muted-foreground">Successful</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Failed</CardTitle>
            <AlertCircle className="h-4 w-4 text-red-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-500">{failedCount}</div>
            <p className="text-xs text-muted-foreground">With errors</p>
          </CardContent>
        </Card>
      </div>

      {/* Quick Actions */}
      <Card>
        <CardHeader>
          <CardTitle>Quick Actions</CardTitle>
          <CardDescription>
            {isHealthy 
              ? 'Start a new test or manage existing ones'
              : '⚠️ Load Tester Service is offline. Please start the service to create tests.'}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-3">
            <CreateTestDialog onTestCreated={handleTestCreated}>
              <Button size="lg" className="gap-2" disabled={!isHealthy}>
                <Play className="h-4 w-4" />
                New Load Test
              </Button>
            </CreateTestDialog>
            <Button
              variant="outline"
              size="lg"
              onClick={() => refresh()}
              className="gap-2"
            >
              <RefreshCw className="h-4 w-4" />
              Refresh Status
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Active Tests Section */}
      {activeTests.length > 0 && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-2xl font-bold tracking-tight">Active Tests</h2>
            <Badge variant="secondary" className="gap-2">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-amber-500"></span>
              </span>
              {activeTests.length} Running
            </Badge>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {activeTests.map((test) => (
              <ActiveTestCard
                key={test.testId}
                testId={test.testId}
                onViewDetails={() => handleViewDetails(test.testId)}
                onStop={() => handleStopTest(test.testId)}
              />
            ))}
          </div>
        </div>
      )}

      <Separator />

      {/* Test History Section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-bold tracking-tight">Test History</h2>
            <p className="text-sm text-muted-foreground mt-1">
              View and manage all your load tests
            </p>
          </div>
          {!isHealthy && (
            <Badge variant="destructive" className="gap-2">
              <AlertCircle className="h-3 w-3" />
              Service Offline
            </Badge>
          )}
        </div>

        {loading && tests.length === 0 ? (
          <div className="space-y-3">
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-64 w-full" />
          </div>
        ) : error && !isHealthy ? (
          <Card>
            <CardContent className="py-12">
              <div className="text-center">
                <AlertCircle className="h-16 w-16 mx-auto mb-4 text-red-500" />
                <p className="text-lg font-semibold mb-2">Load Tester Service Not Available</p>
                <p className="text-sm text-muted-foreground mb-4">
                  Please ensure the Load Tester Service is running on:
                </p>
                <code className="bg-muted px-4 py-2 rounded text-sm">
                  http://localhost:8083
                </code>
                <div className="mt-6 space-y-2">
                  <p className="text-xs text-muted-foreground">To start the service, run:</p>
                  <code className="block bg-muted px-4 py-2 rounded text-xs">
                    # Start your Load Tester Service on port 8083
                  </code>
                </div>
                <Button onClick={() => refresh()} className="mt-6" variant="outline">
                  <RefreshCw className="h-4 w-4 mr-2" />
                  Try Again
                </Button>
              </div>
            </CardContent>
          </Card>
        ) : (
          <TestHistoryTable
            tests={tests}
            onViewDetails={handleViewDetails}
            onExport={handleExport}
            onDelete={handleDelete}
          />
        )}
      </div>

      {/* Test Details Modal */}
      <TestDetailsModal
        test={selectedTestData}
        open={detailsModalOpen}
        onOpenChange={setDetailsModalOpen}
        onStop={selectedTestId ? () => handleStopTest(selectedTestId) : undefined}
      />
    </div>
  );
}

// Active Test Card with Real-time Updates
function ActiveTestCard({ 
  testId, 
  onViewDetails, 
  onStop 
}: { 
  testId: string; 
  onViewDetails: () => void; 
  onStop: () => void; 
}) {
  const { status, loading } = useTestStatus(testId, true);

  if (loading || !status) {
    return (
      <Card>
        <CardContent className="py-6">
          <div className="space-y-3">
            <Skeleton className="h-6 w-full" />
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-20 w-full" />
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <TestCard
      test={status}
      onViewDetails={onViewDetails}
      onStop={onStop}
    />
  );
}
