package com.campusiq.dto;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyEligibilityCriteriaResponse {

    private Long id;

    private Long companyId;

    private Double minimum10thPercentage;

    private Double minimum12thPercentage;

    private Double minimumDiplomaPercentage;

    private Double minimumCgpa;

    private Double minimumAttendancePercentage;

    private Double minimumMockTestScore;

    private Set<String> allowedDepartments;

    private Set<String> requiredSkills;

    private String message;
}