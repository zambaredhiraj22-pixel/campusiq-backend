package com.campusiq.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitMockTestRequest {

    private Long mockTestId;

    private Long testAttemptId;

    private List<StudentAnswerRequest> answers;
}