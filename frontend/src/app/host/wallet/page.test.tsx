import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import HostWalletPage from './page';
import { HostFixtureProvider } from '@/components/host/HostFixtureProvider';

describe('Host wallet page', () => {
  it('renders balance and statement without repeating the Wallet page heading', () => {
    render(<HostFixtureProvider><HostWalletPage /></HostFixtureProvider>);
    expect(screen.getByText('AVAILABLE BALANCE')).toBeInTheDocument();
    expect(screen.getByText('Transaction statement')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Wallet', level: 1 })).not.toBeInTheDocument();
  });
});
