package com.campusiq.service.impl;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.campusiq.dto.LoginRequest;
import com.campusiq.dto.LoginResponse;
import com.campusiq.dto.RegisterRequest;
import com.campusiq.dto.RegisterResponse;
import com.campusiq.entity.User;
import com.campusiq.enums.AccountStatus;
import com.campusiq.enums.Role;
import com.campusiq.exception.PasswordMismatchException;
import com.campusiq.exception.UsernameAlreadyExistsException;
import com.campusiq.repository.UserRepository;
import com.campusiq.security.JwtService;
import com.campusiq.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {

            throw new UsernameAlreadyExistsException(
                    "Username already exists: " + request.getUsername()
            );
        }

        if (!request.getPassword()
                .equals(request.getConfirmPassword())) {

            throw new PasswordMismatchException(
                    "Password and confirm password do not match"
            );
        }

        Role requestedRole =
                request.getRole() == null
                        ? Role.STUDENT
                        : request.getRole();

        if (requestedRole == Role.ADMIN) {

            throw new AccessDeniedException(
                    "Public administrator registration is not allowed"
            );
        }

        AccountStatus accountStatus;

        if (requestedRole == Role.STUDENT) {

            accountStatus = AccountStatus.ACTIVE;

        } else if (
                requestedRole == Role.FACULTY ||
                requestedRole == Role.TPO) {

            accountStatus = AccountStatus.PENDING;

        } else {

            throw new IllegalArgumentException(
                    "Unsupported registration role"
            );
        }

        String encodedPassword =
                passwordEncoder.encode(request.getPassword());

        User user = new User();

        user.setUsername(request.getUsername());
        user.setPassword(encodedPassword);
        user.setRole(requestedRole);
        user.setAccountStatus(accountStatus);

        User savedUser =
                userRepository.save(user);

        String message;

        if (savedUser.getAccountStatus() == AccountStatus.PENDING) {

            message =
                    "Registration submitted successfully. Your account is waiting for administrator approval.";

        } else {

            message =
                    "User registered successfully";
        }

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getRole(),
                message
        );
    }

    @Override
    public LoginResponse login(LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated(
                                request.getUsername(),
                                request.getPassword()
                        )
                );

        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        User user =
                userRepository
                        .findByUsername(request.getUsername())
                        .orElseThrow();

        if (user.getAccountStatus() == AccountStatus.PENDING) {

            throw new DisabledException(
                    "Your account is waiting for administrator approval."
            );
        }

        if (user.getAccountStatus() == AccountStatus.REJECTED) {

            throw new DisabledException(
                    "Your registration has been rejected by the administrator."
            );
        }

        String token =
                jwtService.generateToken(userDetails);

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                token,
                "Bearer",
                "Login successful"
        );
    }
}