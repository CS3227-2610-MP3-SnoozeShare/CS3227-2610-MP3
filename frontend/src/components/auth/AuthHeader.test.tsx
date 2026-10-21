import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { AuthHeader } from './AuthHeader';

describe('AuthHeader', () => {
  it('uses the artifact brand label and centered title treatment', () => {
    render(<AuthHeader title="Welcome back" />);
    expect(screen.getByText('SNOOZESHARE')).toHaveClass('ss-auth-brand');
    expect(screen.getByRole('heading', { name: 'Welcome back' })).toHaveClass('ss-auth-title');
  });
});
