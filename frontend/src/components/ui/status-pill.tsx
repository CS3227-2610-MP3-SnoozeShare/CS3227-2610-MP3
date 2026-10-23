import { cn } from 'cn';

export type Status = 'active' | 'inactive' | 'pending' | 'confirmed' | 'rejected' | 'resolved' | 'available' | 'booked' | 'blocked' | 'other';

const labels: Record<Status, string> = {
  active: 'ACTIVE', inactive: 'INACTIVE', pending: 'PENDING', confirmed: 'CONFIRMED', rejected: 'REJECTED',
  resolved: 'RESOLVED', available: 'AVAILABLE', booked: 'BOOKED', blocked: 'BLOCKED', other: 'OTHER MONTH',
};

const classes: Record<Status, string> = {
  active: 'ss-status-active', inactive: 'ss-status-neutral', pending: 'ss-status-pending', confirmed: 'ss-status-success',
  rejected: 'ss-status-danger', resolved: 'ss-status-success', available: 'ss-status-success', booked: 'ss-status-danger',
  blocked: 'ss-status-pending', other: 'ss-status-neutral',
};

export function StatusPill({ status, className }: { status: Status; className?: string }) {
  return <span className={cn('ss-pill', classes[status], className)}>{labels[status]}</span>;
}
