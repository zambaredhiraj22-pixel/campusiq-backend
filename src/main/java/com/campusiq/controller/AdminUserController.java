package com.campusiq.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusiq.dto.AdminUserResponse;
import com.campusiq.service.AdminUserService;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(
            AdminUserService adminUserService) {

        this.adminUserService = adminUserService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<AdminUserResponse>>
            getPendingUsers() {

        return ResponseEntity.ok(
                adminUserService.getPendingUsers()
        );
    }

    @GetMapping("/staff")
    public ResponseEntity<List<AdminUserResponse>>
            getStaffUsers() {

        return ResponseEntity.ok(
                adminUserService.getStaffUsers()
        );
    }

    @PatchMapping("/{userId}/approve")
    public ResponseEntity<AdminUserResponse>
            approveUser(
                    @PathVariable Long userId) {

        return ResponseEntity.ok(
                adminUserService.approveUser(
                        userId
                )
        );
    }

    @PatchMapping("/{userId}/reject")
    public ResponseEntity<AdminUserResponse>
            rejectUser(
                    @PathVariable Long userId) {

        return ResponseEntity.ok(
                adminUserService.rejectUser(
                        userId
                )
        );
    }
}