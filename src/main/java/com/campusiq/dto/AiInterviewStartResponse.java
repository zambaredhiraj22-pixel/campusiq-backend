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
public class AiInterviewStartResponse {

    private Long interviewSessionId;

    private AiInterviewStatus status;

    private Integer durationMinutes;

    private Double passPercentage;

    private Integer warningCount;

    private Instant startedAt;

    private Instant expiresAt;

    private Instant serverTime;

    private List<AiInterviewQuestionResponse> questions;
}