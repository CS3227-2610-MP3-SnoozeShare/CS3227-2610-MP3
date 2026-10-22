import type { CalendarDay } from './hostTypes';

export type CalendarVisualState = 'booked' | 'blocked' | 'available' | 'other';

export function resolveCalendarState(day: CalendarDay): CalendarVisualState {
  if (day.month === 'other') return day.booked ? 'booked' : day.blocked ? 'blocked' : 'other';
  if (day.booked) return 'booked';
  if (day.blocked) return 'blocked';
  return 'available';
}

export function calendarDayLabel(state: CalendarVisualState) {
  return state === 'other' ? 'Other month' : state[0].toUpperCase() + state.slice(1);
}
