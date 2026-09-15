package com.campusiq.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitMockTestResponse {

    private Long mockTestId;

    private int totalQuestions;

    private int correctAnswers;

    private double percentage;

    private int passPercentage;

    private boolean passed;

    private String message;

    private Long testAttemptId;

    private Long resultId;

    private boolean autoSubmitted;

    // Preserves the constructor currently used by MockTestServiceImpl.
    public SubmitMockTestResponse(
            Long mockTestId,
            int totalQuestions,
            int correctAnswers,
            double percentage,
            int passPercentage,
            boolean passed,
            String message) {

        this(mockTestId, totalQuestions, correctAnswers, percentage,
                passPercentage, passed, message, null, null, false);
    }
}