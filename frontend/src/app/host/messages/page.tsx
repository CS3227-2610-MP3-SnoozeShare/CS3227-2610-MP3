'use client';

import { useCallback, useEffect, useMemo } from 'react';
import { useHostFixtures } from '@/components/host/HostFixtureProvider';
import { useOptionalHostApi } from '@/components/host/HostApiProvider';
import { HostMessagesPanel } from '@/components/host/HostMessagesPanel';
import { HostPageFrame } from '@/components/host/HostPageFrame';
import { mapBookingConversation, mapHostMessages, mapTicketConversation, type BookingConversationDto, type HostMessageDto, type TicketConversationDto } from '@/components/host/hostTypes';

export default function HostMessagesPage() {
  const fixtures = useHostFixtures();
  const api = useOptionalHostApi();
  const apiAvailable = api !== null;
  const hostApi = api?.api;
  const loadResource = api?.loadResource;
  const fetchBookingConversations = api?.api.fetchBookingConversations;
  const fetchTicketConversations = api?.api.fetchTicketConversations;
  const bookingKey = 'host:booking-conversations';
  const ticketKey = 'host:ticket-conversations';
  useEffect(() => {
    if (!loadResource || !fetchBookingConversations || !fetchTicketConversations) return;
    void loadResource(bookingKey, () => fetchBookingConversations()).catch(() => undefined);
    void loadResource(ticketKey, () => fetchTicketConversations()).catch(() => undefined);
  }, [fetchBookingConversations, fetchTicketConversations, loadResource]);
  const bookingResource = api?.getResource<BookingConversationDto[]>(bookingKey);
  const ticketResource = api?.getResource<TicketConversationDto[]>(ticketKey);
  const fixture = fixtures.getMessages();
  const loading = api
    ? !bookingResource || !ticketResource || bookingResource.status === 'loading' || ticketResource.status === 'loading'
    : fixture.state === 'loading';
  const error = api
    ? bookingResource?.status === 'error' ? bookingResource.message : ticketResource?.status === 'error' ? ticketResource.message : ''
    : fixture.state === 'error' ? fixture.error : '';
  const conversations = useMemo(() => apiAvailable
    ? [
      ...(bookingResource?.status === 'ready' || bookingResource?.status === 'empty' ? bookingResource.data.map(mapBookingConversation) : []),
      ...(ticketResource?.status === 'ready' || ticketResource?.status === 'empty' ? ticketResource.data.map(mapTicketConversation) : []),
    ]
    : fixture.data ?? [], [apiAvailable, bookingResource, ticketResource, fixture.data]);
  const loadThread = useCallback(async (conversation: (typeof conversations)[number]) => {
    if (!hostApi || !loadResource || !conversation.kind || !conversation.sourceId) return;
    const key = `host:thread:${conversation.kind}:${conversation.sourceId}`;
    if (conversation.kind === 'booking') {
      await Promise.all([
        loadResource(key, () => hostApi.fetchBookingMessages(conversation.sourceId!)),
        hostApi.markBookingConversationRead(conversation.sourceId),
      ]);
    } else {
      await Promise.all([
        loadResource(key, () => hostApi.fetchTicketMessages(conversation.sourceId!)),
        hostApi.markTicketConversationRead(conversation.sourceId),
      ]);
    }
  }, [hostApi, loadResource]);
  const sendMessage = useCallback(async (conversation: (typeof conversations)[number], body: string) => {
    if (!hostApi || !loadResource || !conversation.kind || !conversation.sourceId) return;
    const key = `host:thread:${conversation.kind}:${conversation.sourceId}`;
    if (conversation.kind === 'booking') await hostApi.postBookingMessage(conversation.sourceId, body);
    else await hostApi.postTicketMessage(conversation.sourceId, body);
    await loadResource(key, () => conversation.kind === 'booking'
      ? hostApi.fetchBookingMessages(conversation.sourceId!)
      : hostApi.fetchTicketMessages(conversation.sourceId!));
  }, [hostApi, loadResource]);
  const messagesByConversation: Record<string, ReturnType<typeof mapHostMessages>> = {};
  if (api) {
    for (const conversation of conversations) {
      if (!conversation.kind || !conversation.sourceId) continue;
      const resource = api.getResource<HostMessageDto[]>(`host:thread:${conversation.kind}:${conversation.sourceId}`);
      if (resource?.status === 'ready' || resource?.status === 'empty') {
        messagesByConversation[conversation.id] = mapHostMessages(resource.data, conversation.counterpartName ?? 'Guest');
      }
    }
  }
  return <HostPageFrame fillHeight title="Messages">
    {loading ? <div className="ss-card h-64 animate-pulse bg-[var(--surface-inset)]" /> : error ? <div className="ss-card p-8 text-center">{error}</div> : <HostMessagesPanel
      conversations={conversations}
      messagesByConversation={messagesByConversation}
      onSelect={api ? loadThread : undefined}
      onSend={api ? sendMessage : undefined}
    />}
  </HostPageFrame>;
}
