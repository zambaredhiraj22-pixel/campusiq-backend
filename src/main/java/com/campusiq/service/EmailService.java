package com.campusiq.service;

public interface EmailService {

    /*
     * Sent when a student passes a mock test.
     */
    void sendMockTestPassedEmail(
            String toEmail,
            String studentName,
            double percentage
    );

    /*
     * Sent only after the complete placement pipeline succeeds:
     *
     * Personalized Mock Test PASS
     *          +
     * AI Interview PASS
     *          ↓
     * placementReady = true
     */
    void sendPlacementReadyEmail(
            String toEmail,
            String studentName,
            double mockTestPercentage,
            double interviewScore
    );
}