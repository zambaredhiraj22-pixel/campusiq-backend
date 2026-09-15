package com.campusiq.dto;

import com.campusiq.enums.AccountStatus;
import com.campusiq.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {

    private Long id;

    private String username;

    private Role role;

    private AccountStatus accountStatus;
}