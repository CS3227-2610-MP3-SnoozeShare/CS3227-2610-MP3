package com.snoozeshare.infra.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PostCommitEventsTest {

    private record TestEvent(UUID eventId, Instant occurredAt) implements DomainEvent {
        TestEvent() {
            this(UUID.randomUUID(), Instant.now());
        }
    }

    private final List<DomainEvent> published = new ArrayList<>();

    @Test
    void commitPublishesQueuedEventsInOrder() {
        PostCommitEvents events = PostCommitEvents.strict(published::add);
        TestEvent first = new TestEvent();
        TestEvent second = new TestEvent();
        events.onBegin();
        events.enqueue(first);
        events.enqueue(second);
        assertTrue(published.isEmpty(), "nothing is published before commit");
        events.afterCommit();
        assertEquals(List.of(first, second), published);
    }

    @Test
    void rollbackDiscardsQueuedEvents() {
        PostCommitEvents events = PostCommitEvents.strict(published::add);
        events.onBegin();
        events.enqueue(new TestEvent());
        events.afterRollback();
        events.onBegin();
        events.afterCommit();
        assertTrue(published.isEmpty());
    }

    @Test
    void strictModeRejectsEnqueueOutsideATransaction() {
        PostCommitEvents events = PostCommitEvents.strict(published::add);
        assertThrows(IllegalStateException.class, () -> events.enqueue(new TestEvent()));
    }

    @Test
    void autocommitModePublishesImmediatelyOutsideATransaction() {
        PostCommitEvents events = PostCommitEvents.autocommit(published::add);
        TestEvent event = new TestEvent();
        events.enqueue(event);
        assertEquals(List.of(event), published);
    }

    @Test
    void beginningWhileActiveIsRejectedAsNesting() {
        PostCommitEvents events = PostCommitEvents.strict(published::add);
        events.onBegin();
        assertThrows(IllegalStateException.class, events::onBegin);
    }

    @Test
    void aFailingSinkDoesNotEscapeAfterCommit() {
        List<DomainEvent> delivered = new ArrayList<>();
        PostCommitEvents events = PostCommitEvents.strict(event -> {
            if (delivered.isEmpty()) {
                delivered.add(event);
                throw new IllegalStateException("subscriber blew up");
            }
            delivered.add(event);
        });
        events.onBegin();
        events.enqueue(new TestEvent());
        events.enqueue(new TestEvent());
        events.afterCommit();
        assertEquals(2, delivered.size(), "remaining events are still delivered");
    }
}
