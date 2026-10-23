// Field names mirror the backend response records (api.md). Amounts arrive as JSON numbers and are only displayed.

export type TicketStatus = 'OPEN' | 'IN_REVIEW' | 'RESOLVED_APPROVED' | 'RESOLVED_REJECTED';
export type RemedyType = 'FULL_REFUND' | 'PARTIAL_REFUND' | 'HOST_PAYOUT' | 'OTHER';
export type ResolutionMode = 'ACCEPT' | 'REJECT' | 'MANUAL';
export type ThreadChannel = 'GUEST' | 'HOST';
export type AssigneeFilter = 'ALL' | 'UNASSIGNED' | 'MINE';
export type Role = 'GUEST' | 'HOST' | 'AGENT' | 'SYSTEM';
export type AccountStatus = 'ACTIVE' | 'SUSPENDED';

export type DisputeSummary = {
  ticketId: string;
  ticketLabel: string;
  title: string;
  category: string;
  listingTitle: string;
  guestName: string;
  hostName: string;
  assignedAgentName: string | null;
  status: TicketStatus;
  createdAt: string;
};

export type DisputeQueue = { items: DisputeSummary[]; unassignedCount: number };

export type Ticket = {
  id: string;
  bookingId: string;
  raisedByUserId: string;
  raisedByRole: Role;
  category: string;
  title: string;
  description: string;
  requestedRemedy: RemedyType;
  supportingText: string | null;
  status: TicketStatus;
  assignedAgentId: string | null;
  resolutionReason: string | null;
  createdAt: string;
  resolvedAt: string | null;
};

export type Assistant = {
  state: 'PENDING' | 'WAITING_GUEST' | 'DONE' | 'STOPPED' | 'UNAVAILABLE';
  rounds: number;
  maxRounds: number;
  flagged: boolean;
  stale: boolean;
  draft: string | null;
  draftUpdatedAt: string | null;
};

export type DisputeDetail = {
  ticket: Ticket;
  ticketLabel: string;
  listingTitle: string;
  startDate: string;
  endDate: string;
  guestName: string;
  hostName: string;
  raisedByName: string;
  assignedAgentName: string | null;
  bookingStatus: string;
  escrowAmount: number;
  escrowHeld: boolean;
  phaseLabel: string;
  agentNotes: string | null;
  assistant: Assistant | null;
};

export type ThreadMessage = { id: string; authorId: string; authorRole: Role; body: string; sentAt: string };

export type ResolveRequest = { mode: ResolutionMode; guestRefund: string | null; reason: string };

export type AccountSummary = {
  userId: string;
  displayName: string;
  email: string;
  role: Role;
  createdAt: string;
  status: AccountStatus;
  suspensionReason: string | null;
};

export type AuditEntry = {
  id: string;
  timestamp: string;
  actorName: string | null;
  actionType: string;
  entityType: string;
  entityId: string;
  beforeState: string | null;
  afterState: string | null;
  walletAdjustment: number | null;
  balanceAfter: number | null;
  reason: string | null;
  subjectName: string | null;
  bookingId: string | null;
  ticketId: string | null;
};

export type TicketCategory = { id: string; label: string; active: boolean };
