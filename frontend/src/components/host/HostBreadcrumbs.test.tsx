import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { HostBreadcrumbs } from './HostBreadcrumbs';

describe('Host breadcrumbs', () => {
  it('bolds the current page while leaving ancestor links regular', () => {
    render(<nav aria-label="Breadcrumb"><HostBreadcrumbs items={[{ label: 'Listings', href: '/host/listings' }, { label: 'Lakeside Cabin' }]} /></nav>);

    expect(screen.getByRole('link', { name: 'Listings' })).not.toHaveClass('font-semibold');
    expect(screen.getByText('Lakeside Cabin')).toHaveAttribute('aria-current', 'page');
    expect(screen.getByText('Lakeside Cabin')).toHaveClass('font-semibold');
  });
});
