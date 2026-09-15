package com.campusiq.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentInterviewProfileResponse {

    private Long id;

    private Long studentProfileId;

    private String resumeFileName;

    private String resumeContentType;

    private String resumeText;

    private String projects;

    private String technologies;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}