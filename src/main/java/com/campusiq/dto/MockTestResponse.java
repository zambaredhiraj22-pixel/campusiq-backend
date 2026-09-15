package com.campusiq.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
     * Used only for personalized AI tests.
     *
     * Example:
     * Java, Python, Spring Boot
     *
     * Manual test:
     * null
     */
    private List<String> selectedSkills;

    /*
     * StudentMockTestAssignment ID.
     *
     * Personalized test:
     * contains assignment ID
     *
     * Manual test:
     * null
     */
    private Long assignmentId;

    /*
     * Time at which the personalized test
     * was assigned to the student.
     *
     * Manual test:
     * null
     */
    private LocalDateTime assignedAt;

    /*
     * Backward-compatible constructor.
     *
     * Existing Faculty/Manual Mock Test code already
     * creates MockTestResponse using these 8 fields.
     *
     * Keeping this constructor means existing code
     * will NOT break.
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

        /*
         * Existing tests are manual by default.
         */
        this.personalized = false;
        this.selectedSkills = null;
        this.assignmentId = null;
        this.assignedAt = null;
    }
}