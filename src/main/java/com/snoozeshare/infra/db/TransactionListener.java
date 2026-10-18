package com.snoozeshare.infra.db;

/** Lifecycle callbacks a {@link TransactionManager} fires; it knows nothing about what a listener does. */
public interface TransactionListener {

    /** Ignores every callback; used when nothing needs to react to the transaction. */
    TransactionListener NONE = new TransactionListener() {
        @Override
        public void onBegin() {
            // nothing to do
        }

        @Override
        public void afterCommit() {
            // nothing to do
        }

        @Override
        public void afterRollback() {
            // nothing to do
        }
    };

    void onBegin();

    /** Called after the commit, with the connection's auto-commit already restored. */
    void afterCommit();

    /** Called after the rollback, with the connection's auto-commit already restored. */
    void afterRollback();
}
