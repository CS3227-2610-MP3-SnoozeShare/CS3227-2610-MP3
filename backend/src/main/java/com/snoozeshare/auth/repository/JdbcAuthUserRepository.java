package com.snoozeshare.auth.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.snoozeshare.auth.model.AuthRole;

/** JDBC implementation of the auth user persistence boundary. */
@Repository
@ConditionalOnBean(JdbcTemplate.class)
public class JdbcAuthUserRepository implements AuthUserRepository {

    private static final String SELECT_COLUMNS =
        "id, firebase_uid, email, role, display_name, status, created_at, updated_at";

    private final JdbcTemplate jdbc;

    public JdbcAuthUserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<AuthUser> findByFirebaseUid(String firebaseUid) {
        return jdbc.query("select " + SELECT_COLUMNS + " from users where firebase_uid = ?", this::mapUser, firebaseUid)
            .stream()
            .findFirst();
    }

    @Override
    public Optional<AuthUser> findById(UUID id) {
        return jdbc.query("select " + SELECT_COLUMNS + " from users where id = ?", this::mapUser, id)
            .stream()
            .findFirst();
    }

    @Override
    public AuthUser insert(AuthUser user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                "insert into users "
                    + "(id, firebase_uid, email, role, display_name, status) "
                    + "values (?, ?, ?, ?, ?, ?)");
            statement.setObject(1, user.id());
            statement.setString(2, user.firebaseUid());
            statement.setString(3, user.email());
            statement.setString(4, user.role().name());
            statement.setString(5, user.displayName());
            statement.setString(6, user.status());
            return statement;
        }, keyHolder);
        return findById(user.id()).orElseThrow();
    }

    private AuthUser mapUser(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AuthUser(
            resultSet.getObject("id", UUID.class),
            resultSet.getString("firebase_uid"),
            resultSet.getString("email"),
            AuthRole.valueOf(resultSet.getString("role")),
            resultSet.getString("display_name"),
            resultSet.getString("status"),
            resultSet.getObject("created_at", java.time.OffsetDateTime.class),
            resultSet.getObject("updated_at", java.time.OffsetDateTime.class));
    }
}
