package com.campusiq.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TpoPlacementReadyStudentResponse {

    private Long studentProfileId;

    private String fullName;

    private String email;

    private String phone;

    private String department;

    private String yearOfStudy;

    private Double cgpa;

    private Double attendancePercentage;

    private Boolean placementReady;
}