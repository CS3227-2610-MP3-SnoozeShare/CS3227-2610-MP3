import { apiJson } from '@/lib/api';
import type { AuditEntry } from './types';

export const AUDIT_PAGE_SIZE = 200;

export type AuditQuery = { text: string; actions: string[]; from: string; to: string };

export function fetchAudit(query: AuditQuery, offset: number): Promise<AuditEntry[]> {
  const params = new URLSearchParams();
  if (query.text.trim()) params.set('text', query.text.trim());
  if (query.actions.length > 0) params.set('actions', query.actions.join(','));
  if (query.from) params.set('from', query.from);
  if (query.to) params.set('to', query.to);
  params.set('limit', String(AUDIT_PAGE_SIZE));
  params.set('offset', String(offset));
  return apiJson<AuditEntry[]>(`/api/agent/audit?${params.toString()}`);
}

export function fetchAuditActions(): Promise<string[]> {
  return apiJson<string[]>('/api/agent/audit/actions');
}
