import { DisputeDetail } from '@/components/agent/DisputeDetail';

export default async function AgentDisputeDetailPage({ params }: { params: Promise<{ ticketId: string }> }) {
  const { ticketId } = await params;
  return <DisputeDetail ticketId={ticketId} />;
}
