package com.snoozeshare.testsupport;

import java.sql.Connection;

import com.snoozeshare.infra.db.TransactionManager;
import com.snoozeshare.infra.events.EventBus;
import com.snoozeshare.infra.events.InProcessEventBus;
import com.snoozeshare.infra.events.PostCommitEvents;
import com.snoozeshare.repository.jdbc.JdbcWalletRepository;
import com.snoozeshare.service.AuditService;
import com.snoozeshare.service.impl.LedgerWriter;

/** Wires a shared transaction manager, a strict post-commit queue and a ledger writer the way AppContext does. */
public record Tx(PostCommitEvents events, TransactionManager manager, LedgerWriter ledger, EventBus bus) {

    public static Tx of(Connection connection) {
        return of(connection, new InProcessEventBus());
    }

    public static Tx of(Connection connection, EventBus bus) {
        return of(connection, bus, LedgerTestSupport.audit(connection));
    }

    /** Like {@link #of(Connection, EventBus)} but the ledger writes its audit rows through {@code audit}. */
    public static Tx of(Connection connection, EventBus bus, AuditService audit) {
        PostCommitEvents events = PostCommitEvents.strict(bus::publish);
        LedgerWriter ledger = new LedgerWriter(new JdbcWalletRepository(connection), audit, events);
        return new Tx(events, new TransactionManager(connection, events), ledger, bus);
    }
}
