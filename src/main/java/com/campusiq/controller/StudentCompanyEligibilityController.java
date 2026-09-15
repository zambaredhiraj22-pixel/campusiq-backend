package com.campusiq.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusiq.dto.StudentCompanyEligibilityResponse;
import com.campusiq.service.StudentCompanyEligibilityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/eligibility")
@RequiredArgsConstructor
public class StudentCompanyEligibilityController {

    private final StudentCompanyEligibilityService eligibilityService;

    @GetMapping("/students/{studentId}/companies/{companyId}")
    public StudentCompanyEligibilityResponse checkEligibility(
            @PathVariable Long studentId,
            @PathVariable Long companyId) {

        return eligibilityService.checkEligibility(
                studentId,
                companyId
        );
    }

    @GetMapping("/companies/{companyId}/students")
    public List<StudentCompanyEligibilityResponse>
            checkAllStudentsForCompany(
                    @PathVariable Long companyId) {

        return eligibilityService
                .checkAllStudentsForCompany(companyId);
    }
}