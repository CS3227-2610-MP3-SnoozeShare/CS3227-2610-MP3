import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import HostWalletPage from './page';
import { HostFixtureProvider } from '@/components/host/HostFixtureProvider';

describe('Host wallet page', () => { it('renders balance and statement', () => { render(<HostFixtureProvider><HostWalletPage /></HostFixtureProvider>); expect(screen.getByText('AVAILABLE BALANCE')).toBeInTheDocument(); expect(screen.getByText('Transaction statement')).toBeInTheDocument(); }); });
