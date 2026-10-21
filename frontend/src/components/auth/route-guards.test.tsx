import { render, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { RouteGuard } from './RouteGuard';

const { replace } = vi.hoisted(() => ({ replace: vi.fn() }));
vi.mock('next/navigation', () => ({ useRouter: () => ({ replace }), usePathname: () => '/guest' }));
vi.mock('@/providers/AuthProvider', () => ({ useAuth: () => ({ state: 'signed_out', profile: null, retry: vi.fn(), signOut: vi.fn() }) }));

describe('role route guards', () => {
  it('redirects signed-out users with a safe return path', async () => {
    render(<RouteGuard role="GUEST"><p>protected</p></RouteGuard>);
    await waitFor(() => expect(replace).toHaveBeenCalledWith('/login?returnTo=%2Fguest'));
  });
});
