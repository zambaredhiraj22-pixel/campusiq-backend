package com.campusiq.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiInterviewAnswerResponse {

    private Long answerId;

    private Long interviewSessionId;

    private Long questionId;

    private String answerText;

    private Boolean evaluated;

    private Instant answeredAt;

    private Instant updatedAt;
}