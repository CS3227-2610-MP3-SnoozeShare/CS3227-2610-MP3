CREATE TABLE ticket_assistants (
    ticket_id UUID PRIMARY KEY REFERENCES tickets(id),
    state VARCHAR(16) NOT NULL CHECK (state IN
        ('PENDING', 'WAITING_GUEST', 'DONE', 'STOPPED', 'UNAVAILABLE')),
    rounds SMALLINT NOT NULL DEFAULT 0 CHECK (rounds BETWEEN 0 AND 5),
    flagged BOOLEAN NOT NULL DEFAULT FALSE,
    draft TEXT CHECK (char_length(draft) <= 6000),
    draft_updated_at TIMESTAMPTZ,
    last_message_id UUID REFERENCES messages(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

-- PostgreSQL names the inline author_role CHECK in V003; migration tests pin this name.
ALTER TABLE messages DROP CONSTRAINT IF EXISTS messages_author_role_check;
ALTER TABLE messages ADD CONSTRAINT messages_author_role_check
    CHECK (author_role IN ('GUEST', 'HOST', 'AGENT', 'SYSTEM'));
