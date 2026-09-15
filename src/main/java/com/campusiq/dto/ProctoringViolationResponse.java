package com.campusiq.dto;

import java.time.LocalDateTime;

import com.campusiq.enums.ProctoringViolationType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProctoringViolationResponse {

    private Long violationId;

    private Long testAttemptId;

    private ProctoringViolationType violationType;

    private LocalDateTime detectedAt;

    private int warningNumber;

    private boolean autoSubmitted;

    private String message;
}