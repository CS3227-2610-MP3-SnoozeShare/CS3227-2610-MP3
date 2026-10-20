-- Domain tables for SnoozeShare, translated from the desktop SQLite schema (V001-V008).
-- The wallet_transactions table is intentionally absent: the unified ledger lives in audit_log.

CREATE TABLE users (
    user_id       UUID PRIMARY KEY,
    role          VARCHAR(10) NOT NULL
                      CHECK (role IN ('GUEST', 'HOST', 'AGENT', 'SYSTEM')),
    display_name  VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    account_status VARCHAR(10) NOT NULL
                      CHECK (account_status IN ('ACTIVE', 'SUSPENDED')),
    registration_code VARCHAR(255),
    created_at    TIMESTAMPTZ NOT NULL,
    suspension_reason TEXT
);

CREATE TABLE wallets (
    wallet_id  UUID PRIMARY KEY,
    user_id    UUID NOT NULL UNIQUE REFERENCES users(user_id),
    balance    NUMERIC(19, 4) NOT NULL CHECK (balance >= 0),
    currency   VARCHAR(3) NOT NULL DEFAULT 'SGD' CHECK (currency = 'SGD'),
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE properties (
    property_id        UUID PRIMARY KEY,
    host_id            UUID NOT NULL REFERENCES users(user_id),
    status             VARCHAR(10) NOT NULL
                           CHECK (status IN ('ACTIVE', 'INACTIVE')),
    title              VARCHAR(255) NOT NULL,
    description        TEXT NOT NULL,
    property_type      VARCHAR(20) NOT NULL
                           CHECK (property_type IN (
                               'APARTMENT', 'HOUSE', 'CONDO', 'PRIVATE_ROOM')),
    street_address     VARCHAR(500) NOT NULL,
    city               VARCHAR(255) NOT NULL,
    region             VARCHAR(255) NOT NULL,
    postal_code        INTEGER NOT NULL,
    max_guests         INTEGER NOT NULL CHECK (max_guests > 0),
    bedrooms           INTEGER NOT NULL CHECK (bedrooms >= 0),
    bathrooms          INTEGER NOT NULL CHECK (bathrooms >= 0),
    base_nightly_rate  NUMERIC(19, 4) NOT NULL CHECK (base_nightly_rate >= 0),
    check_in_time      TIME NOT NULL,
    check_out_time     TIME NOT NULL,
    amenities          TEXT NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL
);

CREATE TABLE bookings (
    booking_id             UUID PRIMARY KEY,
    listing_id             UUID NOT NULL REFERENCES properties(property_id),
    guest_id               UUID NOT NULL REFERENCES users(user_id),
    start_date             DATE NOT NULL,
    end_date               DATE NOT NULL,
    status                 VARCHAR(25) NOT NULL
                               CHECK (status IN (
                                   'PENDING', 'CONFIRMED', 'REJECTED',
                                   'CANCELLED_BY_GUEST', 'CANCELLED_BY_HOST',
                                   'COMPLETED', 'FORCE_CANCELLED',
                                   'FORCE_COMPLETED')),
    nightly_rate_snapshot  NUMERIC(19, 4) NOT NULL,
    total_amount           NUMERIC(19, 4) NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL,
    decided_at             TIMESTAMPTZ,
    completed_at           TIMESTAMPTZ,
    host_decision_message  TEXT
);

CREATE TABLE availability_blocks (
    block_id     UUID PRIMARY KEY,
    property_id  UUID NOT NULL REFERENCES properties(property_id),
    start_date   DATE NOT NULL,
    end_date     DATE NOT NULL,
    source       VARCHAR(15) NOT NULL
                     CHECK (source IN ('HOST_BLOCK', 'BOOKING')),
    booking_id   UUID REFERENCES bookings(booking_id),
    reason       TEXT
);

CREATE TABLE ticket_categories (
    category_id UUID PRIMARY KEY,
    label       VARCHAR(255) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE tickets (
    ticket_id         UUID PRIMARY KEY,
    booking_id        UUID NOT NULL REFERENCES bookings(booking_id),
    raised_by_user_id UUID NOT NULL REFERENCES users(user_id),
    raised_by_role    VARCHAR(10) NOT NULL
                          CHECK (raised_by_role IN ('GUEST', 'HOST')),
    category          VARCHAR(255) NOT NULL,
    title             VARCHAR(500) NOT NULL,
    description       TEXT NOT NULL,
    requested_remedy  VARCHAR(20) NOT NULL
                          CHECK (requested_remedy IN (
                              'FULL_REFUND', 'PARTIAL_REFUND',
                              'HOST_PAYOUT', 'OTHER')),
    supporting_text   TEXT,
    status            VARCHAR(20) NOT NULL
                          CHECK (status IN (
                              'OPEN', 'IN_REVIEW',
                              'RESOLVED_APPROVED', 'RESOLVED_REJECTED')),
    assigned_agent_id UUID REFERENCES users(user_id),
    agent_notes       TEXT,
    resolution_reason TEXT,
    created_at        TIMESTAMPTZ NOT NULL,
    resolved_at       TIMESTAMPTZ
);

CREATE TABLE reviews (
    review_id  UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings(booking_id),
    guest_id   UUID NOT NULL REFERENCES users(user_id),
    rating     INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE audit_log (
    log_id            UUID PRIMARY KEY,
    actor_user_id     UUID NOT NULL REFERENCES users(user_id),
    actor_name        TEXT,
    action_type       VARCHAR(50) NOT NULL,
    entity_type       VARCHAR(50) NOT NULL,
    entity_id         UUID NOT NULL,
    before_state      TEXT,
    after_state       TEXT,
    wallet_adjustment NUMERIC(19, 4),
    reason            TEXT,
    subject_user_id   UUID REFERENCES users(user_id),
    subject_name      TEXT,
    booking_id        UUID REFERENCES bookings(booking_id),
    ticket_id         UUID REFERENCES tickets(ticket_id),
    timestamp         TIMESTAMPTZ NOT NULL,
    balance_after     NUMERIC(19, 4)
);

CREATE INDEX idx_audit_timestamp ON audit_log(timestamp);
CREATE INDEX idx_audit_actor    ON audit_log(actor_user_id);
CREATE INDEX idx_audit_subject  ON audit_log(subject_user_id);
CREATE INDEX idx_audit_booking  ON audit_log(booking_id);
CREATE INDEX idx_audit_ticket   ON audit_log(ticket_id);
CREATE INDEX idx_audit_money    ON audit_log(subject_user_id, wallet_adjustment);

CREATE TABLE messages (
    message_id  UUID PRIMARY KEY,
    ticket_id   UUID NOT NULL REFERENCES tickets(ticket_id),
    channel     VARCHAR(5) NOT NULL CHECK (channel IN ('GUEST', 'HOST')),
    author_id   UUID NOT NULL REFERENCES users(user_id),
    author_role VARCHAR(10) NOT NULL
                    CHECK (author_role IN ('GUEST', 'HOST', 'AGENT')),
    body        TEXT NOT NULL,
    sent_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_messages_thread ON messages(ticket_id, channel);

CREATE TABLE message_reads (
    ticket_id           UUID NOT NULL REFERENCES tickets(ticket_id),
    channel             VARCHAR(5) NOT NULL CHECK (channel IN ('GUEST', 'HOST')),
    user_id             UUID NOT NULL REFERENCES users(user_id),
    last_read_message_id UUID NOT NULL REFERENCES messages(message_id),
    PRIMARY KEY (ticket_id, channel, user_id)
);

CREATE TABLE booking_messages (
    message_id  UUID PRIMARY KEY,
    booking_id  UUID NOT NULL REFERENCES bookings(booking_id),
    author_id   UUID NOT NULL REFERENCES users(user_id),
    author_role VARCHAR(10) NOT NULL
                    CHECK (author_role IN ('GUEST', 'HOST')),
    body        TEXT NOT NULL,
    sent_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_booking_messages_booking ON booking_messages(booking_id);

CREATE TABLE booking_message_reads (
    booking_id           UUID NOT NULL REFERENCES bookings(booking_id),
    user_id              UUID NOT NULL REFERENCES users(user_id),
    last_read_message_id UUID NOT NULL REFERENCES booking_messages(message_id),
    PRIMARY KEY (booking_id, user_id)
);
