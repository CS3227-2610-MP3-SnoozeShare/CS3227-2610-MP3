package com.snoozeshare.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.snoozeshare.domain.model.Wallet;

import jakarta.persistence.LockModeType;

/** Spring Data JPA repository for user wallets. */
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByUserId(UUID userId);

    @Query("SELECT w.id FROM Wallet w WHERE w.userId = :userId")
    Optional<UUID> findIdByUserId(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdForUpdate(UUID id);
}
