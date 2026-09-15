package com.campusiq.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AiQuestionBulkApproveRequest {

    @NotNull(message = "Student profile ID is required")
    private Long studentProfileId;

    @NotEmpty(message = "Select at least one verified technical skill")
    @Size(
            max = 30,
            message = "Maximum 30 technical skills can be selected"
    )
    private List<String> selectedSkills;

    @NotEmpty(message = "Approved questions are required")
    @Size(
            min = 60,
            max = 60,
            message = "AI mock test must contain exactly 60 approved questions"
    )
    @Valid
    private List<QuestionRequest> questions;
}