package com.campusiq.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "test_attempts")
@Getter
@Setter
@NoArgsConstructor
public class TestAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mock_test_id", nullable = false)
    private MockTest mockTest;

    // The service will copy these details when starting a new attempt.
    // Nullable fields keep existing database records readable.
    @Column(name = "selected_skill")
    private String selectedSkill;

    @Column(name = "test_title")
    private String testTitle;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column(name = "pass_percentage")
    private Integer passPercentage;

    private LocalDateTime startedAt;

    // Server deadline; this field alone does not enforce the time limit.
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    private LocalDateTime submittedAt;

    private int warningCount;

    private boolean completed;

    private boolean autoSubmitted;

    // Null until a result has been saved and linked by the service.
    // No cascade: deleting an attempt must not delete its result.
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
            name = "mock_test_result_id",
            unique = true,
            nullable = true
    )
    private MockTestResult mockTestResult;

    @PrePersist
    public void setStartedAt() {

        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }

        if (expiresAt == null
                && durationMinutes != null
                && durationMinutes > 0) {

            expiresAt = startedAt.plusMinutes(durationMinutes);
        }
    }
}





//package com.campusiq.entity;
//
//import java.time.LocalDateTime;
//
//import jakarta.persistence.Entity;
//import jakarta.persistence.FetchType;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.JoinColumn;
//import jakarta.persistence.ManyToOne;
//import jakarta.persistence.PrePersist;
//import jakarta.persistence.Table;
//
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//@Entity
//@Table(name = "test_attempts")
//@Getter
//@Setter
//@NoArgsConstructor
//public class TestAttempt {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "student_profile_id", nullable = false)
//    private StudentProfile studentProfile;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "mock_test_id", nullable = false)
//    private MockTest mockTest;
//
//    private LocalDateTime startedAt;
//
//    private LocalDateTime submittedAt;
//
//    private int warningCount;
//
//    private boolean completed;
//
//    private boolean autoSubmitted;
//
//    @PrePersist
//    public void setStartedAt() {
//
//        if (startedAt == null) {
//            startedAt = LocalDateTime.now();
//        }
//    }
//}