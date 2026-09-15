package com.campusiq.entity;

import java.time.LocalDateTime;

import com.campusiq.enums.ProctoringViolationType;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "proctoring_violations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProctoringViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_attempt_id", nullable = false)
    private TestAttempt testAttempt;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type", nullable = false)
    private ProctoringViolationType violationType;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "warning_number", nullable = false)
    private int warningNumber;

    @Column(length = 500)
    private String description;

    @PrePersist
    public void setDetectedAt() {
        if (detectedAt == null) {
            detectedAt = LocalDateTime.now();
        }
    }
}
