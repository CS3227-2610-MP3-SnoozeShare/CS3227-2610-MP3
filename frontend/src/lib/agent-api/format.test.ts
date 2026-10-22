import { describe, expect, it } from 'vitest';
import { formatDateTime, formatSgd, isMoneyFormat } from './format';

describe('formatSgd (N2)', () => {
  it('prefixes SGD and shows two decimals', () => {
    expect(formatSgd(480)).toBe('SGD 480.00');
    expect(formatSgd(12.5)).toBe('SGD 12.50');
    expect(formatSgd('97')).toBe('SGD 97.00');
    expect(formatSgd('58.2')).toBe('SGD 58.20');
  });

  it('keeps a negative adjustment sign', () => {
    expect(formatSgd(-120)).toBe('-SGD 120.00');
  });

  it('shows an em dash when there is no amount', () => {
    expect(formatSgd(null)).toBe('—');
  });
});

describe('isMoneyFormat (E73)', () => {
  it.each(['0', '0.00', '12', '12.5', '12.50', '123456789.99'])('accepts %s', (text) => {
    expect(isMoneyFormat(text)).toBe(true);
  });

  it.each(['', '-1', 'abc', '1.234', '1.', '.5', '1,000', '1234567890'])('rejects "%s"', (text) => {
    expect(isMoneyFormat(text)).toBe(false);
  });
});

describe('formatDateTime', () => {
  it('formats an ISO instant as day month, time', () => {
    expect(formatDateTime('2026-09-25T15:42:00Z', 'UTC')).toBe('25 Sep 2026, 3:42 PM');
  });
});
