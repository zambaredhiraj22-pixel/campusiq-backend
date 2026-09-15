package com.campusiq.dto;

import java.time.Instant;

import com.campusiq.enums.AiInterviewStatus;
import com.campusiq.enums.ProctoringViolationType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiInterviewViolationResponse {

    private Long violationId;

    private Long interviewSessionId;

    private ProctoringViolationType violationType;

    private Integer warningNumber;

    private String description;

    private Instant detectedAt;

    private boolean autoSubmitted;

    private AiInterviewStatus interviewStatus;

    private String message;
}