package com.campusiq.dto;

import com.campusiq.enums.ProctoringViolationType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProctoringViolationRequest {

    private Long testAttemptId;

    private ProctoringViolationType violationType;

    private String description;
}
