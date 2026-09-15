package com.campusiq.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campusiq.entity.StudentMockTestAssignment;

import jakarta.persistence.LockModeType;

public interface StudentMockTestAssignmentRepository
        extends JpaRepository<
                StudentMockTestAssignment,
                Long> {

    /*
     * Returns all active personalized mock tests
     * assigned to a particular student.
     *
     * Latest assignment appears first.
     */
    @EntityGraph(
            attributePaths = {
                    "mockTest"
            }
    )
    List<StudentMockTestAssignment>
            findByStudentProfileIdAndActiveTrueOrderByAssignedAtDesc(
                    Long studentProfileId
            );

    /*
     * Verifies that a particular active personalized
     * test is assigned to a particular student.
     */
    @EntityGraph(
            attributePaths = {
                    "mockTest"
            }
    )
    Optional<StudentMockTestAssignment>
            findByStudentProfileIdAndMockTestIdAndActiveTrue(
                    Long studentProfileId,
                    Long mockTestId
            );

    /*
     * Quick assignment existence check.
     */
    boolean
            existsByStudentProfileIdAndMockTestIdAndActiveTrue(
                    Long studentProfileId,
                    Long mockTestId
            );

    /*
     * Returns all student assignments for one test.
     *
     * This method is useful for the Faculty screen.
     */
    @EntityGraph(
            attributePaths = {
                    "studentProfile",
                    "studentProfile.user",
                    "mockTest"
            }
    )
    List<StudentMockTestAssignment>
            findByMockTestIdOrderByAssignedAtDesc(
                    Long mockTestId
            );

    /*
     * Finds an assignment even when it is inactive.
     *
     * A database unique constraint prevents duplicate
     * student + mock-test assignment rows.
     */
    @EntityGraph(
            attributePaths = {
                    "studentProfile",
                    "mockTest"
            }
    )
    Optional<StudentMockTestAssignment>
            findByStudentProfileIdAndMockTestId(
                    Long studentProfileId,
                    Long mockTestId
            );

    /*
     * Locks the assignment row while a student starts
     * a personalized test.
     *
     * This prevents two browser tabs from consuming
     * the same retake permission simultaneously.
     */
    @Lock(
            LockModeType.PESSIMISTIC_WRITE
    )
    @Query("""
            SELECT assignment
            FROM StudentMockTestAssignment assignment
            JOIN FETCH assignment.studentProfile student
            JOIN FETCH assignment.mockTest mockTest
            WHERE student.id = :studentProfileId
              AND mockTest.id = :mockTestId
              AND assignment.active = true
            """)
    Optional<StudentMockTestAssignment>
            findActiveAssignmentForUpdate(
                    @Param("studentProfileId")
                    Long studentProfileId,

                    @Param("mockTestId")
                    Long mockTestId
            );

    /*
     * Locks the assignment row while Faculty grants
     * a retake.
     *
     * The assignment ID comes from Faculty UI/API.
     */
    @Lock(
            LockModeType.PESSIMISTIC_WRITE
    )
    @Query("""
            SELECT assignment
            FROM StudentMockTestAssignment assignment
            JOIN FETCH assignment.studentProfile student
            JOIN FETCH student.user
            JOIN FETCH assignment.mockTest mockTest
            WHERE assignment.id = :assignmentId
            """)
    Optional<StudentMockTestAssignment>
            findByIdForUpdate(
                    @Param("assignmentId")
                    Long assignmentId
            );
}