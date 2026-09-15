package com.campusiq.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FingerprintEnrollmentResponse {

    private Long enrollmentId;

    private Long studentProfileId;

    private String studentName;

    private String deviceId;

    private Integer fingerprintTemplateId;

    private String message;
}