export type MessageRole = 'GUEST' | 'HOST' | 'AGENT' | 'SYSTEM';

const authorLabels: Record<MessageRole, string> = {
  GUEST: 'Guest',
  HOST: 'Host',
  AGENT: 'Support agent',
  SYSTEM: 'Snoozy · Automated assistant',
};

export function authorRoleLabel(role: MessageRole): string {
  return authorLabels[role];
}
