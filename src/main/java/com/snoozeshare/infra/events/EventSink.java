package com.snoozeshare.infra.events;

/** Where committed events go. Today it is the in-process bus; a transactional outbox would replace it later. */
@FunctionalInterface
public interface EventSink {
    void publish(DomainEvent event);
}
