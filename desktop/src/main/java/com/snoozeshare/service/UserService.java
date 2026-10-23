package com.snoozeshare.service;

import java.util.List;
import java.util.UUID;

import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.User;

/**
 * Sign-in and registration. Sign-in uses only an email address; this is a demo, not real
 * authentication.
 */
public interface UserService {
    /**
     * @param email the account email
     * @return the matching active user
     * @throws IllegalArgumentException if the email is blank, unknown, or belongs to the System account
     * @throws IllegalStateException if the account is not active
     */
    User authenticate(String email);

    /**
     * Creates an account, and a wallet for roles that need one, in one transaction.
     *
     * @param registrationCode required for Host and Agent accounts, ignored for Guests
     * @throws IllegalArgumentException if a field is blank, the role is null or System, the email is
     *         taken, or the registration code is wrong
     */
    User register(String displayName, String email, Role role, String registrationCode);

    /**
     * @throws IllegalArgumentException if the user does not exist
     */
    User findById(UUID userId);

    /**
     * @return every user with the role
     */
    List<User> listByRole(Role role);
}
