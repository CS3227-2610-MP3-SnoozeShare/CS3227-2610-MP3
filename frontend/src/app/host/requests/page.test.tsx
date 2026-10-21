import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import HostRequestsPage from './page';
import { HostFixtureProvider } from '@/components/host/HostFixtureProvider';

describe('Host requests page', () => {
  it('groups the pending count directly beside the page title and labels history like the artifact', () => {
    render(<HostFixtureProvider><HostRequestsPage /></HostFixtureProvider>);
    const title = screen.getByRole('heading', { name: 'Booking requests' });
    const titleGroup = title.parentElement!;

    expect(titleGroup).toHaveClass('inline-flex', 'items-center', 'gap-2');
    expect(titleGroup).toContainElement(screen.getByText('8 pending'));
    expect(screen.queryByRole('heading', { name: 'Pending requests' })).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'PAST REQUESTS' })).toBeInTheDocument();
  });
});
