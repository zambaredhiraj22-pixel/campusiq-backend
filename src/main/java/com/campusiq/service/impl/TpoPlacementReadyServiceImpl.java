package com.campusiq.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.campusiq.dto.TpoPlacementReadyStudentResponse;
import com.campusiq.entity.StudentProfile;
import com.campusiq.repository.StudentProfileRepository;
import com.campusiq.service.TpoPlacementReadyService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TpoPlacementReadyServiceImpl
        implements TpoPlacementReadyService {

    private final StudentProfileRepository studentProfileRepository;

    @Override
    public List<TpoPlacementReadyStudentResponse>
            getPlacementReadyStudents() {

        return studentProfileRepository
                .findByPlacementReadyTrueOrderByFullNameAsc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private TpoPlacementReadyStudentResponse mapToResponse(
            StudentProfile student) {

        return new TpoPlacementReadyStudentResponse(
                student.getId(),
                student.getFullName(),
                student.getEmail(),
                student.getPhone(),
                student.getDepartment(),
                student.getYearOfStudy(),
                student.getCgpa(),
                student.getAttendancePercentage(),
                student.getPlacementReady()
        );
    }
}