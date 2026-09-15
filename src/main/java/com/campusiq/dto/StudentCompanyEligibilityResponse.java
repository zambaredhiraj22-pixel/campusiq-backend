package com.campusiq.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentCompanyEligibilityResponse {

    private Long studentId;

    private Long companyId;

    private String companyName;

    private boolean eligible;

    private List<String> passedCriteria = new ArrayList<>();

    private List<String> failedCriteria = new ArrayList<>();

    private String message;
}