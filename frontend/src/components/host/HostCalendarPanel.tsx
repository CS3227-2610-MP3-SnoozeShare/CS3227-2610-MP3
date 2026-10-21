'use client';

import { useState } from 'react';
import { CalendarGrid, type CalendarDay as GridDay } from '@/components/ui/calendar-grid';
import { Modal } from '@/components/ui/modal';
import { resolveCalendarState, calendarDayLabel } from './calendarState';
import type { CalendarData } from './hostTypes';

export function HostCalendarPanel({ calendar }: { calendar: CalendarData }) {
  const [monthOffset, setMonthOffset] = useState(0);
  const [open, setOpen] = useState(false);
  const [datePickerOpen, setDatePickerOpen] = useState(false);
  const [pickerMonth, setPickerMonth] = useState(new Date(2026, 8, 1));
  const [choosingEnd, setChoosingEnd] = useState(false);
  const [from, setFrom] = useState('2026-09-15');
  const [to, setTo] = useState('2026-09-17');
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');
  const [blockedDates, setBlockedDates] = useState(calendar.blockedDates);
  const monthDate = new Date(`${calendar.days.find((day) => day.month === 'current')?.date ?? '2026-09-01'}T12:00:00`);
  monthDate.setMonth(monthDate.getMonth() + monthOffset);
  const monthLabel = new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' }).format(monthDate);
  const formatDate = (value: string) => value ? new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric' }).format(new Date(`${value}T12:00:00`)) : 'Select date';
  const days: GridDay[] = (() => {
    const first = new Date(monthDate.getFullYear(), monthDate.getMonth(), 1);
    const gridStart = new Date(first.getFullYear(), first.getMonth(), 1 - first.getDay());
    const rowCount = Math.ceil((first.getDay() + new Date(first.getFullYear(), first.getMonth() + 1, 0).getDate()) / 7);
    return Array.from({ length: rowCount * 7 }, (_, index) => {
      const date = new Date(gridStart.getFullYear(), gridStart.getMonth(), gridStart.getDate() + index);
      const isoDate = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
      const fixture = monthOffset === 0 ? calendar.days.find((day) => day.date === isoDate) : undefined;
      const state = fixture ? resolveCalendarState(fixture) : date.getMonth() === monthDate.getMonth() ? 'available' : 'other';
      return { date: isoDate, label: String(date.getDate()), state };
    });
  })();
  const rangeText = `${formatDate(from)} – ${formatDate(to)}`;
  const pickerFirst = new Date(pickerMonth.getFullYear(), pickerMonth.getMonth(), 1);
  const pickerStart = new Date(pickerFirst.getFullYear(), pickerFirst.getMonth(), 1 - pickerFirst.getDay());
  const pickerRows = Math.ceil((pickerFirst.getDay() + new Date(pickerFirst.getFullYear(), pickerFirst.getMonth() + 1, 0).getDate()) / 7);
  const pickerDays = Array.from({ length: pickerRows * 7 }, (_, index) => {
    const date = new Date(pickerStart.getFullYear(), pickerStart.getMonth(), pickerStart.getDate() + index);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  });
  const chooseDate = (date: string) => {
    if (!choosingEnd || date < from) {
      setFrom(date);
      setTo('');
      setChoosingEnd(true);
      return;
    }
    setTo(date);
    setChoosingEnd(false);
    setDatePickerOpen(false);
    setError('');
  };
  const block = () => {
    if (!reason.trim()) { setError('Reason is required'); return; }
    if (!from || !to || from > to) { setError('End date must be on or after start date'); return; }
    setError('');
    setOpen(true);
  };
  return (
    <section aria-label="Booking calendar and date blocking" className="grid overflow-hidden rounded-xl border border-border bg-white shadow-sm lg:grid-cols-[minmax(0,2.6fr)_minmax(300px,1fr)]">
      <div className="bg-[var(--canvas)] px-4 py-5 md:px-6">
        <div aria-label="Calendar month and status legend" className="mb-4 flex flex-wrap items-center justify-between gap-4" role="group">
          <div className="grid w-52 shrink-0 grid-cols-[2rem_minmax(0,1fr)_2rem] items-center">
            <button aria-label="Previous month" className="grid h-7 w-7 place-items-center rounded-md border border-border text-base leading-none hover:bg-[var(--surface-inset)]" onClick={() => setMonthOffset((offset) => offset - 1)} type="button">‹</button>
            <h2 className="text-center text-base font-semibold leading-tight">{monthLabel}</h2>
            <button aria-label="Next month" className="grid h-7 w-7 place-items-center rounded-md border border-border text-base leading-none hover:bg-[var(--surface-inset)]" onClick={() => setMonthOffset((offset) => offset + 1)} type="button">›</button>
          </div>
          <div aria-label="Calendar status legend" className="flex flex-wrap gap-x-3 gap-y-2 text-xs" role="list">
            {(['available', 'booked', 'blocked', 'other'] as const).map((state) => (
              <span className="flex items-center gap-1" key={state} role="listitem">
                <span aria-hidden="true" className={`h-3 w-3 rounded-sm border ${state === 'available' ? 'border-[#b8c98c] bg-white' : state === 'booked' ? 'border-[#d5e4b7] bg-[#eaf2d8]' : state === 'blocked' ? 'border-[#e9c2c4] bg-[#f4dfe0]' : 'border-[#d0d0d0] bg-[#dedede]'}`} />
                {calendarDayLabel(state)}
              </span>
            ))}
          </div>
        </div>
        <CalendarGrid className="rounded-lg" monthLabel={monthLabel} days={days} />
      </div>

      <aside className="border-t border-border bg-white px-5 py-5 lg:border-l lg:border-t-0">
        <h2 className="mb-4 text-base font-semibold">Block dates</h2>
        <div className="space-y-3">
          <div className="relative space-y-1">
            <label className="text-xs font-semibold" htmlFor="date-range">Date range</label>
            <button aria-label={`Date range: ${rangeText}`} aria-expanded={datePickerOpen} aria-haspopup="dialog" className="flex h-9 w-full items-center rounded-lg border border-input bg-background px-2 text-left text-sm font-normal hover:bg-[var(--surface-inset)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring" id="date-range" onClick={() => { const selectedStart = new Date(`${from}T12:00:00`); setPickerMonth(new Date(selectedStart.getFullYear(), selectedStart.getMonth(), 1)); setDatePickerOpen((value) => !value); }} type="button">{rangeText}</button>
            {datePickerOpen && <div aria-label="Choose date range" className="absolute left-0 top-full z-20 mt-1 w-[min(20rem,calc(100vw-3rem))] rounded-lg border border-border bg-white p-3 shadow-lg" role="dialog">
              <div className="mb-2 flex items-center justify-between">
                <button aria-label="Previous date-picker month" className="h-7 w-7 rounded-md hover:bg-[var(--surface-inset)]" onClick={() => setPickerMonth((value) => new Date(value.getFullYear(), value.getMonth() - 1, 1))} type="button">‹</button>
                <span className="text-sm font-semibold">{new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' }).format(pickerMonth)}</span>
                <button aria-label="Next date-picker month" className="h-7 w-7 rounded-md hover:bg-[var(--surface-inset)]" onClick={() => setPickerMonth((value) => new Date(value.getFullYear(), value.getMonth() + 1, 1))} type="button">›</button>
              </div>
              <div className="grid grid-cols-7 text-center text-[10px] font-semibold text-[var(--text-subtle)]">{['SU', 'MO', 'TU', 'WE', 'TH', 'FR', 'SA'].map((day) => <span className="py-1" key={day}>{day}</span>)}</div>
              <div className="grid grid-cols-7">
                {pickerDays.map((date) => {
                  const day = Number(date.slice(-2));
                  const sameMonth = date.slice(0, 7) === `${pickerMonth.getFullYear()}-${String(pickerMonth.getMonth() + 1).padStart(2, '0')}`;
                  const selected = date === from || date === to;
                  const inRange = from && to && date > from && date < to;
                  return <button aria-label={`${day}${sameMonth ? '' : ', Other month'}`} aria-pressed={selected} className={`h-8 rounded-full text-xs hover:bg-[var(--surface-inset)] ${!sameMonth ? 'text-[var(--text-subtle)]' : ''} ${inRange ? 'bg-[#f4dfe0]' : ''} ${selected ? 'bg-[#98300c] font-semibold text-white hover:bg-[#7e270a]' : ''}`} key={date} onClick={() => chooseDate(date)} type="button">{day}</button>;
                })}
              </div>
              <p className="mt-2 text-center text-xs text-[var(--text-subtle)]">{choosingEnd ? 'Select an end date' : rangeText}</p>
            </div>}
          </div>
          <label className="flex flex-col gap-1 text-xs font-semibold" htmlFor="reason">Reason<input className="h-9 rounded-lg border border-input bg-background px-2 text-sm font-normal" id="reason" onChange={(event) => setReason(event.target.value)} placeholder="e.g. Maintenance" required value={reason} /></label>
          {error && <p className="text-xs text-destructive">{error}</p>}
          <button className="w-full rounded-lg bg-[#941f26] px-3 py-2 text-sm font-semibold text-white hover:bg-[#7f1c22]" onClick={block} type="button">Block these dates</button>
        </div>
        <div className="mt-4 border-t border-border pt-4">
          <h3 className="mb-2 text-sm font-semibold">Current blocked dates</h3>
          {blockedDates.length ? blockedDates.map((item) => (
            <div className="mb-3 text-sm" key={`${item.reason}-${item.range}`}>
              <div className="flex items-center justify-between gap-3"><strong>{item.reason}</strong><button aria-label={`Remove blocked dates for ${item.reason}, ${item.range}`} className="text-xs text-[var(--primary)] underline-offset-2 hover:underline" onClick={() => setBlockedDates((current) => current.filter((entry) => entry !== item))} type="button">remove</button></div>
              <p>{item.range}</p>
            </div>
          )) : <p className="text-sm text-[var(--text-subtle)]">No current blocked dates</p>}
        </div>
      </aside>
      <Modal open={open} title="Block these dates?" description="This is a presentation-only Host control. No booking availability will be changed." onClose={() => setOpen(false)} onConfirm={() => setOpen(false)} confirmLabel="Confirm" />
    </section>
  );
}
