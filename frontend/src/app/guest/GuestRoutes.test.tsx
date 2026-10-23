import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { GuestRoleShell } from '@/components/guest/GuestRoleShell';

const { getWallet } = vi.hoisted(() => ({ getWallet: vi.fn() }));
vi.mock('@/lib/guest-api', () => ({ getWallet }));
vi.mock('@/providers/AuthProvider', () => ({ useAuth: () => ({ signOut: vi.fn(), profile: { displayName: 'Guest User', email: 'guest@example.com' } }) }));
vi.mock('next/navigation', () => ({ usePathname: vi.fn(() => '/guest/search') }));

describe('Guest role shell', () => {
  beforeEach(() => { vi.clearAllMocks(); });

  it('shows the loaded wallet balance in the workspace header', async () => {
    getWallet.mockResolvedValue({ id: 'wallet-1', userId: 'guest-1', balance: 250.5, currency: 'SGD', updatedAt: '' });
    render(<GuestRoleShell><main>content</main></GuestRoleShell>);
    await waitFor(() => expect(screen.getByLabelText('Wallet: $250.50')).toBeInTheDocument());
  });

  it('shows a loading wallet label while the balance is pending', () => {
    getWallet.mockReturnValue(new Promise(() => {}));
    render(<GuestRoleShell><main>content</main></GuestRoleShell>);
    expect(screen.getByLabelText('Wallet: —')).toBeInTheDocument();
  });

  it('shows an unavailable wallet label when the balance fails to load', async () => {
    getWallet.mockRejectedValue(new Error('offline'));
    render(<GuestRoleShell><main>content</main></GuestRoleShell>);
    await waitFor(() => expect(screen.getByLabelText('Wallet: unavailable')).toBeInTheDocument());
  });
});
