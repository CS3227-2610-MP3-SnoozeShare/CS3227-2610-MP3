import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import HostMessagesPage from './page';
import { HostFixtureProvider } from '@/components/host/HostFixtureProvider';

describe('Host messages page', () => { it('renders direct and resolved conversations', () => { render(<HostFixtureProvider><HostMessagesPage /></HostFixtureProvider>); expect(screen.getByText('Messages')).toBeInTheDocument(); expect(screen.getByRole('button', { name: /Ticket #1042/ })).toHaveTextContent('Refund not received'); }); });
