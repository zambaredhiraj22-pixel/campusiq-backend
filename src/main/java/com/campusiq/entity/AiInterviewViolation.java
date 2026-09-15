package com.campusiq.entity;

import java.time.Instant;

import com.campusiq.enums.ProctoringViolationType;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "ai_interview_violations",
        indexes = {
                @Index(
                        name = "idx_ai_interview_violation_session",
                        columnList = "interview_session_id"
                )
        }
)
public class AiInterviewViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "interview_session_id",
            nullable = false
    )
    private AiInterviewSession interviewSession;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "violation_type",
            nullable = false,
            length = 50
    )
    private ProctoringViolationType violationType;

    @Column(
            name = "warning_number",
            nullable = false
    )
    private Integer warningNumber;

    @Column(
            name = "description",
            length = 500
    )
    private String description;

    @Column(
            name = "detected_at",
            nullable = false
    )
    private Instant detectedAt;

    @PrePersist
    protected void onCreate() {
        if (detectedAt == null) {
            detectedAt = Instant.now();
        }
    }
}