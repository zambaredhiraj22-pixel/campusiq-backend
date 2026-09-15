package com.campusiq.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusiq.dto.CompanyEligibilityCriteriaRequest;
import com.campusiq.dto.CompanyEligibilityCriteriaResponse;
import com.campusiq.dto.CompanyRequest;
import com.campusiq.dto.CompanyResponse;
import com.campusiq.entity.Company;
import com.campusiq.entity.CompanyEligibilityCriteria;
import com.campusiq.exception.CompanyNotFoundException;
import com.campusiq.exception.EligibilityCriteriaNotFoundException;
import com.campusiq.repository.CompanyEligibilityCriteriaRepository;
import com.campusiq.repository.CompanyRepository;
import com.campusiq.service.CompanyService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;

    private final CompanyEligibilityCriteriaRepository criteriaRepository;

    @Override
    public CompanyResponse createCompany(CompanyRequest request) {

        Company company = new Company();

        company.setCompanyName(request.getCompanyName());
        company.setJobRole(request.getJobRole());
        company.setPackageLpa(request.getPackageLpa());
        company.setLocation(request.getLocation());
        company.setDriveDate(request.getDriveDate());
        company.setActive(request.isActive());

        Company savedCompany = companyRepository.save(company);

        return mapToCompanyResponse(
                savedCompany,
                "Company created successfully"
        );
    }

    @Override
    public CompanyResponse getCompanyById(Long companyId) {

        Company company = findCompanyById(companyId);

        return mapToCompanyResponse(
                company,
                "Company fetched successfully"
        );
    }

    @Override
    public List<CompanyResponse> getAllCompanies() {

        List<Company> companies = companyRepository.findAll();

        List<CompanyResponse> responses = new ArrayList<>();

        for (Company company : companies) {

            responses.add(
                    mapToCompanyResponse(
                            company,
                            "Company fetched successfully"
                    )
            );
        }

        return responses;
    }

    @Override
    public List<CompanyResponse> getActiveCompanies() {

        List<Company> companies =
                companyRepository.findByActiveTrue();

        List<CompanyResponse> responses = new ArrayList<>();

        for (Company company : companies) {

            responses.add(
                    mapToCompanyResponse(
                            company,
                            "Active company fetched successfully"
                    )
            );
        }

        return responses;
    }

    @Override
    public CompanyEligibilityCriteriaResponse saveEligibilityCriteria(
            Long companyId,
            CompanyEligibilityCriteriaRequest request) {

        Company company = findCompanyById(companyId);

        Optional<CompanyEligibilityCriteria> existingCriteria =
                criteriaRepository.findByCompanyId(companyId);

        boolean updating = existingCriteria.isPresent();

        CompanyEligibilityCriteria criteria;

        if (updating) {

            criteria = existingCriteria.get();

        } else {

            criteria = new CompanyEligibilityCriteria();
        }

        criteria.setCompany(company);

        criteria.setMinimum10thPercentage(
                request.getMinimum10thPercentage());

        criteria.setMinimum12thPercentage(
                request.getMinimum12thPercentage());

        criteria.setMinimumDiplomaPercentage(
                request.getMinimumDiplomaPercentage());

        criteria.setMinimumCgpa(
                request.getMinimumCgpa());

        criteria.setMinimumAttendancePercentage(
                request.getMinimumAttendancePercentage());

        criteria.setMinimumMockTestScore(
                request.getMinimumMockTestScore());

        if (request.getAllowedDepartments() != null) {

            criteria.setAllowedDepartments(
                    new HashSet<>(request.getAllowedDepartments()));

        } else {

            criteria.setAllowedDepartments(new HashSet<>());
        }

        if (request.getRequiredSkills() != null) {

            criteria.setRequiredSkills(
                    new HashSet<>(request.getRequiredSkills()));

        } else {

            criteria.setRequiredSkills(new HashSet<>());
        }

        CompanyEligibilityCriteria savedCriteria =
                criteriaRepository.save(criteria);

        String message;

        if (updating) {

            message = "Eligibility criteria updated successfully";

        } else {

            message = "Eligibility criteria saved successfully";
        }

        return mapToCriteriaResponse(
                savedCriteria,
                message
        );
    }

    @Override
    public CompanyEligibilityCriteriaResponse
            getEligibilityCriteriaByCompanyId(Long companyId) {

        findCompanyById(companyId);

        CompanyEligibilityCriteria criteria =
                criteriaRepository
                        .findByCompanyId(companyId)
                        .orElseThrow(() ->
                                new EligibilityCriteriaNotFoundException(
                                        "Eligibility criteria not found for company id: "
                                                + companyId
                                )
                        );

        return mapToCriteriaResponse(
                criteria,
                "Eligibility criteria fetched successfully"
        );
    }

    private Company findCompanyById(Long companyId) {

        return companyRepository
                .findById(companyId)
                .orElseThrow(() ->
                        new CompanyNotFoundException(
                                "Company not found with id: "
                                        + companyId
                        )
                );
    }

    private CompanyResponse mapToCompanyResponse(
            Company company,
            String message) {

        return new CompanyResponse(
                company.getId(),
                company.getCompanyName(),
                company.getJobRole(),
                company.getPackageLpa(),
                company.getLocation(),
                company.getDriveDate(),
                company.isActive(),
                message
        );
    }

    private CompanyEligibilityCriteriaResponse mapToCriteriaResponse(
            CompanyEligibilityCriteria criteria,
            String message) {

        return new CompanyEligibilityCriteriaResponse(
                criteria.getId(),
                criteria.getCompany().getId(),
                criteria.getMinimum10thPercentage(),
                criteria.getMinimum12thPercentage(),
                criteria.getMinimumDiplomaPercentage(),
                criteria.getMinimumCgpa(),
                criteria.getMinimumAttendancePercentage(),
                criteria.getMinimumMockTestScore(),
                new HashSet<>(criteria.getAllowedDepartments()),
                new HashSet<>(criteria.getRequiredSkills()),
                message
        );
    }
}