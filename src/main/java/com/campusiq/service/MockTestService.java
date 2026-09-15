package com.campusiq.service;

import java.util.List;

import com.campusiq.dto.MockTestRequest;
import com.campusiq.dto.MockTestResponse;
import com.campusiq.dto.StartMockTestRequest;
import com.campusiq.dto.StartMockTestResponse;
import com.campusiq.dto.StudentAnswerRequest;
import com.campusiq.dto.StudentQuestionResponse;
import com.campusiq.dto.SubmitMockTestRequest;
import com.campusiq.dto.SubmitMockTestResponse;

public interface MockTestService {

    /*
     * Faculty creates existing/manual mock test.
     */
    MockTestResponse createMockTest(
            MockTestRequest request
    );

    /*
     * Faculty can view all Manual + AI Mock Tests.
     */
    List<MockTestResponse> getAllMockTests();

    /*
     * Student gets available tests.
     *
     * Manual tests:
     * visible normally.
     *
     * AI Personalized tests:
     * visible only when assigned to that student.
     */
    List<MockTestResponse> getAvailableMockTests(
            String username
    );

    /*
     * Starts either:
     *
     * Manual Mock Test
     * OR
     * Personalized AI Mock Test.
     */
    StartMockTestResponse startMockTest(
            String username,
            StartMockTestRequest request
    );

    /*
     * Save one answer during test.
     */
    StudentQuestionResponse saveAnswer(
            String username,
            Long testAttemptId,
            StudentAnswerRequest request
    );

    /*
     * Restore/reload an existing open attempt.
     */
    StartMockTestResponse getAttempt(
            String username,
            Long testAttemptId
    );

    /*
     * Submit test manually.
     */
    SubmitMockTestResponse submitMockTest(
            String username,
            SubmitMockTestRequest request
    );

    /*
     * Get completed/auto-submitted result.
     */
    SubmitMockTestResponse getResult(
            String username,
            Long testAttemptId
    );

    /*
     * Used internally by scheduler/backend
     * to automatically finalize expired attempts.
     */
    boolean finalizeExpiredAttempt(
            Long testAttemptId
    );
}