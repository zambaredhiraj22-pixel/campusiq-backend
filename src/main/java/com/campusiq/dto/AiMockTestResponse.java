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
public class AiMockTestResponse {

    /*
     * Created MockTest ID.
     */
    private Long mockTestId;

    /*
     * StudentMockTestAssignment ID.
     */
    private Long assignmentId;

    /*
     * Student for whom this personalized
     * AI mock test was created.
     */
    private Long studentProfileId;

    /*
     * Mock test title.
     */
    private String title;

    /*
     * Faculty-selected verified skills
     * used for technical questions.
     */
    private List<String> selectedSkills;

    /*
     * Fixed AI assessment structure.
     */
    private Integer aptitudeQuestionCount;

    private Integer reasoningQuestionCount;

    private Integer technicalQuestionCount;

    /*
     * Total questions.
     *
     * For our AI mock test:
     * 15 + 15 + 30 = 60
     */
    private Integer totalQuestions;

    /*
     * Faculty-selected test duration.
     */
    private Integer durationMinutes;

    /*
     * CAMPUS-IQ pass percentage.
     *
     * Current requirement = 65%.
     */
    private Integer passPercentage;

    /*
     * Whether the created mock test
     * is currently active.
     */
    private Boolean active;

    /*
     * Time when the test was assigned
     * to the selected student.
     */
    private LocalDateTime assignedAt;
}