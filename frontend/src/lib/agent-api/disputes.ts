import { apiJson } from '@/lib/api';
import type { AssigneeFilter, DisputeDetail, DisputeQueue, ResolveRequest, ThreadChannel, ThreadMessage, TicketStatus } from './types';

const BASE = '/api/agent/disputes';

function put<T>(path: string, body?: unknown): Promise<T> {
  return apiJson<T>(path, { method: 'PUT', body: body === undefined ? undefined : JSON.stringify(body) });
}

export function fetchQueue(assignee: AssigneeFilter, status?: TicketStatus): Promise<DisputeQueue> {
  return apiJson<DisputeQueue>(`${BASE}?assignee=${assignee}${status ? `&status=${status}` : ''}`);
}

export function fetchDispute(ticketId: string): Promise<DisputeDetail> {
  return apiJson<DisputeDetail>(`${BASE}/${ticketId}`);
}

export function assignDispute(ticketId: string): Promise<DisputeDetail> {
  return put<DisputeDetail>(`${BASE}/${ticketId}/assign`);
}

export function unassignDispute(ticketId: string): Promise<DisputeDetail> {
  return put<DisputeDetail>(`${BASE}/${ticketId}/unassign`);
}

export function saveNotes(ticketId: string, notes: string): Promise<DisputeDetail> {
  return put<DisputeDetail>(`${BASE}/${ticketId}/notes`, { notes });
}

export function resolveDispute(ticketId: string, request: ResolveRequest): Promise<DisputeDetail> {
  return put<DisputeDetail>(`${BASE}/${ticketId}/resolve`, request);
}

export function fetchThread(ticketId: string, channel: ThreadChannel): Promise<ThreadMessage[]> {
  return apiJson<ThreadMessage[]>(`${BASE}/${ticketId}/messages/${channel}`);
}

export function postMessage(ticketId: string, channel: ThreadChannel, body: string): Promise<ThreadMessage> {
  return apiJson<ThreadMessage>(`${BASE}/${ticketId}/messages/${channel}`, {
    method: 'POST',
    body: JSON.stringify({ body }),
  });
}
