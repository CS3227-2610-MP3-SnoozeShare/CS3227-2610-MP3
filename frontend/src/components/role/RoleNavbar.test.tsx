import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { RoleNavbar } from './RoleNavbar';

describe('RoleNavbar', () => {
  it.each([
    ['GUEST', ['Search', 'Trips', 'Messages', 'Wallet'], null],
    ['HOST', ['Listings', 'Requests', 'Messages', 'Wallet'], 'Host workspace'],
    ['AGENT', ['Disputes', 'Accounts', 'Audit Log', 'Categories'], 'Support Agent workspace'],
  ] as const)('renders the %s workspace navigation', (role, items, workspace) => {
    render(<RoleNavbar role={role} />);
    expect(screen.getByRole('navigation')).toHaveTextContent('SnoozeShare');
    items.forEach((item) => expect(screen.getByRole('link', { name: item })).toBeInTheDocument());
    if (workspace) expect(screen.getByText(workspace)).toBeInTheDocument();
    else expect(screen.queryByText(/workspace/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });
});
