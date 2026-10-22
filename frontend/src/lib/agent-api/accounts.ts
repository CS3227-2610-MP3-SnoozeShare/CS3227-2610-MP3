import { apiJson } from '@/lib/api';
import type { AccountSummary } from './types';

const BASE = '/api/agent/accounts';

export function fetchAccounts(): Promise<AccountSummary[]> {
  return apiJson<AccountSummary[]>(BASE);
}

export function setAccountSuspended(userId: string, suspend: boolean, reason: string): Promise<AccountSummary> {
  return apiJson<AccountSummary>(`${BASE}/${userId}/${suspend ? 'suspend' : 'reactivate'}`, {
    method: 'PUT',
    body: JSON.stringify({ reason }),
  });
}
