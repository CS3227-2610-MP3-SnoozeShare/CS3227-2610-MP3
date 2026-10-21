-- The SYSTEM user owns the platform-fee wallet and acts as the actor for automated operations.
INSERT INTO users (id, firebase_uid, role, display_name, email, status, created_at, updated_at)
VALUES ('a0000000-0000-0000-0000-0000000000ff', 'SYSTEM_INTERNAL',
        'SYSTEM', 'SnoozeShare System', 'system@snoozeshare.invalid',
        'ACTIVE', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z');

INSERT INTO wallets (id, user_id, balance, currency, updated_at)
VALUES ('30000000-0000-0000-0000-0000000000ff',
        'a0000000-0000-0000-0000-0000000000ff',
        0, 'SGD', '2026-01-01T00:00:00Z');
