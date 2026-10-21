'use client';

import { useState } from 'react';
import { CalendarGrid, type CalendarDay as GridDay } from '@/components/ui/calendar-grid';
import { Modal } from '@/components/ui/modal';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { resolveCalendarState, calendarDayLabel } from './calendarState';
import type { CalendarData } from './hostTypes';

export function HostCalendarPanel({ calendar }: { calendar: CalendarData }) {
  const [monthOffset, setMonthOffset] = useState(0);
  const [open, setOpen] = useState(false);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [error, setError] = useState('');
  const monthDate = new Date(`${calendar.days.find((day) => day.month === 'current')?.date ?? '2026-09-01'}T12:00:00`);
  monthDate.setMonth(monthDate.getMonth() + monthOffset);
  const monthLabel = new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' }).format(monthDate);
  const days: GridDay[] = (() => {
    const first = new Date(monthDate.getFullYear(), monthDate.getMonth(), 1);
    const gridStart = new Date(first.getFullYear(), first.getMonth(), 1 - first.getDay());
    return Array.from({ length: 35 }, (_, index) => {
      const date = new Date(gridStart.getFullYear(), gridStart.getMonth(), gridStart.getDate() + index);
      const isoDate = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
      const fixture = monthOffset === 0 ? calendar.days.find((day) => day.date === isoDate) : undefined;
      const state = fixture ? resolveCalendarState(fixture) : date.getMonth() === monthDate.getMonth() ? 'available' : 'other';
      return { date: isoDate, label: String(date.getDate()), state };
    });
  })();
  const block = () => { if (!from || !to || from > to) { setError('End date must be on or after start date'); return; } setOpen(true); };
  return <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_320px]"><Card><CardHeader className="flex-row items-center justify-between"><button aria-label="Previous month" className="rounded-md border border-border px-2 py-1" onClick={() => setMonthOffset((offset) => offset - 1)} type="button">‹</button><CardTitle>{monthLabel}</CardTitle><button aria-label="Next month" className="rounded-md border border-border px-2 py-1" onClick={() => setMonthOffset((offset) => offset + 1)} type="button">›</button></CardHeader><CardContent><CalendarGrid monthLabel={monthLabel} days={days} /><div className="mt-4 flex flex-wrap gap-3 text-xs">{(['available', 'booked', 'blocked', 'other'] as const).map((state) => <span className="flex items-center gap-1" key={state}><span className={`h-3 w-3 rounded-full ss-status-${state === 'available' ? 'success' : state === 'booked' ? 'danger' : state === 'blocked' ? 'warning' : 'neutral'}`} />{calendarDayLabel(state)}</span>)}</div></CardContent></Card><Card><CardHeader><CardTitle>Block dates</CardTitle></CardHeader><CardContent className="space-y-3"><label className="flex flex-col gap-1 text-xs font-semibold" htmlFor="from">FROM<input className="h-9 rounded-lg border border-input bg-background px-2 text-sm font-normal" id="from" type="date" value={from} onChange={(event) => setFrom(event.target.value)} /></label><label className="flex flex-col gap-1 text-xs font-semibold" htmlFor="to">TO<input className="h-9 rounded-lg border border-input bg-background px-2 text-sm font-normal" id="to" type="date" value={to} onChange={(event) => setTo(event.target.value)} /></label><label className="flex flex-col gap-1 text-xs font-semibold" htmlFor="reason">Reason (optional)<input className="h-9 rounded-lg border border-input bg-background px-2 text-sm font-normal" id="reason" /></label>{error && <p className="text-xs text-destructive">{error}</p>}<button className="w-full rounded-lg bg-primary px-3 py-2 text-sm font-semibold text-primary-foreground" onClick={block} type="button">Block these dates</button><h3 className="pt-3 text-xs font-bold tracking-wide text-[var(--text-subtle)]">CURRENT OVERRIDES</h3>{calendar.overrides.length ? calendar.overrides.map((override) => <p className="text-sm" key={override}>{override}</p>) : <p className="text-sm text-[var(--text-subtle)]">No current overrides</p>}</CardContent></Card><Modal open={open} title="Block these dates?" description="This is a presentation-only Host control. No booking availability will be changed." onClose={() => setOpen(false)} onConfirm={() => setOpen(false)} confirmLabel="Confirm" /></div>;
}
