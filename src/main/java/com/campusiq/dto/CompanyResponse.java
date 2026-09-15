package com.campusiq.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponse {

    private Long id;

    private String companyName;

    private String jobRole;

    private Double packageLpa;

    private String location;

    private LocalDate driveDate;

    private boolean active;

    private String message;
}
