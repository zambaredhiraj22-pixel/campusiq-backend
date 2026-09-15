package com.campusiq.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AiQuestionGenerateRequest {

    @NotNull(message = "Student profile ID is required")
    private Long studentProfileId;

    @NotEmpty(message = "Select at least one verified technical skill")
    private List<String> selectedSkills;
}