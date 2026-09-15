package com.campusiq.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.campusiq.entity.CompanyEligibilityCriteria;

@Repository
public interface CompanyEligibilityCriteriaRepository
        extends JpaRepository<CompanyEligibilityCriteria, Long> {

    Optional<CompanyEligibilityCriteria> findByCompanyId(Long companyId);
}