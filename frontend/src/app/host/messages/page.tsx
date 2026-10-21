'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useHostFixtures } from '@/components/host/HostFixtureProvider';
import { useOptionalHostApi } from '@/components/host/HostApiProvider';
import { HostMessagesPanel } from '@/components/host/HostMessagesPanel';
import { HostPageFrame } from '@/components/host/HostPageFrame';
import { mapBookingConversation, mapHostMessages, mapTicketConversation, type BookingConversationDto, type HostMessageDto, type HostPage, type TicketConversationDto } from '@/components/host/hostTypes';

export default function HostMessagesPage() {
  const fixtures = useHostFixtures();
  const api = useOptionalHostApi();
  const apiAvailable = api !== null;
  const [historyPages, setHistoryPages] = useState<Record<string, number>>({});
  const hostApi = api?.api;
  const loadResource = api?.loadResource;
  const fetchBookingConversations = api?.api.fetchBookingConversations;
  const fetchTicketConversations = api?.api.fetchTicketConversations;
  const bookingKey = 'host:booking-conversations:0';
  const ticketKey = 'host:ticket-conversations:0';
  useEffect(() => {
    if (!loadResource || !fetchBookingConversations || !fetchTicketConversations) return;
    void loadResource(bookingKey, () => fetchBookingConversations(0, 50)).catch(() => undefined);
    void loadResource(ticketKey, () => fetchTicketConversations(0, 50)).catch(() => undefined);
  }, [fetchBookingConversations, fetchTicketConversations, loadResource]);
  const bookingResource = api?.getResource<HostPage<BookingConversationDto>>(bookingKey);
  const ticketResource = api?.getResource<HostPage<TicketConversationDto>>(ticketKey);
  const fixture = fixtures.getMessages();
  const loading = api
    ? !bookingResource || !ticketResource || bookingResource.status === 'loading' || ticketResource.status === 'loading'
    : fixture.state === 'loading';
  const error = api
    ? bookingResource?.status === 'error' ? bookingResource.message : ticketResource?.status === 'error' ? ticketResource.message : ''
    : fixture.state === 'error' ? fixture.error : '';
  const conversations = useMemo(() => apiAvailable
    ? [
      ...(bookingResource?.status === 'ready' || bookingResource?.status === 'empty' ? bookingResource.data.items.map(mapBookingConversation) : []),
      ...(ticketResource?.status === 'ready' || ticketResource?.status === 'empty' ? ticketResource.data.items.map(mapTicketConversation) : []),
    ]
    : fixture.data ?? [], [apiAvailable, bookingResource, ticketResource, fixture.data]);
  const loadThread = useCallback(async (conversation: (typeof conversations)[number]) => {
    if (!hostApi || !loadResource || !conversation.kind || !conversation.sourceId) return;
    const key = `host:thread:${conversation.kind}:${conversation.sourceId}:0`;
    if (conversation.kind === 'booking') {
      await Promise.all([
        loadResource(key, () => hostApi.fetchBookingMessages(conversation.sourceId!, 0, 50)),
        hostApi.markBookingConversationRead(conversation.sourceId),
      ]);
    } else {
      await Promise.all([
        loadResource(key, () => hostApi.fetchTicketMessages(conversation.sourceId!, 0, 50)),
        hostApi.markTicketConversationRead(conversation.sourceId),
      ]);
    }
  }, [hostApi, loadResource]);
  const sendMessage = useCallback(async (conversation: (typeof conversations)[number], body: string) => {
    if (!hostApi || !loadResource || !conversation.kind || !conversation.sourceId) return;
    const key = `host:thread:${conversation.kind}:${conversation.sourceId}:0`;
    if (conversation.kind === 'booking') await hostApi.postBookingMessage(conversation.sourceId, body);
    else await hostApi.postTicketMessage(conversation.sourceId, body);
    await loadResource(key, () => conversation.kind === 'booking'
      ? hostApi.fetchBookingMessages(conversation.sourceId!, 0, 50)
      : hostApi.fetchTicketMessages(conversation.sourceId!, 0, 50));
  }, [hostApi, loadResource]);
  const loadOlderMessages = useCallback(async (conversation: (typeof conversations)[number]) => {
    if (!hostApi || !loadResource || !conversation.kind || !conversation.sourceId) return;
    const nextPage = (historyPages[conversation.id] ?? 0) + 1;
    const key = `host:thread:${conversation.kind}:${conversation.sourceId}:${nextPage}`;
    await loadResource(key, () => conversation.kind === 'booking'
      ? hostApi.fetchBookingMessages(conversation.sourceId!, nextPage, 50)
      : hostApi.fetchTicketMessages(conversation.sourceId!, nextPage, 50));
    setHistoryPages((current) => ({ ...current, [conversation.id]: nextPage }));
  }, [hostApi, loadResource, historyPages]);
  const hasOlderMessages = useCallback((conversation: (typeof conversations)[number]) => {
    if (!api || !conversation.kind || !conversation.sourceId) return false;
    const page = historyPages[conversation.id] ?? 0;
    const resource = api.getResource<HostPage<HostMessageDto>>(`host:thread:${conversation.kind}:${conversation.sourceId}:${page}`);
    return (resource?.status === 'ready' || resource?.status === 'empty') && resource.data.hasMore;
  }, [api, historyPages]);
  const messagesByConversation: Record<string, ReturnType<typeof mapHostMessages>> = {};
  if (api) {
    for (const conversation of conversations) {
      if (!conversation.kind || !conversation.sourceId) continue;
      const pages = historyPages[conversation.id] ?? 0;
      const pageItems: HostMessageDto[] = [];
      for (let page = 0; page <= pages; page += 1) {
        const resource = api.getResource<HostPage<HostMessageDto>>(`host:thread:${conversation.kind}:${conversation.sourceId}:${page}`);
        if (resource?.status === 'ready' || resource?.status === 'empty') pageItems.push(...resource.data.items);
      }
      if (pageItems.length) messagesByConversation[conversation.id] = mapHostMessages(pageItems, conversation.counterpartName ?? 'Guest');
    }
  }
  return <HostPageFrame fillHeight title="Messages">
    {loading ? <div className="ss-card h-64 animate-pulse bg-[var(--surface-inset)]" /> : error ? <div className="ss-card p-8 text-center">{error}</div> : <HostMessagesPanel
      conversations={conversations}
      messagesByConversation={messagesByConversation}
      onSelect={api ? loadThread : undefined}
      onSend={api ? sendMessage : undefined}
      historyHasMore={api ? hasOlderMessages : undefined}
      onLoadOlder={api ? loadOlderMessages : undefined}
    />}
  </HostPageFrame>;
}
