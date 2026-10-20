import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { RoleShell } from './RoleShell';

describe('RoleShell', () => {
  it('renders the navbar above an empty landing region', () => {
    render(<RoleShell role="GUEST"><main data-testid="empty-landing" /></RoleShell>);
    expect(screen.getByRole('navigation')).toBeInTheDocument();
    expect(screen.getByTestId('empty-landing')).toBeEmptyDOMElement();
  });
});
