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

  it('renders calendar legend, blocked dates, request tables, and local decision modal', async () => {
    const adapter = createHostFixtureAdapter();
    render(<><HostCalendarPanel calendar={adapter.getCalendar('lakeside-cabin').data!} /><HostRequestsTable requests={adapter.getRequests().data!} /></>);
    expect(screen.getByText('Available')).toBeInTheDocument();
    expect(screen.getByText('Oct 3 – Oct 5')).toBeInTheDocument();
    expect(screen.getByText('Maintenance')).toBeInTheDocument();
    expect(screen.getByText('Jane G.')).toBeInTheDocument();
    fireEvent.click(screen.getAllByRole('button', { name: 'Approve' })[0]);
    await waitFor(() => expect(screen.getByRole('dialog')).toHaveTextContent('presentation-only'));
  });

  it('shows a single required range field and removes a current blocked-date entry locally', () => {
    const calendar = createHostFixtureAdapter().getCalendar('lakeside-cabin').data!;
    render(<HostCalendarPanel calendar={calendar} />);

    expect(screen.getByRole('gridcell', { name: '4, Booked' })).toBeInTheDocument();
    expect(screen.getByRole('gridcell', { name: '5, Booked' })).toBeInTheDocument();
    expect(screen.getByRole('gridcell', { name: '11, Available' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Date range: Sep 15 – Sep 17' })).toBeInTheDocument();
    expect(screen.queryByLabelText('From')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('To')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Reason')).toBeRequired();
    const blockedEntry = screen.getByText('Maintenance').parentElement!;
    expect(blockedEntry).toContainElement(screen.getByRole('button', { name: 'Remove blocked dates for Maintenance, Oct 3 – Oct 5' }));
    expect(blockedEntry).toHaveClass('justify-between');
    expect(screen.getByText('Maintenance').tagName).toBe('STRONG');
    fireEvent.click(screen.getByRole('button', { name: 'Remove blocked dates for Maintenance, Oct 3 – Oct 5' }));
    expect(screen.queryByText('Oct 3 – Oct 5')).not.toBeInTheDocument();
  });

  it('requires a reason before opening the block confirmation', () => {
    render(<HostCalendarPanel calendar={createHostFixtureAdapter().getCalendar('lakeside-cabin').data!} />);
    fireEvent.click(screen.getByRole('button', { name: 'Block these dates' }));
    expect(screen.getByText('Reason is required')).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('uses the date-picker calendar to choose one continuous range', () => {
    render(<HostCalendarPanel calendar={createHostFixtureAdapter().getCalendar('lakeside-cabin').data!} />);
    fireEvent.click(screen.getByRole('button', { name: 'Date range: Sep 15 – Sep 17' }));
    fireEvent.click(screen.getByRole('button', { name: '18' }));
    expect(screen.getByText('Select an end date')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '20' }));
    expect(screen.getByRole('button', { name: 'Date range: Sep 18 – Sep 20' })).toBeInTheDocument();
    expect(screen.queryByRole('dialog', { name: 'Choose date range' })).not.toBeInTheDocument();
  });

  it('reopens the date picker with the selected dates on their correct weekdays', () => {
    render(<HostCalendarPanel calendar={createHostFixtureAdapter().getCalendar('lakeside-cabin').data!} />);
    fireEvent.click(screen.getByRole('button', { name: 'Date range: Sep 15 – Sep 17' }));
    fireEvent.click(screen.getByRole('button', { name: '16' }));
    fireEvent.click(screen.getByRole('button', { name: '18' }));
    fireEvent.click(screen.getByRole('button', { name: 'Date range: Sep 16 – Sep 18' }));

    const picker = screen.getByRole('dialog', { name: 'Choose date range' });
    const dateButtons = within(picker).getAllByRole('button').filter((button) => /^\d+$/.test(button.textContent?.trim() ?? ''));
    const septemberSixteenth = dateButtons.findIndex((button) => button.textContent?.trim() === '16');
    expect(septemberSixteenth).toBeGreaterThanOrEqual(0);
    expect(septemberSixteenth % 7).toBe(3); // September 16, 2026 is Wednesday (Sunday-first grid).
  });

  it('uses the artifact calendar-state colors and taller day cells', () => {
    const calendar = createHostFixtureAdapter().getCalendar('lakeside-cabin').data!;
    render(<HostCalendarPanel calendar={calendar} />);

    expect(screen.getByRole('gridcell', { name: '3, Available' })).toHaveClass('min-h-[88px]', 'bg-white');
    expect(screen.getByRole('gridcell', { name: '4, Booked' })).toHaveClass('bg-[#eaf2d8]');
    expect(screen.getByRole('gridcell', { name: '15, Blocked' })).toHaveClass('bg-[#f4dfe0]');
    expect(screen.getByRole('gridcell', { name: '30, Other month' })).toHaveClass('bg-[#dedede]');
  });

  it('places month navigation and the legend together above the calendar grid', () => {
    const calendar = createHostFixtureAdapter().getCalendar('lakeside-cabin').data!;
    render(<HostCalendarPanel calendar={calendar} />);

    const header = screen.getByRole('group', { name: 'Calendar month and status legend' });
    expect(header).toContainElement(screen.getByRole('heading', { name: 'September 2026' }));
    expect(header).toContainElement(screen.getByText('Available'));
    expect(header.compareDocumentPosition(screen.getByRole('grid')) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(screen.getByRole('region', { name: 'Booking calendar and date blocking' })).toHaveClass('lg:grid-cols-[minmax(0,2.6fr)_minmax(300px,1fr)]');
  });

  it('keeps month navigation arrows fixed around a centered month label', () => {
    const calendar = createHostFixtureAdapter().getCalendar('lakeside-cabin').data!;
    render(<HostCalendarPanel calendar={calendar} />);

    const previous = screen.getByRole('button', { name: 'Previous month' });
    const next = screen.getByRole('button', { name: 'Next month' });
    const navigation = previous.parentElement!;
    expect(navigation).toHaveClass('grid', 'w-52', 'grid-cols-[2rem_minmax(0,1fr)_2rem]');
    expect(navigation.children[0]).toBe(previous);
    expect(navigation.children[1]).toHaveClass('text-center');
    expect(navigation.children[2]).toBe(next);

    for (let month = 0; month < 4; month += 1) fireEvent.click(previous);
    expect(screen.getByRole('heading', { name: 'May 2026' })).toBeInTheDocument();
    expect(previous.parentElement).toBe(navigation);
    expect(next.parentElement).toBe(navigation);
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

  it('renders six calendar weeks for months that need them', () => {
    const calendar = createHostFixtureAdapter().getCalendar('lakeside-cabin').data!;
    render(<HostCalendarPanel calendar={calendar} />);
    fireEvent.click(screen.getByRole('button', { name: 'Previous month' }));
    expect(screen.getByText('August 2026')).toBeInTheDocument();
    expect(screen.getAllByRole('gridcell')).toHaveLength(42);
  });
});
