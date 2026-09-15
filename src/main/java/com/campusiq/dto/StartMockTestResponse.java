package com.campusiq.dto;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StartMockTestResponse {

    private Long mockTestId;

    private String title;

    private int durationMinutes;

    private int passPercentage;

    private String selectedSkill;

    private List<StudentQuestionResponse> questions;

    // Identifies this student's saved attempt.
    private Long testAttemptId;

    // The service supplies these timestamps from the server.
    private Instant startedAt;

    private Instant expiresAt;

    private Instant serverTime;

    // Preserves the constructor currently used by MockTestServiceImpl.
    public StartMockTestResponse(
            Long mockTestId,
            String title,
            int durationMinutes,
            int passPercentage,
            String selectedSkill,
            List<StudentQuestionResponse> questions) {

        this(mockTestId, title, durationMinutes, passPercentage,
                selectedSkill, questions, null, null, null, null);
    }
}