import { describe, expect, it, vi } from 'vitest';

const redirect = vi.fn();
vi.mock('next/navigation', () => ({ redirect }));

describe('agent landing page (F13, E60)', () => {
  it('redirects /agent to /agent/disputes', async () => {
    const { default: AgentLandingPage } = await import('./page');
    AgentLandingPage();
    expect(redirect).toHaveBeenCalledWith('/agent/disputes');
  });
});
