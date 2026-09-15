package com.campusiq.dto;

import com.campusiq.enums.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(
        min = 4,
        max = 50,
        message = "Username must contain 4 to 50 characters"
    )
    private String username;

    @NotBlank(message = "Password is required")
    @Size(
        min = 8,
        max = 100,
        message = "Password must contain 8 to 100 characters"
    )
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    private Role role;
}