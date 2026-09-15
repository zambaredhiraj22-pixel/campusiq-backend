package com.campusiq.service;

import com.campusiq.dto.LoginRequest;
import com.campusiq.dto.LoginResponse;
import com.campusiq.dto.RegisterRequest;
import com.campusiq.dto.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);
    
    LoginResponse login(LoginRequest request);
}