package com.campusiq.entity;

import java.time.Instant;

import com.campusiq.enums.AiInterviewStatus;

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
        name = "ai_interview_sessions",

        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ai_interview_mock_result",
                        columnNames = "mock_test_result_id"
                )
        },

        indexes = {
                @Index(
                        name = "idx_ai_interview_student",
                        columnList = "student_profile_id"
                ),

                @Index(
                        name = "idx_ai_interview_status",
                        columnList = "status"
                )
        }
)
public class AiInterviewSession {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;


    /*
     * Student who owns this interview.
     *
     * This is also used for JWT ownership
     * validation.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "student_profile_id",
            nullable = false
    )
    private StudentProfile studentProfile;


    /*
     * Resume / project / technology context
     * used while preparing this interview.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "interview_profile_id",
            nullable = false
    )
    private StudentInterviewProfile interviewProfile;


    /*
     * The passed personalized Mock Test result
     * that unlocked this AI Interview.
     *
     * One passed mock-test result can create
     * only one AI Interview session.
     */
    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "mock_test_result_id",
            nullable = false,
            unique = true
    )
    private MockTestResult mockTestResult;


    /*
     * READY
     * IN_PROGRESS
     * COMPLETED
     * AUTO_SUBMITTED
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private AiInterviewStatus status =
            AiInterviewStatus.READY;


    /*
     * AI generates a larger pool,
     * but actual interview uses 10 questions.
     */
    @Column(
            name = "question_count",
            nullable = false
    )
    private Integer questionCount = 10;


    /*
     * Default interview duration.
     *
     * Server will calculate expiresAt
     * when interview starts.
     */
    @Column(
            name = "duration_minutes",
            nullable = false
    )
    private Integer durationMinutes = 30;


    /*
     * Final interview passing threshold.
     */
    @Column(
            name = "pass_percentage",
            nullable = false
    )
    private Double passPercentage = 65.0;


    /*
     * Proctoring warning counter.
     *
     * 1st violation -> warning 1
     * 2nd violation -> warning 2
     * 3rd violation -> auto-submit
     */
    @Column(
            name = "warning_count",
            nullable = false
    )
    private Integer warningCount = 0;


    @Column(
            name = "auto_submitted",
            nullable = false
    )
    private Boolean autoSubmitted = false;


    /*
     * =========================
     * AI EVALUATION SCORES
     * =========================
     *
     * Technical Knowledge       = 50
     * Resume / Project Depth    = 25
     * Relevance                 = 15
     * Communication             = 10
     *
     * TOTAL                     = 100
     */


    @Column(
            name = "technical_score"
    )
    private Double technicalScore;


    @Column(
            name = "resume_project_score"
    )
    private Double resumeProjectScore;


    @Column(
            name = "relevance_score"
    )
    private Double relevanceScore;


    @Column(
            name = "communication_score"
    )
    private Double communicationScore;


    @Column(
            name = "total_score"
    )
    private Double totalScore;


    /*
     * null -> interview not evaluated yet
     *
     * true  -> PASS
     * false -> FAIL
     */
    @Column(
            name = "passed"
    )
    private Boolean passed;


    /*
     * Final AI-generated evaluation feedback.
     *
     * Example:
     *
     * Strong Java fundamentals.
     * Good explanation of project architecture.
     * Needs improvement in concurrency concepts.
     */
    @Lob
    @Column(
            name = "overall_feedback",
            columnDefinition = "LONGTEXT"
    )
    private String overallFeedback;


    /*
     * =========================
     * SERVER-SIDE TIMER
     * =========================
     */


    @Column(
            name = "started_at"
    )
    private Instant startedAt;


    @Column(
            name = "expires_at"
    )
    private Instant expiresAt;


    @Column(
            name = "submitted_at"
    )
    private Instant submittedAt;


    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;


    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;


    /*
     * Prevents simultaneous requests from
     * accidentally updating the same interview
     * session at the same time.
     *
     * Useful for:
     * - double submit
     * - answer request + timeout
     * - violation + submit race conditions
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

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status =
                    AiInterviewStatus.READY;
        }

        if (questionCount == null) {
            questionCount = 10;
        }

        if (durationMinutes == null) {
            durationMinutes = 30;
        }

        if (passPercentage == null) {
            passPercentage = 65.0;
        }

        if (warningCount == null) {
            warningCount = 0;
        }

        if (autoSubmitted == null) {
            autoSubmitted = false;
        }
    }


    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                Instant.now();
    }
}