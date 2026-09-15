package com.campusiq.entity;

import com.campusiq.enums.QuestionCategory;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "test_attempt_questions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attempt_question_source",
                        columnNames = {"test_attempt_id", "question_id"}
                ),
                @UniqueConstraint(
                        name = "uk_attempt_question_number",
                        columnNames = {"test_attempt_id", "question_number"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TestAttemptQuestion {

    // ID of this saved snapshot, not the original question-bank ID.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "test_attempt_id",
            nullable = false,
            updatable = false
    )
    private TestAttempt testAttempt;

    // Original question-bank ID used by StudentAnswerRequest.questionId.
    // Stored as a value so question-bank deletion does not delete history.
    @Column(name = "question_id", nullable = false, updatable = false)
    private Long questionId;

    // Position shown to the student: 1, 2, 3, ...
    @Column(name = "question_number", nullable = false, updatable = false)
    private int questionNumber;

    // Copy question content when the attempt starts.
    @Column(
            name = "question_text",
            nullable = false,
            length = 500,
            updatable = false
    )
    private String questionText;

    @Column(name = "option_a", nullable = false, updatable = false)
    private String optionA;

    @Column(name = "option_b", nullable = false, updatable = false)
    private String optionB;

    @Column(name = "option_c", nullable = false, updatable = false)
    private String optionC;

    @Column(name = "option_d", nullable = false, updatable = false)
    private String optionD;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, updatable = false)
    private QuestionCategory category;

    @Column(name = "technical_skill", updatable = false)
    private String technicalSkill;

    // Server-only answer key copied at the start of the attempt.
    @JsonIgnore
    @Column(
            name = "correct_option",
            nullable = false,
            length = 1,
            updatable = false
    )
    private String correctOption;

    // A, B, C or D; null means the student has not answered.
    // The service must validate and save accepted answer changes.
    @Column(name = "selected_option", length = 1)
    private String selectedOption;
}