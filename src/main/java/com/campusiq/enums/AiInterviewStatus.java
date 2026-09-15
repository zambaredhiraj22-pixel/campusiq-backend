package com.campusiq.enums;

public enum AiInterviewStatus {

    /*
     * Student is eligible and the interview
     * question set has been prepared,
     * but the interview has not started yet.
     */
    READY,

    /*
     * Interview has started and answers
     * are currently being accepted.
     */
    IN_PROGRESS,

    /*
     * Interview was normally submitted
     * and evaluated.
     */
    COMPLETED,

    /*
     * Interview was automatically submitted
     * because of timeout or proctoring violations.
     */
    AUTO_SUBMITTED
}