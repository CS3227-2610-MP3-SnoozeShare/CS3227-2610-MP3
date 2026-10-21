package com.snoozeshare.service;

import java.util.List;
import java.util.UUID;

import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.User;

/**
 * Domain user queries. Authentication and registration are handled
 * by the auth layer (W-AUTH); this service serves the domain.
 */
public interface UserService {

    /**
     * @throws IllegalArgumentException if the user does not exist
     */
    User findById(UUID userId);

    /**
     * @return every user with the role
     */
    List<User> listByRole(Role role);
}
