package com.campusiq.dto;

import java.time.Instant;
import java.util.List;

import com.campusiq.enums.AiInterviewStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiInterviewPrepareResponse {

    private Long interviewSessionId;

    private Long studentProfileId;

    private Long interviewProfileId;

    private Long qualifyingMockTestResultId;

    private AiInterviewStatus status;

    private List<String> verifiedSkills;

    private Integer generatedQuestionCount;

    private Integer selectedQuestionCount;

    private Integer durationMinutes;

    private Double passPercentage;

    private Instant preparedAt;

    private String message;
}