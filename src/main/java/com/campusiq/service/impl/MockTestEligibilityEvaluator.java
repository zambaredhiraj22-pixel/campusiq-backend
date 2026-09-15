package com.campusiq.service.impl;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class MockTestEligibilityEvaluator {

    public void evaluate(
            Double studentMockTestScore,
            Double requiredMockTestScore,
            List<String> passedCriteria,
            List<String> failedCriteria) {

        if (requiredMockTestScore == null) {
            return;
        }

        if (studentMockTestScore == null) {

            failedCriteria.add(
                    "Mock test result is not available"
            );

            return;
        }

        if (studentMockTestScore >= requiredMockTestScore) {

            passedCriteria.add(
                    "Mock test score criteria satisfied"
            );

        } else {

            failedCriteria.add(
                    "Mock test score is below company requirement"
            );
        }
    }
}