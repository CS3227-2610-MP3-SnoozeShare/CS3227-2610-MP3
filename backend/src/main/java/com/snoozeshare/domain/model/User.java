package com.snoozeshare.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A platform user (guest, host, agent or the system account). */
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus;

    @Column(name = "registration_code")
    private String registrationCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "suspension_reason")
    private String suspensionReason;

    /** JPA requires a no-arg constructor. */
    protected User() {}

    /** Creates a new user. */
    public User(UUID userId, Role role, String displayName,
                String email, AccountStatus accountStatus,
                String registrationCode, Instant createdAt,
                String suspensionReason) {
        this.userId = userId;
        this.role = role;
        this.displayName = displayName;
        this.email = email;
        this.accountStatus = accountStatus;
        this.registrationCode = registrationCode;
        this.createdAt = createdAt;
        this.suspensionReason = suspensionReason;
    }

    public UUID getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getRegistrationCode() {
        return registrationCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getSuspensionReason() {
        return suspensionReason;
    }

    public void setSuspensionReason(String suspensionReason) {
        this.suspensionReason = suspensionReason;
    }
}
