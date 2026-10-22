import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { RoleNavbar } from './RoleNavbar';

let pathname: string | null = null;
vi.mock('next/navigation', () => ({ usePathname: () => pathname }));

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
    if (role === 'AGENT') expect(topRow).not.toHaveTextContent('Wallet');
    else expect(topRow).toHaveTextContent('Wallet');
    expect(brand).toHaveClass('text-base', 'tracking-[0.04em]');
    if (role === 'AGENT') expect(screen.queryByLabelText('Wallet')).toBeNull();
    else expect(screen.getByLabelText('Wallet')).toHaveClass('text-xs');
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
    expect(screen.getByRole('link', { name: 'Listings' })).toHaveClass('px-3', 'pb-3', 'text-sm');
    expect(screen.getByRole('link', { name: 'Requests' })).not.toHaveAttribute('aria-current');
  });

  it('renders a compact logout control beside the wallet pill', () => {
    const onLogout = vi.fn();
    render(<RoleNavbar role="GUEST" onLogout={onLogout} />);

    const button = screen.getByRole('button', { name: 'Log out' });
    expect(button).toHaveClass('h-6', 'ml-2', 'border-0', 'bg-transparent', 'px-0', 'text-sm');
    expect(screen.getByTestId('workspace-navbar-top')).toContainElement(button);
    fireEvent.click(button);
    expect(onLogout).toHaveBeenCalledOnce();
  });

  describe('agent routes (F13, E60)', () => {
    it('links each agent tab to its own route', () => {
      pathname = '/agent/disputes';
      render(<RoleNavbar role="AGENT" />);
      expect(screen.getByRole('link', { name: 'Disputes' })).toHaveAttribute('href', '/agent/disputes');
      expect(screen.getByRole('link', { name: 'Accounts' })).toHaveAttribute('href', '/agent/accounts');
      expect(screen.getByRole('link', { name: 'Audit Log' })).toHaveAttribute('href', '/agent/audit');
      expect(screen.getByRole('link', { name: 'Categories' })).toHaveAttribute('href', '/agent/categories');
    });

    it('marks Disputes current on a dispute detail route', () => {
      pathname = '/agent/disputes/3f2c';
      render(<RoleNavbar role="AGENT" />);
      expect(screen.getByRole('link', { name: 'Disputes' })).toHaveAttribute('aria-current', 'page');
      expect(screen.getByRole('link', { name: 'Accounts' })).not.toHaveAttribute('aria-current');
    });

    it('marks the tab of the current route', () => {
      pathname = '/agent/audit';
      render(<RoleNavbar role="AGENT" />);
      expect(screen.getByRole('link', { name: 'Audit Log' })).toHaveAttribute('aria-current', 'page');
      expect(screen.getByRole('link', { name: 'Disputes' })).not.toHaveAttribute('aria-current');
    });
  });
});
