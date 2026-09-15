package com.campusiq.service;

import java.util.List;

import com.campusiq.dto.AdminUserResponse;

public interface AdminUserService {

    List<AdminUserResponse> getPendingUsers();

    List<AdminUserResponse> getStaffUsers();

    AdminUserResponse approveUser(Long userId);

    AdminUserResponse rejectUser(Long userId);
}