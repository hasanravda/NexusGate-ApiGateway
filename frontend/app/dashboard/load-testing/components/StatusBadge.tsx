// Status Badge Component

import { Badge } from '@/components/ui/badge';
import { TestStatus } from '../types/loadTester.types';
import { CheckCircle, XCircle, Loader2, StopCircle } from 'lucide-react';

interface StatusBadgeProps {
  status: TestStatus;
  showIcon?: boolean;
  size?: 'default' | 'sm' | 'lg';
}

export function StatusBadge({ status, showIcon = true, size = 'default' }: StatusBadgeProps) {
  const getStatusConfig = (status: TestStatus) => {
    switch (status) {
      case 'RUNNING':
        return {
          variant: 'default' as const,
          className: 'bg-amber-500 hover:bg-amber-600 text-white animate-pulse',
          icon: <Loader2 className="h-3 w-3 animate-spin" />,
          label: 'Running',
        };
      case 'COMPLETED':
        return {
          variant: 'default' as const,
          className: 'bg-green-500 hover:bg-green-600 text-white',
          icon: <CheckCircle className="h-3 w-3" />,
          label: 'Completed',
        };
      case 'FAILED':
        return {
          variant: 'destructive' as const,
          className: '',
          icon: <XCircle className="h-3 w-3" />,
          label: 'Failed',
        };
      case 'STOPPED':
        return {
          variant: 'secondary' as const,
          className: '',
          icon: <StopCircle className="h-3 w-3" />,
          label: 'Stopped',
        };
      default:
        return {
          variant: 'outline' as const,
          className: '',
          icon: null,
          label: status,
        };
    }
  };

  const config = getStatusConfig(status);
  
  const sizeClasses = {
    sm: 'text-xs px-2 py-0.5',
    default: 'text-sm',
    lg: 'text-base px-3 py-1',
  };

  return (
    <Badge 
      variant={config.variant}
      className={`${config.className} ${sizeClasses[size]} flex items-center gap-1.5`}
    >
      {showIcon && config.icon}
      <span>{config.label}</span>
    </Badge>
  );
}
