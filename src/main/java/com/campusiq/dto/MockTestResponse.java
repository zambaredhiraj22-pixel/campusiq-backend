package com.campusiq.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MockTestResponse {

    private Long id;

    private String title;

    private int aptitudeQuestionCount;

    private int reasoningQuestionCount;

    private int technicalQuestionCount;

    private int durationMinutes;

    private int passPercentage;

    private boolean active;

    /*
     * false = Existing Manual Mock Test
     * true  = Student-specific AI Personalized Mock Test
     */
    private boolean personalized;

    /*
     * Technical skills selected by Faculty for
     * this personalized AI Mock Test.
     *
     * Manual test:
     * null
     */
    private List<String> selectedSkills;

    /*
     * StudentMockTestAssignment ID.
     *
     * Personalized test:
     * contains assignment ID.
     *
     * Manual test:
     * null.
     */
    private Long assignmentId;

    /*
     * Time at which the personalized test
     * was assigned to the student.
     *
     * Manual test:
     * null.
     */
    private LocalDateTime assignedAt;

    /*
     * Number of completed attempts made by the
     * currently logged-in student for this test.
     *
     * 0 = first attempt is still available.
     * 1 or more = Faculty retake permission required.
     */
    private long completedAttemptCount;

    /*
     * Number of additional attempts currently
     * granted by Faculty.
     *
     * Normally:
     * 0 = no retake permission.
     * 1 = one retake is available.
     */
    private int retakeCredits;

    /*
     * true when the student may currently start
     * this Mock Test.
     *
     * First attempt:
     * true.
     *
     * Completed test without permission:
     * false.
     *
     * Completed test with Faculty permission:
     * true.
     */
    private boolean attemptAllowed;

    /*
     * Human-readable attempt/retake status for
     * the frontend.
     */
    private String attemptStatus;

    /*
     * Constructor used by existing Manual and
     * Faculty Mock Test mapping code.
     *
     * New retake-related fields receive safe defaults.
     */
    public MockTestResponse(
            Long id,
            String title,
            int aptitudeQuestionCount,
            int reasoningQuestionCount,
            int technicalQuestionCount,
            int durationMinutes,
            int passPercentage,
            boolean active) {

        this.id = id;

        this.title = title;

        this.aptitudeQuestionCount =
                aptitudeQuestionCount;

        this.reasoningQuestionCount =
                reasoningQuestionCount;

        this.technicalQuestionCount =
                technicalQuestionCount;

        this.durationMinutes =
                durationMinutes;

        this.passPercentage =
                passPercentage;

        this.active =
                active;

        this.personalized = false;

        this.selectedSkills = null;

        this.assignmentId = null;

        this.assignedAt = null;

        this.completedAttemptCount = 0L;

        this.retakeCredits = 0;

        this.attemptAllowed = active;

        this.attemptStatus =
                active
                        ? "First attempt available"
                        : "Mock test is inactive";
    }
}