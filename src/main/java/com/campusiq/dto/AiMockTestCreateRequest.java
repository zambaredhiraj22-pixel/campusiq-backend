package com.campusiq.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AiMockTestCreateRequest {

    /*
     * Student for whom the personalized
     * AI mock test is being created.
     */
    @NotNull(message = "Student profile ID is required")
    private Long studentProfileId;

    /*
     * Faculty-defined test title.
     *
     * Example:
     * Dhiraj - Java Python Spring Boot Assessment
     */
    @NotBlank(message = "Mock test title is required")
    private String title;

    /*
     * Faculty-selected VERIFIED technical skills
     * used while generating the AI assessment.
     */
    @NotEmpty(message = "Select at least one verified technical skill")
    @Size(
            max = 30,
            message = "Maximum 30 technical skills can be selected"
    )
    private List<@NotBlank String> selectedSkills;

    /*
     * Exact 60 Faculty-approved Question IDs.
     *
     * These questions must already exist
     * in the official Question Bank.
     */
    @NotEmpty(message = "Approved question IDs are required")
    @Size(
            min = 60,
            max = 60,
            message = "AI mock test must contain exactly 60 questions"
    )
    private List<@NotNull Long> questionIds;

    /*
     * Faculty can configure test duration.
     *
     * Minimum 1 minute.
     * Maximum 180 minutes.
     */
    @NotNull(message = "Test duration is required")
    @Min(
            value = 1,
            message = "Test duration must be at least 1 minute"
    )
    @Max(
            value = 180,
            message = "Test duration cannot exceed 180 minutes"
    )
    private Integer durationMinutes;
}