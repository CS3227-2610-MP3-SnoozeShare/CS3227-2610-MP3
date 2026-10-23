import { apiJson } from '@/lib/api';
import type { TicketCategory } from './types';

const BASE = '/api/agent/categories';

export function fetchCategories(): Promise<TicketCategory[]> {
  return apiJson<TicketCategory[]>(BASE);
}

export function createCategory(label: string): Promise<TicketCategory> {
  return apiJson<TicketCategory>(BASE, { method: 'POST', body: JSON.stringify({ label }) });
}

export function renameCategory(id: string, label: string): Promise<TicketCategory> {
  return apiJson<TicketCategory>(`${BASE}/${id}`, { method: 'PUT', body: JSON.stringify({ label }) });
}

export function setCategoryActive(id: string, active: boolean): Promise<TicketCategory> {
  return apiJson<TicketCategory>(`${BASE}/${id}/active`, { method: 'PUT', body: JSON.stringify({ active }) });
}

/** Resolves to undefined: the backend answers 204 with no body. */
export function deleteCategory(id: string): Promise<void> {
  return apiJson<void>(`${BASE}/${id}`, { method: 'DELETE' });
}
