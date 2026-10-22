import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { RoleShell } from './RoleShell';

const signOut = vi.hoisted(() => vi.fn());
vi.mock('@/providers/AuthProvider', () => ({ useAuth: () => ({ signOut }) }));

describe('RoleShell', () => {
  it('renders the navbar above an empty landing region', () => {
    render(<RoleShell role="GUEST"><main data-testid="empty-landing" /></RoleShell>);
    expect(screen.getByRole('navigation')).toBeInTheDocument();
    expect(screen.getByTestId('empty-landing')).toBeEmptyDOMElement();
  });

  it('constrains the Host messages workspace to the viewport for internal scrolling', () => {
    render(<RoleShell role="HOST" activePath="/host/messages"><main /></RoleShell>);

    expect(screen.getByRole('navigation').parentElement).toHaveClass('h-screen', 'overflow-hidden');
  });

  it.each(['/host/listings', '/host/requests', '/host/wallet'])('constrains %s to the viewport for internal scrolling', (activePath) => {
    render(<RoleShell role="HOST" activePath={activePath}><main /></RoleShell>);

    expect(screen.getByRole('navigation').parentElement).toHaveClass('h-screen', 'overflow-hidden');
  });

  it('signs out from the role navbar', () => {
    render(<RoleShell role="GUEST"><main /></RoleShell>);
    screen.getByRole('button', { name: 'Log out' }).click();
    expect(signOut).toHaveBeenCalledOnce();
  });
});
