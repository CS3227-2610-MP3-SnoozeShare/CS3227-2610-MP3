package com.snoozeshare.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.User;

import jakarta.persistence.LockModeType;

/** Spring Data JPA repository for platform users. */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findAllByOrderByCreatedAtAscIdAsc();

    List<User> findByRoleInOrderByCreatedAtAscIdAsc(List<Role> roles);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);
}
