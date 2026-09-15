package com.campusiq.dto;

import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CompanyEligibilityCriteriaRequest {

    private Double minimum10thPercentage;

    private Double minimum12thPercentage;

    private Double minimumDiplomaPercentage;

    private Double minimumCgpa;

    private Double minimumAttendancePercentage;

    private Double minimumMockTestScore;

    private Set<String> allowedDepartments = new HashSet<>();

    private Set<String> requiredSkills = new HashSet<>();
}