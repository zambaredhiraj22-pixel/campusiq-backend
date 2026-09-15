package com.campusiq.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "student_mock_test_assignments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_student_mock_test_assignment",
                        columnNames = {
                                "student_profile_id",
                                "mock_test_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_assignment_student",
                        columnList = "student_profile_id"
                ),
                @Index(
                        name = "idx_assignment_mock_test",
                        columnList = "mock_test_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class StudentMockTestAssignment {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "student_profile_id",
            nullable = false
    )
    private StudentProfile studentProfile;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "mock_test_id",
            nullable = false
    )
    private MockTest mockTest;

    @Column(
            name = "assigned_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime assignedAt;

    @Column(
            name = "active",
            nullable = false
    )
    private Boolean active = true;

    /*
     * Number of additional attempts currently allowed
     * by Faculty.
     *
     * 0 = no retake permission
     * 1 = one additional attempt allowed
     *
     * The service consumes one credit when the student
     * starts a retake.
     */
    @Column(
            name = "retake_credits",
            nullable = false
    )
    private Integer retakeCredits = 0;

    /*
     * Stores when Faculty most recently granted
     * retake permission.
     */
    @Column(
            name = "last_retake_granted_at"
    )
    private LocalDateTime lastRetakeGrantedAt;

    /*
     * Stores when the most recent retake permission
     * was consumed by starting a new attempt.
     */
    @Column(
            name = "last_retake_consumed_at"
    )
    private LocalDateTime lastRetakeConsumedAt;

    @PrePersist
    protected void onCreate() {

        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }

        if (active == null) {
            active = true;
        }

        if (retakeCredits == null) {
            retakeCredits = 0;
        }
    }

    public int getAvailableRetakeCredits() {

        return retakeCredits == null
                ? 0
                : Math.max(
                        retakeCredits,
                        0
                );
    }

    public void grantOneRetake() {

        retakeCredits = 1;

        lastRetakeGrantedAt =
                LocalDateTime.now();
    }

    public void consumeOneRetake() {

        if (getAvailableRetakeCredits() <= 0) {

            throw new IllegalStateException(
                    "No retake permission is available"
            );
        }

        retakeCredits =
                getAvailableRetakeCredits() - 1;

        lastRetakeConsumedAt =
                LocalDateTime.now();
    }
}