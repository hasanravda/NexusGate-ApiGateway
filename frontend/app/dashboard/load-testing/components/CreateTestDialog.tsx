// Create Test Dialog Component

'use client';

import { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Slider } from '@/components/ui/slider';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { Play, Loader2, Zap, TrendingUp, Target } from 'lucide-react';
import { LoadTestRequest, HttpMethod, RequestPattern } from '../types/loadTester.types';
import { startLoadTest } from '../api/loadTester';
import { useToast } from '@/hooks/use-toast';

interface CreateTestDialogProps {
  onTestCreated?: (testId: string) => void;
  children?: React.ReactNode;
}

export function CreateTestDialog({ onTestCreated, children }: CreateTestDialogProps) {
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();

  const [formData, setFormData] = useState<LoadTestRequest>({
    targetKey: '',
    targetEndpoint: '',
    requestRate: 50,
    durationSeconds: 30,
    concurrencyLevel: 5,
    requestPattern: 'CONSTANT_RATE',
    httpMethod: 'GET',
  });

  const [errors, setErrors] = useState<Partial<Record<keyof LoadTestRequest, string>>>({});

  const validateForm = (): boolean => {
    const newErrors: Partial<Record<keyof LoadTestRequest, string>> = {};

    if (!formData.targetKey.trim()) {
      newErrors.targetKey = 'API key is required';
    }

    if (!formData.targetEndpoint.trim()) {
      newErrors.targetEndpoint = 'Target endpoint is required';
    } else if (!formData.targetEndpoint.startsWith('http')) {
      newErrors.targetEndpoint = 'Must be a valid HTTP/HTTPS URL';
    }

    if (formData.requestRate < 1 || formData.requestRate > 10000) {
      newErrors.requestRate = 'Request rate must be between 1 and 10,000';
    }

    if (formData.durationSeconds < 1 || formData.durationSeconds > 3600) {
      newErrors.durationSeconds = 'Duration must be between 1 and 3600 seconds';
    }

    if (formData.concurrencyLevel < 1 || formData.concurrencyLevel > 500) {
      newErrors.concurrencyLevel = 'Concurrency must be between 1 and 500';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async () => {
    if (!validateForm()) {
      toast({
        variant: 'destructive',
        title: 'Validation Error',
        description: 'Please fix the errors in the form',
      });
      return;
    }

    setLoading(true);
    try {
      const response = await startLoadTest(formData);
      
      toast({
        title: 'Load Test Started! 🚀',
        description: `Test ID: ${response.testId}`,
      });

      setOpen(false);
      
      // Reset form
      setFormData({
        targetKey: '',
        targetEndpoint: '',
        requestRate: 50,
        durationSeconds: 30,
        concurrencyLevel: 5,
        requestPattern: 'CONSTANT_RATE',
        httpMethod: 'GET',
      });

      if (onTestCreated) {
        onTestCreated(response.testId);
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'An error occurred';
      const isConnectionError = errorMessage.includes('fetch') || errorMessage.includes('Failed to fetch');
      
      toast({
        variant: 'destructive',
        title: 'Failed to Start Test',
        description: isConnectionError 
          ? '❌ Cannot connect to Load Tester Service. Please ensure the service is running on http://localhost:8083'
          : errorMessage,
      });
    } finally {
      setLoading(false);
    }
  };

  const applyPreset = (preset: 'light' | 'standard' | 'stress') => {
    const presets = {
      light: { requestRate: 10, durationSeconds: 30, concurrencyLevel: 2 },
      standard: { requestRate: 50, durationSeconds: 60, concurrencyLevel: 5 },
      stress: { requestRate: 500, durationSeconds: 30, concurrencyLevel: 20 },
    };

    setFormData(prev => ({ ...prev, ...presets[preset] }));
    
    toast({
      title: `${preset.charAt(0).toUpperCase() + preset.slice(1)} Preset Applied`,
      description: 'Configuration updated',
    });
  };

  const formatDuration = (seconds: number): string => {
    if (seconds < 60) return `${seconds} seconds`;
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return remainingSeconds > 0 
      ? `${minutes}m ${remainingSeconds}s` 
      : `${minutes} minute${minutes > 1 ? 's' : ''}`;
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        {children || (
          <Button size="lg" className="gap-2">
            <Play className="h-4 w-4" />
            New Load Test
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-2xl">Create New Load Test</DialogTitle>
          <DialogDescription>
            Configure your API load test parameters. All fields are required.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6 py-4">
          {/* Quick Presets */}
          <div className="space-y-2">
            <Label className="text-sm font-semibold">Quick Presets</Label>
            <div className="flex gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => applyPreset('light')}
                className="flex-1"
              >
                <Zap className="h-3 w-3 mr-1" />
                Light Test
              </Button>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => applyPreset('standard')}
                className="flex-1"
              >
                <Target className="h-3 w-3 mr-1" />
                Standard Test
              </Button>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => applyPreset('stress')}
                className="flex-1"
              >
                <TrendingUp className="h-3 w-3 mr-1" />
                Stress Test
              </Button>
            </div>
          </div>

          {/* Target Endpoint */}
          <div className="space-y-2">
            <Label htmlFor="endpoint">
              Target Endpoint <span className="text-red-500">*</span>
            </Label>
            <Input
              id="endpoint"
              placeholder="http://localhost:8081/api/users"
              value={formData.targetEndpoint}
              onChange={(e) => setFormData(prev => ({ ...prev, targetEndpoint: e.target.value }))}
              className={errors.targetEndpoint ? 'border-red-500' : ''}
            />
            {errors.targetEndpoint && (
              <p className="text-xs text-red-500">{errors.targetEndpoint}</p>
            )}
            <p className="text-xs text-muted-foreground">
              Gateway endpoint to test
            </p>
          </div>

          {/* API Key */}
          <div className="space-y-2">
            <Label htmlFor="apiKey">
              API Key <span className="text-red-500">*</span>
            </Label>
            <Input
              id="apiKey"
              placeholder="nx_test_key_123"
              value={formData.targetKey}
              onChange={(e) => setFormData(prev => ({ ...prev, targetKey: e.target.value }))}
              className={errors.targetKey ? 'border-red-500' : ''}
            />
            {errors.targetKey && (
              <p className="text-xs text-red-500">{errors.targetKey}</p>
            )}
            <p className="text-xs text-muted-foreground">
              Authentication key for the gateway
            </p>
          </div>

          {/* Request Rate */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <Label>Request Rate (req/s)</Label>
              <Badge variant="secondary" className="font-mono">
                {formData.requestRate} req/s
              </Badge>
            </div>
            <Slider
              value={[formData.requestRate]}
              onValueChange={(value) => setFormData(prev => ({ ...prev, requestRate: value[0] }))}
              min={1}
              max={1000}
              step={1}
              className="w-full"
            />
            <div className="flex gap-2">
              <Input
                type="number"
                value={formData.requestRate}
                onChange={(e) => setFormData(prev => ({ 
                  ...prev, 
                  requestRate: Math.max(1, Math.min(10000, parseInt(e.target.value) || 1))
                }))}
                min={1}
                max={10000}
                className="w-24"
              />
              <p className="text-xs text-muted-foreground flex items-center">
                1-10,000 requests per second
              </p>
            </div>
          </div>

          {/* Duration */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <Label>Duration</Label>
              <Badge variant="secondary" className="font-mono">
                {formatDuration(formData.durationSeconds)}
              </Badge>
            </div>
            <Slider
              value={[formData.durationSeconds]}
              onValueChange={(value) => setFormData(prev => ({ ...prev, durationSeconds: value[0] }))}
              min={10}
              max={300}
              step={5}
              className="w-full"
            />
            <div className="flex gap-2">
              <Input
                type="number"
                value={formData.durationSeconds}
                onChange={(e) => setFormData(prev => ({ 
                  ...prev, 
                  durationSeconds: Math.max(1, Math.min(3600, parseInt(e.target.value) || 1))
                }))}
                min={1}
                max={3600}
                className="w-24"
              />
              <p className="text-xs text-muted-foreground flex items-center">
                seconds (1-3600)
              </p>
            </div>
          </div>

          {/* Concurrency Level */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <Label>Concurrency Level</Label>
              <Badge variant="secondary" className="font-mono">
                {formData.concurrencyLevel} clients
              </Badge>
            </div>
            <Slider
              value={[formData.concurrencyLevel]}
              onValueChange={(value) => setFormData(prev => ({ ...prev, concurrencyLevel: value[0] }))}
              min={1}
              max={100}
              step={1}
              className="w-full"
            />
            <div className="flex gap-2">
              <Input
                type="number"
                value={formData.concurrencyLevel}
                onChange={(e) => setFormData(prev => ({ 
                  ...prev, 
                  concurrencyLevel: Math.max(1, Math.min(500, parseInt(e.target.value) || 1))
                }))}
                min={1}
                max={500}
                className="w-24"
              />
              <p className="text-xs text-muted-foreground flex items-center">
                Number of parallel clients (1-500)
              </p>
            </div>
          </div>

          {/* Request Pattern */}
          <div className="space-y-2">
            <Label>Request Pattern</Label>
            <Select
              value={formData.requestPattern}
              onValueChange={(value: RequestPattern) => 
                setFormData(prev => ({ ...prev, requestPattern: value }))
              }
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="CONSTANT_RATE">
                  <div className="flex items-center gap-2">
                    <span>⚡</span>
                    <div>
                      <div className="font-medium">Constant Rate</div>
                      <div className="text-xs text-muted-foreground">
                        Steady load for sustained testing
                      </div>
                    </div>
                  </div>
                </SelectItem>
                <SelectItem value="BURST">
                  <div className="flex items-center gap-2">
                    <span>💥</span>
                    <div>
                      <div className="font-medium">Burst</div>
                      <div className="text-xs text-muted-foreground">
                        Maximum throughput stress test
                      </div>
                    </div>
                  </div>
                </SelectItem>
                <SelectItem value="RAMP_UP">
                  <div className="flex items-center gap-2">
                    <span>📈</span>
                    <div>
                      <div className="font-medium">Ramp Up</div>
                      <div className="text-xs text-muted-foreground">
                        Gradually increasing load
                      </div>
                    </div>
                  </div>
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* HTTP Method */}
          <div className="space-y-2">
            <Label>HTTP Method</Label>
            <Tabs
              value={formData.httpMethod}
              onValueChange={(value: string) => 
                setFormData(prev => ({ ...prev, httpMethod: value as HttpMethod }))
              }
              className="w-full"
            >
              <TabsList className="grid w-full grid-cols-4">
                <TabsTrigger value="GET" className="font-mono">GET</TabsTrigger>
                <TabsTrigger value="POST" className="font-mono">POST</TabsTrigger>
                <TabsTrigger value="PUT" className="font-mono">PUT</TabsTrigger>
                <TabsTrigger value="DELETE" className="font-mono">DELETE</TabsTrigger>
              </TabsList>
            </Tabs>
          </div>
        </div>

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={() => setOpen(false)}
            disabled={loading}
          >
            Cancel
          </Button>
          <Button
            type="button"
            onClick={handleSubmit}
            disabled={loading}
            className="gap-2"
          >
            {loading ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                Starting Test...
              </>
            ) : (
              <>
                <Play className="h-4 w-4" />
                Start Test
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
