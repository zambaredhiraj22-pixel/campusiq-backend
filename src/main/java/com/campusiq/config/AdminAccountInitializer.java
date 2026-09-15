package com.campusiq.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.campusiq.entity.User;
import com.campusiq.enums.AccountStatus;
import com.campusiq.enums.Role;
import com.campusiq.repository.UserRepository;

@Component
public class AdminAccountInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${campusiq.admin.username:}")
    private String adminUsername;

    @Value("${campusiq.admin.password:}")
    private String adminPassword;

    public AdminAccountInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        if (adminUsername == null
                || adminUsername.isBlank()
                || adminPassword == null
                || adminPassword.isBlank()) {

            return;
        }

        if (userRepository.existsByUsername(adminUsername)) {

            return;
        }

        User admin = new User();

        admin.setUsername(adminUsername.trim());
        admin.setPassword(
                passwordEncoder.encode(adminPassword)
        );
        admin.setRole(Role.ADMIN);
        admin.setAccountStatus(AccountStatus.ACTIVE);

        userRepository.save(admin);
    }
}