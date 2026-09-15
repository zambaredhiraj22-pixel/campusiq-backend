package com.campusiq.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusiq.dto.AdminUserResponse;
import com.campusiq.entity.User;
import com.campusiq.enums.AccountStatus;
import com.campusiq.enums.Role;
import com.campusiq.repository.UserRepository;
import com.campusiq.service.AdminUserService;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;

    public AdminUserServiceImpl(
            UserRepository userRepository) {

        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getPendingUsers() {

        return userRepository
                .findByAccountStatusAndRoleInOrderByIdAsc(
                        AccountStatus.PENDING,
                        List.of(
                                Role.FACULTY,
                                Role.TPO
                        )
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getStaffUsers() {

        return userRepository
                .findByRoleInOrderByIdAsc(
                        List.of(
                                Role.FACULTY,
                                Role.TPO
                        )
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AdminUserResponse approveUser(
            Long userId) {

        User user =
                getAuthorizedStaffUser(
                        userId
                );

        if (user.getAccountStatus()
                != AccountStatus.PENDING) {

            throw new IllegalStateException(
                    "Only pending accounts can be approved"
            );
        }

        user.setAccountStatus(
                AccountStatus.ACTIVE
        );

        User savedUser =
                userRepository.save(user);

        return toResponse(
                savedUser
        );
    }

    @Override
    @Transactional
    public AdminUserResponse rejectUser(
            Long userId) {

        User user =
                getAuthorizedStaffUser(
                        userId
                );

        if (user.getAccountStatus()
                != AccountStatus.PENDING) {

            throw new IllegalStateException(
                    "Only pending accounts can be rejected"
            );
        }

        user.setAccountStatus(
                AccountStatus.REJECTED
        );

        User savedUser =
                userRepository.save(user);

        return toResponse(
                savedUser
        );
    }

    private User getAuthorizedStaffUser(
            Long userId) {

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "User not found with id: "
                                                        + userId
                                        )
                        );

        if (user.getRole()
                != Role.FACULTY
                &&
                user.getRole()
                != Role.TPO) {

            throw new IllegalArgumentException(
                    "Only Faculty or TPO accounts can be managed by this operation"
            );
        }

        return user;
    }

    private AdminUserResponse toResponse(
            User user) {

        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getAccountStatus()
        );
    }
}