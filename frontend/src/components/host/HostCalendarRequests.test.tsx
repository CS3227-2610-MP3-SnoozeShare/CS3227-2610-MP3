import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
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

  it('renders a complete five-week calendar with weekday headings', () => {
    const calendar = createHostFixtureAdapter().getCalendar('lakeside-cabin').data!;
    render(<HostCalendarPanel calendar={calendar} />);

    expect(screen.getAllByRole('gridcell')).toHaveLength(35);
    expect(screen.getAllByRole('columnheader').map((header) => header.textContent)).toEqual(['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']);
    expect(screen.getByRole('gridcell', { name: '15, Blocked' })).toBeInTheDocument();
  });

  it('uses total and status columns for past requests', () => {
    const requests = createHostFixtureAdapter().getRequests().data!;
    render(<HostRequestsTable requests={requests} />);

    const pastTable = screen.getAllByRole('table')[1];
    expect(within(pastTable).getByText('TOTAL')).toBeInTheDocument();
    expect(within(pastTable).getByText('STATUS')).toBeInTheDocument();
    expect(within(pastTable).getByText('$630.00')).toBeInTheDocument();
    expect(within(pastTable).getAllByText('CONFIRMED')).toHaveLength(2);
    expect(within(pastTable).getByText('REJECTED')).toBeInTheDocument();
    expect(within(pastTable).queryByText('NET EARNING')).not.toBeInTheDocument();
  });

  it('navigates between full month grids locally', () => {
    const calendar = createHostFixtureAdapter().getCalendar('lakeside-cabin').data!;
    render(<HostCalendarPanel calendar={calendar} />);

    fireEvent.click(screen.getByRole('button', { name: 'Next month' }));
    expect(screen.getByText('October 2026')).toBeInTheDocument();
    expect(screen.getAllByRole('gridcell')).toHaveLength(35);
    fireEvent.click(screen.getByRole('button', { name: 'Previous month' }));
    expect(screen.getByText('September 2026')).toBeInTheDocument();
  });
});
