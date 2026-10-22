import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { HostFixtureProvider } from '@/components/host/HostFixtureProvider';
import { HostRoleShell } from '@/components/host/HostRoleShell';
import HostLandingPage from './page';

const { redirect, usePathname } = vi.hoisted(() => ({ redirect: vi.fn(), usePathname: vi.fn(() => '/host/listings') }));
vi.mock('next/navigation', () => ({ redirect, usePathname }));
vi.mock('@/providers/AuthProvider', () => ({ useAuth: () => ({ signOut: vi.fn() }) }));

describe('Host routes and shell', () => {
  it('redirects /host to listings', () => {
    HostLandingPage();
    expect(redirect).toHaveBeenCalledWith('/host/listings');
  });

  it('renders the Host wallet label and active route from the fixture shell', () => {
    usePathname.mockReturnValue('/host/requests');
    render(<HostFixtureProvider><HostRoleShell><main>content</main></HostRoleShell></HostFixtureProvider>);
    expect(screen.getByLabelText('Wallet: $3,920.50')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Requests' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Listings' })).not.toHaveAttribute('aria-current');
    expect(screen.getByRole('main')).toHaveTextContent('content');
  });
});
