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

/** JDBC implementation of the wallet persistence boundary. */
@Repository
@ConditionalOnBean(JdbcTemplate.class)
public class JdbcWalletRepository implements WalletRepository {

    private final JdbcTemplate jdbc;

    public JdbcWalletRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Wallet> findByUserId(UUID userId) {
        return jdbc.query(
                "select id, user_id, currency, balance from wallets where user_id = ?",
                this::mapWallet,
                userId)
            .stream()
            .findFirst();
    }

    @Override
    public Wallet insert(Wallet wallet) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                "insert into wallets (id, user_id, currency, balance) values (?, ?, ?, ?)");
            statement.setObject(1, wallet.id());
            statement.setObject(2, wallet.userId());
            statement.setString(3, wallet.currency());
            statement.setBigDecimal(4, wallet.balance());
            return statement;
        }, keyHolder);
        return findByUserId(wallet.userId()).orElseThrow();
    }

    private Wallet mapWallet(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Wallet(
            resultSet.getObject("id", UUID.class),
            resultSet.getObject("user_id", UUID.class),
            resultSet.getString("currency"),
            resultSet.getBigDecimal("balance"));
    }
}
