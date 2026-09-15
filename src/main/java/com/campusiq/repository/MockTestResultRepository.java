package com.campusiq.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campusiq.entity.MockTestResult;
import com.campusiq.entity.StudentProfile;

public interface MockTestResultRepository
        extends JpaRepository<MockTestResult, Long> {

    Optional<MockTestResult>
            findTopByStudentProfileOrderByAttemptedAtDesc(
                    StudentProfile studentProfile
            );

    @Query("""
            SELECT r
            FROM MockTestResult r
            WHERE r.studentProfile.id = :studentProfileId
              AND r.passed = true
              AND EXISTS (
                    SELECT a.id
                    FROM StudentMockTestAssignment a
                    WHERE a.studentProfile.id = r.studentProfile.id
                      AND a.mockTest.id = r.mockTest.id
                      AND a.active = true
              )
            ORDER BY r.attemptedAt DESC
            """)
    List<MockTestResult>
            findPassedPersonalizedResults(
                    @Param("studentProfileId")
                    Long studentProfileId
            );
}