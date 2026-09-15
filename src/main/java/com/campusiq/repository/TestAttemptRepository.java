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

    /*
     * Returns the student's latest completed attempt.
     */
    Optional<TestAttempt>
            findTopByStudentProfileAndCompletedTrueOrderBySubmittedAtDesc(
                    StudentProfile studentProfile
            );

    /*
     * Checks whether this student has already
     * completed this particular Mock Test.
     *
     * true means that another attempt requires
     * Faculty retake permission.
     */
    boolean
            existsByStudentProfileIdAndMockTestIdAndCompletedTrue(
                    Long studentProfileId,
                    Long mockTestId
            );

    /*
     * Returns how many completed attempts the
     * student has made for this Mock Test.
     */
    long
            countByStudentProfileIdAndMockTestIdAndCompletedTrue(
                    Long studentProfileId,
                    Long mockTestId
            );

    /*
     * Finds an existing incomplete attempt for
     * the same student and Mock Test.
     *
     * This prevents multiple open attempts.
     */
    Optional<TestAttempt>
            findTopByStudentProfileIdAndMockTestIdAndCompletedFalseOrderByStartedAtDesc(
                    Long studentProfileId,
                    Long mockTestId
            );

    /*
     * Locks an attempt before it is updated.
     */
    @Lock(
            LockModeType.PESSIMISTIC_WRITE
    )
    @Query("""
            SELECT attempt
            FROM TestAttempt attempt
            WHERE attempt.id = :testAttemptId
            """)
    Optional<TestAttempt> findByIdForUpdate(
            @Param("testAttemptId")
            Long testAttemptId
    );

    /*
     * Finds expired attempts whose results have
     * not yet been generated.
     *
     * Used by the timeout scheduler.
     */
    @Query("""
            SELECT attempt.id
            FROM TestAttempt attempt
            WHERE attempt.id > :afterId
              AND attempt.expiresAt <= :cutoff
              AND attempt.mockTestResult IS NULL
              AND attempt.totalQuestions > 0
            ORDER BY attempt.id ASC
            """)
    List<Long> findExpiredAttemptIds(
            @Param("cutoff")
            LocalDateTime cutoff,

            @Param("afterId")
            Long afterId,

            Pageable pageable
    );
}