package com.campusiq.dto;

import com.campusiq.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterResponse {

    private Long id;
    private String username;
    private Role role;
    private String message;
}
