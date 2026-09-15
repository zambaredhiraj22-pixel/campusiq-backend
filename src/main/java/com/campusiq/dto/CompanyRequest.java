package com.campusiq.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CompanyRequest {

    private String companyName;

    private String jobRole;

    private Double packageLpa;

    private String location;

    private LocalDate driveDate;

    private boolean active;
}
