import type { HTMLAttributes } from 'react';
import { cn } from 'cn';
import type { Status } from './status-pill';

export type CalendarDay = { date: string; label: string; state: 'available' | 'booked' | 'blocked' | 'other' };

const stateLabels: Record<CalendarDay['state'], string> = {
  available: 'Available', booked: 'Booked', blocked: 'Blocked', other: 'Other month',
};

export function CalendarGrid({ monthLabel, days, className, ...props }: { monthLabel: string; days: CalendarDay[] } & HTMLAttributes<HTMLDivElement>) {
  return (
    <div aria-label={monthLabel} className={cn('grid grid-cols-7 overflow-hidden rounded-lg border border-[var(--border-muted)]', className)} role="grid" {...props}>
      {['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'].map((weekday) => (
        <div className="border-b border-r border-[var(--border-muted)] bg-[var(--surface-inset)] px-2 py-2 text-center text-[11px] font-semibold text-[var(--text-subtle)]" key={weekday} role="columnheader">{weekday}</div>
      ))}
      {days.map((day) => (
        <div aria-label={`${day.label}, ${stateLabels[day.state]}`} className={cn('min-h-[72px] border-b border-r border-[var(--border-muted)] p-2 text-sm', day.state === 'available' && 'bg-[#eaf2d8]', day.state === 'booked' && 'bg-[#f4dfe0]', day.state === 'blocked' && 'bg-[#f6e5df]', day.state === 'other' && 'bg-[var(--surface-inset)] text-[var(--text-subtle)]')} data-state={day.state} key={day.date} role="gridcell">
          <span>{day.label}</span>
        </div>
      ))}
    </div>
  );
}

export function calendarStateToStatus(state: CalendarDay['state']): Status {
  return state;
}
