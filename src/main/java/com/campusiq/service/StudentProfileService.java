package com.campusiq.service;

import com.campusiq.dto.StudentProfileRequest;
import com.campusiq.dto.StudentProfileResponse;

public interface StudentProfileService {

    StudentProfileResponse createProfile(
            String username,
            StudentProfileRequest request
    );

    StudentProfileResponse getMyProfile(String username);

    StudentProfileResponse updateMyProfile(
            String username,
            StudentProfileRequest request
    );
}