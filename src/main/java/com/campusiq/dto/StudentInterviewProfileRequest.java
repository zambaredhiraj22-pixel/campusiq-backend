package com.campusiq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentInterviewProfileRequest {

    @Size(max = 255)
    private String resumeFileName;

    @Size(max = 100)
    private String resumeContentType;

    @NotBlank(message = "Resume text is required")
    @Size(max = 100000)
    private String resumeText;

    @Size(max = 50000)
    private String projects;

    @Size(max = 20000)
    private String technologies;
}