// Test Card Component

'use client';

import { Card, CardContent, CardFooter, CardHeader } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { Badge } from '@/components/ui/badge';
import {
  Eye,
  StopCircle,
  Copy,
  CheckCircle,
  AlertTriangle,
  Clock,
  Activity,
  TrendingUp,
  ExternalLink,
} from 'lucide-react';
import { StatusBadge } from './StatusBadge';
import { LoadTestStatus } from '../types/loadTester.types';
import { useToast } from '@/hooks/use-toast';
import { formatDistanceToNow } from 'date-fns';

interface TestCardProps {
  test: LoadTestStatus;
  onViewDetails: () => void;
  onStop?: () => void;
  showActions?: boolean;
}

export function TestCard({ test, onViewDetails, onStop, showActions = true }: TestCardProps) {
  const { toast } = useToast();
  const isRunning = test.status === 'RUNNING';

  const copyTestId = () => {
    navigator.clipboard.writeText(test.testId);
    toast({
      title: 'Copied! ✓',
      description: 'Test ID copied to clipboard',
    });
  };

  const getProgress = (): number => {
    if (!isRunning) return 100;
    // Calculate progress based on requests made vs configured rate and time
    const requestRate = test.configuredRequestRate ?? 100;
    const totalReqs = test.totalRequests ?? 0;
    const expectedRequests = requestRate * 30; // Assuming 30s default
    return Math.min((totalReqs / expectedRequests) * 100, 100);
  };

  const formatNumber = (num: number): string => {
    if (num >= 1000000) return `${(num / 1000000).toFixed(1)}M`;
    if (num >= 1000) return `${(num / 1000).toFixed(1)}K`;
    return num.toString();
  };

  const getSuccessRateColor = (rate: number): string => {
    if (rate >= 90) return 'text-green-600';
    if (rate >= 70) return 'text-yellow-600';
    return 'text-red-600';
  };

  const getLatencyColor = (latency: number): string => {
    if (latency < 100) return 'text-green-600';
    if (latency < 500) return 'text-yellow-600';
    return 'text-red-600';
  };

  return (
    <Card className="hover:shadow-lg transition-shadow">
      <CardHeader className="space-y-3">
        <div className="flex items-start justify-between">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-2">
              <StatusBadge status={test.status} />
              {isRunning && (
                <Badge variant="outline" className="text-xs">
                  <span className="relative flex h-2 w-2 mr-1">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
                    <span className="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
                  </span>
                  Live
                </Badge>
              )}
            </div>
            <div className="flex items-center gap-2">
              <code 
                className="text-xs font-mono text-muted-foreground cursor-pointer hover:text-foreground transition-colors"
                onClick={copyTestId}
                title="Click to copy"
              >
                {test.testId.substring(0, 12)}...
                <Copy className="inline h-3 w-3 ml-1" />
              </code>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2 text-sm">
          <ExternalLink className="h-4 w-4 text-muted-foreground" />
          <span className="font-medium truncate" title={test.targetEndpoint}>
            {test.targetEndpoint}
          </span>
        </div>

        {/* Progress Bar */}
        <div className="space-y-1">
          <Progress value={getProgress()} className="h-2" />
          <div className="flex justify-between text-xs text-muted-foreground">
            <span>
              {isRunning ? 'In progress...' : 'Completed'}
            </span>
            <span>{Math.round(getProgress())}%</span>
          </div>
        </div>
      </CardHeader>

      <CardContent>
        {/* Metrics Grid */}
        <div className="grid grid-cols-2 gap-3">
          {/* Total Requests */}
          <div className="flex items-center gap-2 p-3 bg-muted/50 rounded-lg">
            <Activity className="h-4 w-4 text-blue-600" />
            <div className="flex-1 min-w-0">
              <div className="text-lg font-bold">
                {formatNumber(test.totalRequests ?? 0)}
              </div>
              <div className="text-xs text-muted-foreground">
                Total Requests
              </div>
            </div>
          </div>

          {/* Success Rate */}
          <div className="flex items-center gap-2 p-3 bg-muted/50 rounded-lg">
            <CheckCircle className={`h-4 w-4 ${getSuccessRateColor(test.successRate ?? 0)}`} />
            <div className="flex-1 min-w-0">
              <div className={`text-lg font-bold ${getSuccessRateColor(test.successRate ?? 0)}`}>
                {(test.successRate ?? 0).toFixed(1)}%
              </div>
              <div className="text-xs text-muted-foreground">
                Success Rate
              </div>
            </div>
          </div>

          {/* Rate Limited */}
          <div className="flex items-center gap-2 p-3 bg-muted/50 rounded-lg">
            <AlertTriangle className="h-4 w-4 text-yellow-600" />
            <div className="flex-1 min-w-0">
              <div className="text-lg font-bold text-yellow-600">
                {formatNumber(test.rateLimitedRequests ?? 0)}
              </div>
              <div className="text-xs text-muted-foreground">
                Rate Limited (429)
              </div>
            </div>
          </div>

          {/* Average Latency */}
          <div className="flex items-center gap-2 p-3 bg-muted/50 rounded-lg">
            <Clock className={`h-4 w-4 ${getLatencyColor(test.averageLatencyMs ?? 0)}`} />
            <div className="flex-1 min-w-0">
              <div className={`text-lg font-bold ${getLatencyColor(test.averageLatencyMs ?? 0)}`}>
                {(test.averageLatencyMs ?? 0).toFixed(0)}ms
              </div>
              <div className="text-xs text-muted-foreground">
                Avg Latency
              </div>
            </div>
          </div>
        </div>

        {/* Additional Info */}
        <div className="mt-4 pt-4 border-t space-y-2">
          <div className="flex justify-between text-xs">
            <span className="text-muted-foreground">Request Rate:</span>
            <span className="font-medium">
              {(test.requestsPerSecond ?? 0).toFixed(1)} / {test.configuredRequestRate ?? 0} req/s
            </span>
          </div>
          <div className="flex justify-between text-xs">
            <span className="text-muted-foreground">Concurrency:</span>
            <span className="font-medium">{test.concurrencyLevel ?? 0} clients</span>
          </div>
          {test.p95LatencyMs != null && (
            <div className="flex justify-between text-xs">
              <span className="text-muted-foreground">P95 Latency:</span>
              <span className={`font-medium ${getLatencyColor(test.p95LatencyMs)}`}>
                {test.p95LatencyMs.toFixed(0)}ms
              </span>
            </div>
          )}
        </div>
      </CardContent>

      {showActions && (
        <CardFooter className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={onViewDetails}
            className="flex-1 gap-2"
          >
            <Eye className="h-4 w-4" />
            View Details
          </Button>
          {isRunning && onStop && (
            <Button
              variant="destructive"
              size="sm"
              onClick={onStop}
              className="gap-2"
            >
              <StopCircle className="h-4 w-4" />
              Stop
            </Button>
          )}
        </CardFooter>
      )}
    </Card>
  );
}
