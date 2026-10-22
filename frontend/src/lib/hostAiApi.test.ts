import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BackendApiError } from './api';
import { hostAiSafeErrorMessage } from './hostAiApi';

describe('host AI API helpers', () => {
  beforeEach(() => vi.clearAllMocks());

  it('maps gateway availability errors to safe guidance', () => {
    expect(hostAiSafeErrorMessage(new BackendApiError('DISABLED', 'raw upstream detail', undefined, 503)))
      .toContain('temporarily unavailable');
    expect(hostAiSafeErrorMessage(new BackendApiError('RATE_LIMITED', 'raw upstream detail', undefined, 429)))
      .toContain('busy');
  });

  it('does not expose arbitrary upstream errors', () => {
    expect(hostAiSafeErrorMessage(new Error('provider key leaked'))).not.toContain('provider key');
  });
});
