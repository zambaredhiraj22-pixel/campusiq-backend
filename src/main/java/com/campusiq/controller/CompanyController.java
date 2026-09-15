package com.campusiq.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusiq.dto.CompanyEligibilityCriteriaRequest;
import com.campusiq.dto.CompanyEligibilityCriteriaResponse;
import com.campusiq.dto.CompanyRequest;
import com.campusiq.dto.CompanyResponse;
import com.campusiq.service.CompanyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    public CompanyResponse createCompany(
            @RequestBody CompanyRequest request) {

        return companyService.createCompany(request);
    }

    @GetMapping("/{companyId}")
    public CompanyResponse getCompanyById(
            @PathVariable Long companyId) {

        return companyService.getCompanyById(companyId);
    }

    @GetMapping
    public List<CompanyResponse> getAllCompanies() {

        return companyService.getAllCompanies();
    }

    @GetMapping("/active")
    public List<CompanyResponse> getActiveCompanies() {

        return companyService.getActiveCompanies();
    }

    @PutMapping("/{companyId}/eligibility")
    public CompanyEligibilityCriteriaResponse saveEligibilityCriteria(
            @PathVariable Long companyId,
            @RequestBody CompanyEligibilityCriteriaRequest request) {

        return companyService.saveEligibilityCriteria(
                companyId,
                request
        );
    }

    @GetMapping("/{companyId}/eligibility")
    public CompanyEligibilityCriteriaResponse getEligibilityCriteria(
            @PathVariable Long companyId) {

        return companyService
                .getEligibilityCriteriaByCompanyId(companyId);
    }
}