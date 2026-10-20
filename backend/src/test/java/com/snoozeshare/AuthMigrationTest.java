package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Verifies the minimal auth-owned schema contract. */
class AuthMigrationTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createsUsersAndWalletsWithAuthConstraints() {
        assertThat(tableNames()).containsExactlyInAnyOrder("users", "wallets");
        assertThat(columnNames("users")).contains("firebase_uid", "email", "role", "status");
        assertThat(columnNames("wallets")).contains("user_id", "currency", "balance");
        assertThat(constraintNames("users")).contains("users_firebase_uid_key");
        assertThat(constraintNames("wallets")).contains("wallets_user_id_key");
    }

    @Test
    void createsWalletForeignKeyToUsers() {
        Integer foreignKeys = jdbc.queryForObject(
            "select count(*) from information_schema.table_constraints "
                + "where table_schema = 'public' and table_name = 'wallets' "
                + "and constraint_type = 'FOREIGN KEY'",
            Integer.class
        );

        assertThat(foreignKeys).isEqualTo(1);
    }

    @Test
    void appliesRegistrationDefaultsAndUniquenessConstraints() {
        UUID userId = UUID.randomUUID();
        jdbc.update(
            "insert into users (id, firebase_uid, email, role) values (?, ?, ?, ?)",
            userId,
            "uid-" + userId,
            "user@example.com",
            "GUEST");
        jdbc.update("insert into wallets (id, user_id) values (?, ?)", UUID.randomUUID(), userId);

        var user = jdbc.queryForMap("select status, created_at, updated_at from users where id = ?", userId);
        var wallet = jdbc.queryForMap("select currency, balance from wallets where user_id = ?", userId);
        assertThat(user.get("status")).isEqualTo("ACTIVE");
        assertThat(user.get("created_at")).isNotNull();
        assertThat(user.get("updated_at")).isNotNull();
        assertThat(wallet.get("currency")).isEqualTo("SGD");
        assertThat(wallet.get("balance")).isEqualTo(new BigDecimal("0.00"));

        assertThatThrownBy(() -> jdbc.update(
            "insert into users (id, firebase_uid, email, role) values (?, ?, ?, ?)",
            UUID.randomUUID(),
            "uid-" + userId,
            "other@example.com",
            "GUEST"))
            .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> jdbc.update(
            "insert into wallets (id, user_id) values (?, ?)", UUID.randomUUID(), userId))
            .isInstanceOf(RuntimeException.class);
    }

    private Set<String> tableNames() {
        return Set.copyOf(jdbc.queryForList(
            "select table_name from information_schema.tables "
                + "where table_schema = 'public' and table_name in ('users', 'wallets')",
            String.class
        ));
    }

    private Set<String> columnNames(String table) {
        return Set.copyOf(jdbc.queryForList(
            "select column_name from information_schema.columns "
                + "where table_schema = 'public' and table_name = ?",
            String.class,
            table
        ));
    }

    private Set<String> constraintNames(String table) {
        return Set.copyOf(jdbc.queryForList(
            "select constraint_name from information_schema.table_constraints "
                + "where table_schema = 'public' and table_name = ?",
            String.class,
            table
        ));
    }
}
