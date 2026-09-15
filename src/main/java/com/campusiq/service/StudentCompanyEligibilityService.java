package com.campusiq.service;

import java.util.List;

import com.campusiq.dto.StudentCompanyEligibilityResponse;

public interface StudentCompanyEligibilityService {

    StudentCompanyEligibilityResponse checkEligibility(
            Long studentId,
            Long companyId);

    List<StudentCompanyEligibilityResponse> checkAllStudentsForCompany(
            Long companyId);

    StudentCompanyEligibilityResponse checkMyEligibility(
            String username,
            Long companyId);
}