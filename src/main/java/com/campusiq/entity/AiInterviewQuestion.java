package com.campusiq.entity;

import java.time.Instant;

import com.campusiq.enums.AiInterviewQuestionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "ai_interview_questions",

        uniqueConstraints = {

                @UniqueConstraint(
                        name = "uk_ai_interview_pool_order",
                        columnNames = {
                                "interview_session_id",
                                "pool_order"
                        }
                ),

                @UniqueConstraint(
                        name = "uk_ai_interview_selected_order",
                        columnNames = {
                                "interview_session_id",
                                "interview_order"
                        }
                )
        },

        indexes = {

                @Index(
                        name = "idx_ai_question_session",
                        columnList = "interview_session_id"
                ),

                @Index(
                        name = "idx_ai_question_type",
                        columnList = "question_type"
                ),

                @Index(
                        name = "idx_ai_question_selected",
                        columnList = "selected_for_interview"
                )
        }
)
public class AiInterviewQuestion {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;


    /*
     * Interview session that owns
     * this generated question.
     *
     * One session will initially have
     * a pool of 25 questions.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "interview_session_id",
            nullable = false
    )
    private AiInterviewSession interviewSession;


    /*
     * TECHNICAL
     * RESUME_PROJECT
     * BEHAVIORAL
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "question_type",
            nullable = false,
            length = 30
    )
    private AiInterviewQuestionType questionType;


    /*
     * Main interview question.
     *
     * Example:
     *
     * Explain how JWT authentication
     * works in your CAMPUS-IQ project.
     */
    @Lob
    @Column(
            name = "question_text",
            nullable = false,
            columnDefinition = "LONGTEXT"
    )
    private String questionText;


    /*
     * Used mainly for TECHNICAL questions.
     *
     * Example:
     * Java
     * Spring Boot
     * MySQL
     *
     * null for questions where no specific
     * technical skill is required.
     */
    @Column(
            name = "technical_skill",
            length = 150
    )
    private String technicalSkill;


    /*
     * AI-generated reference / ideal answer.
     *
     * IMPORTANT:
     *
     * This must NEVER be sent to the student.
     *
     * It will later help the evaluation engine
     * compare the student's response with the
     * expected concepts.
     */
    @Lob
    @Column(
            name = "reference_answer",
            nullable = false,
            columnDefinition = "LONGTEXT"
    )
    private String referenceAnswer;


    /*
     * Important concepts or evaluation guidance
     * for this question.
     *
     * Example:
     *
     * - JWT contains signed claims
     * - server validates signature
     * - stateless authentication
     * - token sent in Authorization header
     *
     * This is internal AI evaluation data.
     * It must not be exposed to the student.
     */
    @Lob
    @Column(
            name = "evaluation_criteria",
            nullable = false,
            columnDefinition = "LONGTEXT"
    )
    private String evaluationCriteria;


    /*
     * Position inside the original
     * generated 25-question pool.
     *
     * Values:
     * 1 ... 25
     */
    @Column(
            name = "pool_order",
            nullable = false
    )
    private Integer poolOrder;


    /*
     * false:
     * question exists only in generated pool
     *
     * true:
     * question has been selected for the
     * actual 10-question interview.
     */
    @Column(
            name = "selected_for_interview",
            nullable = false
    )
    private Boolean selectedForInterview = false;


    /*
     * Actual interview sequence.
     *
     * Values:
     * 1 ... 10
     *
     * null when the question remains only
     * inside the generated pool.
     */
    @Column(
            name = "interview_order"
    )
    private Integer interviewOrder;


    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;


    @PrePersist
    protected void onCreate() {

        createdAt =
                Instant.now();

        if (selectedForInterview == null) {
            selectedForInterview = false;
        }
    }
}