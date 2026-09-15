package com.campusiq.service;

import com.campusiq.dto.ProctoringViolationRequest;
import com.campusiq.dto.ProctoringViolationResponse;
import com.campusiq.dto.TestAttemptRequest;
import com.campusiq.dto.TestAttemptResponse;

public interface ProctoringService {

    TestAttemptResponse startTest(
            TestAttemptRequest request,
            String username
    );

    ProctoringViolationResponse recordViolation(
            ProctoringViolationRequest request,
            String username
    );

    TestAttemptResponse getTestAttemptStatus(
            Long testAttemptId,
            String username
    );
}