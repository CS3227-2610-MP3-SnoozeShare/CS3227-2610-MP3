package com.snoozeshare.ai.testsupport;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.repository.UserRepository;

/**
 * Saves and commits real {@link User} rows for tests that write audit rows (the audit table has foreign keys to
 * {@code users}), and deletes them and their audit rows afterwards.
 */
public class AiTestUsers {

    private final UserRepository users;
    private final JdbcTemplate jdbc;
    private final List<UUID> created = new ArrayList<>();

    /** Creates the helper over a user repository and a JDBC template on the same database. */
    public AiTestUsers(UserRepository users, JdbcTemplate jdbc) {
        this.users = users;
        this.jdbc = jdbc;
    }

    /** Saves and commits a new active user of the given role. */
    public User create(Role role) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        String tag = id.toString();
        User saved = users.save(new User(id, "fb-ai-" + tag, role, "AI Test " + tag.substring(0, 8),
                "ai-" + tag + "@test.com", AccountStatus.ACTIVE, null, now, now, null));
        created.add(id);
        return saved;
    }

    /** Deletes the audit rows the users acted on or were the subject of, then the users. */
    public void cleanup() {
        for (UUID id : created) {
            jdbc.update("DELETE FROM audit_log WHERE actor_user_id = ? OR subject_user_id = ?", id, id);
        }
        for (UUID id : created) {
            jdbc.update("DELETE FROM users WHERE id = ?", id);
        }
        created.clear();
    }
}
