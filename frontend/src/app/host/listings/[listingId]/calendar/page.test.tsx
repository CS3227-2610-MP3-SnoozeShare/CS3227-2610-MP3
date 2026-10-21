import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import HostCalendarPage from './page';
import { HostFixtureProvider } from '@/components/host/HostFixtureProvider';

vi.mock('next/navigation', () => ({ useParams: () => ({ listingId: 'lakeside-cabin' }) }));

describe('Host calendar page', () => {
  it('renders the populated calendar fixture', () => { render(<HostFixtureProvider><HostCalendarPage /></HostFixtureProvider>); expect(screen.getByText('September 2026')).toBeInTheDocument(); expect(screen.getByText('Block dates')).toBeInTheDocument(); });
});
