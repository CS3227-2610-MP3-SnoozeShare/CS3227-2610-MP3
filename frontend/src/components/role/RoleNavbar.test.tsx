import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { RoleNavbar } from './RoleNavbar';

describe('RoleNavbar', () => {
  it.each([
    ['GUEST', ['Search', 'Trips', 'Messages', 'Wallet'], null],
    ['HOST', ['Listings', 'Requests', 'Messages', 'Wallet'], 'Host workspace'],
    ['AGENT', ['Disputes', 'Accounts', 'Audit Log', 'Categories'], 'Support Agent workspace'],
  ] as const)('renders the %s workspace navigation', (role, items, workspace) => {
    render(<RoleNavbar role={role} />);
    const topRow = screen.getByTestId('workspace-navbar-top');
    const divider = screen.getByTestId('workspace-navbar-divider');
    const tabRow = screen.getByTestId('workspace-navbar-tabs');
    const brand = screen.getByRole('link', { name: 'SnoozeShare' });

    expect(topRow).toHaveTextContent('SnoozeShare');
    expect(topRow).toHaveTextContent('Wallet');
    expect(brand).toHaveClass('text-sm', 'tracking-[0.04em]');
    expect(screen.getByRole('navigation')).toHaveClass('pb-0');
    expect(divider).toHaveClass('-mx-6');
    expect(tabRow).toHaveClass('mt-3');
    expect(tabRow).toHaveTextContent(items.join(''));
    items.forEach((item) => expect(screen.getByRole('link', { name: item })).toBeInTheDocument());
    if (workspace) {
      expect(topRow).toHaveTextContent(workspace);
      expect(topRow).toContainElement(screen.getByText(workspace));
    }
    else expect(screen.queryByText(/workspace/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });

  it('marks only the first role tab as active', () => {
    render(<RoleNavbar role="HOST" />);

    expect(screen.getByRole('link', { name: 'Listings' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Listings' })).toHaveClass('px-3', 'pb-3', 'text-xs');
    expect(screen.getByRole('link', { name: 'Requests' })).not.toHaveAttribute('aria-current');
  });

  it('renders a compact logout control beside the wallet pill', () => {
    const onLogout = vi.fn();
    render(<RoleNavbar role="GUEST" onLogout={onLogout} />);

    const button = screen.getByRole('button', { name: 'Log out' });
    expect(button).toHaveClass('h-6', 'border-0', 'bg-transparent', 'px-0');
    expect(screen.getByTestId('workspace-navbar-top')).toContainElement(button);
    fireEvent.click(button);
    expect(onLogout).toHaveBeenCalledOnce();
  });
});
