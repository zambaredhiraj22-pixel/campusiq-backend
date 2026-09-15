package com.campusiq.service;

import java.util.List;

import com.campusiq.dto.MockTestRequest;
import com.campusiq.dto.MockTestResponse;
import com.campusiq.dto.MockTestRetakeResponse;
import com.campusiq.dto.StartMockTestRequest;
import com.campusiq.dto.StartMockTestResponse;
import com.campusiq.dto.StudentAnswerRequest;
import com.campusiq.dto.StudentQuestionResponse;
import com.campusiq.dto.SubmitMockTestRequest;
import com.campusiq.dto.SubmitMockTestResponse;

public interface MockTestService {

    MockTestResponse createMockTest(MockTestRequest request);

    List<MockTestResponse> getAllMockTests();

    List<MockTestResponse> getAvailableMockTests(String username);

    StartMockTestResponse startMockTest(
            String username,
            StartMockTestRequest request
    );

    StudentQuestionResponse saveAnswer(
            String username,
            Long testAttemptId,
            StudentAnswerRequest request
    );

    StartMockTestResponse getAttempt(
            String username,
            Long testAttemptId
    );

    SubmitMockTestResponse submitMockTest(
            String username,
            SubmitMockTestRequest request
    );

    SubmitMockTestResponse getResult(
            String username,
            Long testAttemptId
    );

    boolean finalizeExpiredAttempt(Long testAttemptId);

    List<MockTestRetakeResponse> getRetakeStatuses();

    MockTestRetakeResponse allowRetake(Long assignmentId);
}