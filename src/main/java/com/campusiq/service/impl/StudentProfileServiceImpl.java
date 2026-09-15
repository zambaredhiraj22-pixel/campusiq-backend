package com.campusiq.service.impl;

import org.springframework.beans.BeanUtils;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusiq.dto.StudentProfileRequest;
import com.campusiq.dto.StudentProfileResponse;
import com.campusiq.entity.StudentProfile;
import com.campusiq.entity.User;
import com.campusiq.exception.StudentProfileAlreadyExistsException;
import com.campusiq.exception.StudentProfileNotFoundException;
import com.campusiq.repository.StudentProfileRepository;
import com.campusiq.repository.UserRepository;
import com.campusiq.service.StudentProfileService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentProfileServiceImpl implements StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;

    @Override
    public StudentProfileResponse createProfile(String username,StudentProfileRequest request) {

        User user = findUser(username);

        if (studentProfileRepository.existsByUser(user)) {
            throw new StudentProfileAlreadyExistsException(
                    "Student profile already exists"
            );
        }

        StudentProfile profile = new StudentProfile();

        BeanUtils.copyProperties(request, profile);

        profile.setPlacementReady(false);
        profile.setUser(user);

        StudentProfile savedProfile =
                studentProfileRepository.save(profile);

        return convertToResponse(savedProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentProfileResponse getMyProfile(String username) {

        User user = findUser(username);

        StudentProfile profile = studentProfileRepository
                .findByUser(user)
                .orElseThrow(() ->
                        new StudentProfileNotFoundException(
                                "Student profile not found"
                        )
                );

        return convertToResponse(profile);
    }

    @Override
    public StudentProfileResponse updateMyProfile(
            String username,
            StudentProfileRequest request) {

        User user = findUser(username);

        StudentProfile profile = studentProfileRepository
                .findByUser(user)
                .orElseThrow(() ->
                        new StudentProfileNotFoundException(
                                "Student profile not found"
                        )
                );

        BeanUtils.copyProperties(request, profile);

        StudentProfile updatedProfile =
                studentProfileRepository.save(profile);

        return convertToResponse(updatedProfile);
    }

    private User findUser(String username) {

        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + username
                        )
                );
    }

    private StudentProfileResponse convertToResponse(
            StudentProfile profile) {

        StudentProfileResponse response =
                new StudentProfileResponse();

        BeanUtils.copyProperties(profile, response);

        response.setUserId(profile.getUser().getId());
        response.setUsername(profile.getUser().getUsername());

        return response;
    }
}
