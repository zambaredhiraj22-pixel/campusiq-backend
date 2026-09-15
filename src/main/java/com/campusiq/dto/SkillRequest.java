package com.campusiq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SkillRequest {

    @NotBlank(message = "Skill name is required")
    @Size(max = 100, message = "Skill name cannot exceed 100 characters")
    private String skillName;

    @NotBlank(message = "Proficiency level is required")
    @Pattern(
        regexp = "(?i)BEGINNER|INTERMEDIATE|ADVANCED",
        message = "Level must be BEGINNER, INTERMEDIATE or ADVANCED"
    )
    private String proficiencyLevel;

    @Size(max = 500, message = "Evidence URL cannot exceed 500 characters")
    private String evidenceUrl;
}