package com.campusiq.service;

import com.campusiq.dto.AiMockTestCreateRequest;
import com.campusiq.dto.AiMockTestResponse;

public interface AiMockTestService {

    /*
     * Creates one personalized AI mock test
     * for the selected student.
     *
     * The implementation will:
     *
     * 1. Validate student.
     * 2. Validate faculty-verified selected skills.
     * 3. Validate exactly 60 unique approved questions.
     * 4. Enforce:
     *      15 Aptitude
     *      15 Reasoning
     *      30 Technical
     * 5. Validate balanced technical-skill distribution.
     * 6. Create MockTest with:
     *      pass percentage = 65
     *      active = true
     * 7. Map the exact 60 questions using MockTestQuestion.
     * 8. Assign the test to the selected student.
     * 9. Perform everything inside one transaction.
     *
     * No random question selection is used
     * for personalized AI mock tests.
     */
    AiMockTestResponse createPersonalizedMockTest(
            AiMockTestCreateRequest request
    );
}