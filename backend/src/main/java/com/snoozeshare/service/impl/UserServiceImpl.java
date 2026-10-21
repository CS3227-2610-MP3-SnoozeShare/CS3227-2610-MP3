package com.snoozeshare.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.service.UserService;

@Service
@ConditionalOnProperty(name = "snoozeshare.persistence.enabled", havingValue = "true", matchIfMissing = false)
public class UserServiceImpl implements UserService {

    private final UserRepository users;

    public UserServiceImpl(UserRepository users) {
        this.users = users;
    }

    @Override
    public User findById(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
    }

    @Override
    public List<User> listByRole(Role role) {
        return users.findByRole(role);
    }
}
