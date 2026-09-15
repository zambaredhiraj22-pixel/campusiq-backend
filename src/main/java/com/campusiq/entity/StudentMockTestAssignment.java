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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Student for whom this mock test is assigned.
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
     * Personalized mock test assigned to the student.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "mock_test_id",
            nullable = false
    )
    private MockTest mockTest;

    /*
     * Date and time when faculty assigned the test.
     */
    @Column(
            name = "assigned_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime assignedAt;

    /*
     * Allows faculty/backend to enable or disable
     * this assignment without deleting it.
     */
    @Column(
            name = "active",
            nullable = false
    )
    private Boolean active = true;

    @PrePersist
    protected void onCreate() {

        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }

        if (active == null) {
            active = true;
        }
    }
}