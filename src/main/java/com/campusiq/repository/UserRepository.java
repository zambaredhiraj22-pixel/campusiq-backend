package com.campusiq.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusiq.entity.User;
import com.campusiq.enums.AccountStatus;
import com.campusiq.enums.Role;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByAccountStatusAndRoleInOrderByIdAsc(
            AccountStatus accountStatus,
            Collection<Role> roles
    );

    List<User> findByRoleInOrderByIdAsc(
            Collection<Role> roles
    );
}