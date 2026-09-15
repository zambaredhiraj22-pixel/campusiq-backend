package com.campusiq.dto;

import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentEligibilityData {

    private Long studentId;

    private Double tenthPercentage;

    private Double twelfthPercentage;

    private Double diplomaPercentage;

    private Double cgpa;

    private String department;

    private Double attendancePercentage;

    private Set<String> verifiedSkills = new HashSet<>();

    private Double latestMockTestScore;
}