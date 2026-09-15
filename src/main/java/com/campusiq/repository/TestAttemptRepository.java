package com.campusiq.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campusiq.entity.StudentProfile;
import com.campusiq.entity.TestAttempt;

import jakarta.persistence.LockModeType;

public interface TestAttemptRepository
        extends JpaRepository<TestAttempt, Long> {

    Optional<TestAttempt>
            findTopByStudentProfileAndCompletedTrueOrderBySubmittedAtDesc(
                    StudentProfile studentProfile);

    // Prevent simultaneous updates to the same attempt.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select attempt from TestAttempt attempt "
            + "where attempt.id = :testAttemptId")
    Optional<TestAttempt> findByIdForUpdate(
            @Param("testAttemptId") Long testAttemptId);

    // Find expired attempts whose results are not yet created.
    @Query("select attempt.id from TestAttempt attempt "
            + "where attempt.id > :afterId "
     
            + "and attempt.expiresAt <= :cutoff "
            + "and attempt.mockTestResult is null "
            + "and attempt.totalQuestions > 0 "
            + "order by attempt.id asc")
    List<Long> findExpiredAttemptIds(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("afterId") Long afterId,
            Pageable pageable);
}