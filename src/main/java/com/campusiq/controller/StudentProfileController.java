package com.campusiq.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusiq.dto.StudentCompanyEligibilityResponse;
import com.campusiq.dto.StudentProfileRequest;
import com.campusiq.dto.StudentProfileResponse;
import com.campusiq.service.StudentCompanyEligibilityService;
import com.campusiq.service.StudentProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    private final StudentCompanyEligibilityService eligibilityService;

    @PostMapping("/profile")
    public ResponseEntity<StudentProfileResponse> createProfile(
            @Valid @RequestBody StudentProfileRequest request,
            Authentication authentication) {

        StudentProfileResponse response =
                studentProfileService.createProfile(
                        authentication.getName(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<StudentProfileResponse> getMyProfile(
            Authentication authentication) {

        StudentProfileResponse response =
                studentProfileService.getMyProfile(
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<StudentProfileResponse> updateMyProfile(
            @Valid @RequestBody StudentProfileRequest request,
            Authentication authentication) {

        StudentProfileResponse response =
                studentProfileService.updateMyProfile(
                        authentication.getName(),
                        request
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/eligibility/companies/{companyId}")
    public ResponseEntity<StudentCompanyEligibilityResponse>
            checkMyCompanyEligibility(
                    @PathVariable Long companyId,
                    Authentication authentication) {

        StudentCompanyEligibilityResponse response =
                eligibilityService.checkMyEligibility(
                        authentication.getName(),
                        companyId
                );

        return ResponseEntity.ok(response);
    }
}