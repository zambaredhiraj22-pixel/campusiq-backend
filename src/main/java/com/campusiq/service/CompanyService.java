package com.campusiq.service;

import java.util.List;

import com.campusiq.dto.CompanyEligibilityCriteriaRequest;
import com.campusiq.dto.CompanyEligibilityCriteriaResponse;
import com.campusiq.dto.CompanyRequest;
import com.campusiq.dto.CompanyResponse;

public interface CompanyService {

    CompanyResponse createCompany(CompanyRequest request);

    CompanyResponse getCompanyById(Long companyId);

    List<CompanyResponse> getAllCompanies();

    List<CompanyResponse> getActiveCompanies();

    CompanyEligibilityCriteriaResponse saveEligibilityCriteria(
            Long companyId,
            CompanyEligibilityCriteriaRequest request);

    CompanyEligibilityCriteriaResponse getEligibilityCriteriaByCompanyId(
            Long companyId);
}
