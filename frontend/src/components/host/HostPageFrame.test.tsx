import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { HostPageFrame } from './HostPageFrame';

describe('Host page frame', () => {
  it('allows Host content to span the full available page width', () => {
    render(<HostPageFrame title="My listings"><div>Content</div></HostPageFrame>);

    expect(screen.getByRole('main')).toHaveClass('w-full', 'max-w-none');
    expect(screen.getByRole('main').className).not.toMatch(/max-w-(?:6xl|7xl)/);
  });

  it('provides a bounded flex column when page content must scroll internally', () => {
    render(<HostPageFrame fillHeight title="My listings"><div>Content</div></HostPageFrame>);

    expect(screen.getByRole('main')).toHaveClass('flex', 'min-h-0', 'flex-1', 'flex-col');
  });
});
