import type { DisputeDetail, DisputeSummary, Ticket } from '@/lib/agent-api/types';

// Fixture shapes use the same field names as the backend response records (contract rule).

export function ticket(overrides: Partial<Ticket> = {}): Ticket {
  return {
    id: 't-1',
    bookingId: 'b-1',
    raisedByUserId: 'g-1',
    raisedByRole: 'GUEST',
    category: 'Cleanliness',
    title: 'Dirty flat',
    description: 'It was not clean',
    requestedRemedy: 'FULL_REFUND',
    supportingText: null,
    status: 'IN_REVIEW',
    assignedAgentId: 'agent-1',
    resolutionReason: null,
    createdAt: '2026-10-01T04:00:00Z',
    resolvedAt: null,
    ...overrides,
  };
}

export function detail(overrides: Partial<DisputeDetail> = {}, ticketOverrides: Partial<Ticket> = {}): DisputeDetail {
  return {
    ticket: ticket(ticketOverrides),
    ticketLabel: '#0001',
    listingTitle: 'Marina flat',
    startDate: '2026-09-25',
    endDate: '2026-09-28',
    guestName: 'Gina Guest',
    hostName: 'Hal Host',
    raisedByName: 'Gina Guest',
    assignedAgentName: 'Ann Agent',
    bookingStatus: 'CONFIRMED',
    escrowAmount: 120,
    escrowHeld: true,
    phaseLabel: 'Completed',
    agentNotes: null,
    ...overrides,
  };
}

export function summary(overrides: Partial<DisputeSummary> = {}): DisputeSummary {
  return {
    ticketId: 't-1',
    ticketLabel: '#0001',
    title: 'Dirty flat',
    category: 'Cleanliness',
    listingTitle: 'Marina flat',
    guestName: 'Gina Guest',
    hostName: 'Hal Host',
    assignedAgentName: null,
    status: 'OPEN',
    createdAt: '2026-10-01T04:00:00Z',
    ...overrides,
  };
}
