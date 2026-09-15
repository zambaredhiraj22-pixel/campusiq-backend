package com.campusiq.dto;

import com.campusiq.enums.AiInterviewStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiInterviewEligibilityResponse {

    private Long studentProfileId;

    private boolean eligible;

    private String message;

    private boolean passedPersonalizedMockTest;

    private boolean interviewProfileAvailable;

    private Long qualifyingMockTestResultId;

    private Long qualifyingMockTestId;

    private String qualifyingMockTestTitle;

    private Double mockTestPercentage;

    private Long interviewSessionId;

    private AiInterviewStatus interviewStatus;
}