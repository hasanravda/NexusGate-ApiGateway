// Test Details Modal Component

'use client';

import { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion';
import {
  Copy,
  Download,
  StopCircle,
  Activity,
  Clock,
  CheckCircle,
  XCircle,
  AlertTriangle,
  Loader2,
  Eye,
  EyeOff,
  TrendingUp,
  Zap,
} from 'lucide-react';
import { StatusBadge } from './StatusBadge';
import { MetricsCard } from './MetricsCard';
import { LoadTestResult, LoadTestStatus } from '../types/loadTester.types';
import { useToast } from '@/hooks/use-toast';
import { format } from 'date-fns';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Progress } from '@/components/ui/progress';

interface TestDetailsModalProps {
  test: LoadTestStatus | LoadTestResult | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onStop?: () => void;
}

export function TestDetailsModal({ test, open, onOpenChange, onStop }: TestDetailsModalProps) {
  const { toast } = useToast();
  const [showApiKey, setShowApiKey] = useState(false);
  const [stopping, setStopping] = useState(false);

  if (!test) return null;

  const isFullResult = 'statusCodeDistribution' in test;
  const isRunning = test.status === 'RUNNING';

  const copyTestId = () => {
    navigator.clipboard.writeText(test.testId);
    toast({
      title: 'Copied! ✓',
      description: 'Test ID copied to clipboard',
    });
  };

  const exportResults = () => {
    const dataStr = JSON.stringify(test, null, 2);
    const dataUri = `data:application/json;charset=utf-8,${encodeURIComponent(dataStr)}`;
    const exportFileDefaultName = `load-test-${test.testId}-${Date.now()}.json`;

    const linkElement = document.createElement('a');
    linkElement.setAttribute('href', dataUri);
    linkElement.setAttribute('download', exportFileDefaultName);
    linkElement.click();

    toast({
      title: 'Export Successful',
      description: 'Test results downloaded',
    });
  };

  const handleStop = async () => {
    if (onStop) {
      setStopping(true);
      try {
        await onStop();
      } finally {
        setStopping(false);
      }
    }
  };

  const getStatusCodeData = () => {
    if (!isFullResult || !test.statusCodeDistribution) return [];
    
    return Object.entries(test.statusCodeDistribution).map(([code, count]) => ({
      code: parseInt(code),
      count,
      name: `${code}`,
      fill: getStatusCodeColor(parseInt(code)),
    }));
  };

  const getStatusCodeColor = (code: number): string => {
    if (code >= 200 && code < 300) return '#22c55e'; // green
    if (code >= 400 && code < 500) return '#eab308'; // yellow
    if (code >= 500) return '#ef4444'; // red
    return '#6b7280'; // gray
  };

  const formatDateTime = (dateString: string) => {
    try {
      return format(new Date(dateString), 'MMM dd, yyyy HH:mm:ss');
    } catch {
      return dateString;
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-5xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-start justify-between">
            <div className="space-y-2">
              <DialogTitle className="text-2xl">Load Test Details</DialogTitle>
              <div className="flex items-center gap-2">
                <code 
                  className="text-sm font-mono text-muted-foreground cursor-pointer hover:text-foreground transition-colors"
                  onClick={copyTestId}
                  title="Click to copy"
                >
                  {test.testId}
                  <Copy className="inline h-3 w-3 ml-2" />
                </code>
                <StatusBadge status={test.status} size="sm" />
              </div>
            </div>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={exportResults}
                className="gap-2"
              >
                <Download className="h-4 w-4" />
                Export
              </Button>
              {isRunning && onStop && (
                <Button
                  variant="destructive"
                  size="sm"
                  onClick={handleStop}
                  disabled={stopping}
                  className="gap-2"
                >
                  {stopping ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Stopping...
                    </>
                  ) : (
                    <>
                      <StopCircle className="h-4 w-4" />
                      Stop Test
                    </>
                  )}
                </Button>
              )}
            </div>
          </div>
          {isFullResult && test.startTime && test.endTime && (
            <DialogDescription className="text-sm">
              {formatDateTime(test.startTime)} → {formatDateTime(test.endTime)}
            </DialogDescription>
          )}
        </DialogHeader>

        <div className="space-y-6 py-4">
          {/* Metrics Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {/* Request Metrics Card */}
            <MetricsCard
              title="Total Requests"
              value={(test.totalRequests ?? 0).toLocaleString()}
              icon={Activity}
              color="default"
            >
              <div className="space-y-2 text-xs">
                <div className="flex justify-between items-center">
                  <span className="text-muted-foreground">Successful</span>
                  <span className="text-green-600 font-semibold">
                    {(test.successfulRequests ?? 0).toLocaleString()} ({(test.successRate ?? 0).toFixed(1)}%)
                  </span>
                </div>
                <Progress value={test.successRate ?? 0} className="h-1" />
                
                <div className="flex justify-between items-center">
                  <span className="text-muted-foreground">Rate Limited</span>
                  <span className="text-yellow-600 font-semibold">
                    {(test.rateLimitedRequests ?? 0).toLocaleString()} ({(test.rateLimitRate ?? 0).toFixed(1)}%)
                  </span>
                </div>
                <Progress value={test.rateLimitRate ?? 0} className="h-1 [&>div]:bg-yellow-500" />
                
                <div className="flex justify-between items-center">
                  <span className="text-muted-foreground">Errors</span>
                  <span className="text-red-600 font-semibold">
                    {(test.errorRequests ?? 0).toLocaleString()}
                    {isFullResult && test.errorRate != null && ` (${test.errorRate.toFixed(1)}%)`}
                  </span>
                </div>
                {isFullResult && test.errorRate != null && (
                  <Progress value={test.errorRate} className="h-1 [&>div]:bg-red-500" />
                )}
              </div>
            </MetricsCard>

            {/* Performance Metrics Card */}
            <MetricsCard
              title="Average Latency"
              value={`${(test.averageLatencyMs ?? 0).toFixed(0)}ms`}
              icon={Clock}
              color={(test.averageLatencyMs ?? 0) < 100 ? 'success' : (test.averageLatencyMs ?? 0) < 500 ? 'warning' : 'error'}
            >
              <div className="space-y-2 text-xs">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">P95 Latency</span>
                  <span className={`font-semibold ${
                    (test.p95LatencyMs ?? 0) < 100 ? 'text-green-600' : 
                    (test.p95LatencyMs ?? 0) < 500 ? 'text-yellow-600' : 'text-red-600'
                  }`}>
                    {(test.p95LatencyMs ?? 0).toFixed(0)}ms
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Min Latency</span>
                  <span className="font-semibold text-green-600">
                    {(test.minLatencyMs ?? 0).toFixed(0)}ms
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Max Latency</span>
                  <span className="font-semibold text-red-600">
                    {(test.maxLatencyMs ?? 0).toFixed(0)}ms
                  </span>
                </div>
              </div>
            </MetricsCard>

            {/* Throughput Metrics Card */}
            <MetricsCard
              title="Throughput"
              value={`${(test.requestsPerSecond ?? 0).toFixed(1)} req/s`}
              icon={Zap}
              color="default"
            >
              <div className="space-y-2 text-xs">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Configured Rate</span>
                  <span className="font-semibold">
                    {test.configuredRequestRate ?? 0} req/s
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Achievement</span>
                  <span className="font-semibold text-blue-600">
                    {(((test.requestsPerSecond ?? 0) / (test.configuredRequestRate ?? 1)) * 100).toFixed(1)}%
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Concurrency</span>
                  <span className="font-semibold">
                    {test.concurrencyLevel ?? 0} clients
                  </span>
                </div>
              </div>
            </MetricsCard>
          </div>

          {/* Status Code Distribution Chart */}
          {isFullResult && getStatusCodeData().length > 0 && (
            <div className="space-y-3">
              <h3 className="text-lg font-semibold">Status Code Distribution</h3>
              <div className="bg-muted/30 p-4 rounded-lg">
                <ResponsiveContainer width="100%" height={250}>
                  <BarChart data={getStatusCodeData()}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" />
                    <YAxis />
                    <Tooltip 
                      formatter={(value) => value.toLocaleString()}
                      labelFormatter={(label) => `Status Code: ${label}`}
                    />
                    <Legend />
                    <Bar dataKey="count" name="Count" />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          )}

          <Separator />

          {/* Configuration Details */}
          <Accordion type="single" collapsible className="w-full">
            <AccordionItem value="config">
              <AccordionTrigger className="text-lg font-semibold">
                Test Configuration
              </AccordionTrigger>
              <AccordionContent>
                <div className="space-y-3 pt-2">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="text-sm font-medium text-muted-foreground">
                        Target Endpoint
                      </label>
                      <p className="text-sm font-mono mt-1 break-all">
                        {test.targetEndpoint}
                      </p>
                    </div>

                    {isFullResult && (
                      <div>
                        <label className="text-sm font-medium text-muted-foreground">
                          API Key
                        </label>
                        <div className="flex items-center gap-2 mt-1">
                          <code className="text-sm font-mono">
                            {showApiKey ? test.targetKey : '••••••••••••••••'}
                          </code>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setShowApiKey(!showApiKey)}
                          >
                            {showApiKey ? (
                              <EyeOff className="h-3 w-3" />
                            ) : (
                              <Eye className="h-3 w-3" />
                            )}
                          </Button>
                        </div>
                      </div>
                    )}

                    {isFullResult && (
                      <>
                        <div>
                          <label className="text-sm font-medium text-muted-foreground">
                            Request Pattern
                          </label>
                          <p className="text-sm mt-1">
                            <Badge variant="outline">{test.requestPattern}</Badge>
                          </p>
                        </div>

                        <div>
                          <label className="text-sm font-medium text-muted-foreground">
                            HTTP Method
                          </label>
                          <p className="text-sm mt-1">
                            <Badge variant="outline" className="font-mono">
                              {test.httpMethod}
                            </Badge>
                          </p>
                        </div>

                        <div>
                          <label className="text-sm font-medium text-muted-foreground">
                            Test Duration
                          </label>
                          <p className="text-sm mt-1 font-semibold">
                            {test.testDurationSeconds} seconds
                          </p>
                        </div>
                      </>
                    )}

                    <div>
                      <label className="text-sm font-medium text-muted-foreground">
                        Configured Rate
                      </label>
                      <p className="text-sm mt-1 font-semibold">
                        {test.configuredRequestRate} req/s
                      </p>
                    </div>

                    <div>
                      <label className="text-sm font-medium text-muted-foreground">
                        Concurrency Level
                      </label>
                      <p className="text-sm mt-1 font-semibold">
                        {test.concurrencyLevel} clients
                      </p>
                    </div>
                  </div>
                </div>
              </AccordionContent>
            </AccordionItem>
          </Accordion>

          {/* Real-time indicator */}
          {isRunning && (
            <div className="flex items-center justify-center gap-2 text-sm text-muted-foreground">
              <span className="relative flex h-3 w-3">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-3 w-3 bg-green-500"></span>
              </span>
              <span>Live data - updating every 3 seconds</span>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
