package com.campusiq.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "ai_interview_answers",

        uniqueConstraints = {

                /*
                 * One question can have only one
                 * final answer record inside a session.
                 */
                @UniqueConstraint(
                        name = "uk_ai_interview_answer_question",
                        columnNames = {
                                "interview_session_id",
                                "interview_question_id"
                        }
                )
        },

        indexes = {

                @Index(
                        name = "idx_ai_answer_session",
                        columnList = "interview_session_id"
                ),

                @Index(
                        name = "idx_ai_answer_question",
                        columnList = "interview_question_id"
                )
        }
)
public class AiInterviewAnswer {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;


    /*
     * Interview session that owns
     * this answer.
     *
     * Used for:
     * - JWT ownership validation
     * - loading all interview answers
     * - final evaluation
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
     * The selected interview question
     * being answered.
     *
     * Only questions where:
     *
     * selectedForInterview = true
     *
     * will be accepted by the service.
     */
    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "interview_question_id",
            nullable = false
    )
    private AiInterviewQuestion interviewQuestion;


    /*
     * Student's actual answer.
     *
     * Initially this can come from:
     * - typed text
     *
     * Later frontend speech-to-text can
     * send the transcript into this same field.
     */
    @Lob
    @Column(
            name = "answer_text",
            nullable = false,
            columnDefinition = "LONGTEXT"
    )
    private String answerText;


    /*
     * ==========================
     * PER-QUESTION AI EVALUATION
     * ==========================
     *
     * All scores are stored on a
     * 0 to 100 scale.
     *
     * Final weighted interview score
     * will be calculated separately
     * at session level.
     */


    /*
     * Measures correctness / depth /
     * understanding of the answer.
     *
     * For TECHNICAL:
     * technical knowledge.
     *
     * For RESUME_PROJECT:
     * project/resume understanding.
     *
     * For BEHAVIORAL:
     * quality and completeness
     * of the response.
     */
    @Column(
            name = "content_score"
    )
    private Double contentScore;


    /*
     * Measures how directly the student
     * answered the asked question.
     */
    @Column(
            name = "relevance_score"
    )
    private Double relevanceScore;


    /*
     * Measures clarity, structure and
     * communication quality.
     *
     * This works for both:
     * - typed answer
     * - speech transcript
     */
    @Column(
            name = "communication_score"
    )
    private Double communicationScore;


    /*
     * General per-question score.
     *
     * Useful for:
     * - interview report
     * - weakest question identification
     * - frontend result breakdown
     */
    @Column(
            name = "overall_score"
    )
    private Double overallScore;


    /*
     * AI-generated feedback for
     * this particular answer.
     *
     * Example:
     *
     * "Good explanation of JWT,
     * but you should mention token
     * signature verification and
     * stateless authentication."
     */
    @Lob
    @Column(
            name = "ai_feedback",
            columnDefinition = "LONGTEXT"
    )
    private String aiFeedback;


    /*
     * false:
     * answer saved but not yet evaluated.
     *
     * true:
     * AI evaluation completed.
     */
    @Column(
            name = "evaluated",
            nullable = false
    )
    private Boolean evaluated = false;


    /*
     * First time this answer record
     * was created.
     */
    @Column(
            name = "answered_at",
            nullable = false,
            updatable = false
    )
    private Instant answeredAt;


    /*
     * Updated whenever student changes
     * the answer before submission.
     */
    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;


    /*
     * Set only after AI evaluation.
     */
    @Column(
            name = "evaluated_at"
    )
    private Instant evaluatedAt;


    /*
     * Protects answer autosave/update
     * from simultaneous requests.
     *
     * Example:
     * frontend autosave and manual save
     * arriving almost together.
     */
    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;


    @PrePersist
    protected void onCreate() {

        Instant now =
                Instant.now();

        answeredAt = now;
        updatedAt = now;

        if (evaluated == null) {
            evaluated = false;
        }
    }


    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                Instant.now();
    }
}