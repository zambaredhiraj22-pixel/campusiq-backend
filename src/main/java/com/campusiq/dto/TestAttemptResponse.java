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
public class TestAttemptResponse {

    private Long testAttemptId;

    private Long mockTestId;

    private LocalDateTime startedAt;

    private int warningCount;

    private boolean completed;

    private boolean autoSubmitted;

    private String message;
}
