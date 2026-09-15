package com.campusiq.dto;

import java.time.Instant;

import com.campusiq.enums.AiInterviewStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiInterviewResultResponse {

    private Long interviewSessionId;

    private AiInterviewStatus status;

    private Integer totalQuestions;

    private Integer answeredQuestions;

    private Double technicalScore;

    private Double resumeProjectScore;

    private Double relevanceScore;

    private Double communicationScore;

    private Double totalScore;

    private Double passPercentage;

    private Boolean passed;

    private Boolean autoSubmitted;

    private String overallFeedback;

    private Instant submittedAt;

    private String message;
}