package com.campusiq.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MockTestRequest {

    private String title;

    private int aptitudeQuestionCount;

    private int reasoningQuestionCount;

    private int technicalQuestionCount;

    private int durationMinutes;

    private boolean active;
}