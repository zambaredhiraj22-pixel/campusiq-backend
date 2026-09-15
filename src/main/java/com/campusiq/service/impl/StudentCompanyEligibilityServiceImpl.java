package com.campusiq.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusiq.dto.StudentCompanyEligibilityResponse;
import com.campusiq.dto.StudentEligibilityData;
import com.campusiq.entity.Company;
import com.campusiq.entity.CompanyEligibilityCriteria;
import com.campusiq.entity.StudentProfile;
import com.campusiq.exception.CompanyNotFoundException;
import com.campusiq.exception.EligibilityCriteriaNotFoundException;
import com.campusiq.exception.StudentProfileNotFoundException;
import com.campusiq.repository.CompanyEligibilityCriteriaRepository;
import com.campusiq.repository.CompanyRepository;
import com.campusiq.repository.StudentProfileRepository;
import com.campusiq.service.StudentCompanyEligibilityService;
import com.campusiq.service.StudentEligibilityDataProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentCompanyEligibilityServiceImpl
        implements StudentCompanyEligibilityService {

    private final CompanyRepository companyRepository;

    private final CompanyEligibilityCriteriaRepository criteriaRepository;

    private final StudentProfileRepository studentProfileRepository;

    private final StudentEligibilityDataProvider dataProvider;

    private final AcademicEligibilityEvaluator academicEvaluator;

    private final SkillEligibilityEvaluator skillEvaluator;

    private final MockTestEligibilityEvaluator mockTestEvaluator;

    @Override
    public StudentCompanyEligibilityResponse checkEligibility(
            Long studentId,
            Long companyId) {

        Company company = getCompany(companyId);

        CompanyEligibilityCriteria criteria =
                getCriteria(companyId);

        StudentEligibilityData studentData =
                dataProvider.getStudentEligibilityData(studentId);

        return evaluateStudent(
                studentData,
                company,
                criteria
        );
    }

    @Override
    public List<StudentCompanyEligibilityResponse>
            checkAllStudentsForCompany(Long companyId) {

        Company company = getCompany(companyId);

        CompanyEligibilityCriteria criteria =
                getCriteria(companyId);

        List<StudentEligibilityData> students =
                dataProvider.getAllStudentsEligibilityData();

        List<StudentCompanyEligibilityResponse> responses =
                new ArrayList<>();

        for (StudentEligibilityData studentData : students) {

            StudentCompanyEligibilityResponse response =
                    evaluateStudent(
                            studentData,
                            company,
                            criteria
                    );

            responses.add(response);
        }

        return responses;
    }

    @Override
    public StudentCompanyEligibilityResponse checkMyEligibility(
            String username,
            Long companyId) {

        StudentProfile studentProfile =
                studentProfileRepository
                        .findByUserUsername(username)
                        .orElseThrow(() ->
                                new StudentProfileNotFoundException(
                                        "Student profile not found for username: "
                                                + username
                                )
                        );

        return checkEligibility(
                studentProfile.getId(),
                companyId
        );
    }

    private StudentCompanyEligibilityResponse evaluateStudent(
            StudentEligibilityData studentData,
            Company company,
            CompanyEligibilityCriteria criteria) {

        List<String> passedCriteria =
                new ArrayList<>();

        List<String> failedCriteria =
                new ArrayList<>();

        academicEvaluator.evaluate(
                studentData.getTenthPercentage(),
                studentData.getTwelfthPercentage(),
                studentData.getDiplomaPercentage(),
                studentData.getCgpa(),
                studentData.getDepartment(),
                studentData.getAttendancePercentage(),
                criteria,
                passedCriteria,
                failedCriteria
        );

        skillEvaluator.evaluate(
                criteria.getRequiredSkills(),
                studentData.getVerifiedSkills(),
                passedCriteria,
                failedCriteria
        );

        mockTestEvaluator.evaluate(
                studentData.getLatestMockTestScore(),
                criteria.getMinimumMockTestScore(),
                passedCriteria,
                failedCriteria
        );

        boolean eligible =
                failedCriteria.isEmpty();

        String message;

        if (eligible) {

            message =
                    "Student is eligible for this company";

        } else {

            message =
                    "Student is not eligible for this company";
        }

        return new StudentCompanyEligibilityResponse(
                studentData.getStudentId(),
                company.getId(),
                company.getCompanyName(),
                eligible,
                passedCriteria,
                failedCriteria,
                message
        );
    }

    private Company getCompany(Long companyId) {

        return companyRepository
                .findById(companyId)
                .orElseThrow(() ->
                        new CompanyNotFoundException(
                                "Company not found with id: "
                                        + companyId
                        )
                );
    }

    private CompanyEligibilityCriteria getCriteria(
            Long companyId) {

        return criteriaRepository
                .findByCompanyId(companyId)
                .orElseThrow(() ->
                        new EligibilityCriteriaNotFoundException(
                                "Eligibility criteria not found for company id: "
                                        + companyId
                        )
                );
    }
}