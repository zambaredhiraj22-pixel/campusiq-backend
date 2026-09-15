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
public class MockTestRetakeResponse {

    /*
     * StudentMockTestAssignment ID.
     *
     * Faculty uses this ID while granting
     * a one-time retake permission.
     */
    private Long assignmentId;

    /*
     * Student details shown to Faculty.
     */
    private Long studentProfileId;

    private String studentName;

    private String username;

    /*
     * Personalized AI Mock Test details.
     */
    private Long mockTestId;

    private String mockTestTitle;

    /*
     * Number of completed attempts made by
     * this student for this Mock Test.
     */
    private long completedAttemptCount;

    /*
     * Number of additional attempts currently
     * approved by Faculty.
     *
     * The current design keeps this value
     * between 0 and 1.
     */
    private int retakeCredits;

    /*
     * true when the student can currently
     * start this test.
     */
    private boolean attemptAllowed;

    /*
     * Assignment must be active for the test
     * to remain available to the student.
     */
    private boolean assignmentActive;

    /*
     * Original assignment time.
     */
    private LocalDateTime assignedAt;

    /*
     * Most recent Faculty retake approval time.
     */
    private LocalDateTime lastRetakeGrantedAt;

    /*
     * Time at which the most recent retake
     * permission was consumed.
     */
    private LocalDateTime lastRetakeConsumedAt;

    /*
     * Human-readable status for Faculty UI.
     *
     * Examples:
     *
     * First attempt available
     * Retake permission required
     * One retake approved
     */
    private String status;
}