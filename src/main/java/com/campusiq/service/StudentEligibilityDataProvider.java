package com.campusiq.service;

import java.util.List;

import com.campusiq.dto.StudentEligibilityData;

public interface StudentEligibilityDataProvider {

    StudentEligibilityData getStudentEligibilityData(
            Long studentId);

    List<StudentEligibilityData> getAllStudentsEligibilityData();
}