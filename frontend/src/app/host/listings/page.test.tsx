import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import HostListingsPage from './page';
import { HostFixtureProvider } from '@/components/host/HostFixtureProvider';
import { createHostFixtureAdapter } from '@/components/host/hostFixtures';

describe('Host listings page', () => {
  it('renders populated listings from the adapter', () => { render(<HostFixtureProvider><HostListingsPage /></HostFixtureProvider>); expect(screen.getByText('My listings')).toBeInTheDocument(); expect(screen.getByText('Lakeside Cabin')).toBeInTheDocument(); expect(screen.getByText('Hilltop House')).toBeInTheDocument(); });
  it('renders exact empty and error states', () => { const { rerender } = render(<HostFixtureProvider adapter={createHostFixtureAdapter('empty')}><HostListingsPage /></HostFixtureProvider>); expect(screen.getByText("You don't have any listings yet.")).toBeInTheDocument(); rerender(<HostFixtureProvider adapter={createHostFixtureAdapter('error')}><HostListingsPage /></HostFixtureProvider>); expect(screen.getByText('Unable to load your listings.')).toBeInTheDocument(); });
});
