import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { HostCalendarPanel } from './HostCalendarPanel';
import { HostRequestsTable } from './HostRequestsTable';
import { createHostFixtureAdapter } from './hostFixtures';
import { resolveCalendarState } from './calendarState';

describe('Host calendar and requests', () => {
  it('applies Booked > Blocked > Available > Other month precedence', () => {
    expect(resolveCalendarState({ date: 'x', month: 'current', booked: true, blocked: true })).toBe('booked');
    expect(resolveCalendarState({ date: 'x', month: 'current', booked: false, blocked: true })).toBe('blocked');
    expect(resolveCalendarState({ date: 'x', month: 'current', booked: false, blocked: false })).toBe('available');
    expect(resolveCalendarState({ date: 'x', month: 'other', booked: false, blocked: false })).toBe('other');
  });

  it('renders calendar legend, overrides, request tables, and local decision modal', async () => {
    const adapter = createHostFixtureAdapter();
    render(<><HostCalendarPanel calendar={adapter.getCalendar('lakeside-cabin').data!} /><HostRequestsTable requests={adapter.getRequests().data!} /></>);
    expect(screen.getByText('Available')).toBeInTheDocument();
    expect(screen.getByText('Oct 3 – Oct 5')).toBeInTheDocument();
    expect(screen.getByText('Jane G.')).toBeInTheDocument();
    fireEvent.click(screen.getAllByRole('button', { name: 'Approve' })[0]);
    await waitFor(() => expect(screen.getByRole('dialog')).toHaveTextContent('presentation-only'));
  });
});
