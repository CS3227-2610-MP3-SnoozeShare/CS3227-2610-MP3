import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { HostCalendarPanel } from './HostCalendarPanel';
import { HostRequestsTable } from './HostRequestsTable';
import { createHostFixtureAdapter } from './hostFixtures';
import { resolveCalendarState } from './calendarState';
import { BackendApiError } from '@/lib/api';

describe('Host calendar and requests', () => {
  it('applies Booked > Blocked > Available > Other month precedence', () => {
    expect(resolveCalendarState({ date: 'x', month: 'current', booked: true, blocked: true })).toBe('booked');
    expect(resolveCalendarState({ date: 'x', month: 'current', booked: false, blocked: true })).toBe('blocked');
    expect(resolveCalendarState({ date: 'x', month: 'current', booked: false, blocked: false })).toBe('available');
    expect(resolveCalendarState({ date: 'x', month: 'other', booked: false, blocked: false })).toBe('other');
  });

  it('renders calendar legend, blocked dates, request tables, and contextual approval details', async () => {
    const adapter = createHostFixtureAdapter();
    render(<><HostCalendarPanel calendar={adapter.getCalendar('lakeside-cabin').data!} /><HostRequestsTable requests={adapter.getRequests().data!} /></>);
    expect(screen.getByText('Available')).toBeInTheDocument();
    expect(screen.getByText('Oct 3 – Oct 5, 2026')).toBeInTheDocument();
    expect(screen.getByText('Maintenance')).toBeInTheDocument();
    expect(screen.getByText('Jane G.')).toBeInTheDocument();
    fireEvent.click(screen.getAllByRole('button', { name: 'Approve' })[0]);
    await waitFor(() => expect(screen.getByRole('dialog')).toHaveTextContent('The booking moves to CONFIRMED'));
    const dialog = screen.getByRole('dialog');
    const summary = within(dialog).getByRole('group', { name: 'Booking summary' });
    const explanation = within(dialog).getByText('The booking moves to CONFIRMED and the guest is notified. Funds stay held in escrow until check-in.');
    expect(dialog).toHaveClass('max-w-[31rem]');
    expect(within(dialog).getByRole('heading', { name: 'Approve booking request?' })).toHaveClass('text-base');
    expect(summary).toHaveClass('bg-[var(--success)]', 'text-white', 'p-4');
    expect(summary).toHaveTextContent('Jane G. · Oct 3–6 (3 nights)');
    expect(summary).toHaveTextContent('$360.00 earning');
    expect(summary.compareDocumentPosition(explanation) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(explanation).toHaveClass('bg-[var(--surface-inset)]');
    expect(within(dialog).getByRole('button', { name: 'Confirm approve' })).toHaveClass('bg-[var(--success)]', 'text-sm');
  });

  it('shows a single required range field and removes a current blocked-date entry locally', () => {
    const calendar = createHostFixtureAdapter().getCalendar('lakeside-cabin').data!;
    render(<HostCalendarPanel calendar={calendar} />);

    expect(screen.getByRole('gridcell', { name: '4, Booked' })).toBeInTheDocument();
    expect(screen.getByRole('gridcell', { name: '5, Booked' })).toBeInTheDocument();
    expect(screen.getByRole('gridcell', { name: '11, Available' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Date range: Select dates' })).toBeInTheDocument();
    expect(screen.queryAllByRole('button', { pressed: true })).toHaveLength(0);
    fireEvent.click(screen.getByRole('button', { name: 'Date range: Select dates' }));
    expect(screen.getByRole('dialog', { name: 'Choose date range' })).toHaveTextContent('September 2026');
    expect(screen.queryByLabelText('From')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('To')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Reason')).toBeRequired();
    const blockedEntry = screen.getByText('Maintenance').parentElement!;
    expect(blockedEntry).toContainElement(screen.getByRole('button', { name: 'Remove blocked dates for Maintenance, Oct 3 – Oct 5, 2026' }));
    expect(blockedEntry).toHaveClass('justify-between');
    expect(screen.getByText('Maintenance').tagName).toBe('STRONG');
    fireEvent.click(screen.getByRole('button', { name: 'Remove blocked dates for Maintenance, Oct 3 – Oct 5, 2026' }));
    expect(screen.queryByText('Oct 3 – Oct 5, 2026')).not.toBeInTheDocument();
  });

  it('requires a reason before submitting a block and does not open a confirmation dialog', () => {
    render(<HostCalendarPanel calendar={createHostFixtureAdapter().getCalendar('lakeside-cabin').data!} />);
    fireEvent.click(screen.getByRole('button', { name: 'Block these dates' }));
    expect(screen.getByText('Reason is required')).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('uses Block these dates as the direct confirmation action', async () => {
    const user = userEvent.setup();
    const createBlock = vi.fn().mockResolvedValue(undefined);
    render(<HostCalendarPanel calendar={createHostFixtureAdapter().getCalendar('lakeside-cabin').data!} onCreateBlock={createBlock} />);
    await user.click(screen.getByRole('button', { name: 'Date range: Select dates' }));
    const picker = screen.getByRole('dialog', { name: 'Choose date range' });
    await user.click(within(picker).getByRole('button', { name: '18' }));
    await user.click(within(picker).getByRole('button', { name: '20' }));
    await user.click(screen.getByRole('button', { name: 'Apply' }));
    await user.type(screen.getByLabelText('Reason'), 'Maintenance');

    await user.click(screen.getByRole('button', { name: 'Block these dates' }));

    expect(createBlock).toHaveBeenCalledWith({ startDate: '2026-09-18', endDate: '2026-09-20', reason: 'Maintenance' });
    expect(screen.queryByRole('dialog', { name: 'Block these dates?' })).not.toBeInTheDocument();
  });

  it('submits the block form on Enter and shows a specific overlap error', async () => {
    const user = userEvent.setup();
    const createBlock = vi.fn().mockRejectedValue(new BackendApiError('DATE_RANGE_UNAVAILABLE', 'Request could not be completed', 'req', 409));
    render(<HostCalendarPanel calendar={createHostFixtureAdapter().getCalendar('lakeside-cabin').data!} onCreateBlock={createBlock} />);
    await user.click(screen.getByRole('button', { name: 'Date range: Select dates' }));
    const picker = screen.getByRole('dialog', { name: 'Choose date range' });
    await user.click(within(picker).getByRole('button', { name: '18' }));
    await user.click(within(picker).getByRole('button', { name: '20' }));
    await user.click(screen.getByRole('button', { name: 'Apply' }));
    await user.type(screen.getByLabelText('Reason'), 'Maintenance{Enter}');

    await waitFor(() => expect(createBlock).toHaveBeenCalledOnce());
    expect(await screen.findByRole('alert')).toHaveTextContent('These dates overlap an existing booking or blocked period. Choose another date range.');
  });

  it('keeps a chosen range open until Apply', () => {
    render(<HostCalendarPanel calendar={createHostFixtureAdapter().getCalendar('lakeside-cabin').data!} />);
    fireEvent.click(screen.getByRole('button', { name: 'Date range: Select dates' }));
    fireEvent.click(screen.getByRole('button', { name: '18' }));
    expect(screen.getByText('Select an end date')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '20' }));
    expect(screen.getByRole('dialog', { name: 'Choose date range' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Date range: Select dates' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Apply' }));
    expect(screen.getByRole('button', { name: 'Date range: Sep 18 – Sep 20, 2026' })).toBeInTheDocument();
    expect(screen.queryByRole('dialog', { name: 'Choose date range' })).not.toBeInTheDocument();
  });

  it('reopens the date picker with the selected dates on their correct weekdays', () => {
    render(<HostCalendarPanel calendar={createHostFixtureAdapter().getCalendar('lakeside-cabin').data!} />);
    fireEvent.click(screen.getByRole('button', { name: 'Date range: Select dates' }));
    fireEvent.click(screen.getByRole('button', { name: '16' }));
    fireEvent.click(screen.getByRole('button', { name: '18' }));
    fireEvent.click(screen.getByRole('button', { name: 'Apply' }));
    fireEvent.click(screen.getByRole('button', { name: 'Date range: Sep 16 – Sep 18, 2026' }));

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

  it('always renders six calendar weeks with weekday headings', () => {
    const calendar = createHostFixtureAdapter().getCalendar('lakeside-cabin').data!;
    render(<HostCalendarPanel calendar={calendar} />);

    expect(screen.getAllByRole('gridcell')).toHaveLength(42);
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
    expect(within(pastTable).getAllByText('CONFIRMED')).toHaveLength(7);
    expect(within(pastTable).getAllByText('REJECTED')).toHaveLength(3);
    expect(within(pastTable).queryByText('NET EARNING')).not.toBeInTheDocument();
    expect(within(pastTable).getAllByText('CONFIRMED')[0]).toHaveClass('ss-pill', 'ss-status-success');
    expect(within(pastTable).getByText('$630.00')).toHaveClass('font-semibold');
  });

  it('uses artifact-style card tables, emphasis, and approve/reject buttons', () => {
    const requests = createHostFixtureAdapter().getRequests().data!;
    render(<HostRequestsTable requests={requests} />);
    const pendingTable = screen.getAllByRole('table')[0];
    const guestCell = within(pendingTable).getByText('Jane G.');
    const earningCell = within(pendingTable).getByText('$349.20');

    expect(pendingTable.parentElement?.parentElement).toHaveClass('rounded-xl', 'border', 'bg-white');
    expect(pendingTable.querySelector('thead')).toHaveClass('bg-[var(--surface-inset)]');
    expect(guestCell).toHaveClass('font-semibold');
    expect(earningCell).toHaveClass('font-semibold', 'text-primary');
    expect(within(pendingTable).getAllByRole('button', { name: 'Approve' })[0]).toHaveClass('bg-[var(--success)]');
    expect(within(pendingTable).getAllByRole('button', { name: 'Reject' })[0]).toHaveClass('border', 'border-destructive');
  });

  it('caps pending requests and lets the past-request table fill and scroll within remaining height', () => {
    render(<HostRequestsTable requests={createHostFixtureAdapter().getRequests().data!} />);

    const pendingScroll = screen.getByLabelText('Pending requests table');
    const pastScroll = screen.getByLabelText('Past requests table');
    expect(pendingScroll).toHaveClass('max-h-[244px]', 'overflow-auto');
    expect(pendingScroll.querySelectorAll('tbody tr')).toHaveLength(8);
    expect(pendingScroll.querySelector('tbody tr')).toHaveClass('whitespace-nowrap');
    expect(pendingScroll.querySelector('tbody tr td')).toHaveClass('py-2');
    expect(pastScroll).toHaveClass('min-h-0', 'flex-1', 'overflow-auto');
    expect(pendingScroll.querySelector('thead')).toHaveClass('sticky', 'top-0');
    expect(pastScroll.querySelector('thead')).toHaveClass('sticky', 'top-0');
  });

  it('shows contextual rejection details and an optional message field', () => {
    render(<HostRequestsTable requests={createHostFixtureAdapter().getRequests().data!} />);
    fireEvent.click(screen.getAllByRole('button', { name: 'Reject' })[0]);

    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveTextContent('Jane G. · Oct 3–6 (3 nights)');
    expect(dialog).toHaveTextContent('$360.00 to be refunded');
    expect(dialog).toHaveTextContent('no fee is charged for a host rejection');
    const summary = within(dialog).getByRole('group', { name: 'Booking summary' });
    expect(summary).toHaveClass('bg-destructive', 'text-white', 'p-4');
    expect(within(dialog).getByLabelText('Message to guest (optional)')).toHaveClass('min-h-[52px]');
    expect(within(dialog).getByLabelText('Message to guest (optional)')).toHaveAttribute('placeholder', 'e.g. dates unavailable due to maintenance');
    expect(within(dialog).getByRole('button', { name: 'Confirm reject' })).toHaveClass('bg-destructive', 'text-sm');
  });

  it('navigates between full month grids locally', () => {
    const calendar = createHostFixtureAdapter().getCalendar('lakeside-cabin').data!;
    render(<HostCalendarPanel calendar={calendar} />);

    fireEvent.click(screen.getByRole('button', { name: 'Next month' }));
    expect(screen.getByText('October 2026')).toBeInTheDocument();
    expect(screen.getAllByRole('gridcell')).toHaveLength(42);
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

  it('clears the draft range without closing the picker', () => {
    render(<HostCalendarPanel calendar={createHostFixtureAdapter().getCalendar('lakeside-cabin').data!} />);
    fireEvent.click(screen.getByRole('button', { name: 'Date range: Select dates' }));
    fireEvent.click(screen.getByRole('button', { name: 'Clear' }));

    expect(screen.getByRole('dialog', { name: 'Choose date range' })).toBeInTheDocument();
    expect(within(screen.getByRole('dialog', { name: 'Choose date range' })).queryAllByRole('button', { pressed: true })).toHaveLength(0);
    fireEvent.click(screen.getByRole('button', { name: 'Apply' }));
    expect(screen.getByRole('button', { name: 'Date range: Select dates' })).toBeInTheDocument();
  });

  it('closes on outside click without applying the draft range', () => {
    render(<HostCalendarPanel calendar={createHostFixtureAdapter().getCalendar('lakeside-cabin').data!} />);
    fireEvent.click(screen.getByRole('button', { name: 'Date range: Select dates' }));
    fireEvent.click(screen.getByRole('button', { name: '18' }));
    fireEvent.mouseDown(screen.getByLabelText('Reason'));

    expect(screen.queryByRole('dialog', { name: 'Choose date range' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Date range: Select dates' })).toBeInTheDocument();
  });

  it('opens the fixture picker at the visible month and discards an unapplied draft when navigating months', () => {
    render(<HostCalendarPanel calendar={createHostFixtureAdapter().getCalendar('lakeside-cabin').data!} />);
    fireEvent.click(screen.getByRole('button', { name: 'Date range: Select dates' }));
    expect(screen.getByRole('dialog', { name: 'Choose date range' })).toHaveTextContent('September 2026');
    fireEvent.click(screen.getByRole('button', { name: '18' }));
    const nextMonth = screen.getByRole('button', { name: 'Next month' });
    fireEvent.mouseDown(nextMonth);
    fireEvent.click(nextMonth);

    expect(screen.queryByRole('dialog', { name: 'Choose date range' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Date range: Select dates' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'October 2026' })).toBeInTheDocument();
    expect(screen.getByLabelText('Reason')).toHaveValue('');
  });

  it('shows the year on both ends when a selected range crosses years', () => {
    render(<HostCalendarPanel calendar={createHostFixtureAdapter().getCalendar('lakeside-cabin').data!} />);
    fireEvent.click(screen.getByRole('button', { name: 'Date range: Select dates' }));
    for (let month = 0; month < 3; month += 1) fireEvent.click(screen.getByRole('button', { name: 'Next date-picker month' }));
    fireEvent.click(screen.getByRole('button', { name: '30' }));
    fireEvent.click(screen.getByRole('button', { name: '1, Other month' }));
    fireEvent.click(screen.getByRole('button', { name: 'Apply' }));

    expect(screen.getByRole('button', { name: 'Date range: Dec 30, 2026 – Jan 1, 2027' })).toBeInTheDocument();
  });

  it('formats current blocked-date ranges with the same year rules', () => {
    const calendar = createHostFixtureAdapter().getCalendar('lakeside-cabin').data!;
    render(<HostCalendarPanel calendar={{ ...calendar, blockedDates: [{ reason: 'New Year', startDate: '2026-12-30', endDate: '2027-01-01' }] }} />);

    expect(screen.getByText('Dec 30, 2026 – Jan 1, 2027')).toBeInTheDocument();
  });

  it('shows a calendar icon at the right side of the date range field', () => {
    render(<HostCalendarPanel calendar={createHostFixtureAdapter().getCalendar('lakeside-cabin').data!} />);
    const trigger = screen.getByRole('button', { name: 'Date range: Select dates' });
    expect(trigger.querySelector('svg[aria-hidden="true"]')).toBeInTheDocument();
    expect(trigger.lastElementChild?.tagName.toLowerCase()).toBe('svg');
  });
});
