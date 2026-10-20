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

  it('signs out from the role navbar', () => {
    render(<RoleShell role="GUEST"><main /></RoleShell>);
    screen.getByRole('button', { name: 'Log out' }).click();
    expect(signOut).toHaveBeenCalledOnce();
  });
});
