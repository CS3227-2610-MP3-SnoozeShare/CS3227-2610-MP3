package com.snoozeshare.infra.events;

import java.util.ArrayList;
import java.util.List;

import com.snoozeshare.infra.db.TransactionListener;

/**
 * Queues events during a transaction and publishes them, in order, only after it commits; a rollback discards
 * them. Single-process assumption: this one shared queue relies on there being one connection and one thread of
 * work; it must become per-transaction if concurrency is ever introduced (see PROJECT_STATE § Known Gaps).
 */
public final class PostCommitEvents implements TransactionListener {

    private final EventSink sink;
    private final boolean autocommitMode;
    private final List<DomainEvent> pending = new ArrayList<>();
    private boolean active;

    private PostCommitEvents(EventSink sink, boolean autocommitMode) {
        this.sink = sink;
        this.autocommitMode = autocommitMode;
    }

    /** Production mode: enqueuing outside a transaction is a programming error and throws. */
    public static PostCommitEvents strict(EventSink sink) {
        return new PostCommitEvents(sink, false);
    }

    /** For code that runs in auto-commit mode (tests): outside a transaction each write is already committed. */
    public static PostCommitEvents autocommit(EventSink sink) {
        return new PostCommitEvents(sink, true);
    }

    public void enqueue(DomainEvent event) {
        if (!active) {
            if (autocommitMode) {
                publish(event);
                return;
            }
            throw new IllegalStateException("Events can only be queued inside a transaction");
        }
        pending.add(event);
    }

    @Override
    public void onBegin() {
        if (active) {
            throw new IllegalStateException("Nested transactions are not supported");
        }
        active = true;
        pending.clear();
    }

    @Override
    public void afterCommit() {
        active = false;
        List<DomainEvent> toPublish = new ArrayList<>(pending);
        pending.clear();
        toPublish.forEach(this::publish);
    }

    @Override
    public void afterRollback() {
        active = false;
        pending.clear();
    }

    private void publish(DomainEvent event) {
        try {
            sink.publish(event);
        } catch (RuntimeException ignored) {
            // The change is already committed; one failing subscriber must not hide it or stop later events.
        }
    }
}
