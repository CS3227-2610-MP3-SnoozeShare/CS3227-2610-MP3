import type { HTMLAttributes } from 'react';
import { cn } from 'cn';
import type { Status } from './status-pill';

export type CalendarDay = { date: string; label: string; state: 'available' | 'booked' | 'blocked' | 'other' };

const stateLabels: Record<CalendarDay['state'], string> = {
  available: 'Available', booked: 'Booked', blocked: 'Blocked', other: 'Other month',
};

export function CalendarGrid({ monthLabel, days, className, ...props }: { monthLabel: string; days: CalendarDay[] } & HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={cn('grid grid-cols-7 overflow-hidden rounded-lg border border-[var(--border-muted)]', className)} {...props}>
      <div aria-label={monthLabel} className="col-span-7 grid grid-cols-7" role="grid">
        {days.map((day) => (
          <div aria-label={`${day.label}, ${stateLabels[day.state]}`} className="min-h-16 border-b border-r border-[var(--border-muted)] p-2 text-sm" data-state={day.state} key={day.date} role="gridcell">
            <span>{day.label}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

export function calendarStateToStatus(state: CalendarDay['state']): Status {
  return state;
}
