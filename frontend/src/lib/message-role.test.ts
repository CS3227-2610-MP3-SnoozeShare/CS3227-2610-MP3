import { describe, expect, it } from 'vitest';
import { authorRoleLabel } from './message-role';

describe('authorRoleLabel', () => {
  it.each([
    ['GUEST', 'Guest'],
    ['HOST', 'Host'],
    ['AGENT', 'Support agent'],
    ['SYSTEM', 'Snoozy · Automated assistant'],
  ] as const)('F7 labels %s as %s', (role, label) => {
    expect(authorRoleLabel(role)).toBe(label);
  });
});
