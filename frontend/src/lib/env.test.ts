import { afterEach, describe, expect, it } from 'vitest';
import { apiBaseUrl, validateEnv } from './env';

describe('validateEnv (E27)', () => {
  it('requires the variable in a production build', () => {
    expect(() => validateEnv({ NODE_ENV: 'production' })).toThrow('NEXT_PUBLIC_API_BASE_URL is required');
  });

  it('rejects a value that is not an http(s) URL', () => {
    expect(() => validateEnv({ NODE_ENV: 'production', NEXT_PUBLIC_API_BASE_URL: 'localhost:8080' })).toThrow(
      'NEXT_PUBLIC_API_BASE_URL must be an http(s) URL without a trailing slash',
    );
  });

  it('rejects a trailing slash', () => {
    expect(() => validateEnv({ NODE_ENV: 'production', NEXT_PUBLIC_API_BASE_URL: 'http://x/' })).toThrow(
      'without a trailing slash',
    );
  });

  it('accepts a valid production value', () => {
    expect(() =>
      validateEnv({ NODE_ENV: 'production', NEXT_PUBLIC_API_BASE_URL: 'https://api.example.com' }),
    ).not.toThrow();
  });

  it('does not throw outside production', () => {
    expect(() => validateEnv({ NODE_ENV: 'development' })).not.toThrow();
  });
});

describe('apiBaseUrl', () => {
  const original = process.env.NEXT_PUBLIC_API_BASE_URL;
  afterEach(() => {
    if (original === undefined) delete process.env.NEXT_PUBLIC_API_BASE_URL;
    else process.env.NEXT_PUBLIC_API_BASE_URL = original;
  });

  it('defaults to localhost:8080 when unset', () => {
    delete process.env.NEXT_PUBLIC_API_BASE_URL;
    expect(apiBaseUrl()).toBe('http://localhost:8080');
  });

  it('returns the configured value', () => {
    process.env.NEXT_PUBLIC_API_BASE_URL = 'https://api.example.com';
    expect(apiBaseUrl()).toBe('https://api.example.com');
  });
});
