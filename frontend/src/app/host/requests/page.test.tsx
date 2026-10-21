import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import HostRequestsPage from './page';
import { HostFixtureProvider } from '@/components/host/HostFixtureProvider';

describe('Host requests page', () => { it('renders pending and past request sections', () => { render(<HostFixtureProvider><HostRequestsPage /></HostFixtureProvider>); expect(screen.getByText('3 pending')).toBeInTheDocument(); expect(screen.getByText('Past requests')).toBeInTheDocument(); }); });
