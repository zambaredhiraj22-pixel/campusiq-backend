package com.campusiq.dto;

import com.campusiq.enums.ProctoringViolationType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AiInterviewViolationRequest {

    @NotNull(message = "Violation type is required")
    private ProctoringViolationType violationType;

    @Size(
            max = 500,
            message = "Violation description is too long"
    )
    private String description;
}