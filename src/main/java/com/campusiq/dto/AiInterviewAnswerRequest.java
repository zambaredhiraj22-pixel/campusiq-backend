package com.campusiq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AiInterviewAnswerRequest {

    @NotNull(message = "Interview question ID is required")
    private Long questionId;

    @NotBlank(message = "Interview answer is required")
    @Size(
            max = 20000,
            message = "Interview answer is too long"
    )
    private String answerText;
}