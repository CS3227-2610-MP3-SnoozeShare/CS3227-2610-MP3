package com.snoozeshare.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.User;

/** Spring Data JPA repository for platform users. */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findAllByOrderByCreatedAtAscIdAsc();
}
